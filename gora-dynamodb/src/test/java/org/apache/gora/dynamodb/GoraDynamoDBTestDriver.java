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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.dynamodb.store.DynamoDBStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.ws.impl.WSDataStoreFactory;
import org.apache.gora.util.GoraException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;

/**
 * Helper class for third part tests using gora-dynamodb backend. 
 * @see GoraTestDriver
 */
public class GoraDynamoDBTestDriver extends GoraTestDriver {

  static long waitTime = 10L * 60L * 1000L;
  
  static AmazonDynamoDBClient dynamoDBClient;
  
  static String awsCredentialsFile = "AwsCredentials.properties";
  
  static String awsCredentialsPath = "gora-dynamodb/conf/";
  
  protected Object auth;
  
  public GoraDynamoDBTestDriver() {
    super(DynamoDBStore.class);
    
    try {
    	
    	File file = new File(awsCredentialsPath + awsCredentialsFile);
    	AWSCredentials credentials = new PropertiesCredentials(file);
    	
    	auth = credentials;
        dynamoDBClient = new AmazonDynamoDBClient(credentials);
        
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IllegalArgumentException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
  }

  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Initializing DynamoDB.");
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Finishing DynamoDB.");
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void deleteTable(String tableName) throws Exception {
	  DeleteTableRequest deleteTableRequest = new DeleteTableRequest().withTableName(tableName);
	  dynamoDBClient.deleteTable(deleteTableRequest);
	  waitForTableToBeDeleted(tableName);  
  }
  
  public void waitForTableToBecomeAvailable(String tableName) {
      log.info("Waiting for " + tableName + " to become ACTIVE...");

      long startTime = System.currentTimeMillis();
      long endTime = startTime + (10 * 60 * 1000);
      while (System.currentTimeMillis() < endTime) {
          try {Thread.sleep(1000 * 20);} catch (Exception e) {}
          try {
              DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
              TableDescription tableDescription = dynamoDBClient.describeTable(request).getTable();
              String tableStatus = tableDescription.getTableStatus();
              log.info("  - current state: " + tableStatus);
              if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
          } catch (AmazonServiceException ase) {
              if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) throw ase;
          }
      }

      throw new RuntimeException("Table " + tableName + " never went active");
  }
  
  private static void waitForTableToBeDeleted(String tableName) {
      
	  log.info("Waiting for " + tableName + " while status DELETING...");

      long startTime = System.currentTimeMillis();
      long endTime = startTime + (waitTime);
      
      while (System.currentTimeMillis() < endTime) {
          try {Thread.sleep(1000 * 20);} catch (Exception e) {}
          try {
              DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
              TableDescription tableDescription = dynamoDBClient.describeTable(request).getTable();
              String tableStatus = tableDescription.getTableStatus();
              log.info("  - current state: " + tableStatus);
              if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                  return;
              }
          } catch (AmazonServiceException ase) {
              if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == true) {
            	  log.info("Table " + tableName + " is not found. It was deleted.");
                  return;
              }
              else {
                  throw ase;
              }
          }
      }
      throw new RuntimeException("Table " + tableName + " did not go active after 10 minutes.");
  }
  
  public Object getAuth() {
      return auth;
  }
  
  public AmazonDynamoDBClient getDynamoDBClient() {
    return dynamoDBClient;
  }
  
  public TableDescription checkResource(String tableName){
  	TableDescription tableDescription = null;
  	
  	try{
  		DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
  		tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
  	}
  	catch(ResourceNotFoundException e){
  		tableDescription = null;
  	}
      
  	return tableDescription;
  }
  
  @SuppressWarnings("unchecked")
  public<K, T extends Persistent> DataStore<K,T>
    createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    setProperties(DataStoreFactory.createProps());
    DataStore<K,T> dataStore = WSDataStoreFactory.createDataStore(
        (Class<? extends DataStore<K,T>>)dataStoreClass, keyClass, persistentClass, auth);
    dataStores.add(dataStore);

    return dataStore;
  }
}
