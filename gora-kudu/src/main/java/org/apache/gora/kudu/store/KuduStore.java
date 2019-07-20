/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.kudu.store;

import org.apache.gora.kudu.query.KuduResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javafx.util.Pair;
import org.apache.gora.kudu.mapping.Column;
import org.apache.gora.kudu.mapping.KuduMapping;
import org.apache.gora.kudu.mapping.KuduMappingBuilder;
import org.apache.gora.kudu.utils.KuduParameters;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.kudu.ColumnSchema;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.kudu.query.KuduQuery;
import org.apache.gora.kudu.utils.KuduClientUtils;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.util.IOUtils;
import org.apache.kudu.Type;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.Delete;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduPredicate;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.OperationResponse;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.RowResult;
import org.apache.kudu.client.RowResultIterator;
import org.apache.kudu.client.SessionConfiguration;
import org.apache.kudu.client.Update;
import org.apache.kudu.client.Upsert;
import org.apache.kudu.util.DecimalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Apache Kudu data store to be used by Apache Gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class KuduStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(KuduStore.class);
  private static final String PARSE_MAPPING_FILE_KEY = "gora.kudu.mapping.file";
  private static final String DEFAULT_MAPPING_FILE = "gora-kudu-mapping.xml";
  private static final String XML_MAPPING_DEFINITION = "gora.mapping";
  private KuduParameters kuduParameters;
  private KuduMapping kuduMapping;
  private KuduClient client;
  private KuduSession session;
  private KuduTable table;

  private static final ConcurrentHashMap<Schema, SpecificDatumReader<?>> readerMap = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Schema, SpecificDatumWriter<?>> writerMap = new ConcurrentHashMap<>();

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);
      KuduMappingBuilder<K, T> builder = new KuduMappingBuilder<K, T>(this);
      InputStream mappingStream;
      if (properties.containsKey(XML_MAPPING_DEFINITION)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace(XML_MAPPING_DEFINITION + " = " + properties.getProperty(XML_MAPPING_DEFINITION));
        }
        mappingStream = org.apache.commons.io.IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
      } else {
        mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      }
      builder.readMappingFile(mappingStream);
      kuduMapping = builder.getKuduMapping();
      kuduParameters = KuduParameters.load(properties, getConf());
      KuduClient.KuduClientBuilder kuduClientBuilder = new KuduClient.KuduClientBuilder(kuduParameters.getMasterAddresses());
      if (kuduParameters.getBossCount() != null) {
        kuduClientBuilder.bossCount(kuduParameters.getBossCount());
      }
      if (kuduParameters.getDefaultAdminOperationTimeoutMs() != null) {
        kuduClientBuilder.defaultAdminOperationTimeoutMs(kuduParameters.getDefaultAdminOperationTimeoutMs());
      }
      if (kuduParameters.getDefaultOperationTimeoutMs() != null) {
        kuduClientBuilder.defaultOperationTimeoutMs(kuduParameters.getDefaultOperationTimeoutMs());
      }
      if (kuduParameters.getDefaultSocketReadTimeoutMs() != null) {
        kuduClientBuilder.defaultSocketReadTimeoutMs(kuduParameters.getDefaultSocketReadTimeoutMs());
      }
      if (kuduParameters.getWorkerCount() != null) {
        kuduClientBuilder.workerCount(kuduParameters.getWorkerCount());
      }
      if (kuduParameters.isClientStatistics() != null && !kuduParameters.isClientStatistics()) {
        kuduClientBuilder.disableStatistics();
      }
      client = kuduClientBuilder.build();
      session = client.newSession();
      if (kuduParameters.getFlushMode() != null) {
        session.setFlushMode(SessionConfiguration.FlushMode.valueOf(kuduParameters.getFlushMode()));
      }
      if (kuduParameters.getFlushInterval() != null) {
        session.setFlushInterval(kuduParameters.getFlushInterval());
      }

      LOG.info("Kudu store was successfully initialized");
      if (!schemaExists()) {
        createSchema();
      } else {
        table = client.openTable(kuduMapping.getTableName());
      }
    } catch (Exception ex) {
      LOG.error("Error while initializing Kudu store", ex);
      throw new GoraException(ex);
    }
  }

  @Override
  public String getSchemaName() {
    return kuduMapping.getTableName();
  }

  @Override
  public String getSchemaName(final String mappingSchemaName, final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  @Override
  public void createSchema() throws GoraException {
    if (client == null) {
      throw new GoraException(
          "Impossible to create the schema as no connection has been initiated.");
    }
    if (schemaExists()) {
      return;
    }
    try {
      List<ColumnSchema> columns = new ArrayList<>();
      List<String> keys = new ArrayList<>();
      for (Column pk : kuduMapping.getPrimaryKey()) {
        columns.add(new ColumnSchema.ColumnSchemaBuilder(pk.getName(), pk.getDataType().getType()).key(true).build());
        keys.add(pk.getName());
      }
      for (Map.Entry<String, Column> clt : kuduMapping.getFields().entrySet()) {
        Column aColumn = clt.getValue();
        ColumnSchema aColumnSch;
        ColumnSchema.ColumnSchemaBuilder aBaseColumn = new ColumnSchema.ColumnSchemaBuilder(aColumn.getName(), aColumn.getDataType().getType()).nullable(true);
        if (aColumn.getDataType().getType() == Type.DECIMAL) {
          aColumnSch = aBaseColumn.typeAttributes(DecimalUtil.typeAttributes(aColumn.getDataType().getPrecision(), aColumn.getDataType().getScale())).build();
        } else {
          aColumnSch = aBaseColumn.build();
        }
        columns.add(aColumnSch);
      }
      org.apache.kudu.Schema sch = new org.apache.kudu.Schema(columns);
      CreateTableOptions cto = new CreateTableOptions();
      if (kuduMapping.getHashBuckets() > 0) {
        cto.addHashPartitions(keys, kuduMapping.getHashBuckets());
      }
      if (!kuduMapping.getRangePartitions().isEmpty()) {
        cto.setRangePartitionColumns(keys);
        for (Pair<String, String> range : kuduMapping.getRangePartitions()) {
          PartialRow lowerPar = sch.newPartialRow();
          PartialRow upperPar = sch.newPartialRow();
          for (String ky : keys) {
            if (!range.getKey().isEmpty()) {
              lowerPar.addString(ky, range.getKey());
            }
            if (!range.getValue().isEmpty()) {
              upperPar.addString(ky, range.getValue());
            }
          }
          cto.addRangePartition(lowerPar, upperPar);
        }
      }
      table = client.createTable(kuduMapping.getTableName(), sch, cto);
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    try {
      client.deleteTable(kuduMapping.getTableName());
      table = null;
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    try {
      return client.tableExists(kuduMapping.getTableName());
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public boolean exists(K key) throws GoraException {
    try {
      ColumnSchema column = table.getSchema().getColumn(kuduMapping.getPrimaryKey().get(0).getName());
      KuduScanner.KuduScannerBuilder scannerBuilder = client.newScannerBuilder(table);
      scannerBuilder.limit(1);
      scannerBuilder.setProjectedColumnIndexes(new ArrayList<>());
      scannerBuilder.addPredicate(KuduClientUtils.createEqualPredicate(column, key));
      KuduScanner build = scannerBuilder.build();
      RowResult waitFirstResult = KuduClientUtils.waitFirstResult(build);
      build.close();
      return waitFirstResult != null;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    String[] avFields = getFieldsToQuery(fields);
    List<String> dbFields = new ArrayList<>();
    for (String af : avFields) {
      dbFields.add(kuduMapping.getFields().get(af).getName());
    }
    try {
      ColumnSchema column = table.getSchema().getColumn(kuduMapping.getPrimaryKey().get(0).getName());
      KuduScanner.KuduScannerBuilder scannerBuilder = client.newScannerBuilder(table);
      scannerBuilder.limit(1);
      scannerBuilder.setProjectedColumnNames(dbFields);
      scannerBuilder.addPredicate(KuduClientUtils.createEqualPredicate(column, key));
      KuduScanner build = scannerBuilder.build();
      RowResult waitGetOneOrZero = KuduClientUtils.waitFirstResult(build);
      T resp = null;
      if (waitGetOneOrZero != null) {
        resp = newInstance(waitGetOneOrZero, fields);
      }
      build.close();
      return resp;
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void put(K key, T obj) throws GoraException {
    try {
      if (obj.isDirty()) {
        Column pkc = kuduMapping.getPrimaryKey().get(0);
        Upsert upsert = table.newUpsert();
        PartialRow row = upsert.getRow();
        KuduClientUtils.addObjectRow(row, pkc, key);
        Schema schema = obj.getSchema();
        List<Schema.Field> fields = schema.getFields();
        for (Schema.Field field : fields) {
          Column mappedColumn = kuduMapping.getFields().get(field.name());
          Object fieldValue = obj.get(field.pos());
          if (mappedColumn != null && fieldValue != null) {
            Schema fieldSchema = field.schema();
            Object serializedObj = serializeFieldValue(fieldSchema, fieldValue);
            KuduClientUtils.addObjectRow(row, mappedColumn, serializedObj);
          }else{
            row.setNull(mappedColumn.getName());
          }
        }
        session.apply(upsert);
      } else {
        LOG.info("Ignored putting object {} in the store as it is neither "
            + "new, neither dirty.", new Object[]{obj});
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    try {
      Column pkc = kuduMapping.getPrimaryKey().get(0);
      Delete delete = table.newDelete();
      PartialRow row = delete.getRow();
      KuduClientUtils.addObjectRow(row, pkc, key);
      OperationResponse apply = session.apply(delete);
      return !apply.hasRowError();
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    try {
      long count = 0;
      Column pkc = kuduMapping.getPrimaryKey().get(0);
      ColumnSchema column = table.getSchema().getColumn(pkc.getName());
      KuduScanner.KuduScannerBuilder scannerBuilder = client.newScannerBuilder(table);
      if (query.getLimit() != -1) {
        scannerBuilder.limit(query.getLimit());
      }
      List<String> dbFields = new ArrayList<>();
      dbFields.add(pkc.getName());
      scannerBuilder.setProjectedColumnNames(dbFields);
      List<KuduPredicate> rangePredicates = KuduClientUtils.createRangePredicate(column, query.getStartKey(), query.getEndKey());
      for (KuduPredicate predicate : rangePredicates) {
        scannerBuilder.addPredicate(predicate);
      }
      scannerBuilder.addPredicate(KuduPredicate.newIsNotNullPredicate(column));
      KuduScanner build = scannerBuilder.build();
      while (build.hasMoreRows()) {
        RowResultIterator nextRows = build.nextRows();
        for (RowResult it : nextRows) {
          count++;
          K key = (K) KuduClientUtils.getObjectRow(it, pkc);
          if (query.getFields() != null && query.getFields().length < kuduMapping.getFields().size()) {
            Update updateOp = table.newUpdate();
            PartialRow row = updateOp.getRow();
            String[] avFields = getFieldsToQuery(query.getFields());
            KuduClientUtils.addObjectRow(row, pkc, key);
            for (String af : avFields) {
              row.setNull(kuduMapping.getFields().get(af).getName());
            }
            session.apply(updateOp);
          } else {
            delete(key);
          }
        }
      }
      build.close();
      return count;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    String[] avFields = getFieldsToQuery(query.getFields());
    List<String> dbFields = new ArrayList<>();
    for (String af : avFields) {
      dbFields.add(kuduMapping.getFields().get(af).getName());
    }
    try {
      ColumnSchema column = table.getSchema().getColumn(kuduMapping.getPrimaryKey().get(0).getName());
      dbFields.add(kuduMapping.getPrimaryKey().get(0).getName());
      KuduScanner.KuduScannerBuilder scannerBuilder = client.newScannerBuilder(table);
      if (query.getLimit() != -1) {
        scannerBuilder.limit(query.getLimit());
      }
      scannerBuilder.setProjectedColumnNames(dbFields);
      List<KuduPredicate> rangePredicates = KuduClientUtils.createRangePredicate(column, query.getStartKey(), query.getEndKey());
      for (KuduPredicate predicate : rangePredicates) {
        scannerBuilder.addPredicate(predicate);
      }
      scannerBuilder.addPredicate(KuduPredicate.newIsNotNullPredicate(column));
      KuduScanner build = scannerBuilder.build();
      KuduResult<K, T> kuduResult = new KuduResult<>(this, query, build);
      return kuduResult;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public Query<K, T> newQuery() {
    KuduQuery<K, T> query = new KuduQuery<>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    if (kuduMapping.getRangePartitions().isEmpty()) {
      PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
          query);
      partitionQuery.setConf(getConf());
      partitions.add(partitionQuery);
    } else {
      for (Pair<String, String> rang : kuduMapping.getRangePartitions()) {
        PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query, rang.getKey().isEmpty() ? null : (K) rang.getKey(),
            rang.getValue().isEmpty() ? null : (K) rang.getValue());
        partitionQuery.setConf(getConf());
        partitions.add(partitionQuery);

      }
    }
    return partitions;
  }

  @Override
  public void flush() throws GoraException {
    try {
      session.flush();
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void close() {
    try {
      session.close();
      client.close();
      LOG.info("Kudu datastore destroyed successfully.");
    } catch (KuduException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  public T newInstance(RowResult next, String[] fields) throws GoraException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      Column column = kuduMapping.getFields().get(f);
      if (next.isNull(column.getName())) {
        continue;
      }
      Object fieldValue = KuduClientUtils.getObjectRow(next, column);
      Object v = deserializeFieldValue(field, fieldSchema, fieldValue, persistent);
      persistent.put(field.pos(), v);
      persistent.setDirty(field.pos());
    }
    return persistent;
  }

  @SuppressWarnings("unchecked")
  public K extractKey(RowResult r) {
    Column column = kuduMapping.getPrimaryKey().get(0);
    return (K) KuduClientUtils.getObjectRow(r, column);
  }

  @SuppressWarnings("unchecked")
  private Object deserializeFieldValue(Schema.Field field, Schema fieldSchema,
      Object kuduValue, T persistent) throws IOException {
    Object fieldValue = null;
    switch (fieldSchema.getType()) {
      case MAP:
      case ARRAY:
      case RECORD:
        @SuppressWarnings("rawtypes") SpecificDatumReader reader = getDatumReader(fieldSchema);
        fieldValue = IOUtils.deserialize((byte[]) kuduValue, reader,
            persistent.get(field.pos()));
        break;
      case ENUM:
        fieldValue = AvroUtils.getEnumValue(fieldSchema, kuduValue.toString());
        break;
      case FIXED:
        break;
      case BYTES:
        fieldValue = ByteBuffer.wrap((byte[]) kuduValue);
        break;
      case STRING:
        fieldValue = new Utf8(kuduValue.toString());
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(kuduValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          fieldValue = deserializeFieldValue(field, unionSchema, kuduValue, persistent);
        } else {
          reader = getDatumReader(fieldSchema);
          fieldValue = IOUtils.deserialize((byte[]) kuduValue, reader,
              persistent.get(field.pos()));
        }
        break;
      default:
        fieldValue = kuduValue;
    }
    return fieldValue;
  }

  @SuppressWarnings("unchecked")
  private Object serializeFieldValue(Schema fieldSchema, Object fieldValue) {
    Object output = fieldValue;
    switch (fieldSchema.getType()) {
      case ARRAY:
      case MAP:
      case RECORD:
        byte[] data = null;
        try {
          @SuppressWarnings("rawtypes")
          SpecificDatumWriter writer = getDatumWriter(fieldSchema);
          data = IOUtils.serialize(writer, fieldValue);
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
        output = data;
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(fieldValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          output = serializeFieldValue(unionSchema, fieldValue);
        } else {
          data = null;
          try {
            @SuppressWarnings("rawtypes")
            SpecificDatumWriter writer = getDatumWriter(fieldSchema);
            data = IOUtils.serialize(writer, fieldValue);
          } catch (IOException e) {
            LOG.error(e.getMessage(), e);
          }
          output = data;
        }
        break;
      case FIXED:
        break;
      case ENUM:
      case STRING:
        output = fieldValue.toString();
        break;
      case BYTES:
        output = ((ByteBuffer) fieldValue).array();
        break;
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
        output = fieldValue;
        break;
      case NULL:
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return output;
  }

  /**
   * Method to retrieve the corresponding schema type index of a particular
   * object having UNION schema. As UNION type can have one or more types and at
   * a given instance, it holds an object of only one type of the defined types,
   * this method is used to figure out the corresponding instance's schema type
   * index.
   *
   * @param instanceValue value that the object holds
   * @param unionSchema union schema containing all of the data types
   * @return the unionSchemaPosition corresponding schema position
   */
  private int getUnionSchema(Object instanceValue, Schema unionSchema) {
    int unionSchemaPos = 0;
    for (Schema currentSchema : unionSchema.getTypes()) {
      Schema.Type schemaType = currentSchema.getType();
      if (instanceValue instanceof CharSequence && schemaType.equals(Schema.Type.STRING)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof ByteBuffer && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Integer && schemaType.equals(Schema.Type.INT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Long && schemaType.equals(Schema.Type.LONG)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Double && schemaType.equals(Schema.Type.DOUBLE)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Float && schemaType.equals(Schema.Type.FLOAT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Boolean && schemaType.equals(Schema.Type.BOOLEAN)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Map && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof List && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Persistent && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      unionSchemaPos++;
    }
    return 0;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumReader getDatumReader(Schema fieldSchema) {
    SpecificDatumReader<?> reader = readerMap.get(fieldSchema);
    if (reader == null) {
      reader = new SpecificDatumReader(fieldSchema);// ignore dirty bits
      SpecificDatumReader localReader = null;
      if ((localReader = readerMap.putIfAbsent(fieldSchema, reader)) != null) {
        reader = localReader;
      }
    }
    return reader;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumWriter getDatumWriter(Schema fieldSchema) {
    SpecificDatumWriter writer = writerMap.get(fieldSchema);
    if (writer == null) {
      writer = new SpecificDatumWriter(fieldSchema);// ignore dirty bits
      writerMap.put(fieldSchema, writer);
    }
    return writer;
  }

  private boolean isNullable(Schema unionSchema) {
    for (Schema innerSchema : unionSchema.getTypes()) {
      if (innerSchema.getType().equals(Schema.Type.NULL)) {
        return true;
      }
    }
    return false;
  }
}
