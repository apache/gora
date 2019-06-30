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
package org.apache.gora.benchmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;
import com.yahoo.ycsb.workloads.CoreWorkload;

import generated.User;


/**
 * @author sc306
 * This class extends the Yahoo! Cloud Service Benchmark benchmark {@link #com.yahoo.ycsb.DB DB} class to provide functionality 
 * for {@link #insert(String, String, HashMap) insert}, {@link #read(String, String, Set, HashMap) read}, {@link #scan(String, String, int, Set, Vector) scan}
 * and {@link #update(String, String, HashMap) update} methods as per Apache Gora implementation.
 *
 */
public class GoraBenchmarkClient extends DB {
  
  private static final Logger LOG = LoggerFactory.getLogger(GoraBenchmarkClient.class);
  
  private static final int SUCCESS = 0;
  
  private static final int FAILED = 1;
  
  private static final String FIELDS[] = User._ALL_FIELDS;
  
  private static volatile boolean executed;
  
  public static  int fieldCount;
  
  /**This is only for set to array conversion in {@link read()} method*/
  private String[] DUMMY_ARRAY = new String[0];
  
  DataStore<String, User> dataStore;
  
  GoraBenchmarkUtils goraBenchmarkUtils = new GoraBenchmarkUtils();
  
  User user = new User();
  
  private Properties prop;

  public GoraBenchmarkClient() {}
 

  /* (non-Javadoc)
   * @see com.yahoo.ycsb.DB#init()
   */
  public void init() throws DBException {
    try {
      
        //Get YCSB properties
        prop = getProperties();
        
        fieldCount = Integer.parseInt(prop.getProperty(CoreWorkload.FIELD_COUNT_PROPERTY, CoreWorkload.FIELD_COUNT_PROPERTY_DEFAULT));
        
        String keyClass = prop.getProperty("key.class", "java.lang.String");
        String persistentClass = prop.getProperty("persistent.class", "generated.User");
        Properties p = DataStoreFactory.createProps();
        dataStore = DataStoreFactory.getDataStore(keyClass, persistentClass, p, new Configuration());
        synchronized(GoraBenchmarkClient.class) {
          if(executed)
            return;
          executed = true;
          goraBenchmarkUtils.generateAvroSchema(fieldCount);
          String dataStoreName = goraBenchmarkUtils.getDataStore(p);
          goraBenchmarkUtils.generateMappingFile(dataStoreName);
          goraBenchmarkUtils.generateDataBeans();
        }
        
        
    } catch (GoraException e) {
      //e.printStackTrace();
    }
  }

  /**
   * Cleanup any state for this DB.
   * 
   * It is very important to close the datastore properly, otherwise some data
   * loss might occur.
   */
  public void cleanup() throws DBException {
    synchronized (GoraBenchmarkClient.class) {
      if (dataStore != null)
        dataStore.close();
    }

  }
  
  /**
   * Delete a record from the database.
   *
   * @param table The name of the table
   * @param key The recordallSetMethods key of the record to delete.
   * @return The result of the operation.
   */
  @Override
  public int delete(String table, String key) {
    try {
      //LOG.info("Deleting Key "+key+" From Table "+table);
      dataStore.delete(key);
    }catch(Exception e){
      
      return FAILED;
    }
    return SUCCESS;
  }

  
  /**
   * Insert a record in the selected Gora database. 
   * Any field/value pairs in the specified values HashMap will be written into the
   * record with the specified record key.
   *
   * @param table The name of the table"field"+i
   * @param key The record key of the record to insert.
   * @param values A HashMap of field/value pairs to insert in the record. Each HashMap will have a 
   * @return The result of the operation.
   */
  @Override
  public int insert(String table, String key, HashMap<String, ByteIterator> values) {
    // TODO Auto-generated method stub
    try {
      user.setUserId(key);
      for (int i = 0; i < fieldCount; i++) {
          String field = FIELDS[i+1];
          int fieldIndex = i+1;
          String fieldValue = values.get(field).toString();
          user.put(fieldIndex, fieldValue);
          user.setDirty(fieldIndex);
      }
      dataStore.put(key, user);
    } catch (Exception e) {
      //e.printStackTrace();
      return FAILED;
    }
    return SUCCESS;
  }
  
  /**
   * Read a record from the database. Each field/value pair from the result will be stored in a HashMap.
   *
   * @param table The name of the table
   * @param key The record key of the record to read.
   * @param fields The list of fields to read, or null for all of them
   * @param result A HashMap oftestInsert field/value pairs for the result
   * @return The result of the operation.
   */
  @Override
  public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
    try {
      //check for null is necessary. 
      User user = (fields == null || fields.size() == 0 ) ? dataStore.get(key) : dataStore.get(key, fields.toArray(DUMMY_ARRAY));
      for (int i = 0; i < fieldCount; i++) {
        String field = FIELDS[i+1];
        int fieldIndex = i + 1;
        String value = user.get(fieldIndex).toString();
        result.put(field, new StringByteIterator(value));
      }
    } catch (Exception e) {
     return FAILED;
    }
    return SUCCESS;
  }

  @Override
  /**
   * Perform a range scan for a set of records in the database. Each field/value pair from the result will be stored
   * in a HashMap.
   *
   * @param table The name of the table
   * @param startkey The record key of the first record to read.
   * @param recordcount The number of records to read
   * @param fields The list of fields to read, or null for all of them
   * @param result A Vector of HashMaps, where each HashMap is a set field/value pairs for one record
   * @return The result of the operation.
   */
  public int scan(String table, String startKey, int recordCount, Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result) {
    try {
      Query<String, User> goraQuery = dataStore.newQuery();
      goraQuery.setStartKey(startKey);
      goraQuery.setLimit(recordCount);
      Result<String, User> resultSet = goraQuery.execute();
      while(resultSet.next()) {
        User user = resultSet.get();
        HashMap<String, ByteIterator> hm = new HashMap<>();
        for (int i = 0; i < fieldCount; i++) {
          String field = FIELDS[i+1];
          int fieldIndex = i + 1;
          String value = user.get(fieldIndex).toString();
          hm.put(field, new StringByteIterator(value));
        }
        result.add(hm);
      }
    } catch (Exception e) {
      
      return FAILED;
    }
    return SUCCESS;
  }
  
  /**
   * Update a record in the database. Any field/value pairs in the specified values HashMap will be written into the
   * record with the specified record key, overwriting any existing values with the same field name.
   *
   * @param table The name of the table
   * @param key The record key of the record to write.
   * @param values A HashMap of field/value pairs to update in the record
   * @return The result of the operation.
   */
  @Override
  public int update(String table, String key, HashMap<String, ByteIterator> values) {
    try {
      //We first get the database object to update
      User user = dataStore.get(key);
      List<String> allFields = Arrays.asList(FIELDS);
      for(String field: values.keySet()) {
        if(GoraBenchmarkUtils.isFieldUpdatable(field, values)) {
          /**Get the index of the field from the global fields array and call the right method*/
          int indexOfFieldInArray = allFields.indexOf(field);
          user.put(indexOfFieldInArray, values.get(field).toString());
          user.setDirty(indexOfFieldInArray);
        }
      }
      dataStore.put(user.getUserId().toString(), user);
      //dataStore.flush();
    } catch (Exception e) {
      
      return FAILED;
    }
    return SUCCESS;
  }

}
