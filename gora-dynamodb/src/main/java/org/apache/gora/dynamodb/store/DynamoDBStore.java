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

package org.apache.gora.dynamodb.store;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.dynamodb.query.DynamoDBResult;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.ws.impl.PartitionWSQueryImpl;
import org.apache.gora.store.ws.impl.WSDataStoreBase;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.QueryResult;

public class DynamoDBStore<K, T extends Persistent> extends WSDataStoreBase<K, T> {
	
  //public static final Logger LOG = LoggerFactory.getLogger(DynamoDBStore.class);
  
  /**
   * Path where the AWS Credential will reside
   */
  private static String awsCredentialsProperties = "conf/AwsCredentials.properties";
  
  private static String wsProvider = "Amazon.Web.Services";
	 
  /**
   * TODO Amazon DynamoDB decorator 
   * because all DynamoDBAsyncClients are DynamoDBClients   
   */
  private AmazonDynamoDBClient dynamoDBClient;

  public DynamoDBStore(){
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
	      Properties properties) throws Exception {
	  try {
		File file = new File(awsCredentialsProperties);
		AWSCredentials credentials = new PropertiesCredentials(file);
		setConf(credentials);
		setWsProvider(wsProvider);
		//TODO Create decorator to create different AmazonDynamoDB clients
		// this is because all amazonDynamoDBAsynClients are AmazonDynamoDBClients
		dynamoDBClient = new AmazonDynamoDBClient(credentials);
		
	  }
	    catch (Exception e) {
	      throw new IOException(e.getMessage(), e);
	  }
  }
  /*
  public void close() throws IOException {
    LOG.debug("close");
    flush();
  }

  @Override
  public void createSchema() {
    LOG.debug("create schema");
    this.cassandraClient.checkKeyspace();
  }

  @Override
  public boolean delete(K key) throws IOException {
    LOG.debug("delete " + key);
    return false;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws IOException {
    LOG.debug("delete by query " + query);
    return 0;
  }

  @Override
  public void deleteSchema() throws IOException {
    LOG.debug("delete schema");
    this.cassandraClient.dropKeyspace();
  }*/

  @Override
  public Result<K, T> execute(Query<K, T> query) throws Exception {
    
	 DynamoDBQuery<K, T> dynamoDBQuery = new DynamoDBQuery<K, T>();
	 DynamoDBResult<K, T> dynamoDBResult = new DynamoDBResult<K, T>(this, dynamoDBQuery);
	 
	 dynamoDBQuery.setQuery(query);
	 
	 // TODO we should return the object that the class is supposed to return
	 QueryResult dynamodbResult = dynamoDBClient.query(dynamoDBQuery.getQuery());
	 
	 return (Result<K, T>)dynamoDBResult;
  }

  /*
  private void addSubColumns(String family, DynamoDBQuery<K, T> dynamoDBQuery,
      CassandraResultSet cassandraResultSet) {
    // select family columns that are included in the query
    List<Row<K, ByteBuffer, ByteBuffer>> rows = this.cassandraClient.execute(dynamoDBQuery, family);
    
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
  */
/*
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
*/
 
  @Override
  public T get(K key, String[] fields) throws Exception {
    DynamoDBQuery<K,T> query = new DynamoDBQuery<K,T>();
    query.setDataStore(this);
    query.setKeyRange(key, key);
    query.setFields(fields);
    query.setLimit(1);
    Result<K,T> result = execute(query);
    boolean hasResult = result.next();
    return hasResult ? result.get() : null;
  }

