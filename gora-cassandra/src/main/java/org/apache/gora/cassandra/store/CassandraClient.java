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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.OrderedSuperRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.RangeSuperSlicesQuery;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Serializer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.serializers.GenericArraySerializer;
import org.apache.gora.cassandra.serializers.GoraSerializerTypeInferer;
import org.apache.gora.cassandra.serializers.TypeUtils;
import org.apache.gora.mapreduce.GoraRecordReader;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StatefulHashMap;
import org.apache.gora.query.Query;
import org.apache.gora.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraClient<K, T extends PersistentBase> {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);
  
  private Cluster cluster;
  private Keyspace keyspace;
  private Mutator<K> mutator;
  private Class<K> keyClass;
  private Class<T> persistentClass;
  
  private CassandraMapping cassandraMapping = null;

  private Serializer<K> keySerializer;
  
  public void initialize(Class<K> keyClass, Class<T> persistentClass) throws Exception {
    this.keyClass = keyClass;

    // get cassandra mapping with persistent class
    this.persistentClass = persistentClass;
    this.cassandraMapping = CassandraMappingManager.getManager().get(persistentClass);
    // LOG.info("persistentClass=" + persistentClass.getName() + " -> cassandraMapping=" + cassandraMapping);

    this.cluster = HFactory.getOrCreateCluster(this.cassandraMapping.getClusterName(), new CassandraHostConfigurator(this.cassandraMapping.getHostName()));
    
    // add keyspace to cluster
    checkKeyspace();
    
    // Just create a Keyspace object on the client side, corresponding to an already existing keyspace with already created column families.
    this.keyspace = HFactory.createKeyspace(this.cassandraMapping.getKeyspaceName(), this.cluster);
    
    this.keySerializer = GoraSerializerTypeInferer.getSerializer(keyClass);
    this.mutator = HFactory.createMutator(this.keyspace, this.keySerializer);
  }

  /**
   * Check if keyspace already exists.
   */
  public boolean keyspaceExists() {
    KeyspaceDefinition keyspaceDefinition = this.cluster.describeKeyspace(this.cassandraMapping.getKeyspaceName());
    return (keyspaceDefinition != null);
  }
  
  /**
   * Check if keyspace already exists. If not, create it.
   * In this method, we also utilise Hector's {@ConfigurableConsistencyLevel}
   * logic. It is set by passing a ConfigurableConsistencyLevel object right 
   * when the Keyspace is created. Currently consistency level is .ONE which 
   * permits consistency to wait until one replica has responded. 
   */
  public void checkKeyspace() {
    // "describe keyspace <keyspaceName>;" query
    KeyspaceDefinition keyspaceDefinition = this.cluster.describeKeyspace(this.cassandraMapping.getKeyspaceName());
    if (keyspaceDefinition == null) {
      List<ColumnFamilyDefinition> columnFamilyDefinitions = this.cassandraMapping.getColumnFamilyDefinitions();      

      // GORA-197
      for (ColumnFamilyDefinition cfDef : columnFamilyDefinitions) {
        cfDef.setComparatorType(ComparatorType.BYTESTYPE);
      }

      keyspaceDefinition = HFactory.createKeyspaceDefinition(this.cassandraMapping.getKeyspaceName(), "org.apache.cassandra.locator.SimpleStrategy", 1, columnFamilyDefinitions);      
      this.cluster.addKeyspace(keyspaceDefinition, true);
      // LOG.info("Keyspace '" + this.cassandraMapping.getKeyspaceName() + "' in cluster '" + this.cassandraMapping.getClusterName() + "' was created on host '" + this.cassandraMapping.getHostName() + "'");
      
      // Create a customized Consistency Level
      ConfigurableConsistencyLevel configurableConsistencyLevel = new ConfigurableConsistencyLevel();
      Map<String, HConsistencyLevel> clmap = new HashMap<String, HConsistencyLevel>();

      // Define CL.ONE for ColumnFamily "ColumnFamily"
      clmap.put("ColumnFamily", HConsistencyLevel.ONE);

      // In this we use CL.ONE for read and writes. But you can use different CLs if needed.
      configurableConsistencyLevel.setReadCfConsistencyLevels(clmap);
      configurableConsistencyLevel.setWriteCfConsistencyLevels(clmap);

      // Then let the keyspace know
      HFactory.createKeyspace("Keyspace", this.cluster, configurableConsistencyLevel);

      keyspaceDefinition = null;
    }
    else {
      List<ColumnFamilyDefinition> cfDefs = keyspaceDefinition.getCfDefs();
      if (cfDefs == null || cfDefs.size() == 0) {
        LOG.warn(keyspaceDefinition.getName() + " does not have any column family.");
      }
      else {
        for (ColumnFamilyDefinition cfDef : cfDefs) {
          ComparatorType comparatorType = cfDef.getComparatorType();
          if (! comparatorType.equals(ComparatorType.BYTESTYPE)) {
            // GORA-197
            LOG.warn("The comparator type of " + cfDef.getName() + " column family is " + comparatorType.getTypeName()
                   + ", not BytesType. It may cause a fatal error on column validation later.");
          }
          else {
            // LOG.info("The comparator type of " + cfDef.getName() + " column family is " + comparatorType.getTypeName() + ".");
          }
        }
      }
    }
  }
  
  /**
   * Drop keyspace.
   */
  public void dropKeyspace() {
    // "drop keyspace <keyspaceName>;" query
    this.cluster.dropKeyspace(this.cassandraMapping.getKeyspaceName());
  }

  /**
   * Insert a field in a column.
   * @param key the row key
   * @param fieldName the field name
   * @param value the field value.
   */
  public void addColumn(K key, String fieldName, Object value) {
    if (value == null) {
      return;
    }

    ByteBuffer byteBuffer = toByteBuffer(value);
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String columnName = this.cassandraMapping.getColumn(fieldName);

    if (columnName == null) {
      LOG.warn("Column name is null for field=" + fieldName + " with value=" + value.toString());
      return;
    }
    
    synchronized(mutator) {
      HectorUtils.insertColumn(mutator, key, columnFamily, columnName, byteBuffer);
    }
  }

  /**
   * Insert a member in a super column. This is used for map and record Avro types.
   * @param key the row key
   * @param fieldName the field name
   * @param columnName the column name (the member name, or the index of array)
   * @param value the member value
   */
  @SuppressWarnings("unchecked")
  public void addSubColumn(K key, String fieldName, ByteBuffer columnName, Object value) {
    if (value == null) {
      return;
    }

    ByteBuffer byteBuffer = toByteBuffer(value);
    
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    
    synchronized(mutator) {
      HectorUtils.insertSubColumn(mutator, key, columnFamily, superColumnName, columnName, byteBuffer);
    }
  }

  /**
   * Adds an subColumn inside the cassandraMapping file when a String is serialized
   * @param key
   * @param fieldName
   * @param columnName
   * @param value
   */
  public void addSubColumn(K key, String fieldName, String columnName, Object value) {
    addSubColumn(key, fieldName, StringSerializer.get().toByteBuffer(columnName), value);
  }

  /**
   * Adds an subColumn inside the cassandraMapping file when an Integer is serialized
   * @param key
   * @param fieldName
   * @param columnName
   * @param value
   */
  public void addSubColumn(K key, String fieldName, Integer columnName, Object value) {
    addSubColumn(key, fieldName, IntegerSerializer.get().toByteBuffer(columnName), value);
  }


  /**
   * Delete a member in a super column. This is used for map and record Avro types.
   * @param key the row key
   * @param fieldName the field name
   * @param columnName the column name (the member name, or the index of array)
   */
  @SuppressWarnings("unchecked")
  public void deleteSubColumn(K key, String fieldName, ByteBuffer columnName) {

    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    
    synchronized(mutator) {
      HectorUtils.deleteSubColumn(mutator, key, columnFamily, superColumnName, columnName);
    }
  }

  public void deleteSubColumn(K key, String fieldName, String columnName) {
    deleteSubColumn(key, fieldName, StringSerializer.get().toByteBuffer(columnName));
  }


  @SuppressWarnings("unchecked")
  public void addGenericArray(K key, String fieldName, GenericArray array) {
    if (isSuper( cassandraMapping.getFamily(fieldName) )) {
      int i= 0;
      for (Object itemValue: array) {

        // TODO: hack, do not store empty arrays
        if (itemValue instanceof GenericArray<?>) {
          if (((GenericArray)itemValue).size() == 0) {
            continue;
          }
        } else if (itemValue instanceof StatefulHashMap<?,?>) {
          if (((StatefulHashMap)itemValue).size() == 0) {
            continue;
          }
        }

        addSubColumn(key, fieldName, i++, itemValue);
      }
    }
    else {
      addColumn(key, fieldName, array);
    }
  }

  @SuppressWarnings("unchecked")
  public void addStatefulHashMap(K key, String fieldName, StatefulHashMap<Utf8,Object> map) {
    if (isSuper( cassandraMapping.getFamily(fieldName) )) {
      int i= 0;
      for (Utf8 mapKey: map.keySet()) {
        if (map.getState(mapKey) == State.DELETED) {
          deleteSubColumn(key, fieldName, mapKey.toString());
          continue;
        }

        // TODO: hack, do not store empty arrays
        Object mapValue = map.get(mapKey);
        if (mapValue instanceof GenericArray<?>) {
          if (((GenericArray)mapValue).size() == 0) {
            continue;
          }
        } else if (mapValue instanceof StatefulHashMap<?,?>) {
          if (((StatefulHashMap)mapValue).size() == 0) {
            continue;
          }
        }

        addSubColumn(key, fieldName, mapKey.toString(), mapValue);
      }
    }
    else {
      addColumn(key, fieldName, map);
    }
  }

  /**
   * Serialize value to ByteBuffer.
   * @param value the member value
   * @return ByteBuffer object
   */
  @SuppressWarnings("unchecked")
  public ByteBuffer toByteBuffer(Object value) {
    ByteBuffer byteBuffer = null;
    Serializer serializer = GoraSerializerTypeInferer.getSerializer(value);
    if (serializer == null) {
      LOG.info("Serializer not found for: " + value.toString());
    }
    else {
      byteBuffer = serializer.toByteBuffer(value);
    }

    if (byteBuffer == null) {
      LOG.info("value class=" + value.getClass().getName() + " value=" + value + " -> null");
    }
    
    return byteBuffer;
  }

  /**
   * Select a family column in the keyspace.
   * @param cassandraQuery a wrapper of the query
   * @param family the family name to be queried
   * @return a list of family rows
   */
  public List<Row<K, ByteBuffer, ByteBuffer>> execute(CassandraQuery<K, T> cassandraQuery, String family) {
    
    String[] columnNames = cassandraQuery.getColumns(family);
    ByteBuffer[] columnNameByteBuffers = new ByteBuffer[columnNames.length];
    for (int i = 0; i < columnNames.length; i++) {
      columnNameByteBuffers[i] = StringSerializer.get().toByteBuffer(columnNames[i]);
    }
    Query<K, T> query = cassandraQuery.getQuery();
    int limit = (int) query.getLimit();
    if (limit < 1) {
      limit = Integer.MAX_VALUE;
    }
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    
    RangeSlicesQuery<K, ByteBuffer, ByteBuffer> rangeSlicesQuery = HFactory.createRangeSlicesQuery(this.keyspace, this.keySerializer, ByteBufferSerializer.get(), ByteBufferSerializer.get());
    rangeSlicesQuery.setColumnFamily(family);
    rangeSlicesQuery.setKeys(startKey, endKey);
    rangeSlicesQuery.setRange(ByteBuffer.wrap(new byte[0]), ByteBuffer.wrap(new byte[0]), false, GoraRecordReader.BUFFER_LIMIT_READ_VALUE);
    rangeSlicesQuery.setRowCount(limit);
    rangeSlicesQuery.setColumnNames(columnNameByteBuffers);
    
    QueryResult<OrderedRows<K, ByteBuffer, ByteBuffer>> queryResult = rangeSlicesQuery.execute();
    OrderedRows<K, ByteBuffer, ByteBuffer> orderedRows = queryResult.get();
    
    
    return orderedRows.getList();
  }
  
  private String getMappingFamily(String pField){
    String family = null;
    // TODO checking if it was a UNION field the one we are retrieving
      family = this.cassandraMapping.getFamily(pField);
    return family;
  }
  
  private String getMappingColumn(String pField){
    String column = null;
    // TODO checking if it was a UNION field the one we are retrieving e.g. column = pField;
      column = this.cassandraMapping.getColumn(pField);
    return column;
  }

  /**
   * Select the families that contain at least one column mapped to a query field.
   * @param query indicates the columns to select
   * @return a map which keys are the family names and values the corresponding column names required to get all the query fields.
   */
  public Map<String, List<String>> getFamilyMap(Query<K, T> query) {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    for (String field: query.getFields()) {
      String family = this.getMappingFamily(field);
      String column = this.getMappingColumn(field);
      
      // check if the family value was already initialized 
      List<String> list = map.get(family);
      if (list == null) {
        list = new ArrayList<String>();
        map.put(family, list);
      }
      
      if (column != null) {
        list.add(column);
      }
      
    }
    
    return map;
  }

  /**
   * Retrieves the cassandraMapping which holds whatever was mapped from the gora-cassandra-mapping.xml
   * @return
   */
  public CassandraMapping getCassandraMapping(){
    return this.cassandraMapping;
  }
  
  /**
   * Select the field names according to the column names, which format if fully qualified: "family:column"
   * @param query
   * @return a map which keys are the fully qualified column names and values the query fields
   */
  public Map<String, String> getReverseMap(Query<K, T> query) {
    Map<String, String> map = new HashMap<String, String>();
    for (String field: query.getFields()) {
      String family = this.getMappingFamily(field);
      String column = this.getMappingColumn(field);
      
      map.put(family + ":" + column, field);
    }
    
    return map;
  }
  
  public boolean isSuper(String family) {
    return this.cassandraMapping.isSuper(family);
  }

  public List<SuperRow<K, String, ByteBuffer, ByteBuffer>> executeSuper(CassandraQuery<K, T> cassandraQuery, String family) {
    String[] columnNames = cassandraQuery.getColumns(family);
    Query<K, T> query = cassandraQuery.getQuery();
    int limit = (int) query.getLimit();
    if (limit < 1) {
      limit = Integer.MAX_VALUE;
    }
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    
    RangeSuperSlicesQuery<K, String, ByteBuffer, ByteBuffer> rangeSuperSlicesQuery = HFactory.createRangeSuperSlicesQuery(this.keyspace, this.keySerializer, StringSerializer.get(), ByteBufferSerializer.get(), ByteBufferSerializer.get());
    rangeSuperSlicesQuery.setColumnFamily(family);    
    rangeSuperSlicesQuery.setKeys(startKey, endKey);
    rangeSuperSlicesQuery.setRange("", "", false, GoraRecordReader.BUFFER_LIMIT_READ_VALUE);
    rangeSuperSlicesQuery.setRowCount(limit);
    rangeSuperSlicesQuery.setColumnNames(columnNames);
    
    
    QueryResult<OrderedSuperRows<K, String, ByteBuffer, ByteBuffer>> queryResult = rangeSuperSlicesQuery.execute();
    OrderedSuperRows<K, String, ByteBuffer, ByteBuffer> orderedRows = queryResult.get();
    return orderedRows.getList();


  }

  /**
   * Obtain Schema/Keyspace name
   * @return Keyspace
   */
  public String getKeyspaceName() {
	return this.cassandraMapping.getKeyspaceName();
  }
}
