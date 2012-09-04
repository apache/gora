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

package org.apache.gora;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;

/**
 * GoraTestDriver is a helper class for third party tests and should
 * be used to initialize and tear down mini clusters (such as mini HBase 
 * or Cassandra cluster, local Hsqldb instance, etc) so that these 
 * details are abstracted away.
 */
public class GoraTestDriver {

  protected static final Logger log = LoggerFactory.getLogger(GoraTestDriver.class);

  protected Class<? extends DataStore> dataStoreClass;
  protected Configuration conf = new Configuration();

  @SuppressWarnings("rawtypes")
  protected HashSet<DataStore> dataStores;

  @SuppressWarnings("rawtypes")
  protected GoraTestDriver(Class<? extends DataStore> dataStoreClass) {
    this.dataStoreClass = dataStoreClass;
    this.dataStores = new HashSet<DataStore>();
  }

  /** Should be called once before the tests are started, probably in the
   * method annotated with org.junit.BeforeClass
   */
  public void setUpClass() throws Exception {
    setProperties(DataStoreFactory.createProps());
  }

  /** Should be called once after the tests have finished, probably in the
   * method annotated with org.junit.AfterClass
   */
  public void tearDownClass() throws Exception {

  }

  /** Should be called once before each test, probably in the
   * method annotated with org.junit.Before
   */
  public void setUp() throws Exception {
    log.info("setting up test");
    try {
      for(DataStore store : dataStores) {
        store.truncateSchema();
      }
    }catch (IOException ignore) {
    }
  }
    
  /** Should be called once after each test, probably in the
   * method annotated with org.junit.After
   */
  @SuppressWarnings("rawtypes")
  public void tearDown() throws Exception {
    log.info("tearing down test");
    //delete everything
    for(DataStore store : dataStores) {
      try {
        //store.flush();
        store.deleteSchema();
        store.close();
      }catch (Exception ignore) {
      }
    }
    dataStores.clear();
  }

  protected void setProperties(Properties properties) {
  }

  @SuppressWarnings("unchecked")
  public<K, T extends Persistent> DataStore<K,T>
    createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    setProperties(DataStoreFactory.createProps());
    DataStore<K,T> dataStore = DataStoreFactory.createDataStore(
        (Class<? extends DataStore<K,T>>)dataStoreClass, keyClass, persistentClass, conf);
    dataStores.add(dataStore);

    return dataStore;
  }
  
  public Class<?> getDataStoreClass() {
    return dataStoreClass;
  }
}
