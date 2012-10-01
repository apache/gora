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
/**
 * @author Renato Marroquin Mogrovejo
 */
package org.apache.gora.dynamodb.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.gora.dynamodb.query.DynamoDBKey;
import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.dynamodb.query.DynamoDBResult;
import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.ws.impl.WSDataStoreBase;
import org.apache.gora.util.GoraException;
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
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableResult;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;


public class DynamoDBStore<K, T extends Persistent> extends WSDataStoreBase<K, T> {
  
  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBStore.class);

  /**
   * Schema name which will be used from within the data store.
   * If not set, all the available schemas from the mapping file will be used.
   */
  private static String preferredSchema;
  
  /**
   * The mapping file to create the tables from
   */
  private static final String MAPPING_FILE = "gora-dynamodb-mapping.xml";

  /**
   * Default times to wait while requests are performed
   */
  private static long waitTime = 10L * 60L * 1000L;
  private static long sleepTime = 1000L * 20L;
  private static long sleepDeleteTime = 1000L * 10L;

  /**
   * AWS Credential file name.
   */
  private static String awsCredentialsProperties = "AwsCredentials.properties";
  
  /**
   * Name of the cloud database provider.
   */
  private static String wsProvider = "Amazon.Web.Services";
  
  /**
   * Parameter to decide what type of Amazon DynamoDB client to use
   */
  private static String CLI_TYP_PROP = "gora.dynamodb.client";
  
  /**
   * Parameter to decide where the data store will make its computations
   */
  private static String ENDPOINT_PROP = "gora.dynamodb.endpoint";
  
  /**
   * Parameter to decide which schema will be used
   */
  private static String PREF_SCH_NAME = "preferred.schema.name";
  
  /**
   * Parameter to decide how reads will be made i.e. using strong consistency or eventual consistency. 
   */
  private static String CONSISTENCY_READS = "gora.dynamodb.consistent.reads";

  /**
   * The mapping object that contains the mapping file
   */
  private DynamoDBMapping mapping;
  
  /**
   * Amazon DynamoDB client which can be asynchronous or nor   
   */
  private AmazonDynamoDB dynamoDBClient;
 
  /**
   * Contains the consistency level to be used
   */
  private String consistency;
  
  /**
   * TODO This would be useful for the batch read/write operations
   * Contains the elements to be written or read from the data store
   */
  //private Map<K, T> buffer = new LinkedHashMap<K, T>();
  
  /**
   * The class that will be persisted
   */
  Class<T> persistentClass;  

  /**
   * Constructor
   */
  public DynamoDBStore(){
  }

  /**
   * Initialize the data store by reading the credentials, setting the cloud provider,
   * setting the client's properties up, setting the end point and reading the mapping file  
   */
  public void initialize(Class<K> keyClass, Class<T> pPersistentClass,
       Properties properties) throws Exception {
    try {
      LOG.debug("Initializing DynamoDB store");
      getCredentials();
      setWsProvider(wsProvider);
      preferredSchema = properties.getProperty(PREF_SCH_NAME);
      dynamoDBClient = getClient(properties.getProperty(CLI_TYP_PROP),(AWSCredentials)getConf());
      dynamoDBClient.setEndpoint(properties.getProperty(ENDPOINT_PROP));
      mapping = readMapping();
      consistency = properties.getProperty(CONSISTENCY_READS);
      persistentClass = pPersistentClass;
    }
    catch (Exception e) {
      LOG.error("Error while initializing DynamoDB store");
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
  
  /**
   * Reads the schema file and converts it into a data structure to be used
   * @param pMapFile	The schema file to be mapped into a table
   * @return DynamoDBMapping	Object containing all necessary information to create tables
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private DynamoDBMapping readMapping() throws IOException {

    DynamoDBMappingBuilder mappingBuilder = new DynamoDBMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader().getResourceAsStream(MAPPING_FILE));
      
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
      LOG.error("Error while performing xml mapping.");
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
    
  if(authentication == null){
    InputStream awsCredInpStr = getClass().getClassLoader().getResourceAsStream(awsCredentialsProperties);
      if (awsCredInpStr == null)
        LOG.info("AWS Credentials File was not found on the classpath!");
      AWSCredentials credentials = new PropertiesCredentials(awsCredInpStr);
      setConf(credentials);
  }
  return (AWSCredentials)authentication;
  }

  /**
   * Builds a DynamoDB query from a generic Query object
   * @param query	Generic query object
   * @return	DynamoDBQuery 
   */
  private DynamoDBQuery<K, T> buildDynamoDBQuery(Query<K, T> query){
    if(getSchemaName() == null) throw new IllegalStateException("There is not a preferred schema defined.");
    
      DynamoDBQuery<K, T> dynamoDBQuery = new DynamoDBQuery<K, T>();
      dynamoDBQuery.setKeySchema(mapping.getKeySchema(getSchemaName()));
      dynamoDBQuery.setQuery(query);
      dynamoDBQuery.setConsistencyReadLevel(getConsistencyReads());
      dynamoDBQuery.buildExpression();
    
      return dynamoDBQuery;
  }
  
  /**
   * Gets consistency level for reads
   * @return True for strong consistency or false for eventual consistent reads
   */
  private boolean getConsistencyReads(){
    if(consistency != null)
      if(consistency.equals("true")) 
        return true;
    return false;
  }
  
  /**
   * Executes a query after building a DynamoDB specific query based on the received one
   */
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
    Object rangeKey = getRangeKey(key);
    Object hashKey = getHashKey(key);
    if (hashKey != null){
      DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
      if (rangeKey != null)
        object = mapper.load(persistentClass, hashKey, rangeKey);
      else
        object = mapper.load(persistentClass, hashKey);
    }
    else
      throw new GoraException("Error while retrieving keys from object: " + key.toString());
    return object;
  }
    
  /**
   * Creates a new DynamoDBQuery
   */
  public Query<K, T> newQuery() {
    Query<K,T> query = new DynamoDBQuery<K, T>(this);
    //query.setFields(getFieldsToQuery(null));
    return query;
  }

  /**
   * Gets the preferred schema
   */
  public String getSchemaName() {
    if (preferredSchema != null)
      return preferredSchema;
    return null;
  }
  
  /**
   * Sets the preferred schema
   * @param pSchemaName
   */
  public void setSchemaName(String pSchemaName){
    preferredSchema = pSchemaName;
  }
  
  /**
   * Creates the table within the data store for a preferred schema or 
   * for a group of schemas defined withing the mapping file
   */
  public void createSchema() throws Exception {
    LOG.info("Creating schema");
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
   * Executes a create table request using the DynamoDB client and waits
   * the default time until it's been created.
   * @param tableName
   */
  private void executeCreateTableRequest(String tableName){
    CreateTableRequest createTableRequest = getCreateTableRequest(tableName,
      mapping.getKeySchema(tableName), mapping.getProvisionedThroughput(tableName));
    // use the client to perform the request
    dynamoDBClient.createTable(createTableRequest).getTableDescription();
    // wait for table to become active
    waitForTableToBecomeAvailable(tableName);
    LOG.info(tableName + "Schema now available");
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
  public void executeDeleteTableRequest(String pTableName){
    try{
      DeleteTableRequest deleteTableRequest = new DeleteTableRequest().withTableName(pTableName);
      DeleteTableResult result = dynamoDBClient.deleteTable(deleteTableRequest);
      waitForTableToBeDeleted(pTableName);
      LOG.debug("Schema: " + result.getTableDescription() + " deleted successfully.");
    }
    catch(Exception e){
      LOG.debug("Schema: " + pTableName + " deleted.");
      e.printStackTrace();
    }
  }

  /**
   * Waits up to 6 minutes to confirm if a table has been deleted or not
   * @param pTableName
   */
  private void waitForTableToBeDeleted(String pTableName){
    LOG.debug("Waiting for " + pTableName + " to be deleted.");
    long startTime = System.currentTimeMillis();
    long endTime = startTime + waitTime;
    while (System.currentTimeMillis() < endTime) {
      try {Thread.sleep(sleepDeleteTime);} catch (Exception e) {}
      try {
        DescribeTableRequest request = new DescribeTableRequest().withTableName(pTableName);
        TableDescription tableDescription = dynamoDBClient.describeTable(request).getTable();
        String tableStatus = tableDescription.getTableStatus();
        LOG.debug(pTableName + " - current state: " + tableStatus);
      } catch (AmazonServiceException ase) {
        if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == true)
          return;
        ase.printStackTrace();
      }
    }
    LOG.debug(pTableName + " deleted.");
  }
  
  /**
   * Waits up to 6 minutes to confirm if a table has been created or not
   * @param pTableName
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
    LOG.info("Verifying schemas.");
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
      LOG.info("Verifying schema " + preferredSchema);
      success = getTableSchema(preferredSchema);
  }
    LOG.info("Finished verifying schemas.");
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
      LOG.error("Error while getting table schema: " + tableName);
      return tableDescription;
    }
    return tableDescription;
  }
  
  public K newKey() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns a new persistent object
   */
  public T newPersistent() throws Exception {
    T obj = persistentClass.newInstance();
    return obj;
  }

  /**
   * Puts an object identified by a key
   */
  public void put(K key, T obj) throws Exception {
    try{
      Object rangeKey = getRangeKey(key);
      Object hashKey = getHashKey(key);
      // if the key does not have these attributes then try to get them from the object
      if (hashKey == null)
        hashKey = getHashKey(obj);
      if (rangeKey == null)
        rangeKey = getRangeKey(obj);
      if (hashKey != null){
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDBClient);
        if (rangeKey != null)
          mapper.load(persistentClass, hashKey.toString(), rangeKey.toString());
        else
          mapper.load(persistentClass, hashKey.toString());
          mapper.save(obj);
      }
      else
        throw new GoraException("Error while retrieving keys from object: " + obj.toString());
    }catch(NullPointerException npe){
      LOG.error("Error while putting an item. " + npe.toString());
      npe.printStackTrace();
    }
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
      LOG.error("Error while deleting value with key " + key.toString());
      LOG.error(e.getMessage());
      return false;
    }
  }
  
  /**
   * Deletes items using a specific query
   */
  @SuppressWarnings("unchecked")
  public long deleteByQuery(Query<K, T> query) throws Exception {
    // TODO verify whether or not we are deleting a whole row
    //String[] fields = getFieldsToQuery(query.getFields());
    //find whether all fields are queried, which means that complete
    //rows will be deleted
    //boolean isAllFields = Arrays.equals(fields
    //    , getBeanFactory().getCachedPersistent().getFields());
    Result<K, T> result = execute(query);
    ArrayList<T> deletes = new ArrayList<T>();
    while(result.next()) {
      T resultObj = result.get(); 
      deletes.add(resultObj);
      
      @SuppressWarnings("rawtypes")
      DynamoDBKey dKey = new DynamoDBKey();
      dKey.setHashKey(getHashKey(resultObj));
      dKey.setRangeKey(getRangeKey(resultObj));
      delete((K)dKey);
    }
    return deletes.size();
  }
  
  /**
   * Gets a hash key from an object of type T
   * @param obj	Object from which we will get a hash key
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getHashKey(T obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
    Object hashKey = null;
    for (Method met : obj.getClass().getDeclaredMethods()){
      if(met.getName().equals("getHashKey")){
        Object [] params = null;
        hashKey = met.invoke(obj, params);
        break;
      }
    }
    return hashKey;
  }
  
  /**
   * Gets a hash key from a key of type K
   * @param obj	Object from which we will get a hash key
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getHashKey(K obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
    Object hashKey = null;
    for (Method met : obj.getClass().getDeclaredMethods()){
      if(met.getName().equals("getHashKey")){
        Object [] params = null;
        hashKey = met.invoke(obj, params);
        break;
      }
    }
    return hashKey;
  }
  
  /**
   * Gets a range key from an object T
   * @param obj	Object from which a range key will be extracted
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getRangeKey(T obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
    Object rangeKey = null;
    for (Method met : obj.getClass().getDeclaredMethods()){
      if(met.getName().equals("getRangeKey")){
        Object [] params = null;
        rangeKey = met.invoke(obj, params);
        break;
      }
    }
    return rangeKey;
  }
  
  /**
   * Gets a range key from a key obj
   * @param obj	Object from which a range key will be extracted
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getRangeKey(K obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
    Object rangeKey = null;
    for (Method met : obj.getClass().getDeclaredMethods()){
      if(met.getName().equals("getRangeKey")){
        Object [] params = null;
        rangeKey = met.invoke(obj, params);
        break;
      }
    }
    return rangeKey;
  }
  
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
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

  /**
   * Closes the data store.
   */
  public void close() throws IOException, InterruptedException, Exception {
    LOG.debug("Datastore closed.");
    flush();
  }
}
