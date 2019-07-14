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

package org.apache.gora.store;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.persistency.Persistent;

/**
 * A base class for {@link DataStore} tests. This is just a convenience
 * class, which actually only uses {@link DataStoreTestUtil} methods to
 * run the tests. Not all test cases can extend this class (like TestHBaseStore),
 * so all test logic should reside in DataStoreTestUtil class.
 * 
 */
public abstract class WSDataStoreTestBase<K, T extends Persistent> {

  public static final Logger log = LoggerFactory.getLogger(WSDataStoreTestBase.class);

  protected static GoraTestDriver testDriver;

  protected DataStore<K, T> dataStore;
  
  private static boolean setUpClassCalled = false;
  
  public Class<K> persistentKeyClass;
  public Class<T> persistentValClass;

  protected abstract DataStore<K,T> createDataStore();
  
  /** junit annoyingly forces BeforeClass to be static, so this method
   * should be called from a static block
   */
  protected static void setTestDriver(GoraTestDriver driver) {
    testDriver = driver;
  }

  public void setPersistentKeyClass(Class<K> pKeyClass){
	  persistentKeyClass = pKeyClass;
  }
  
  public void setPersistentValClass(Class<T> pValClass){
	  persistentValClass = pValClass;
  }
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    if(testDriver != null && !setUpClassCalled) {
      log.info("setting up class");
      testDriver.setUpClass();
      setUpClassCalled = true;
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
	log.info("tearing down class");
    //if(testDriver != null) 
    //  testDriver.tearDownClass();
  }

  @Before
  public void setUp() throws Exception {
    //There is an issue in JUnit 4 tests in Eclipse where TestSqlStore static
    //methods are not called BEFORE setUpClass. I think this is a bug in 
    //JUnitRunner in Eclipse. Below is a workaround for that problem.
    if(!setUpClassCalled) setUpClass();  
    log.info("setting up test");
    if(dataStore == null){
      if(testDriver != null) {
        dataStore = testDriver.createDataStore(persistentKeyClass, persistentValClass);
        testDriver.setUp();
      } else {
        dataStore = createDataStore();
        dataStore.truncateSchema();
      }
    }
  }

  @After
  public void tearDown() throws Exception {
    log.info("tearing down test");
    if(testDriver != null)
      testDriver.tearDown();
  }
  
  @Test
  public void testNewInstance() throws Exception {
    log.info("test method: testNewInstance");
    DataStoreTestUtil.testNewPersistent(dataStore);
  }

  @Test
  public void testCreateSchema() throws Exception {
    log.info("test method: testCreateSchema");
    assertSchemaExists("person");
  }

  // Override this to assert that schema is created correctly
  public void assertSchemaExists(String schemaName) throws Exception {
  }

  @Test
  public void testAutoCreateSchema() throws Exception {
    log.info("test method: testAutoCreateSchema");
    assertAutoCreateSchema();
  }

  public void assertAutoCreateSchema() throws Exception {
    assertSchemaExists("person");
  }

  @Test
  public  void testTruncateSchema() throws Exception {
    log.info("test method: testTruncateSchema");
    assertSchemaExists("person");
  }

  @Test
  public void testDeleteSchema() throws Exception {
    log.info("test method: testDeleteSchema");
    assertDeleteSchema();
  }


  public void assertDeleteSchema(){
  }

  @Test
  public void testSchemaExists() throws Exception {
    log.info("test method: testSchemaExists");
    assertTrue(dataStore.schemaExists());
  }

  @Test
  public void testPut() throws Exception {
    log.info("test method: testPut");
    assertPut();
  }


  public void assertPut() throws IOException {
  }

  @Test
  public void testPutNested() throws Exception {
    log.info("test method: testPutNested");

  }

  @Test
  public void testPutArray() throws Exception {
    log.info("test method: testPutArray");
    assertPutArray();
  }


  public void assertPutArray() throws IOException {
  }

  @Test
  public void testPutBytes() throws Exception {
    log.info("test method: testPutBytes");
    byte[] contentBytes = "example content in example.com".getBytes(Charset.defaultCharset());

    assertPutBytes(contentBytes);
  }

  @Ignore
  public void assertPutBytes(byte[] contentBytes) throws IOException {
  }

  @Test
  public void testPutMap() throws Exception {
    log.info("test method: testPutMap");
    assertPutMap();
  }

  @Ignore
  public void assertPutMap() throws IOException {
  }

  @Test
  public void testUpdate() throws Exception {
    log.info("test method: testUpdate");
    assertTestUpdateDataStore();
  }

  @Ignore
  public void assertTestUpdateDataStore(){
  }

  @Ignore
  @Test
  public void testEmptyUpdate() throws Exception {
  }

  @Test
  public void testGet() throws Exception {
    log.info("test method: testGet");
    assertTestGetDataStore();
  }

  @Ignore
  public void assertTestGetDataStore() throws IOException {
  }
  
  @Test
  public void testGetWithFields() throws Exception {
    log.info("test method: testGetWithFields");
  }

 @Test
  public void testQuery() throws Exception {
    log.info("test method: testQuery");
    assertTestQueryDataStore();
  }

  @Ignore
  public void assertTestQueryDataStore() throws IOException {
  }

  @Test
  public void testQueryStartKey() throws Exception {
    log.info("test method: testQueryStartKey");
  }

  @Test
  public void testQueryEndKey() throws Exception {
    log.info("test method: testQueryEndKey");
  }

  @Test
  public void testQueryKeyRange() throws Exception {
    log.info("test method: testQueryKetRange");
    assertTestQueryKeyRange();
  }

  @Ignore
  public void assertTestQueryKeyRange(){}
  
  @Test
  public void testDelete() throws Exception {
    log.info("test method: testDelete");
    assertTestDeleteDataStore();
  }

  @Ignore
  public void assertTestDeleteDataStore(){}
  
  @Test
  public void testDeleteByQuery() throws Exception {
    log.info("test method: testDeleteByQuery");
    assertTestDeleteByQueryDataStore();
  }

  @Ignore
  public void assertTestDeleteByQueryDataStore(){
  }
  
  @Test
  public void testDeleteByQueryFields() throws Exception {
    log.info("test method: testQueryByQueryFields");
  }

}
