/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.hive.store;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.avro.Schema.Field;
import org.apache.gora.hive.query.HiveQuery;
import org.apache.gora.hive.query.HiveResult;
import org.apache.gora.hive.util.HiveQueryBuilder;
import org.apache.gora.hive.util.HiveResultParser;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;
import org.apache.metamodel.delete.DeleteFrom;
import org.apache.metamodel.drop.DropTable;
import org.apache.metamodel.query.SelectItem;
import org.apache.metamodel.query.builder.SatisfiedSelectBuilder;
import org.apache.metamodel.query.builder.SatisfiedWhereBuilder;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Hive data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class HiveStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(HiveStore.class);

  private static final String PARSE_MAPPING_FILE_KEY = "gora.hive.mapping.file";

  private static final String DEFAULT_MAPPING_FILE = "gora-hive-mapping.xml";


  private volatile HiveDataContext dataContext;
  private HiveStoreParameters hiveStoreParameters;
  private HiveMapping mapping;
  private Table schemaTable;
  private HiveQueryBuilder queryBuilder;
  private HiveResultParser resultParser;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties)
      throws GoraException {
    LOG.debug("Initializing Hive store");
    super.initialize(keyClass, persistentClass, properties);
    hiveStoreParameters = HiveStoreParameters.load(properties);
    mapping = new HiveMappingBuilder<Object, Object>(this)
        .readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
    try {
      dataContext = new HiveDataContext(hiveStoreParameters);
    } catch (Exception e) {
      LOG.error("Data context creation failed", e);
      throw new GoraException(e);
    }
    queryBuilder = new HiveQueryBuilder(this, mapping);
    resultParser = new HiveResultParser(this);
  }

  @Override
  public String getSchemaName() {
    return getSchemaName(mapping.getTableName(), persistentClass);
  }

  @Override
  protected String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  @Override
  public void createSchema() throws GoraException {
    if (!schemaExists()) {
      LOG.info("Creating hive schema {}", getSchemaName());
      String hiveQuery = queryBuilder.buildCreateQuery(hiveStoreParameters.getDatabaseName(),
          fieldMap);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Hive schema creation query : {}", hiveQuery);
      }
      dataContext.executeHiveQL(hiveQuery);
      dataContext.refreshSchemas();
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    if (schemaExists()) {
      LOG.info("Deleting hive schema {}", getSchemaName());
      DropTable dropTable = new DropTable(getSchemaTable());
      dataContext.executeUpdate(dropTable);
      dataContext.refreshSchemas();
      schemaTable = null;
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    Table table = getSchemaTable();
    return (table != null);
  }

  @Override
  public boolean exists(K key) throws GoraException {
    DataSet dataSet = executeGetQuery(key, new String[]{HiveMapping.DEFAULT_KEY_NAME});
    return (dataSet != null && dataSet.next());
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    if (fields == null || fields.length == 0) {
      fields = getFields();
    }
    DataSet dataSet = executeGetQuery(key, fields);
    return newInstance(dataSet);
  }

  /**
   * Put an persistent object into the hive store
   * Though we use a key value, currently hive does not validate integrity constraints and
   * Hive jdbc driver doesn't support update queries on a particular key
   * @param key the key of the object.
   * @param obj the Persistent object.
   * @throws GoraException throws if putting object is failed
   */
  @Override
  public void put(K key, T obj) throws GoraException {
    try {
      List<Object> parementerList = new ArrayList<>();
      String sql = queryBuilder.buildInsertQuery(key, obj, fieldMap, parementerList);
      PreparedStatement statement = dataContext.getPreparedStatement(sql);
      for (int i = 0; i < parementerList.size(); i++) {
        statement.setObject(i + 1, parementerList.get(i));
      }
      statement.execute();
    } catch (IOException | SQLException e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    Table table = getSchemaTable();

    DeleteFrom delete = new DeleteFrom(table).where(HiveMapping.DEFAULT_KEY_NAME).eq(key);
    dataContext.executeUpdate(delete);
    return true;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    throw new UnsupportedOperationException(
        "Currently hive jdbc driver doesn't support delete query");
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    String[] fields = query.getFields();
    SatisfiedSelectBuilder<?> builder;
    if (fields == null || fields.length == 0) {
      builder = dataContext.query().from(getSchemaTable()).selectAll();
    } else {
      int fieldLength = fields.length;
      fields = Arrays.copyOf(fields, fieldLength + 1);
      fields[fieldLength] = HiveMapping.DEFAULT_KEY_NAME;
      builder = dataContext.query().from(getSchemaTable()).select(fields);
    }
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    if (startKey != null && startKey.equals(endKey)) {
      builder.where(HiveMapping.DEFAULT_KEY_NAME).eq(startKey);
    } else {
      if (startKey != null) {
        builder.where(HiveMapping.DEFAULT_KEY_NAME).greaterThanOrEquals(startKey);
      }

      if (endKey != null) {
        builder.where(HiveMapping.DEFAULT_KEY_NAME).lessThanOrEquals(endKey);
      }
    }
    if (query.getLimit() > 0) {
      builder.limit(((Long) query.getLimit()).intValue());
    }
    return new HiveResult<>(this, query, dataContext.executeQuery(builder.toQuery()));
  }

  @Override
  public Query<K, T> newQuery() {
    return new HiveQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
        query);
    partitionQuery.setConf(getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  @Override
  public void flush() {
    dataContext.refreshSchemas();
  }

  @Override
  public void close() {
    dataContext.close();
  }

  @Override
  protected String[] getFields() {
    List<String> fields = mapping.getFields();
    return fields.toArray(new String[0]);
  }

  /**
   * Creates a new Persistent instance with the values in 'dataSet' for the fields selected in the
   * query
   *
   * @param dataSet result data set from a hive query
   * @return a persistence class instance which content was deserialized
   */
  private T newInstance(DataSet dataSet) throws GoraException {
    if (dataSet == null) {
      return null;
    }
    dataSet.next();
    return readObject(dataSet.getRow());
  }

  /**
   * Return database schemaTable for a given schema name
   *
   * @return org.apache.metamodel.schema.Table schemaTable
   */
  private Table getSchemaTable() throws GoraException {
    if (schemaTable != null && schemaTable.getName().equalsIgnoreCase(getSchemaName())) {
      return schemaTable;
    }
    org.apache.metamodel.schema.Schema schema = dataContext
        .getSchemaByName(hiveStoreParameters.getDatabaseName());
    if (schema == null) {
      throw new GoraException(
          "Could not find database for name : " + hiveStoreParameters.getDatabaseName());
    }
    schemaTable = schema.getTableByName(getSchemaName());
    return schemaTable;
  }

  /**
   * Read a resulted row object and parse it to a persistent object
   *
   * @param row {@link org.apache.metamodel.data.Row} Resulted row object
   * @return Persistent object
   * @throws GoraException throws if reading of data is failed
   */
  public T readObject(Row row) throws GoraException {
    if (row == null || row.size() == 0) {
      LOG.error("Data set is empty");
      return null;
    }
    T persistent = newPersistent();
    List<SelectItem> selectItems = row.getSelectItems();
    for (SelectItem selectItem : selectItems) {
      Column column = selectItem.getColumn();
      if (HiveMapping.DEFAULT_KEY_NAME.equalsIgnoreCase(column.getName())) {
        continue;
      }
      Field field = fieldMap.get(mapping.getColumnFieldMap().get(column.getName()));
      Object value = row.getValue(column);
      if (value != null) {
        persistent.put(field.name(), resultParser.parseSchema(field.schema(), value));
        persistent.isDirty(field.name());
      }
    }
    persistent.clearDirty();
    return persistent;
  }

  /**
   * Read the key value from a given data row
   * @param row Resulted data row
   * @return value of the key field
   * @throws GoraException throws if reading of the key value is failed
   */
  @SuppressWarnings("unchecked")
  public K readKey(Row row) throws GoraException{
    if (row == null || row.size() == 0) {
      LOG.error("Data set is empty");
      return null;
    }
    Column keyColumn = getSchemaTable().getColumnByName(HiveMapping.DEFAULT_KEY_NAME);
    return (K)row.getValue(keyColumn);
  }

  /**
   * Execute a select query based on key value
   * @param key key value
   * @param fields filed names to be selected
   * @return Resulted data set
   * @throws GoraException throws if selection query is failed
   */
  private DataSet executeGetQuery(K key, String[] fields) throws GoraException {
    Table table = getSchemaTable();
    SatisfiedWhereBuilder<?> query = dataContext.query().from(table).select(fields)
        .where(HiveMapping.DEFAULT_KEY_NAME).eq(key);
    String sql = query.toQuery().toSql();
    return dataContext.executeQuery(sql);
  }
}

