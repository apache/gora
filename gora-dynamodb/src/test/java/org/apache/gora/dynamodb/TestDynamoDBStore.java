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

package org.apache.gora.dynamodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.Assert;

import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.dynamodb.store.DynamoDBStore;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.person;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.store.ws.impl.WSDataStoreFactory;
import org.junit.After;
import org.junit.Test;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.PutItemResult;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;
import com.amazonaws.services.dynamodb.model.TableDescription;

/**
 * Test case for DynamoDBStore.
 */
public class TestDynamoDBStore extends DataStoreTestBase {

  private Object auth;
  
  private static String tableName = "persons";
  
  private static DynamoDBStore<String,person> personStore;
  
  static {
    setTestDriver(new GoraDynamoDBTestDriver());
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    auth = getTestDriver().getAuth();
    createDataStore();
    setUpResources();
  }
  
  @Override
  protected DataStore<String, Employee> createEmployeeDataStore()
      throws IOException {
    return WSDataStoreFactory.createDataStore(DynamoDBStore.class, String.class, 
        Employee.class, auth);
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore()
      throws IOException {
    return WSDataStoreFactory.createDataStore(DynamoDBStore.class, String.class, 
        WebPage.class, auth);
  }
  
  private void setUpResources(){
	try {
		personStore.createSchema();
		//createResources();		
	} catch (Exception e) {
		log.error("Set up error while creating resources");
		e.printStackTrace();
	}
  
  }
  
  private void cleanResources() throws Exception{
	  if (personStore != null){
		  log.info("Cleaning up resource: " + tableName);
		  personStore.deleteSchema();
	  }
	  else
		  log.info("Data store was null.");
	  //getTestDriver().deleteTable(tableName);  
  }
  
  private void createResources() throws Exception{
	log.info("Creating table " + tableName);
	// Create a table with a primary key named 'name', which holds a string
	CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
	          .withKeySchema(
	        	new KeySchema(new KeySchemaElement().withAttributeName("name").withAttributeType("S"))
	        	)
	          .withProvisionedThroughput(
	        	new ProvisionedThroughput().withReadCapacityUnits(10L).withWriteCapacityUnits(10L)
	        	);
	TableDescription createdTableDescription = getTestDriver().getDynamoDBClient().createTable(createTableRequest).getTableDescription();
	
	log.info("Waiting for it to become available");
	Assert.assertNotNull(createdTableDescription);
    // Wait for it to become active
    getTestDriver().waitForTableToBecomeAvailable(tableName);
    Assert.assertEquals("CREATING", createdTableDescription.getTableStatus());
  }
  
  @SuppressWarnings("unchecked")
  protected DataStore<String, person> createDataStore() throws IOException {
    if(personStore == null)
      personStore = WSDataStoreFactory.createDataStore(DynamoDBStore.class, 
      String.class,person.class, auth);
      return personStore;
  }

  public GoraDynamoDBTestDriver getTestDriver() {
    return (GoraDynamoDBTestDriver) testDriver;
  }
  
  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    
    TableDescription tableDescription = getTestDriver().checkResource(schemaName);
    Assert.assertNotNull(tableDescription);
  }

  public void assertPutArrayDriver() throws IOException{
	  // Add an item
      Map<String, AttributeValue> item = createNewItem("Cereal", "2001-10-12", 12.5, "Pedro", "Juan");
      PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
      PutItemResult putItemResult = getTestDriver().getDynamoDBClient().putItem(putItemRequest);
      log.info("Putting Result: " + putItemResult);
      
      Assert.assertEquals(1D, putItemResult.getConsumedCapacityUnits());
      
      // Query request
      QueryRequest queryRequest = new QueryRequest(tableName,new AttributeValue("Pedro"));
      QueryResult queryResult = getTestDriver().getDynamoDBClient().query(queryRequest);
      log.info("Reading Result: " + queryResult.getItems());

      Assert.assertEquals(new ArrayList<Map<String, AttributeValue>>().add(item), queryResult.getItems());
  }
  
