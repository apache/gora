/**
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

package org.apache.gora.cassandra.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Collections;

import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.beans.SuperSlice;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.query.CassandraResult;
import org.apache.gora.cassandra.query.CassandraResultSet;
import org.apache.gora.cassandra.query.CassandraRow;
import org.apache.gora.cassandra.query.CassandraSubColumn;
import org.apache.gora.cassandra.query.CassandraSuperColumn;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.gora.cassandra.store.CassandraStore} is the primary class 
 * responsible for directing Gora CRUD operations into Cassandra. We (delegate) rely 
 * heavily on {@ link org.apache.gora.cassandra.store.CassandraClient} for many operations
 * such as initialization, creating and deleting schemas (Cassandra Keyspaces), etc.  
 */
public class CassandraStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  /** Logging implementation */
  public static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  private CassandraClient<K, T>  cassandraClient = new CassandraClient<K, T>();

  /**
   * Fixed string used to generate an extra column based on 
   * the original field's name
   */
  public static String UNION_COL_SUFIX = "UnionIndex";

  /**
   * Default schema index used when AVRO Union data types are stored
   */
  public static int DEFAULT_UNION_SCHEMA = 0;

  /**
   * The values are Avro fields pending to be stored.
   *
   * We want to iterate over the keys in insertion order.
   * We don't want to lock the entire collection before iterating over the keys, 
   * since in the meantime other threads are adding entries to the map.
   */
  private Map<K, T> buffer = Collections.synchronizedMap(new LinkedHashMap<K, T>());

  /** The default constructor for CassandraStore */
  public CassandraStore() throws Exception {
    // this.cassandraClient.initialize();
  }

  /** 
   * Initialize is called when then the call to 
   * {@link org.apache.gora.store.DataStoreFactory#createDataStore(Class<D> dataStoreClass, Class<K> keyClass, Class<T> persistent, org.apache.hadoop.conf.Configuration conf)}
   * is made. In this case, we merely delegate the store initialization to the 
   * {@link org.apache.gora.cassandra.store.CassandraClient#initialize(Class<K> keyClass, Class<T> persistentClass)}. 
   */
  public void initialize(Class<K> keyClass, Class<T> persistent, Properties properties) {
    try {
      super.initialize(keyClass, persistent, properties);
      this.cassandraClient.initialize(keyClass, persistent);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      LOG.error(e.getStackTrace().toString());
    }
  }

  @Override
  public void close() {
    LOG.debug("close");
    flush();
  }

  @Override
  public void createSchema() {
    LOG.debug("creating Cassandra keyspace");
    this.cassandraClient.checkKeyspace();
  }

  @Override
  public boolean delete(K key) {
    LOG.debug("delete " + key);
    return false;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    LOG.debug("delete by query " + query);
    return 0;
  }

  @Override
  public void deleteSchema() {
    LOG.debug("delete schema");
    this.cassandraClient.dropKeyspace();
  }

  /**
   * When executing Gora Queries in Cassandra we query the Cassandra keyspace by families.
   * When we add sub/supercolumns, Gora keys are mapped to Cassandra partition keys only. 
   * This is because we follow the Cassandra logic where column family data is 
   * partitioned across nodes based on row Key.
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) {

    Map<String, List<String>> familyMap = this.cassandraClient.getFamilyMap(query);
    Map<String, String> reverseMap = this.cassandraClient.getReverseMap(query);

    CassandraQuery<K, T> cassandraQuery = new CassandraQuery<K, T>();
    cassandraQuery.setQuery(query);
    cassandraQuery.setFamilyMap(familyMap);

    CassandraResult<K, T> cassandraResult = new CassandraResult<K, T>(this, query);
    cassandraResult.setReverseMap(reverseMap);

    CassandraResultSet<K> cassandraResultSet = new CassandraResultSet<K>();

    // We query Cassandra keyspace by families.
    for (String family : familyMap.keySet()) {
      if (family == null) {
        continue;
      }
      if (this.cassandraClient.isSuper(family)) {
        addSuperColumns(family, cassandraQuery, cassandraResultSet);

      } else {
        addSubColumns(family, cassandraQuery, cassandraResultSet);
      }
    }

    cassandraResult.setResultSet(cassandraResultSet);

    return cassandraResult;
  }

  /**
   * When we add subcolumns, Gora keys are mapped to Cassandra partition keys only. 
   * This is because we follow the Cassandra logic where column family data is 
   * partitioned across nodes based on row Key.
   */
  private void addSubColumns(String family, CassandraQuery<K, T> cassandraQuery,
      CassandraResultSet cassandraResultSet) {
    // select family columns that are included in the query
    List<Row<K, ByteBuffer, ByteBuffer>> rows = this.cassandraClient.execute(cassandraQuery, family);

    for (Row<K, ByteBuffer, ByteBuffer> row : rows) {
      K key = row.getKey();

      // find associated row in the resultset
      CassandraRow<K> cassandraRow = cassandraResultSet.getRow(key);
      if (cassandraRow == null) {
        cassandraRow = new CassandraRow<K>();
        cassandraResultSet.putRow(key, cassandraRow);
        cassandraRow.setKey(key);
      }

      ColumnSlice<ByteBuffer, ByteBuffer> columnSlice = row.getColumnSlice();

      for (HColumn<ByteBuffer, ByteBuffer> hColumn : columnSlice.getColumns()) {
        CassandraSubColumn cassandraSubColumn = new CassandraSubColumn();
        cassandraSubColumn.setValue(hColumn);
        cassandraSubColumn.setFamily(family);
        cassandraRow.add(cassandraSubColumn);
      }

    }
  }

  /**
   * When we add supercolumns, Gora keys are mapped to Cassandra partition keys only. 
   * This is because we follow the Cassandra logic where column family data is 
   * partitioned across nodes based on row Key.
   */
  private void addSuperColumns(String family, CassandraQuery<K, T> cassandraQuery, 
      CassandraResultSet cassandraResultSet) {

    List<SuperRow<K, String, ByteBuffer, ByteBuffer>> superRows = this.cassandraClient.executeSuper(cassandraQuery, family);
    for (SuperRow<K, String, ByteBuffer, ByteBuffer> superRow: superRows) {
      K key = superRow.getKey();
      CassandraRow<K> cassandraRow = cassandraResultSet.getRow(key);
      if (cassandraRow == null) {
        cassandraRow = new CassandraRow();
        cassandraResultSet.putRow(key, cassandraRow);
        cassandraRow.setKey(key);
      }

      SuperSlice<String, ByteBuffer, ByteBuffer> superSlice = superRow.getSuperSlice();
      for (HSuperColumn<String, ByteBuffer, ByteBuffer> hSuperColumn: superSlice.getSuperColumns()) {
        CassandraSuperColumn cassandraSuperColumn = new CassandraSuperColumn();
        cassandraSuperColumn.setValue(hSuperColumn);
        cassandraSuperColumn.setFamily(family);
        cassandraRow.add(cassandraSuperColumn);
      }
    }
  }

  /**
   * Flush the buffer which is a synchronized {@link java.util.LinkedHashMap}
   * storing fields pending to be stored by 
   * {@link org.apache.gora.cassandra.store.CassandraStore#put(Object, PersistentBase)}
   * operations. Invoking this method therefore writes the buffered rows
   * into Cassandra.
   * @see org.apache.gora.store.DataStore#flush()
   */
  @Override
  public void flush() {

    Set<K> keys = this.buffer.keySet();

    // this duplicates memory footprint
    @SuppressWarnings("unchecked")
    K[] keyArray = (K[]) keys.toArray();

    // iterating over the key set directly would throw 
    //ConcurrentModificationException with java.util.HashMap and subclasses
    for (K key: keyArray) {
      T value = this.buffer.get(key);
      if (value == null) {
        LOG.info("Value to update is null for key: " + key);
        continue;
      }
      Schema schema = value.getSchema();

      for (Field field: schema.getFields()) {
        if (value.isDirty(field.pos())) {
          addOrUpdateField(key, field, value.get(field.pos()));
        }
      }
    }

    // remove flushed rows from the buffer as all 
    // added or updated fields should now have been written.
    for (K key: keyArray) {
      this.buffer.remove(key);
    }
  }

  @Override
  public T get(K key, String[] fields) {
    CassandraQuery<K,T> query = new CassandraQuery<K,T>();
    query.setDataStore(this);
    query.setKeyRange(key, key);
    query.setFields(fields);
    query.setLimit(1);
    Result<K,T> result = execute(query);
    boolean hasResult = false;
    try {
      hasResult = result.next();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return hasResult ? result.get() : null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
      throws IOException {
    // TODO right now this just obtains a single partition
    // we need to obtain the correct splits for partitions in 
    // order to achieve data locality.
    List<PartitionQuery<K,T>> partitions = new ArrayList<PartitionQuery<K,T>>();
    PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<K, T>(query);
    pqi.setConf(getConf());
    partitions.add(pqi);
    return partitions;
  }

  /**
   * In Cassandra Schemas are referred to as Keyspaces
   * @return Keyspace
   */
  @Override
  public String getSchemaName() {
    return this.cassandraClient.getKeyspaceName();
  }

  @Override
  public Query<K, T> newQuery() {
    Query<K,T> query = new CassandraQuery<K, T>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }

  /**
   * 
   * When doing the 
   * {@link org.apache.gora.cassandra.store.CassandraStore#put(Object, PersistentBase)}
   * operation, the logic is as follows:
   * <ol>
   * <li>Obtain the Avro {@link org.apache.avro.Schema} for the object.</li>
   * <li>Create a new duplicate instance of the object (explained in more detail below) **.</li>
   * <li>Obtain a {@link java.util.List} of the {@link org.apache.avro.Schema} 
   * {@link org.apache.avro.Schema.Field}'s.</li>
   * <li>Iterate through the {@link java.util.List}. This allows us to process
   * each item appropriately.</li>
   * <li>Check to see if the {@link org.apache.avro.Schema.Field} is either at 
   * position 0 OR it is NOT dirty. If one of these conditions is true then we DO NOT
   * process this field.</li>
   * <li>Obtain the element at the specified position in this list.</li>
   * <li>Obtain the {@link org.apache.avro.Schema.Type} of the element obtained 
   * above and process it accordingly. N.B. For nested type RECORD we shadow 
   * the checks to see if the {@link org.apache.avro.Schema.Field} is either at 
   * position 0 OR it is NOT dirty. If one of these conditions is true then we DO NOT
   * process this field.</li>
   * </ol>
   * ** We create a duplicate instance of the object to be persisted and insert processed
   * objects into a synchronized {@link java.util.LinkedHashMap}. This allows 
   * us to keep all the objects in memory till flushing.
   * @see org.apache.gora.store.DataStore#put(java.lang.Object, 
   * org.apache.gora.persistency.Persistent).
   * @param key for the Avro Record (object).
   * @param value Record object to be persisted in Cassandra
   */
  @Override
  public void put(K key, T value) {
    Schema schema = value.getSchema();
    @SuppressWarnings("unchecked")
    T p = (T) SpecificData.get().newRecord(value, schema);
    List<Field> fields = schema.getFields();
    for (int i = 1; i < fields.size(); i++) {
      if (!value.isDirty(i)) {
        continue;
      }
      Field field = fields.get(i);
      Type type = field.schema().getType();
      Object fieldValue = value.get(field.pos());
      // check if field has a nested structure (array, map, record or union)

      switch(type) {
      case RECORD:
        Persistent persistent = (Persistent) fieldValue;
        Persistent newRecord = (Persistent) SpecificData.get().newRecord(persistent, persistent.getSchema());
        for (Field member: field.schema().getFields()) {
          if (member.pos() == 0 || !persistent.isDirty()) {
            continue;
          }
          newRecord.put(member.pos(), persistent.get(member.pos()));
        }
        fieldValue = newRecord;
        break;
      case MAP:
        Map<?, ?> map = (Map<?, ?>) fieldValue;
        fieldValue = map;
        break;
      case ARRAY:
        fieldValue = (List<?>) fieldValue;
        break;
      case UNION:
        // storing the union selected schema, the actual value will 
        // be stored as soon as we get break out.
        int schemaPos = getUnionSchema(fieldValue,field.schema());
        p.put( schemaPos, p.getSchema().getField(field.name() + CassandraStore.UNION_COL_SUFIX));
        //p.put(fieldPos, fieldValue);
        break;
      default:
        break;
      }
      p.put(field.pos(), fieldValue);
    }
    // this performs a structural modification of the map
    this.buffer.put(key, p);
  }

  /**
   * Add a field to Cassandra according to its type.
   * @param key     the key of the row where the field should be added
   * @param field   the Avro field representing a datum
   * @param value   the field value
   */
  @SuppressWarnings({ "unchecked", "null" })
  private void addOrUpdateField(K key, Field field, Object value) {
    Schema schema = field.schema();
    Type type = schema.getType();
    // checking if the value to be updated is used for saving union schema
    if (field.name().indexOf(CassandraStore.UNION_COL_SUFIX) < 0){
      switch (type) {
      case STRING:
      case BOOLEAN:
      case INT:
      case LONG:
      case BYTES:
      case FLOAT:
      case DOUBLE:
      case FIXED:
        this.cassandraClient.addColumn(key, field.name(), value);
        break;
      case RECORD:
        if (value != null) {
          if (value instanceof PersistentBase) {
            PersistentBase persistentBase = (PersistentBase) value;
            for (Field member: schema.getFields()) {

              // TODO: hack, do not store empty arrays
              Object memberValue = persistentBase.get(member.pos());
              if (memberValue instanceof List<?>) {
                if (((List<?>)memberValue).size() == 0) {
                  continue;
                }
              } else if (memberValue instanceof Map<?,?>) {
                if (((Map<?, ?>)memberValue).size() == 0) {
                  continue;
                }
              }
              this.cassandraClient.addSubColumn(key, field.name(), 
                  member.name(), memberValue);
            }
          } else {
            LOG.warn("Record with value: " + value.toString() + " not supported for field: " + field.name());
          }
        }
        break;
      case MAP:
        if (value != null) {
          if (value instanceof Map<?, ?>) {
            this.cassandraClient.addStatefulHashMap(key, field.name(), (Map<CharSequence,Object>)value);
          } else {
            LOG.warn("Map with value: " + value.toString() + " not supported for field: " + field.name());
          }
        }
        break;
      case ARRAY:
        if (value != null) {
          if (value instanceof GenericArray<?>) {
            this.cassandraClient.addGenericArray(key, field.name(), (GenericArray<?>)value);
          } else {
            LOG.warn("Array with value: " + value.toString() + " not supported for field: " + field.name());
          }
        }
        break;
      case UNION:
        if(value != null) {
          LOG.debug("Union with value: " + value.toString() + " at index: " + getUnionSchema(value, schema) + " supported for field: " + field.name());
          // adding union schema index
          String columnName = field.name() + UNION_COL_SUFIX;
          String familyName = this.cassandraClient.getCassandraMapping().getFamily(field.name());
          this.cassandraClient.getCassandraMapping().addColumn(familyName, columnName, columnName);
          this.cassandraClient.addColumn(key, columnName, getUnionSchema(value, schema));
          // adding union value
          this.cassandraClient.addColumn(key, field.name(), value);
        } else {
          LOG.warn("Union with 'null' value not supported for field: " + field.name());
        }
        break;
      default:
        LOG.warn("Type: " + type.name() + " not considered for field: " + field.name() + ". Please report this to dev@gora.apache.org");
      }
    }
  }

  /**
   * Given an object and the object schema this function obtains,
   * from within the UNION schema, the position of the type used.
   * If no data type can be inferred then we return a default value
   * of position 0.
   * @param pValue
   * @param pUnionSchema
   * @return the unionSchemaPosition.
   */
  private int getUnionSchema(Object pValue, Schema pUnionSchema){
    int unionSchemaPos = 0;
    String valueType = pValue.getClass().getSimpleName();
    Iterator<Schema> it = pUnionSchema.getTypes().iterator();
    while ( it.hasNext() ){
      String schemaName = it.next().getName();
      if (valueType.equals("Utf8") && schemaName.equals(Type.STRING.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("HeapByteBuffer") && schemaName.equals(Type.STRING.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("Integer") && schemaName.equals(Type.INT.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("Long") && schemaName.equals(Type.LONG.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("Double") && schemaName.equals(Type.DOUBLE.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("Float") && schemaName.equals(Type.FLOAT.name().toLowerCase()))
        return unionSchemaPos;
      else if (valueType.equals("Boolean") && schemaName.equals(Type.BOOLEAN.name().toLowerCase()))
        return unionSchemaPos;
      unionSchemaPos ++;
    }
    // if we weren't able to determine which data type it is, then we return the default
    return DEFAULT_UNION_SCHEMA;
  }

  /**
   * Simple method to check if a Cassandra Keyspace exists.
   * @return true if a Keyspace exists.
   */
  @Override
  public boolean schemaExists() {
    LOG.info("schema exists");
    return cassandraClient.keyspaceExists();
  }

}
