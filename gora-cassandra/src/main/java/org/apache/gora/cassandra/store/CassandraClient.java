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

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.FloatSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.OrderedSuperRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.RangeSuperSlicesQuery;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Serializer;

import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.mapreduce.GoraRecordReader;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraClient<K, T extends Persistent> {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);
  
  private Cluster cluster;
  private Keyspace keyspace;
  private Mutator<K> mutator;
  private Class<K> keyClass;
  
  private CassandraMapping cassandraMapping = new CassandraMapping();

  private StringSerializer stringSerializer = new StringSerializer();
  private ByteBufferSerializer byteBufferSerializer = new ByteBufferSerializer();
  private Serializer<K> keySerializer;
  
  public void initialize(Class<K> keyClass) throws Exception {
    this.keyClass = keyClass;
    this.cassandraMapping.loadConfiguration();
    this.cluster = HFactory.getOrCreateCluster(this.cassandraMapping.getClusterName(), new CassandraHostConfigurator(this.cassandraMapping.getHostName()));
    
    // add keyspace to cluster
    checkKeyspace();
    
    // Just create a Keyspace object on the client side, corresponding to an already existing keyspace with already created column families.
    this.keyspace = HFactory.createKeyspace(this.cassandraMapping.getKeyspaceName(), this.cluster);
    
    this.keySerializer = SerializerTypeInferer.getSerializer(keyClass);
    this.mutator = HFactory.createMutator(this.keyspace, this.keySerializer);
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
      keyspaceDefinition = HFactory.createKeyspaceDefinition(this.cassandraMapping.getKeyspaceName(), "org.apache.cassandra.locator.SimpleStrategy", 1, columnFamilyDefinitions);      
      this.cluster.addKeyspace(keyspaceDefinition, true);
      LOG.info("Keyspace '" + this.cassandraMapping.getKeyspaceName() + "' in cluster '" + this.cassandraMapping.getClusterName() + "' was created on host '" + this.cassandraMapping.getHostName() + "'");
      
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

    ByteBuffer byteBuffer = null;
    if (value instanceof ByteBuffer) {
      byteBuffer = (ByteBuffer) value;
    }
    else if (value instanceof Utf8) {
      byteBuffer = stringSerializer.toByteBuffer(((Utf8)value).toString());
    }
    else if (value instanceof Float) {
      // workaround for hector-core-1.0-1.jar
      // because SerializerTypeInferer.getSerializer(Float ) returns ObjectSerializer !?
      byteBuffer = FloatSerializer.get().toByteBuffer((Float)value);
    }
    else if (value instanceof Double) {
      // workaround for hector-core-1.0-1.jar
      // because SerializerTypeInferer.getSerializer(Double ) returns ObjectSerializer !?
      byteBuffer = DoubleSerializer.get().toByteBuffer((Double)value);
    }
    else {
      byteBuffer = SerializerTypeInferer.getSerializer(value).toByteBuffer(value);
    }
    
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String columnName = this.cassandraMapping.getColumn(fieldName);
    
    this.mutator.insert(key, columnFamily, HFactory.createColumn(columnName, byteBuffer, stringSerializer, byteBufferSerializer));
  }

  /**
   * Insert a member in a super column. This is used for map and record Avro types.
   * @param key the row key
   * @param fieldName the field name
   * @param memberName the member name
   * @param value the member value
   */
  @SuppressWarnings("unchecked")
