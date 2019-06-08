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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
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
  
  public static String fieldCount;
  
  DataStore<String, User> dataStore;
  
  private Properties prop;

  public GoraBenchmarkClient() {}

  public void init() throws DBException {
    try {
      //Get YCSB properties
      prop = getProperties();
      //I planned to make this generic, so that users can define their own key and persistence class. 
      //However this will require the user to generate corresponding AVRO and mapping files. So I think 
      //it may be best to leave because that will have less effect on the benchmark results.
      String keyClass = prop.getProperty("key.class", "java.lang.String");
      String persistentClass = prop.getProperty("persistent.class", "generated.User");
      Properties p = DataStoreFactory.createProps();
      fieldCount = prop.getProperty(CoreWorkload.FIELD_COUNT_PROPERTY, CoreWorkload.FIELD_COUNT_PROPERTY_DEFAULT);
      dataStore = DataStoreFactory.getDataStore(keyClass, persistentClass, p, new Configuration());
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
    if (dataStore != null)
      dataStore.close();
  }
  
  /**
   * Delete a record from the database.
   *
   * @param table The name of the table
   * @param key The record key of the record to delete.
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
   * @param table The name of the table
   * @param key The record key of the record to insert.
   * @param values A HashMap of field/value pairs to insert in the record. Each HashMap will have a 
   * @return The result of the operation.
   */
  @Override
  public int insert(String table, String key, HashMap<String, ByteIterator> values) {
    // TODO Auto-generated method stub
    try {
      User user = new User();
      user.setUserId(key);
      //Another method of getting the fieldCount is to use value.size() since we have field,value structure in each
      //data to be inserted.
      Class<?> clazz = Class.forName(prop.getProperty("persistent.class"));
      for (int i = 0; i < Integer.parseInt(fieldCount); i++) {
        String methodName = "setField"+i;
        //LOG.info(methodName);
        Method m = clazz.getMethod(methodName, CharSequence.class);
        m.invoke(user, values.get("field"+i).toString());
      }
      dataStore.put(user.getUserId().toString(), user);
      //dataStore.flush();
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
   * @param result A HashMap of field/value pairs for the result
   * @return The result of the operation.
   */
  @Override
  public int read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
    try {
      //check for null is necessary. 
      User user = (fields == null || fields.size() == 0 ) ? dataStore.get(key) : dataStore.get(key, fields.toArray(new String[fields.size()]));
      Class<?> clazz = Class.forName(prop.getProperty("persistent.class"));
      for (int i = 0; i < Integer.parseInt(fieldCount); i++) {
        String methodName = "getField"+i;
        //LOG.info(methodName);
        Method m = clazz.getMethod(methodName);
        String value = m.invoke(user).toString();
        result.put("field"+i, new StringByteIterator(value));
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
      Class<?> clazz = Class.forName(prop.getProperty("persistent.class"));
      //LOG.info(resultSet.size()+"Scan results returned");
      while(resultSet.next()) {
        User user = resultSet.get();
        HashMap<String, ByteIterator> hm = new HashMap<>();
        for (int i = 0; i < Integer.parseInt(fieldCount); i++) {
          String methodName = "getField"+i;
          //LOG.info(methodName);
          Method m = clazz.getMethod(methodName);
            String value = m.invoke(user).toString();
            hm.put("field"+i, new StringByteIterator(value));
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
      Class<?> clazz = Class.forName(prop.getProperty("persistent.class"));
      for(String field: values.keySet()) {
        if(GoraBenchmarkUtils.isFieldUpdatable(field, values)) {
          String methodName = "set"+field.substring(0,1).toUpperCase(Locale.ROOT)+field.substring(1);
          Method m = clazz.getMethod(methodName, CharSequence.class);
          m.invoke(user, values.get(field).toString());
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
