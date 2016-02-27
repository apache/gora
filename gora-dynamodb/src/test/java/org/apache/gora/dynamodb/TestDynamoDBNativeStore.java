/*
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

package org.apache.gora.dynamodb;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;

import org.apache.gora.dynamodb.example.generated.Person;
import org.apache.gora.dynamodb.query.DynamoDBKey;
import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.WSDataStoreTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;

/**
 * Test case for DynamoDBStore.
 */
public class TestDynamoDBNativeStore extends
WSDataStoreTestBase<DynamoDBKey, Person> {

  public static final Logger log = LoggerFactory
      .getLogger(TestDynamoDBNativeStore.class);

  static {
    setTestDriver(new GoraDynamoDBTestDriver());
  }

  @Before
  public void setUp() throws Exception {
    setPersistentKeyClass(DynamoDBKey.class);
    setPersistentValClass(Person.class);
    super.setUp();
  }

  public GoraDynamoDBTestDriver getTestDriver() {
    return (GoraDynamoDBTestDriver) testDriver;
  }

  // ============================================================================
  // We need to skip the following tests for a while until we fix some issues..
  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testQueryStartKey() throws IOException {
    log.info("test method: TestQueryStartKey SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testQueryEndKey() throws IOException {
    log.info("test method: TestQueryEndKey SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testDeleteByQueryFields() throws IOException {
    log.info("test method: TestDeleteByQueryFields SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testNewInstance() throws IOException, Exception {
    log.info("test method: TestNewInstance SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testAutoCreateSchema() throws Exception {
    log.info("test method: TestAutoCreateSchema SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testTruncateSchema() throws Exception {
    log.info("test method: TestTruncateSchema SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testPutNested() throws IOException, Exception {
    log.info("test method: TestPutNested SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testPutArray() throws IOException, Exception {
    log.info("test method: TestPutArray SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testPutBytes() throws IOException, Exception {
    log.info("test method: TestPutBytes SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testPutMap() throws IOException, Exception {
    log.info("test method: TestPutMap SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testEmptyUpdate() throws IOException, Exception {
    log.info("test method: TestEmptyUpdate SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testDeleteSchema() throws IOException, Exception {
    log.info("test method: TestDeleteSchema SKIPPED.");
  }

  @Ignore("Needs to be skipped for a while until some issues are fixed")
  @Override
  public void testGetWithFields() throws IOException, Exception {
    log.info("test method: TestGetWithFields SKIPPED.");
  }

  // ==========================================================================

  /**
   * Tests deleting items using a query
   */
  @Override
  public void assertTestDeleteByQueryDataStore() {
    try {
      log.info("test method: TestDeleteByQuery using DynamoDB store.");
      DynamoDBKey<Long, String> dKey = new DynamoDBKey<>();
      dKey.setHashKey(100L);
      dKey.setRangeKey("10/10/1880");
      Person p1 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "John", "Doe", "Peru", "Brazil", "Ecuador");
      dataStore.put(dKey, p1);
      dKey.setRangeKey("11/10/1707");
      Person p2 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "Juan", "Perez", "Germany", "USA", "Scotland");
      dataStore.put(dKey, p2);
      DynamoDBQuery.setScanCompOp(ComparisonOperator.LE);
      DynamoDBQuery.setType(DynamoDBQuery.SCAN_QUERY);
      Query<DynamoDBKey, Person> query = new DynamoDBQuery<DynamoDBKey, Person>();
      query.setKey(dKey);
      log.info("Number of records deleted: " + dataStore.deleteByQuery(query));
    } catch (Exception e) {
      log.error("Error while running test: TestDeleteByQuery", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Tests updating a specific item
   */
  @Override
  public void assertTestUpdateDataStore() {
    try {
      log.info("test method: TestUpdate using DynamoDB store.");
      DynamoDBKey<Long, String> dKey = new DynamoDBKey<>();
      dKey.setHashKey(13L);
      dKey.setRangeKey("10/10/1880");
      Person p1 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "Inca", "Atahualpa", "Peru", "Brazil", "Ecuador");
      dataStore.put(dKey, p1);
      p1.setFirstName("Ataucuri");
      dataStore.put(dKey, p1);
    } catch (Exception e) {
      log.error("error in test method: testUpdate.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to test deleting a schema
   */
  @Override
  public void assertDeleteSchema() {
    try {
      log.info("test method: TestDeleteSchema using DynamoDB store.");
      dataStore.deleteSchema();
    } catch (Exception e) {
      log.error("error in test method: testDeleteSchema.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to verify if a schema exists or not
   */
  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    log.info("test method: TestSchemaExists using DynamoDB store.");
    assertTrue(dataStore.schemaExists());
  }

  /**
   * Method to put items into the data store
   */
  @Override
  public void assertPut() {
    try {
      log.info("test method: TestPut using DynamoDB store.");
      DynamoDBKey<Long, String> dKey = new DynamoDBKey<>();
      dKey.setHashKey(12L);
      dKey.setRangeKey("10/10/1880");
      Person p1 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "Inca", "Atahualpa", "Peru", "Brazil", "Ecuador");
      dataStore.put(dKey, p1);
      dKey.setRangeKey("11/10/1707");
      Person p2 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "William", "Wallace", "Germany", "USA", "Scotland");
      dataStore.put(dKey, p2);
    } catch (Exception e) {
      log.error("error in test method: testPut.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to query the data store
   */
  @Override
  public void assertTestQueryDataStore() {
    log.info("test method: testQuery using DynamoDB store.");
    try {
      DynamoDBKey<String, String> dKey = new DynamoDBKey<String, String>();
      dKey.setHashKey("Peru");
      DynamoDBQuery.setScanCompOp(ComparisonOperator.LE);
      DynamoDBQuery.setType(DynamoDBQuery.SCAN_QUERY);
      Query<DynamoDBKey, Person> query = new DynamoDBQuery<DynamoDBKey, Person>();
      query.setKey(dKey);
      Result<DynamoDBKey, Person> queryResult = dataStore.execute(query);
      processQueryResult(queryResult);
    } catch (Exception e) {
      log.error("error in test method: testQuery.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to query items into the data store
   */
  @Override
  public void assertTestQueryKeyRange() {
    log.info("test method: testQueryKeyRange using specific data store.");
    try {
      DynamoDBKey<String, String> dKey = new DynamoDBKey<String, String>();
      DynamoDBKey<String, String> startKey = new DynamoDBKey<String, String>();
      DynamoDBKey<String, String> endKey = new DynamoDBKey<String, String>();
      dKey.setHashKey("Peru");
      startKey.setRangeKey("01/01/1700");
      endKey.setRangeKey("31/12/1900");
      DynamoDBQuery.setRangeCompOp(ComparisonOperator.BETWEEN);
      DynamoDBQuery.setType(DynamoDBQuery.RANGE_QUERY);
      Query<DynamoDBKey, Person> query = new DynamoDBQuery<DynamoDBKey, Person>();
      query.setKey(dKey);
      query.setStartKey(startKey);
      query.setEndKey(endKey);
      Result<DynamoDBKey, Person> queryResult = dataStore.execute(query);
      processQueryResult(queryResult);
    } catch (Exception e) {
      log.error("error in test method: testQueryKeyRange.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to get an specific object using a key
   */
  @Override
  public void assertTestGetDataStore() {
    log.info("test method: testGet using specific data store.");
    try {
      DynamoDBKey<Long, String> dKey = new DynamoDBKey<>();
      dKey.setHashKey(11L);
      dKey.setRangeKey("10/10/1999");
      // insert item
      Person p1 = buildPerson(dKey.getHashKey(), dKey.getRangeKey().toString(),
          "Inca", "Atahualpa", "Peru", "Brazil", "Ecuador");
      dataStore.put(dKey, p1);
      // get item
      Person p2 = dataStore.get(dKey);
      printPersonInfo(p2);
    } catch (Exception e) {
      log.error("error in test method: testGetDataStore.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to delete items into the data store
   */
  @Override
  public void assertTestDeleteDataStore() {
    log.info("test method: testDelete by key");
    try {
      DynamoDBKey<Long, String> dKey = new DynamoDBKey<Long, String>();
      dKey.setHashKey(10L);
      dKey.setRangeKey("10/10/1985");
      Person p1 = new Person();
      p1.setHashKey(dKey.getHashKey());
      p1.setRangeKey(dKey.getRangeKey());
      p1.setFirstName("Joao");
      p1.setLastName("Velasco");
      dataStore.put(dKey, p1);
      assertTrue(dataStore.delete(dKey));
      dKey.setRangeKey("10/10/1000");
      assertFalse(dataStore.delete(dKey));
    } catch (Exception e) {
      log.error("error in test method: testDeleteDataStore.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to create the data store
   */
  @Override
  protected DataStore<DynamoDBKey, Person> createDataStore() {
    log.info("Creating DynamoDB data store.");
    try {
      dataStore = getTestDriver().getDataStore();
      dataStore.createSchema();
    } catch (Exception e) {
      log.error("error while creating DynamoDB data store", e.getMessage());
      throw new RuntimeException(e);
    }
    return dataStore;
  }

  /**
   * Processes query results from an query execution
   * 
   * @param pQueryResult
   */
  private void processQueryResult(Result<DynamoDBKey, Person> pQueryResult) {
    try {
      log.debug("Processing tests results.");
      while (pQueryResult.next())
        printPersonInfo(pQueryResult.get());
    } catch (IOException e) {
      log.error("error while processing tests results.", e.getMessage());
      throw new RuntimeException(e);
    } catch (Exception e) {
      log.error("error while processing tests results.", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to generate persisten objects
   * 
   * @param key
   * @param pRangeKey
   * @param pFirstName
   * @param pLastName
   * @param places
   * @return
   */
  private Person buildPerson(Long key, String pRangeKey, String pFirstName,
      String pLastName, String... places) {
    Person newPerson = new Person();
    newPerson.setRangeKey(pRangeKey);
    newPerson.setHashKey(key);
    newPerson.setFirstName(pFirstName);
    newPerson.setLastName(pLastName);
    newPerson.setVisitedplaces(new HashSet<String>());
    for (String place : places)
      newPerson.getVisitedplaces().add(place);
    return newPerson;
  }

  /**
   * Method to print the object returned from Get method
   * 
   * @param pPerson
   */
  private void printPersonInfo(Person pPerson) {
    log.info("Origin:\t" + pPerson.getHashKey() + "\n Birthday:\t"
        + pPerson.getRangeKey() + "\n FirstName:" + pPerson.getFirstName()
        + "\n LastName:" + pPerson.getLastName() + "\n Visited Places:");
    for (String place : pPerson.getVisitedplaces())
      log.info("\t" + place);
  }

}