  private person buildPerson(String key, String pFirstName, String pLastName, String ...places){
	  person newPerson = new person();
	  //p1.setRangeKey("10/10/1985");
	  newPerson.setHashKey(key);
	  newPerson.setFirstName(pFirstName);
	  newPerson.setLastName(pLastName);
	  newPerson.setPlacesVisited(new HashSet<String>());
	  for(String place : places)
		  newPerson.getPlacesVisited().add(place);
	  
	  return newPerson;
  }
  public void assertPutArrayDataStore(){
	  try {
		String key = "123456789012345";
		person p1 = buildPerson(key, "Inca", "Atahualpa", "Peru", "Brazil", "Ecuador");
		personStore.put(key, p1);
		key = "10143024255";
		person p2 = buildPerson(key, "William", "Wallace", "Germany", "USA", "Scotland");
		personStore.put(key, p2);
	  } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  }
  }
  
  @Override
  public void assertPutArray() throws IOException {
	  // testing the driver
	  //assertPutArrayDriver();
	  
	  // testing the datastore
	  assertPutArrayDataStore();
  }
  
  private Map<String, AttributeValue> createNewItem(String name, String prd_date, double price, String... fans) {
      Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
      item.put("name", new AttributeValue(name));
      item.put("prd_date", new AttributeValue().withS(prd_date));
      item.put("price", new AttributeValue().withN(Double.toString(price)));
      item.put("buyers", new AttributeValue().withSS(fans));

      return item;
  }
  
  private void assertTestQueryDriver(){
	  Map<String, AttributeValue> item = createNewItem("Cereal", "2001-10-12", 12.5, "Pedro", "Juan");
	  // Query request
	  QueryRequest queryRequest = new QueryRequest(tableName,new AttributeValue("Juan"));
	  QueryResult queryResult = getTestDriver().getDynamoDBClient().query(queryRequest);
	  log.info("Reading Result: " + queryResult.getItems());

	  Assert.assertEquals(new ArrayList<Map<String, AttributeValue>>().add(item), queryResult.getItems());  
  }
  
  /**
   * Method to query the data store
   */
  private void assertTestQueryDataStore(){
	log.info("test method: testQuery using specific data store.");
	try {
	  String key = "123456789012345";
	  DynamoDBQuery.setScanCompOp(ComparisonOperator.LE);
	  Query<String, person> query = new DynamoDBQuery<String, person>();
	  query.setKey(key);
	  Result<String, person> queryResult = personStore.execute(query);
	  processQueryResult(queryResult);
	} catch (Exception e) {
	  e.printStackTrace();
    }
  }
  
  private void processQueryResult(Result<String, person> pQueryResult){
	  try {
		while(pQueryResult.next())
			  printPersonInfo(pQueryResult.get());
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  /**
   * Method to get an specific object using a key
   */
  private void assertTestGetDataStore(){
	log.info("test method: testGet using specific data store.");
	try {
	  String key = "123456789012345";
	  person p1 = personStore.get(key);
	  p1.setHashKey(key);
	  printPersonInfo(p1);
	} catch (Exception e) {
	  e.printStackTrace();
	}
  }
  
  /**
   * Method to print the object returned from Get method
   * @param pPerson
   */
  private void printPersonInfo(person pPerson){
	  System.out.println(	"SSN:\t" + pPerson.getHashKey() + 
			  				"\n FirstName:" + pPerson.getFirstName() +
			  				"\n LastName:" + pPerson.getLastName() + 
			  				"\n Visited Places:");
	  for(String place : pPerson.getPlacesVisited())
		  System.out.println("\t" + place);
  }
  
  @Test
  public void testQuery() throws IOException, Exception {
    // testing the driver
 	//assertTestQueryDriver();
	  
	log.info("test method: testQuery using specific data store.");
    // testing the datastore
 	assertTestQueryDataStore();
 	assertTestGetDataStore();
  }

  @Override
  public void testQueryEndKey() throws IOException {
      //We need to skip this test since gora considers endRow inclusive.
      //TODO: Raise for further discussion
  }

  @Override
  public void testQueryKeyRange() throws IOException {
      //We need to skip this test since gora considers endRow inclusive.
      //TODO: Raise for further discussion
  }

  @Override
  public void testDeleteByQuery() throws IOException {
      //We need to skip this test since gora considers endRow inclusive.
      //TODO: Raise for further discussion
  }

  @After
  public void tearDown() throws Exception {
    log.info("Tearing down test");
    if(getTestDriver() != null) {
      cleanResources();
      getTestDriver().tearDown();
    }
    if( personStore != null)
      personStore.close();
    else 
      log.info("Data store was null.");
    //employeeStore.close();
    //webPageStore.close();
  }
  
  @Test
  public void testCreateSchema() throws Exception {
    log.info("test method: testCreateSchema");
    personStore.createSchema();
    DataStoreTestUtil.testCreateEmployeeSchema(employeeStore);
    assertSchemaExists("Employee");
  }
  
  @Test
  public void testDelete() throws Exception{
	log.info("test method: testDelete by key");
	
	String key = "1111111111";
	person p1 = new person();
	//p1.setRangeKey("10/10/1985");
	p1.setHashKey(key);
	p1.setFirstName("Joao");
	p1.setLastName("Velasco");
	personStore.put(key, p1);
	
	Assert.assertTrue(personStore.delete(key));
	
	key = "222222222";
	Assert.assertFalse(personStore.delete(key));
  }
  
  public static void main(String[] args) throws Exception {
	TestDynamoDBStore test = new TestDynamoDBStore();
    try{
      TestDynamoDBStore.setUpClass();
      test.setUp();
      test.assertPutArray();
      test.testQuery();
      test.testDelete();
    }catch (Exception e){
      log.error("Error while executing tests.");
    }finally{
      test.tearDown();
      TestDynamoDBStore.tearDownClass();
    }
  }

}