public void addSubColumn(K key, String fieldName, String memberName, Object value) {
    if (value == null) {
      return;
    }
    
    ByteBuffer byteBuffer = null;
    if (value instanceof ByteBuffer) {
      byteBuffer = (ByteBuffer) value;
    }
    else if (value instanceof Utf8) {
      byteBuffer = stringSerializer.toByteBuffer(((Utf8)value).toString());
    }
    else if (value instanceof Float) {
      // workaround for hector-core-1.0-1.jar
      // because SerializerTypeInferer.getSerializer(Float ) returns ObjectSerializer !?
      byteBuffer = FloatSerializer.get().toByteBuffer((Float)value);
    }
    else if (value instanceof Double) {
      // workaround for hector-core-1.0-1.jar
      // because SerializerTypeInferer.getSerializer(Double ) returns ObjectSerializer !?
      byteBuffer = DoubleSerializer.get().toByteBuffer((Double)value);
    }
    else {
      byteBuffer = SerializerTypeInferer.getSerializer(value).toByteBuffer(value);
    }
    
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    
    this.mutator.insert(key, columnFamily, HFactory.createSuperColumn(superColumnName, Arrays.asList(HFactory.createColumn(memberName, byteBuffer, stringSerializer, byteBufferSerializer)), this.stringSerializer, this.stringSerializer, this.byteBufferSerializer));
    
  }
  
  /**
   * Select a family column in the keyspace.
   * @param cassandraQuery a wrapper of the query
   * @param family the family name to be queried
   * @return a list of family rows
   */
  public List<Row<K, String, ByteBuffer>> execute(CassandraQuery<K, T> cassandraQuery, String family) {
    
    String[] columnNames = cassandraQuery.getColumns(family);
    Query<K, T> query = cassandraQuery.getQuery();
    int limit = (int) query.getLimit();
    if (limit < 1) {
      limit = Integer.MAX_VALUE;
    }
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    
    RangeSlicesQuery<K, String, ByteBuffer> rangeSlicesQuery = HFactory.createRangeSlicesQuery(this.keyspace, this.keySerializer, stringSerializer, byteBufferSerializer);
    rangeSlicesQuery.setColumnFamily(family);
    rangeSlicesQuery.setKeys(startKey, endKey);
    rangeSlicesQuery.setRange("", "", false, GoraRecordReader.BUFFER_LIMIT_READ_VALUE);
    rangeSlicesQuery.setRowCount(limit);
    rangeSlicesQuery.setColumnNames(columnNames);
    

    QueryResult<OrderedRows<K, String, ByteBuffer>> queryResult = rangeSlicesQuery.execute();
    OrderedRows<K, String, ByteBuffer> orderedRows = queryResult.get();
    
    
    return orderedRows.getList();
  }

  /**
   * Select the families that contain at least one column mapped to a query field.
   * @param query indicates the columns to select
   * @return a map which keys are the family names and values the corresponding column names required to get all the query fields.
   */
  public Map<String, List<String>> getFamilyMap(Query<K, T> query) {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    for (String field: query.getFields()) {
      String family = this.cassandraMapping.getFamily(field);
      String column = this.cassandraMapping.getColumn(field);
      
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
   * Select the field names according to the column names, which format if fully qualified: "family:column"
   * @param query
   * @return a map which keys are the fully qualified column names and values the query fields
   */
  public Map<String, String> getReverseMap(Query<K, T> query) {
    Map<String, String> map = new HashMap<String, String>();
    for (String field: query.getFields()) {
      String family = this.cassandraMapping.getFamily(field);
      String column = this.cassandraMapping.getColumn(field);
      
      map.put(family + ":" + column, field);
    }
    
    return map;
     
  }

  public boolean isSuper(String family) {
    return this.cassandraMapping.isSuper(family);
  }

  public List<SuperRow<K, String, String, ByteBuffer>> executeSuper(CassandraQuery<K, T> cassandraQuery, String family) {
    String[] columnNames = cassandraQuery.getColumns(family);
    Query<K, T> query = cassandraQuery.getQuery();
    int limit = (int) query.getLimit();
    if (limit < 1) {
      limit = Integer.MAX_VALUE;
    }
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    
    RangeSuperSlicesQuery<K, String, String, ByteBuffer> rangeSuperSlicesQuery = HFactory.createRangeSuperSlicesQuery(this.keyspace, this.keySerializer, this.stringSerializer, this.stringSerializer, this.byteBufferSerializer);
    rangeSuperSlicesQuery.setColumnFamily(family);    
    rangeSuperSlicesQuery.setKeys(startKey, endKey);
    rangeSuperSlicesQuery.setRange("", "", false, GoraRecordReader.BUFFER_LIMIT_READ_VALUE);
    rangeSuperSlicesQuery.setRowCount(limit);
    rangeSuperSlicesQuery.setColumnNames(columnNames);
    
    
    QueryResult<OrderedSuperRows<K, String, String, ByteBuffer>> queryResult = rangeSuperSlicesQuery.execute();
    OrderedSuperRows<K, String, String, ByteBuffer> orderedRows = queryResult.get();
    return orderedRows.getList();


  }
}
