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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.dynamodb.query.DynamoDBResult;
import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.ws.impl.WSDataStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemResult;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableResult;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;


public class DynamoDBStore<K, T extends Persistent> extends WSDataStoreBase<K, T> {
	
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBStore.class);

  private static String preferredSchema;
  
  /**
   * The mapping file to create the tables from
   */
  private static final String MAPPING_FILE = "gora-dynamodb-mapping.xml";

  /**
   * Path where the AWS Credential will reside.
   */
  // TODO this should point to properties file within the DynamoDB module 
  private static String awsCredentialsProperties = "AwsCredentials.properties";
  
  private static long waitTime = 10L * 60L * 1000L;
  private static long sleepTime = 1000L * 20L;
  
  /**
   * Name of the cloud database provider.
   */
  private static String wsProvider = "Amazon.Web.Services";
  
  private static String CLI_TYP_PROP = "gora.dynamodb.client";
  
  private static String ENDPOINT_PROP = "gora.dynamodb.endpoint";
  
  private static String PREF_SCH_NAME = "preferred.schema.name";
  
  private static String CONSISTENCY_READS = "gora.dynamodb.consistent.reads";

  /**
   * The mapping object that contains the mapping file
   */
  private DynamoDBMapping mapping;
  
  /**
   * Amazon DynamoDB client which can be asynchronous or nor   
   */
  private AmazonDynamoDB dynamoDBClient;
  
  private String consistency;
  
  /**
   * The values are Avro fields pending to be stored.
   *
   * We want to iterate over the keys in insertion order.
   * We don't want to lock the entire collection before iterating over the keys, since in the meantime other threads are adding entries to the map.
   */
  private Map<K, T> buffer = new LinkedHashMap<K, T>();
  
  Class<T> persistentClass;  

  public DynamoDBStore(){
  }

  public void initialize(Class<K> keyClass, Class<T> pPersistentClass,
	     Properties properties) throws Exception {
	 try {

		 getCredentials();
		 setWsProvider(wsProvider);
		 preferredSchema = properties.getProperty(PREF_SCH_NAME);
		 //preferredSchema = "person";
		 dynamoDBClient = getClient(properties.getProperty(CLI_TYP_PROP),(AWSCredentials)getConf());
		 //dynamoDBClient = getClient("sync",(AWSCredentials)getConf());
		 dynamoDBClient.setEndpoint(properties.getProperty(ENDPOINT_PROP));
		 //dynamoDBClient.setEndpoint("http://dynamodb.us-east-1.amazonaws.com/");
		 mapping = readMapping();
		 
		 consistency = properties.getProperty(CONSISTENCY_READS);
		 
		 persistentClass = pPersistentClass;
	 }
	 catch (Exception e) {
	     throw new IOException(e.getMessage(), e);
	 }
  }
  
   /**
    * Method to create the specific client to be used
    * @param clientType
    * @param credentials
    * @return
    */
  public AmazonDynamoDB getClient(String clientType, AWSCredentials credentials){
	  if (clientType.equals("sync"))
		  return new AmazonDynamoDBClient(credentials);
	  if (clientType.equals("async"))
		  return new AmazonDynamoDBAsyncClient(credentials);
	  return null;
  }
  
  @SuppressWarnings("unchecked")
  private DynamoDBMapping readMapping() throws IOException {

    DynamoDBMappingBuilder mappingBuilder = new DynamoDBMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader().getResourceAsStream(MAPPING_FILE));
      //Document doc = builder.build(new File(MAPPING_FILE_PATH + MAPPING_FILE));
      
      Element root = doc.getRootElement();

      List<Element> tableElements = root.getChildren("table");
      for(Element tableElement : tableElements) {
    	  
    	String tableName = tableElement.getAttributeValue("name");
    	long readCapacUnits = Long.parseLong(tableElement.getAttributeValue("readcunit"));
    	long writeCapacUnits = Long.parseLong(tableElement.getAttributeValue("readcunit"));
    	
    	mappingBuilder.setTableName(tableName);
    	mappingBuilder.setProvisionedThroughput(tableName, readCapacUnits, writeCapacUnits);
    	LOG.debug("Basic table properties have been set: Name, and Provisioned throughput.");
    	
    	// Retrieving key's features
    	List<Element> fieldElements = tableElement.getChildren("key");
    	for(Element fieldElement : fieldElements) {
    		String keyName  = fieldElement.getAttributeValue("name");
    		String keyType  = fieldElement.getAttributeValue("type");
    		String keyAttrType  = fieldElement.getAttributeValue("att-type");
    		if(keyType.equals("hash"))
    			mappingBuilder.setHashKeySchema(tableName, keyName, keyAttrType);
    		else if(keyType.equals("hashrange"))
    			mappingBuilder.setHashRangeKeySchema(tableName, keyName, keyAttrType);
    	}
    	LOG.debug("Table key schemas have been set.");
    	
    	// Retrieving attributes
        fieldElements = tableElement.getChildren("attribute");
        for(Element fieldElement : fieldElements) {
          String attributeName  = fieldElement.getAttributeValue("name");
          String attributeType = fieldElement.getAttributeValue("type");
          mappingBuilder.addAttribute(tableName, attributeName, attributeType, 0);
        }
        LOG.debug("Table attributes have been read.");
      }

    } catch(IOException ex) {
      LOG.error("Error while performing xml mapping.");
      ex.printStackTrace();
      throw ex;

    } catch(Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex);
    }

    return mappingBuilder.build();
  }
  
  /**
   * Creates the AWSCredentials object based on the properties file.
   * @return AWSCredentials object
   * @throws FileNotFoundException
   * @throws IllegalArgumentException
   * @throws IOException
   */
  private AWSCredentials getCredentials() throws FileNotFoundException, 
    IllegalArgumentException, IOException {
    
    //File file = new File(MAPPING_FILE_PATH + awsCredentialsProperties);
	if(authentication == null){
	  InputStream awsCredInpStr = getClass().getClassLoader().getResourceAsStream(awsCredentialsProperties);
      if (awsCredInpStr == null)
        LOG.info("AWS Credentials File was not found on the classpath!");
      AWSCredentials credentials = new PropertiesCredentials(awsCredInpStr);
      setConf(credentials);
	}
	return (AWSCredentials)authentication;
  }

  private DynamoDBQuery<K, T> buildDynamoDBQuery(Query<K, T> query){
	  if(getSchemaName() == null) throw new IllegalStateException("There is not a preferred schema defined.");
	  
	  DynamoDBQuery<K, T> dynamoDBQuery = new DynamoDBQuery<K, T>();
	  dynamoDBQuery.setKeySchema(mapping.getKeySchema(getSchemaName()));
	  dynamoDBQuery.setQuery(query);
	  dynamoDBQuery.setConsistencyReadLevel(getConsistencyReads());
	  dynamoDBQuery.buildExpression();
	  //dynamoDBQuery.getQueryExpression();
	  
	  return dynamoDBQuery;
  }
  private boolean getConsistencyReads(){
	  if(consistency != null)
		  if(consistency.equals("true")) 
			  return true;
	  return false;
  }
  
  @Override
  public Result<K, T> execute(Query<K, T> query) throws Exception {
	 
	 DynamoDBQuery<K, T> dynamoDBQuery = buildDynamoDBQuery(query);
	 DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
	 List<T> objList = null;
	 if (DynamoDBQuery.getType().equals(DynamoDBQuery.RANGE_QUERY))
		 objList = mapper.query(persistentClass, (DynamoDBQueryExpression)dynamoDBQuery.getQueryExpression());
	 if (DynamoDBQuery.getType().equals(DynamoDBQuery.SCAN_QUERY))
		 objList = mapper.scan(persistentClass, (DynamoDBScanExpression)dynamoDBQuery.getQueryExpression());
	 return new DynamoDBResult<K, T>(this, query, objList);  
  }
  
  @Override
  public T get(K key, String[] fields) throws Exception {
   /* DynamoDBQuery<K,T> query = new DynamoDBQuery<K,T>();
    query.setDataStore(this);
    //query.setKeyRange(key, key);
    //query.setFields(fields);
    //query.setLimit(1);
    Result<K,T> result = execute(query);
    boolean hasResult = result.next();
    return hasResult ? result.get() : null;*/
	  return null;
  }

  /**
   * Gets the object with the specific key
   */
  public T get(K key) throws Exception {
	T object = null;
	Object rangeKey = null;
    for (Method met :key.getClass().getDeclaredMethods()){
	  if(met.getName().equals("getRangeKey")){
	    Object [] params = null;
	    rangeKey = met.invoke(key, params);
	    break;
      }
    }
	DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
	object = (rangeKey == null)?(T) mapper.load(persistentClass, key):(T) mapper.load(persistentClass, key, rangeKey);
	return object;
  }
    
  public Query<K, T> newQuery() {
    Query<K,T> query = new DynamoDBQuery<K, T>(this);
   // query.setFields(getFieldsToQuery(null));
    return query;
  }

  public String getSchemaName() {
	if (preferredSchema != null)
		return preferredSchema;
	return null;
  }
  
  public void setSchemaName(String pSchemaName){
	  preferredSchema = pSchemaName;
  }
  
  public void createSchema() throws Exception {
	  if (mapping.getTables().isEmpty())	throw new IllegalStateException("There are not tables defined.");
	  if (preferredSchema == null){
		  LOG.debug("create schemas");
		  // read the mapping object
		  for(String tableName : mapping.getTables().keySet())
			  executeCreateTableRequest(tableName);
		  LOG.debug("tables created successfully.");
	  }
	  else{
		  LOG.debug("create schema " + preferredSchema);
		  executeCreateTableRequest(preferredSchema);
	  }
  }
  
  /**
   * Executes a create table request using the DynamoDB client
   * @param tableName
   */
  private void executeCreateTableRequest(String tableName){
	  CreateTableRequest createTableRequest = getCreateTableRequest(tableName,
				mapping.getKeySchema(tableName), 
				mapping.getProvisionedThroughput(tableName));
	  // use the client to perform the request
	  dynamoDBClient.createTable(createTableRequest).getTableDescription();
	  // wait for table to become active
	  waitForTableToBecomeAvailable(tableName);
  }
  
  /**
   * Builds the necessary requests to create tables 
   * @param tableName
   * @param keySchema
   * @param proThrou
   * @return
   */
  private CreateTableRequest getCreateTableRequest(String tableName, KeySchema keySchema, ProvisionedThroughput proThrou){
	  CreateTableRequest createTableRequest = new CreateTableRequest();
	  createTableRequest.setTableName(tableName);
	  createTableRequest.setKeySchema(keySchema);
	  createTableRequest.setProvisionedThroughput(proThrou);
	  return createTableRequest;
  }
  
  /**
   * Deletes all tables present in the mapping object.
   */
  public void deleteSchema() throws Exception {
	  if (mapping.getTables().isEmpty())	throw new IllegalStateException("There are not tables defined.");
	  if (preferredSchema == null){
		LOG.debug("Delete schemas");
		if (mapping.getTables().isEmpty())	throw new IllegalStateException("There are not tables defined.");
		// read the mapping object
		for(String tableName : mapping.getTables().keySet())
		   executeDeleteTableRequest(tableName);
		LOG.debug("All schemas deleted successfully.");
	  }
	  else{
		  LOG.debug("create schema " + preferredSchema);
		  executeDeleteTableRequest(preferredSchema);
	  }
  }
  
  /**
   * Executes a delete table request using the DynamoDB client
   * @param tableName
   */
  public void executeDeleteTableRequest(String tableName){
	  DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
      .withTableName(tableName);
	  DeleteTableResult result = dynamoDBClient.deleteTable(deleteTableRequest);
	  LOG.debug("Schema: " + result.getTableDescription() + " deleted successfully.");
  }
  
  /**
   * Waits up to 6 minutes to confirm if a table has been created or not
   * @param tableName
   */
  private void waitForTableToBecomeAvailable(String tableName) {
      LOG.debug("Waiting for " + tableName + " to become available");

      long startTime = System.currentTimeMillis();
      long endTime = startTime + waitTime;
      while (System.currentTimeMillis() < endTime) {
          try {Thread.sleep(sleepTime);} catch (Exception e) {}
          try {
              DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
              TableDescription tableDescription = dynamoDBClient.describeTable(request).getTable();
              String tableStatus = tableDescription.getTableStatus();
              
              LOG.debug(tableName + " - current state: " + tableStatus);
              
              if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
          } catch (AmazonServiceException ase) {
              if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) throw ase;
          }
      }

      throw new RuntimeException("Table " + tableName + " never became active");
  }

  /**
   * Verifies if the specified schemas exist
   */
  public boolean schemaExists() throws Exception {
	TableDescription success = null;
	if (mapping.getTables().isEmpty())	throw new IllegalStateException("There are not tables defined.");
	if (preferredSchema == null){
		LOG.debug("Verifying schemas");
		if (mapping.getTables().isEmpty())	throw new IllegalStateException("There are not tables defined.");
		// read the mapping object
		for(String tableName : mapping.getTables().keySet()){
		   success = getTableSchema(tableName);
		   if (success == null) return false;
		}
	}
	else{
		LOG.debug("Verifying schema " + preferredSchema);
		success = getTableSchema(preferredSchema);
	}
	LOG.debug("Finished verifying schemas.");
	return (success != null)? true: false;
  }

  /**
   * Retrieves the table description for the specific resource name
   * @param tableName
   * @return
   */
  private TableDescription getTableSchema(String tableName){
	TableDescription tableDescription = null;
  	try{
  		DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
  		tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
  	}
  	catch(ResourceNotFoundException e){
  		return tableDescription;
  	}
  	return tableDescription;
  }
  
  public K newKey() throws Exception {
	// TODO Auto-generated method stub
	return null;
  }

  public T newPersistent() throws Exception {
	T obj = persistentClass.newInstance();
	return obj;
  }

  public void put(K key, T obj) throws Exception {
	Object rangeKey = null;
    for (Method met :key.getClass().getDeclaredMethods()){
	  if(met.getName().equals("getRangeKey")){
	    Object [] params = null;
	    rangeKey = met.invoke(key, params);
	    break;
      }
    }
    DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
    if (rangeKey != null)
	  mapper.load(persistentClass, key.toString(), rangeKey.toString());
	else
	  mapper.load(persistentClass, key.toString());
	  
    mapper.save(obj);
  }

  /**
   * Deletes the object using key
   * @return true for a successful process  
   */
  public boolean delete(K key) throws Exception {
	try{
		T object = null;
		Object rangeKey = null, hashKey = null;
		DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
	    for (Method met :key.getClass().getDeclaredMethods()){
		  if(met.getName().equals("getRangeKey")){
		    Object [] params = null;
		    rangeKey = met.invoke(key, params);
		    break;
	      }
	    }
	    for (Method met :key.getClass().getDeclaredMethods()){
			  if(met.getName().equals("getHashKey")){
			    Object [] params = null;
			    hashKey = met.invoke(key, params);
			    break;
		      }
		    }
	    if (hashKey == null) object = (T) mapper.load(persistentClass, key);
        if (rangeKey == null)
        	object = (T) mapper.load(persistentClass, hashKey);
        else
        	object = (T) mapper.load(persistentClass, hashKey, rangeKey);
	
		if (object == null) return false;
		
		// setting key for dynamodbMapper
		mapper.delete(object);
		return true;
	}catch(Exception e){
		LOG.debug("Error while deleting value with key " + key.toString());
		LOG.debug(e.getMessage());
		return false;
	}
  }

  public long deleteByQuery(Query<K, T> query) throws Exception {
	// TODO Auto-generated method stub
	return 0;
  }	

  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
		throws IOException {
	// TODO Auto-generated method stub
	return null;
  }

  public void flush() throws Exception {
	// TODO Auto-generated method stub
	
  }

  public void setBeanFactory(BeanFactory<K, T> beanFactory) {
	// TODO Auto-generated method stub
	
  }

  public BeanFactory<K, T> getBeanFactory() {
	// TODO Auto-generated method stub
	return null;
  }

  public void close() throws IOException, InterruptedException, Exception {
	  LOG.debug("Datastore closed.");
	  flush();
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
