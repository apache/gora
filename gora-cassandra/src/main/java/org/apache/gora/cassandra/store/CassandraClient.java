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

import static org.apache.gora.cassandra.store.CassandraStore.colFamConsLvl;
import static org.apache.gora.cassandra.store.CassandraStore.readOpConsLvl;
import static org.apache.gora.cassandra.store.CassandraStore.writeOpConsLvl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.serializers.GoraSerializerTypeInferer;
import org.apache.gora.mapreduce.GoraRecordReader;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CassandraClient is where all of the primary datastore functionality is 
 * executed. Typically CassandraClient is invoked by calling 
 * {@link org.apache.gora.cassandra.store.CassandraStore#initialize(Class, Class, Properties)}.
 * CassandraClient deals with Cassandra data model definition, mutation, 
 * and general/specific mappings.
 * @see {@link org.apache.gora.cassandra.store.CassandraStore#initialize(Class, Class, Properties)} 
 *
 * @param <K>
 * @param <T>
 */
public class CassandraClient<K, T extends PersistentBase> {
  
  /** The logging implementation */
  public static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);
  
  private Cluster cluster;
  private Keyspace keyspace;
  private Mutator<K> mutator;
  private Class<K> keyClass;
  private Class<T> persistentClass;
  
  /** Object which holds the XML mapping for Cassandra. */
  private CassandraMapping cassandraMapping = null;

  /** Hector client default column family consistency level. */
  public static final String DEFAULT_HECTOR_CONSIS_LEVEL = "QUORUM";
  
  /** Cassandra serializer to be used for serializing Gora's keys. */
  private Serializer<K> keySerializer;
  
  /**
   * Method to maintain backward compatibility with earlier versions. 
  */
  public void initialize(Class<K> keyClass, Class<T> persistentClass)
    throws Exception {
	initialize(keyClass, persistentClass, null);
  }
  
  /**
   * Given our key, persistentClass from 
   * {@link org.apache.gora.cassandra.store.CassandraStore#initialize(Class, Class, Properties)}
   * we make best efforts to dictate our data model. 
   * We make a quick check within {@link org.apache.gora.cassandra.store.CassandraClient#checkKeyspace(String)
   * to see if our keyspace has already been invented, this simple check prevents us from 
   * recreating the keyspace if it already exists. 
   * We then simple specify (based on the input keyclass) an appropriate serializer
   * via {@link org.apache.gora.cassandra.serializers.GoraSerializerTypeInferer} before
   * defining a mutator from and by which we can mutate this object.
   * @param keyClass the Key by which we wish o assign a record object
   * @param persistentClass the generated {@link org.apache.org.gora.persistency.Peristent} bean representing the data.
   * @param properties key value pairs from gora.properties
   * @throws Exception
   */
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws Exception {
    this.keyClass = keyClass;

    // get cassandra mapping with persistent class
    this.persistentClass = persistentClass;
    this.cassandraMapping = CassandraMappingManager.getManager().get(persistentClass);
	Map<String, String> accessMap = null;
	if (properties != null) {
		String username = properties
				.getProperty("gora.cassandrastore.username");
		if (username != null) {
			accessMap = new HashMap<String, String>();
			accessMap.put("username", username);
			String password = properties
					.getProperty("gora.cassandrastore.password");
			if (password != null) {
				accessMap.put("password", password);
			}
		}
	}

    this.cluster = HFactory.getOrCreateCluster(this.cassandraMapping.getClusterName(), 
        new CassandraHostConfigurator(this.cassandraMapping.getHostName()), accessMap);
    
    // add keyspace to cluster
    checkKeyspace();
    
    // Just create a Keyspace object on the client side, corresponding to an already 
    // existing keyspace with already created column families.
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
   * In this method, we also utilize Hector's 
   * {@link me.prettyprint.cassandra.model.ConfigurableConsistencyLevel} logic. 
   * It is set by passing a 
   * {@link me.prettyprint.cassandra.model.ConfigurableConsistencyLevel} object right 
   * when the {@link me.prettyprint.hector.api.Keyspace} is created. 
   * If we cannot find a consistency level within <code>gora.properites</code>, 
   * then column family consistency level is set to QUORUM (by default) which permits 
   * consistency to wait for a quorum of replicas to respond regardless of data center.
   * QUORUM is Hector Client's default setting and we respect that here as well.
   * 
   * @see http://hector-client.github.io/hector/build/html/content/consistency_level.html
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

      keyspaceDefinition = HFactory.createKeyspaceDefinition(
        this.cassandraMapping.getKeyspaceName(), 
        this.cassandraMapping.getKeyspaceReplicationStrategy(),
        this.cassandraMapping.getKeyspaceReplicationFactor(),
        columnFamilyDefinitions
      );
      
      this.cluster.addKeyspace(keyspaceDefinition, true);
      
      // GORA-167 Create a customized Consistency Level
      ConfigurableConsistencyLevel ccl = new ConfigurableConsistencyLevel();
      Map<String, HConsistencyLevel> clmap = getConsisLevelForColFams(columnFamilyDefinitions);
      // Column family consistency levels
      ccl.setReadCfConsistencyLevels(clmap);
      ccl.setWriteCfConsistencyLevels(clmap);
      // Operations consistency levels
      String opConsisLvl = (readOpConsLvl!=null || !readOpConsLvl.isEmpty())?readOpConsLvl:DEFAULT_HECTOR_CONSIS_LEVEL;
      ccl.setDefaultReadConsistencyLevel(HConsistencyLevel.valueOf(opConsisLvl));
      LOG.debug("Hector read consistency configured to '" + opConsisLvl + "'.");
      opConsisLvl = (writeOpConsLvl!=null || !writeOpConsLvl.isEmpty())?writeOpConsLvl:DEFAULT_HECTOR_CONSIS_LEVEL;
      ccl.setDefaultWriteConsistencyLevel(HConsistencyLevel.valueOf(opConsisLvl));
      LOG.debug("Hector write consistency configured to '" + opConsisLvl + "'.");

      HFactory.createKeyspace("Keyspace", this.cluster, ccl);
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
            LOG.debug("The comparator type of " + cfDef.getName() + " column family is " 
              + comparatorType.getTypeName() + ".");
          }
        }
      }
    }
  }

  /**
   * Method in charge of setting the consistency level for defined column families.
   * @param pColFams  Column families
   * @return Map<String, HConsistencyLevel> with the mapping between colFams and consistency level.
   */
  private Map<String, HConsistencyLevel> getConsisLevelForColFams(List<ColumnFamilyDefinition> pColFams) {
    Map<String, HConsistencyLevel> clMap = new HashMap<String, HConsistencyLevel>();
    // Get columnFamily consistency level.
    String colFamConsisLvl = (colFamConsLvl != null && !colFamConsLvl.isEmpty())?colFamConsLvl:DEFAULT_HECTOR_CONSIS_LEVEL;
    LOG.debug("ColumnFamily consistency level configured to '" + colFamConsisLvl + "'.");
    // Define consistency for ColumnFamily "ColumnFamily"
    for (ColumnFamilyDefinition colFamDef : pColFams)
      clMap.put(colFamDef.getName(), HConsistencyLevel.valueOf(colFamConsisLvl));
    return clMap;
  }
  
  /**
   * Drop keyspace.
   */
  public void dropKeyspace() {
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
    	LOG.debug( "field:"+fieldName+", its value is null.");
      return;
    }

    ByteBuffer byteBuffer = toByteBuffer(value);
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String columnName = this.cassandraMapping.getColumn(fieldName);
    
    if (columnName == null) {
    	LOG.warn("Column name is null for field: " + fieldName );
        return;
    }
      
    if( LOG.isDebugEnabled() ) LOG.debug( "fieldName: "+fieldName +" columnName: " + columnName );
    
    String ttlAttr = this.cassandraMapping.getColumnsAttribs().get(columnName);
    
    if ( null == ttlAttr ){
    	ttlAttr = CassandraMapping.DEFAULT_COLUMNS_TTL;
    	if( LOG.isDebugEnabled() ) LOG.debug( "ttl was not set for field: " + fieldName + ". Using " + ttlAttr );
    } else {
    	if( LOG.isDebugEnabled() ) LOG.debug( "ttl for field: " + fieldName + " is " + ttlAttr );
    }

    synchronized(mutator) {
      HectorUtils.insertColumn(mutator, key, columnFamily, columnName, byteBuffer, ttlAttr);
    }
  }

  /**
   * Delete a row within the keyspace.
   * @param key
   * @param fieldName
   * @param columnName
   */
  public void deleteColumn(K key, String familyName, ByteBuffer columnName) {
    synchronized(mutator) {
      HectorUtils.deleteColumn(mutator, key, familyName, columnName);
    }
  }

  /**
   * Deletes an entry based on its key.
   * @param key
   */
  public void deleteByKey(K key) {
    Map<String, String> familyMap = this.cassandraMapping.getFamilyMap();
    deleteColumn(key, familyMap.values().iterator().next().toString(), null);
  }

  /**
   * Insert a member in a super column. This is used for map and record Avro types.
   * @param key the row key
   * @param fieldName the field name
   * @param columnName the column name (the member name, or the index of array)
   * @param value the member value
   */
  public void addSubColumn(K key, String fieldName, ByteBuffer columnName, Object value) {
    if (value == null) {
      return;
    }

    ByteBuffer byteBuffer = toByteBuffer(value);
    
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    String ttlAttr = this.cassandraMapping.getColumnsAttribs().get(superColumnName);
    if ( null == ttlAttr ) {
      ttlAttr = CassandraMapping.DEFAULT_COLUMNS_TTL;
      if( LOG.isDebugEnabled() ) LOG.debug( "ttl was not set for field:" + fieldName + " .Using " + ttlAttr );
    } else {
      if( LOG.isDebugEnabled() ) LOG.debug( "ttl for field:" + fieldName + " is " + ttlAttr );
    }

    synchronized(mutator) {
      HectorUtils.insertSubColumn(mutator, key, columnFamily, superColumnName, columnName, byteBuffer, ttlAttr);
    }
  }

  /**
   * Adds an subColumn inside the cassandraMapping file when a String is serialized
   * @param key the row key
   * @param fieldName the field name
   * @param columnName the column name (the member name, or the index of array)
   * @param value the member value
   */
  public void addSubColumn(K key, String fieldName, String columnName, Object value) {
    addSubColumn(key, fieldName, StringSerializer.get().toByteBuffer(columnName), value);
  }

  /**
   * Adds an subColumn inside the cassandraMapping file when an Integer is serialized
   * @param key the row key
   * @param fieldName the field name
   * @param columnName the column name (the member name, or the index of array)
   * @param value the member value
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
  public void deleteSubColumn(K key, String fieldName, ByteBuffer columnName) {

    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    
    synchronized(mutator) {
      HectorUtils.deleteSubColumn(mutator, key, columnFamily, superColumnName, columnName);
    }
  }

  /**
   * Deletes a subColumn 
   * @param key
   * @param fieldName
   * @param columnName
   */
  public void deleteSubColumn(K key, String fieldName, String columnName) {
    deleteSubColumn(key, fieldName, StringSerializer.get().toByteBuffer(columnName));
  }

  /**
   * Deletes all subcolumns from a super column.
   * @param key the row key.
   * @param fieldName the field name.
   */
  public void deleteSubColumn(K key, String fieldName) {
    String columnFamily = this.cassandraMapping.getFamily(fieldName);
    String superColumnName = this.cassandraMapping.getColumn(fieldName);
    synchronized(mutator) {
      HectorUtils.deleteSubColumn(mutator, key, columnFamily, superColumnName, null);
    }
  }

  public void deleteGenericArray(K key, String fieldName) {
    //TODO Verify this. Everything that goes inside a genericArray will go inside a column so let's just delete that.
    deleteColumn(key, cassandraMapping.getFamily(fieldName), toByteBuffer(fieldName));
  }
  
  public void addGenericArray(K key, String fieldName, GenericArray<?> array) {
    if (isSuper( cassandraMapping.getFamily(fieldName) )) {
      int i= 0;
      for (Object itemValue: array) {

        // TODO: hack, do not store empty arrays
        if (itemValue instanceof GenericArray<?>) {
          if (((List<?>)itemValue).size() == 0) {
            continue;
          }
        } else if (itemValue instanceof Map<?,?>) {
          if (((Map<?, ?>)itemValue).size() == 0) {
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

  public void deleteStatefulHashMap(K key, String fieldName) {
    if (isSuper( cassandraMapping.getFamily(fieldName) )) {
      deleteSubColumn(key, fieldName);
    } else {
      deleteColumn(key, cassandraMapping.getFamily(fieldName), toByteBuffer(fieldName));
    }
  }

  public void addStatefulHashMap(K key, String fieldName, Map<CharSequence,Object> map) {
    if (isSuper( cassandraMapping.getFamily(fieldName) )) {
      // as we don't know what has changed inside the map or If it's an empty map, then delete its content.
      deleteSubColumn(key, fieldName);
      // update if there is anything to update.
      if (!map.isEmpty()) {
        // If it's not empty, then update its content.
        for (CharSequence mapKey: map.keySet()) {
          // TODO: hack, do not store empty arrays
          Object mapValue = map.get(mapKey);
          if (mapValue instanceof GenericArray<?>) {
            if (((List<?>)mapValue).size() == 0) {
              continue;
            }
          } else if (mapValue instanceof Map<?,?>) {
            if (((Map<?, ?>)mapValue).size() == 0) {
              continue;
            }
          }
          addSubColumn(key, fieldName, mapKey.toString(), mapValue);
        }
      }
    }
    else {
      addColumn(key, fieldName, map);
    }
  }

  /**
   * Serialize value to ByteBuffer using 
   * {@link org.apache.gora.cassandra.serializers.GoraSerializerTypeInferer#getSerializer(Object)}.
   * @param value the member value {@link java.lang.Object}.
   * @return ByteBuffer object
   */
  public ByteBuffer toByteBuffer(Object value) {
    ByteBuffer byteBuffer = null;
    Serializer<Object> serializer = GoraSerializerTypeInferer.getSerializer(value);
    if (serializer == null) {
      LOG.warn("Serializer not found for: " + value.toString());
    }
    else {
      LOG.debug(serializer.getClass() + " selected as appropriate Serializer.");
      byteBuffer = serializer.toByteBuffer(value);
    }
    if (byteBuffer == null) {
      LOG.warn("Serialization value for: " + value.getClass().getName() + " = null");
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
    
    RangeSlicesQuery<K, ByteBuffer, ByteBuffer> rangeSlicesQuery = HFactory.createRangeSlicesQuery
        (this.keyspace, this.keySerializer, ByteBufferSerializer.get(), ByteBufferSerializer.get());
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
    // checking if it was a UNION field the one we are retrieving
    if (pField.indexOf(CassandraStore.UNION_COL_SUFIX) > 0)
      family = this.cassandraMapping.getFamily(pField.substring(0,pField.indexOf(CassandraStore.UNION_COL_SUFIX)));
    else
      family = this.cassandraMapping.getFamily(pField);
     return family;
   }
 
  private String getMappingColumn(String pField){
    String column = null;
    if (pField.indexOf(CassandraStore.UNION_COL_SUFIX) > 0)
      column = pField;
    else
      column = this.cassandraMapping.getColumn(pField);
      return column;
    }

  /**
   * Select the families that contain at least one column mapped to a query field.
   * @param query indicates the columns to select
   * @return a map which keys are the family names and values the 
   * corresponding column names required to get all the query fields.
   */
  public Map<String, List<String>> getFamilyMap(Query<K, T> query) {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    Schema persistentSchema = query.getDataStore().newPersistent().getSchema();
    for (String field: query.getFields()) {
      String family = this.getMappingFamily(field);
      String column = this.getMappingColumn(field);
      
      // check if the family value was already initialized 
      List<String> list = map.get(family);
      if (list == null) {
        list = new ArrayList<String>();
        map.put(family, list);
      }
      if (persistentSchema.getField(field).schema().getType() == Type.UNION)
        list.add(field + CassandraStore.UNION_COL_SUFIX);
      if (column != null) {
        list.add(column);
      }
    }
    
    return map;
  }

  /**
   * Retrieves the cassandraMapping which holds whatever was mapped 
   * from the gora-cassandra-mapping.xml
   * @return 
   */
  public CassandraMapping getCassandraMapping(){
    return this.cassandraMapping;
  }
  
  /**
   * Select the field names according to the column names, which format 
   * if fully qualified: "family:column"
   * @param query
   * @return a map which keys are the fully qualified column 
   * names and values the query fields
   */
  public Map<String, String> getReverseMap(Query<K, T> query) {
    Map<String, String> map = new HashMap<String, String>();
    Schema persistentSchema = query.getDataStore().newPersistent().getSchema();
    for (String field: query.getFields()) {
      String family = this.getMappingFamily(field);
      String column = this.getMappingColumn(field);
      if (persistentSchema.getField(field).schema().getType() == Type.UNION)
        map.put(family + ":" + field + CassandraStore.UNION_COL_SUFIX, field + CassandraStore.UNION_COL_SUFIX);
      map.put(family + ":" + column, field);
    }
    
    return map;
  }

  /**
   * Determines if a column is a superColumn or not.
   * @param family
   * @return boolean
   */
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
    
    RangeSuperSlicesQuery<K, String, ByteBuffer, ByteBuffer> rangeSuperSlicesQuery = HFactory.createRangeSuperSlicesQuery
        (this.keyspace, this.keySerializer, StringSerializer.get(), ByteBufferSerializer.get(), ByteBufferSerializer.get());
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
