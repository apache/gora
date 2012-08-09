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
import org.apache.gora.dynamodb.query.DynamoDBKey;
import org.apache.gora.dynamodb.store.DynamoDBStore;
import org.apache.gora.examples.generated.person;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.ws.impl.WSDataStoreFactory;
import org.apache.gora.util.GoraException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.TableDescription;

/**
 * Helper class for third part tests using gora-dynamodb backend. 
 * @see GoraTestDriver
 */
public class GoraDynamoDBTestDriver extends GoraTestDriver {

  private static DynamoDBStore<DynamoDBKey,person> personStore;
  
  static AmazonDynamoDBClient dynamoDBClient;
  
  static String awsCredentialsFile = "AwsCredentials.properties";
  
  static String awsCredentialsPath = "target/test-classes/";
  
  protected Object auth;
  
  public GoraDynamoDBTestDriver() {
    super(DynamoDBStore.class);
	try {
	  AWSCredentials credentials;
	  File file = new File(awsCredentialsPath + awsCredentialsFile);
	  credentials = new PropertiesCredentials(file);
	  auth = credentials;
	} catch (FileNotFoundException e) {
	  e.printStackTrace();
	} catch (IllegalArgumentException e) {
	  e.printStackTrace();
	} catch (IOException e) {
	  e.printStackTrace();
	}
  }

  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Initializing DynamoDB.");
    createDataStore();
  }
  
  @Override
  public void setUp() throws Exception {
	  personStore.createSchema();
  }
  
  
  @SuppressWarnings("unchecked")
  protected DataStore<DynamoDBKey, person> createDataStore() throws IOException {
    if(personStore == null)
      personStore = WSDataStoreFactory.createDataStore(DynamoDBStore.class, 
    		  DynamoDBKey.class,person.class, auth);
      return personStore;
  }
  
  @SuppressWarnings("unchecked")
  public<K, T extends Persistent> DataStore<K,T>
    createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
	personStore = (DynamoDBStore<DynamoDBKey, person>) WSDataStoreFactory.createDataStore(
        (Class<? extends DataStore<K,T>>)dataStoreClass, keyClass, persistentClass, auth);
    dataStores.add(personStore);

    return (DataStore<K, T>) personStore;
  }
  
  public DataStore<DynamoDBKey, person> getDataStore(){
	try {
	  if(personStore != null)
	    return personStore;
	  else
		return createDataStore();
	} catch (IOException e) {
		e.printStackTrace();
		return null;
	}
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Finished DynamoDB driver.");
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
  
  
}