  public Query<K, T> newQuery() {
    Query<K,T> query = new DynamoDBQuery<K, T>(this);
   // query.setFields(getFieldsToQuery(null));
    return query;
  }

@Override
public String getSchemaName() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void createSchema() throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void deleteSchema() throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public boolean schemaExists() throws Exception {
	// TODO Auto-generated method stub
	return false;
}

@Override
public K newKey() throws Exception {
	// TODO Auto-generated method stub
	return null;
}

@Override
public T newPersistent() throws Exception {
	// TODO Auto-generated method stub
	return null;
}

@Override
public T get(K key) throws Exception {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void put(K key, T obj) throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public boolean delete(K key) throws Exception {
	// TODO Auto-generated method stub
	return false;
}

@Override
public long deleteByQuery(Query<K, T> query) throws Exception {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
		throws IOException {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void flush() throws Exception {
	// TODO Auto-generated method stub
	
}

@Override
public void setBeanFactory(BeanFactory<K, T> beanFactory) {
	// TODO Auto-generated method stub
	
}

@Override
public BeanFactory<K, T> getBeanFactory() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void close() throws IOException, InterruptedException, Exception {
	// TODO Auto-generated method stub
	
}

  /**
   * Duplicate instance to keep all the objects in memory till flushing.
   * @see org.apache.gora.store.DataStore#put(java.lang.Object, org.apache.gora.persistency.Persistent)
   
  @Override
  public void put(K key, T value) throws IOException {
    T p = (T) value.newInstance(new StateManagerImpl());
    Schema schema = value.getSchema();
    for (Field field: schema.getFields()) {
      if (value.isDirty(field.pos())) {
        Object fieldValue = value.get(field.pos());
        
        // check if field has a nested structure (array, map, or record)
        Schema fieldSchema = field.schema();
        Type type = fieldSchema.getType();
        switch(type) {
          case RECORD:
            Persistent persistent = (Persistent) fieldValue;
            Persistent newRecord = persistent.newInstance(new StateManagerImpl());
            for (Field member: fieldSchema.getFields()) {
              newRecord.put(member.pos(), persistent.get(member.pos()));
            }
            fieldValue = newRecord;
            break;
          case MAP:
            StatefulHashMap<?, ?> map = (StatefulHashMap<?, ?>) fieldValue;
            StatefulHashMap<?, ?> newMap = new StatefulHashMap(map);
            fieldValue = newMap;
            break;
          case ARRAY:
            GenericArray array = (GenericArray) fieldValue;
            Type elementType = fieldSchema.getElementType().getType();
            GenericArray newArray = new ListGenericArray(Schema.create(elementType));
            Iterator iter = array.iterator();
            while (iter.hasNext()) {
              newArray.add(iter.next());
            }
            fieldValue = newArray;
            break;
        }
        
        p.put(field.pos(), fieldValue);
      }
    }
    
    // this performs a structural modification of the map
    this.buffer.put(key, p);
 }
*/
  /**
   * Add a field to Cassandra according to its type.
   * @param key     the key of the row where the field should be added
   * @param field   the Avro field representing a datum
   * @param value   the field value
   
  private void addOrUpdateField(K key, Field field, Object value) {
    Schema schema = field.schema();
    Type type = schema.getType();
    switch (type) {
      case STRING:
      case INT:
      case LONG:
      case BYTES:
      case FLOAT:
      case DOUBLE:
        this.cassandraClient.addColumn(key, field.name(), value);
        break;
      case RECORD:
        if (value != null) {
          if (value instanceof PersistentBase) {
            PersistentBase persistentBase = (PersistentBase) value;
            for (Field member: schema.getFields()) {
              
              // TODO: hack, do not store empty arrays
              Object memberValue = persistentBase.get(member.pos());
              if (memberValue instanceof GenericArray<?>) {
                GenericArray<String> array = (GenericArray<String>) memberValue;
                if (array.size() == 0) {
                  continue;
                }
              }
              
              if (memberValue instanceof Utf8) {
                memberValue = memberValue.toString();
              }
              this.cassandraClient.addSubColumn(key, field.name(), StringSerializer.get().toByteBuffer(member.name()), memberValue);
            }
          } else {
            LOG.info("Record not supported: " + value.toString());
            
          }
        }
        break;
      case MAP:
        if (value != null) {
          if (value instanceof StatefulHashMap<?, ?>) {
            //TODO cast to stateful map and only write dirty keys
            Map<Utf8, Object> map = (Map<Utf8, Object>) value;
            for (Utf8 mapKey: map.keySet()) {
              
              // TODO: hack, do not store empty arrays
              Object keyValue = map.get(mapKey);
              if (keyValue instanceof GenericArray<?>) {
                GenericArray<String> array = (GenericArray<String>) keyValue;
                if (array.size() == 0) {
                  continue;
                }
              }
              
              if (keyValue instanceof Utf8) {
                keyValue = keyValue.toString();
              }
              this.cassandraClient.addSubColumn(key, field.name(), StringSerializer.get().toByteBuffer(mapKey.toString()), keyValue);              
            }
          } else {
            LOG.info("Map not supported: " + value.toString());
          }
        }
        break;
      case ARRAY:
        if (value != null) {
          if (value instanceof GenericArray<?>) {
            GenericArray<Object> array = (GenericArray<Object>) value;
            int i= 0;
            for (Object itemValue: array) {
              if (itemValue instanceof Utf8) {
                itemValue = itemValue.toString();
              }
              this.cassandraClient.addSubColumn(key, field.name(), IntegerSerializer.get().toByteBuffer(i++), itemValue);              
            }
          } else {
            LOG.info("Array not supported: " + value.toString());
          }
        }
        break;
      default:
        LOG.info("Type not considered: " + type.name());      
    }
  }
*/
}
