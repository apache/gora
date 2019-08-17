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

import static org.apache.gora.dynamodb.store.DynamoDBUtils.CLI_TYP_PROP;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.CONSISTENCY_READS;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.CONSISTENCY_READS_TRUE;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.ENDPOINT_PROP;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.MAPPING_FILE;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.PREF_SCH_NAME;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.REGION_PROP;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.SERIALIZATION_TYPE;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.SLEEP_DELETE_TIME;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.WAIT_TIME;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.WS_PROVIDER;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.gora.dynamodb.query.DynamoDBKey;
import org.apache.gora.dynamodb.query.DynamoDBQuery;
import org.apache.gora.dynamodb.query.DynamoDBResult;
import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.ws.impl.PersistentWSBase;
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
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

/**
 * Class for using DynamoDBStores
 * 
 * @param <K>
 * @param <T>
 */
public class DynamoDBStore<K, T extends PersistentWSBase> extends WSDataStoreBase<K, T> {

  /** Method's names for getting range and hash keys. */
  private static final String GET_RANGE_KEY_METHOD = "getRangeKey";
  private static final String GET_HASH_KEY_METHOD = "getHashKey";

  /** Helper to write useful information into the logs. */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBStore.class);

  /**
   * The mapping object that contains the mapping file
   */
  private DynamoDBMapping mapping;

  /**
   * Amazon DynamoDB client which can be asynchronous or not
   */
  private AmazonDynamoDB dynamoDBClient;

  /**
   * Contains the consistency level to be used
   */
  private String consistency;

  /** Specifies how the objects will be serialized inside DynamoDb. */
  private DynamoDBUtils.DynamoDBType serializationType;

  /**
   * Schema name which will be used from within the data store. If not set, all
   * the available schemas from the mapping file will be used.
   */
  private String preferredSchema;

  @Override
  public void close() {}

  /**
   * Creates the table within the data store for a preferred schema or for a
   * group of schemas defined within the mapping file
   */
  @Override
  public void createSchema() throws GoraException {
    LOG.info("Creating Native DynamoDB Schemas.");
    if (getDynamoDbMapping().getTables().isEmpty()) {
      throw new GoraException("There are not tables defined.");
    }
    try {
      if (getPreferredSchema() == null) {
        LOG.debug("Creating schemas.");
        // read the mapping object
        for (String tableName : getDynamoDbMapping()
                .getTables().keySet())
          DynamoDBUtils.executeCreateTableRequest(
                  getDynamoDbClient(), tableName,
                  getTableKeySchema(tableName),
                  getTableAttributes(tableName),
                  getTableProvisionedThroughput(tableName));
        LOG.debug("tables created successfully.");
      } else {
        String tableName = getPreferredSchema();
        LOG.debug("Creating schema " + tableName);
        DynamoDBUtils.executeCreateTableRequest(
                getDynamoDbClient(), tableName,
                getTableKeySchema(tableName),
                getTableAttributes(tableName),
                getTableProvisionedThroughput(tableName));
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    try {
      T object = null;
      Object rangeKey = null, hashKey = null;
      DynamoDBMapper mapper = new DynamoDBMapper(
              getDynamoDbClient());
      for (Method met : key.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_RANGE_KEY_METHOD)) {
          Object[] params = null;
          rangeKey = met.invoke(key, params);
          break;
        }
      }
      for (Method met : key.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_HASH_KEY_METHOD)) {
          Object[] params = null;
          hashKey = met.invoke(key, params);
          break;
        }
      }
      if (hashKey == null)
        object = (T) mapper.load(persistentClass, key);
      if (rangeKey == null)
        object = (T) mapper.load(persistentClass, hashKey);
      else
        object = (T) mapper.load(persistentClass, hashKey, rangeKey);

      if (object == null)
        return false;

      // setting key for dynamodbMapper
      mapper.delete(object);
      return true;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    // TODO verify whether or not we are deleting a whole row
    // String[] fields = getFieldsToQuery(query.getFields());
    // find whether all fields are queried, which means that complete
    // rows will be deleted
    // boolean isAllFields = Arrays.equals(fields
    // , getBeanFactory().getCachedPersistent().getFields());
    ArrayList<T> deletes = null ;
    try {
      Result<K, T> result = execute(query);
      deletes = new ArrayList<T>();
      while (result.next()) {
        T resultObj = result.get();
        deletes.add(resultObj);

        @SuppressWarnings("rawtypes")
        DynamoDBKey dKey = new DynamoDBKey();

        dKey.setHashKey(getHashFromObj(resultObj));

        dKey.setRangeKey(getRangeKeyFromObj(resultObj));
        delete((K) dKey);
      }
    } catch (GoraException e) {
      throw e ; // If it is a GoraException we assume it is already logged
    } catch (Exception e) {
      throw new GoraException(e);
    }
    return deletes.size();
  }

  @Override
  public void deleteSchema() throws GoraException {
    try {
      if (getDynamoDbMapping().getTables().isEmpty())
        return ; // Nothing to delete
      if (preferredSchema == null) {
        LOG.debug("Delete schemas");
        if (getDynamoDbMapping().getTables().isEmpty())
          throw new IllegalStateException("There are not tables defined.");
        // read the mapping object
        for (String tableName : getDynamoDbMapping().getTables().keySet())
          executeDeleteTableRequest(tableName);
        LOG.debug("All schemas deleted successfully.");
      } else {
        LOG.debug("create schema " + preferredSchema);
        executeDeleteTableRequest(preferredSchema);
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    try {
      DynamoDBQuery<K, T> dynamoDBQuery = buildDynamoDBQuery(query);
      DynamoDBMapper mapper = new DynamoDBMapper(
              getDynamoDbClient());
      List<T> objList = null;
      if (DynamoDBQuery.getType().equals(DynamoDBQuery.RANGE_QUERY))
        objList = mapper.scan(persistentClass,
                (DynamoDBScanExpression) dynamoDBQuery.getQueryExpression());
      if (DynamoDBQuery.getType().equals(DynamoDBQuery.SCAN_QUERY))
        objList = mapper.scan(persistentClass,
                (DynamoDBScanExpression) dynamoDBQuery.getQueryExpression());
      return new DynamoDBResult<K, T>(this, query, objList);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void flush() throws GoraException {
    LOG.info("DynamoDBNativeStore puts and gets directly into the datastore");
  }

  @Override
  public T get(K key) throws GoraException {
    T object = null;
    try {
      Object rangeKey;
      rangeKey = getRangeKeyFromKey(key);
      Object hashKey = getHashFromKey(key);
      if (hashKey != null) {
        DynamoDBMapper mapper = new DynamoDBMapper(
                getDynamoDbClient());
        if (rangeKey != null)
          object = mapper.load(persistentClass, hashKey, rangeKey);
        else
          object = mapper.load(persistentClass, hashKey);
        return object;

      } else {
        throw new GoraException("Error while retrieving keys from object: "
                + key.toString());
      }
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    DynamoDBQuery<K,T> query = new DynamoDBQuery<K,T>();
    query.setDataStore(this); //query.setKeyRange(key, key);
    query.setFields(fields); //query.setLimit(1);
    Result<K,T> result = execute(query); 
    boolean hasResult = false;
    try {
      hasResult = result.next();
    } catch (Exception e) {
      throw new GoraException(e);
    } 
    return hasResult ? result.get() : null;
  }

  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return null;
  }

  @Override
  public Class<K> getKeyClass() {
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> arg0)
          throws IOException {
    return null;
  }

  @Override
  public Class<T> getPersistentClass() {
    return null;
  }

  @Override
  public String getSchemaName() {
    return this.getPreferredSchema();
  }

  /**
   * Initialize the data store by reading the credentials, setting the client's properties up and
   * reading the mapping file. Initialize is called when then the call to
   * {@link org.apache.gora.store.DataStoreFactory#createDataStore} is made.
   *
   * @param keyClass
   * @param persistentClass
   * @param properties
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
          Properties properties) throws GoraException {
    try {
      LOG.debug("Initializing DynamoDB store");
      setDynamoDBProperties(properties);
      super.initialize(keyClass, persistentClass, properties);
      setWsProvider(WS_PROVIDER);
      if (autoCreateSchema) {
        createSchema();
      }
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Builds a DynamoDB query from a generic Query object
   * 
   * @param query
   *          Generic query object
   * @return DynamoDBQuery
   */
  private DynamoDBQuery<K, T> buildDynamoDBQuery(Query<K, T> query) {
    if (getSchemaName() == null)
      throw new IllegalStateException("There is not a preferred schema set.");

    DynamoDBQuery<K, T> dynamoDBQuery = new DynamoDBQuery<K, T>();
    dynamoDBQuery.setKeySchema(getDynamoDbMapping()
            .getKeySchema(getSchemaName()));
    dynamoDBQuery.setKeyItems(getDynamoDbMapping().getItems(getSchemaName()));
    dynamoDBQuery.setQuery(query);
    dynamoDBQuery.setConsistencyReadLevel(getConsistencyReads());
    dynamoDBQuery.buildExpression();

    return dynamoDBQuery;
  }

  private void setDynamoDBProperties(Properties properties) throws IOException {
    setSerializationType(properties.getProperty(SERIALIZATION_TYPE));
    PropertiesCredentials creds = DynamoDBUtils.getCredentials(this.getClass());
    setPreferredSchema(properties.getProperty(PREF_SCH_NAME));
    setDynamoDBClient(DynamoDBUtils.getClient(properties.getProperty(CLI_TYP_PROP), creds, 
            properties.getProperty(ENDPOINT_PROP), properties.getProperty(REGION_PROP)));
    setDynamoDbMapping(readMapping());
    setConsistency(properties.getProperty(CONSISTENCY_READS));
  }

  @Override
  public K newKey() throws GoraException {
    return null;
  }

  @Override
  public T newPersistent() throws GoraException {
    T obj = null;
    try {
      obj = persistentClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException e) {
      LOG.error("Error instantiating " + persistentClass.getCanonicalName(), e);
      throw new GoraException(e);
    } catch (IllegalAccessException e) {
      LOG.error("Error instantiating " + persistentClass.getCanonicalName(),e );
      throw new GoraException(e);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | SecurityException e) {
      LOG.error("Error instantiating " + persistentClass.getCanonicalName(),e );
      throw new GoraException(e);
    }
    return obj;
  }

  @Override
  public Query<K, T> newQuery() {
    Query<K, T> query = new DynamoDBQuery<K, T>(this);
    // query.setFields(getFieldsToQuery(null));
    return query;
  }

  @Override
  public void put(K key, T value) throws GoraException {
    try {
      Object hashKey = getHashKey(key, value);
      Object rangeKey = getRangeKey(key, value);
      if (hashKey != null) {
        DynamoDBMapper mapper = new DynamoDBMapper(getDynamoDbClient());
        if (rangeKey != null) {
          mapper.load(persistentClass, hashKey, rangeKey);
        } else {
          mapper.load(persistentClass, hashKey);
        }
        mapper.save(value);
      } else
        throw new GoraException("No HashKey found in Key nor in Object.");
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Verifies if the specified schemas exist
   *
   * @return
   */
  @Override
  public boolean schemaExists() throws GoraException {
    try {
      LOG.info("Verifying schemas.");
      TableDescription success = null;
      if (getDynamoDbMapping().getTables().isEmpty())
        throw new IllegalStateException("There are not tables defined.");
      if (getPreferredSchema() == null) {
        LOG.debug("Verifying schemas");
        if (getDynamoDbMapping().getTables().isEmpty())
          throw new IllegalStateException("There are not tables defined.");
        // read the mapping object
        for (String tableName : getDynamoDbMapping().getTables().keySet()) {
          success = getTableSchema(tableName);
          if (success == null)
            return false;
        }
      } else {
        LOG.info("Verifying schema " + preferredSchema);
        success = getTableSchema(preferredSchema);
      }
      LOG.info("Finished verifying schemas.");
      return (success != null) ? true : false;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void setBeanFactory(BeanFactory<K, T> arg0) {
  }

  @Override
  public void setKeyClass(Class<K> arg0) {
    setKeyClass(arg0);
  }

  @Override
  public void setPersistentClass(Class<T> arg0) {
    setPersistentClass(arg0);
  }

  @Override
  public void truncateSchema() throws GoraException {
  }

  /** 
   * Reads the schema file and converts it into a data structure to be used
   *
   * @return DynamoDBMapping Object containing all necessary information to
   *         create tables
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private DynamoDBMapping readMapping() throws IOException {

    DynamoDBMappingBuilder mappingBuilder = new DynamoDBMappingBuilder();

    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader()
              .getResourceAsStream(MAPPING_FILE));
      if (doc == null || doc.getRootElement() == null)
        throw new GoraException("Unable to load " + MAPPING_FILE
                + ". Please check its existance!");

      Element root = doc.getRootElement();
      List<Element> tableElements = root.getChildren("table");
      boolean keys = false;
      for (Element tableElement : tableElements) {

        String tableName = tableElement.getAttributeValue("name");
        long readCapacUnits = Long.parseLong(tableElement
                .getAttributeValue("readcunit"));
        long writeCapacUnits = Long.parseLong(tableElement
                .getAttributeValue("writecunit"));

        mappingBuilder.setProvisionedThroughput(tableName, readCapacUnits,
                writeCapacUnits);
        LOG.debug("Basic table properties have been set: Name, and Provisioned throughput.");

        // Retrieving attributes
        List<Element> fieldElements = tableElement.getChildren("attribute");
        for (Element fieldElement : fieldElements) {
          String key = fieldElement.getAttributeValue("key");
          String attributeName = fieldElement.getAttributeValue("name");
          String attributeType = fieldElement.getAttributeValue("type");
          mappingBuilder.addAttribute(tableName, attributeName, attributeType);
          // Retrieving key's features
          if (key != null) {
            mappingBuilder.setKeySchema(tableName, attributeName, key);
            keys = true;
          }
        }
        LOG.debug("Attributes for table '" + tableName + "' have been read.");
        if (!keys)
          LOG.warn("Keys for table '" + tableName + "' have NOT been set.");
      }
    } catch (IOException ex) {
      LOG.error("Error while performing xml mapping.", ex.getMessage());
      throw new IOException(ex);
    } catch (Exception ex) {
      LOG.error("Error while performing xml mapping.", ex.getMessage());
      throw new RuntimeException(ex);
    }

    return mappingBuilder.build();
  }

  /**
   * Executes a delete table request using the DynamoDB client
   * 
   * @param pTableName
   */
  public void executeDeleteTableRequest(String pTableName) {
    try {
      DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
              .withTableName(pTableName);
      DeleteTableResult result = getDynamoDBClient().deleteTable(
              deleteTableRequest);
      waitForTableToBeDeleted(pTableName);
      LOG.debug("Schema: " + result.getTableDescription()
      + " deleted successfully.");
    } catch (Exception e) {
      LOG.debug("Schema: {} deleted.", pTableName, e.getMessage());
      throw new RuntimeException(e);
    }
  }



  /**
   * Waits up to 6 minutes to confirm if a table has been deleted or not
   * 
   * @param pTableName
   */
  private void waitForTableToBeDeleted(String pTableName) {
    LOG.debug("Waiting for " + pTableName + " to be deleted.");
    long startTime = System.currentTimeMillis();
    long endTime = startTime + WAIT_TIME;
    while (System.currentTimeMillis() < endTime) {
      try {
        Thread.sleep(SLEEP_DELETE_TIME);
      } catch (Exception e) {
      }
      try {
        DescribeTableRequest request = new DescribeTableRequest()
                .withTableName(pTableName);
        TableDescription tableDescription = getDynamoDBClient().describeTable(
                request).getTable();
        String tableStatus = tableDescription.getTableStatus();
        LOG.debug(pTableName + " - current state: " + tableStatus);
      } catch (AmazonServiceException ase) {
        if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == true)
          return;
        LOG.error(ase.getMessage());
      }
    }
    LOG.debug(pTableName + " deleted.");
  }

  /**
   * Retrieves the table description for the specific resource name
   * 
   * @param tableName
   * @return
   */
  private TableDescription getTableSchema(String tableName) {
    TableDescription tableDescription = null;
    try {
      DescribeTableRequest describeTableRequest = new DescribeTableRequest()
              .withTableName(tableName);
      tableDescription = getDynamoDBClient()
              .describeTable(describeTableRequest).getTable();
    } catch (ResourceNotFoundException e) {
      LOG.error("Error while getting table schema: " + tableName);
      return tableDescription;
    }
    return tableDescription;
  }

  /**
   * Gets a specific table key schema.
   * 
   * @param tableName
   *          from which key schema is to be obtained.
   * @return KeySchema from table.
   */
  public ArrayList<KeySchemaElement> getTableKeySchema(String tableName) {
    return getDynamoDbMapping().getKeySchema(tableName);
  }

  /**
   * Gets the provisioned throughput for a specific table.
   * 
   * @param tableName
   *          to get the ProvisionedThroughput.
   * @return ProvisionedThroughput for a specific table
   */
  public ProvisionedThroughput getTableProvisionedThroughput(String tableName) {
    return getDynamoDbMapping().getProvisionedThroughput(tableName);
  }
  /**
   * Returns a table attribues.
   * @param tableName
   * @return
   */
  public Map<String, String> getTableAttributes(String tableName) {
    return getDynamoDbMapping().getItems(tableName);
  }

  /**
   * Gets consistency level for reads
   * 
   * @return True for strong consistency or false for eventual consistent reads
   */
  public boolean getConsistencyReads() {
    if (getConsistency() != null)
      if (getConsistency().equals(CONSISTENCY_READS_TRUE))
        return true;
    return false;
  }

  /**
   * @param serializationType
   *          the serializationType to set
   */
  private void setSerializationType(String serializationType) {
    if (serializationType == null || serializationType.isEmpty()
            || serializationType.equals(DynamoDBUtils.AVRO_SERIALIZATION)) {
      LOG.warn("Using AVRO serialization.");
      this.serializationType = DynamoDBUtils.DynamoDBType.AVRO;
    } else {
      LOG.warn("Using DynamoDB serialization.");
      this.serializationType = DynamoDBUtils.DynamoDBType.DYNAMO;
    }
  }

  /**
   * @return the preferredSchema
   */
  public String getPreferredSchema() {
    return preferredSchema;
  }

  /**
   * @param preferredSchema
   *          the preferredSchema to set
   */
  public void setPreferredSchema(String preferredSchema) {
    this.preferredSchema = preferredSchema;
  }

  /**
   * Gets DynamoDBClient.
   * 
   * @return
   */
  public AmazonDynamoDB getDynamoDbClient() {
    return getDynamoDBClient();
  }


  /**
   * @return the mapping
   */
  public DynamoDBMapping getDynamoDbMapping() {
    return mapping;
  }

  /**
   * @param mapping
   *          the mapping to set
   */
  public void setDynamoDbMapping(DynamoDBMapping mapping) {
    this.mapping = mapping;
  }

  /**
   * @return the consistency
   */
  public String getConsistency() {
    return consistency;
  }

  /**
   * @param consistency
   *          the consistency to set
   */
  public void setConsistency(String consistency) {
    this.consistency = consistency;
  }

  /**
   * @return the dynamoDBClient
   */
  public AmazonDynamoDB getDynamoDBClient() {
    return dynamoDBClient;
  }

  /**
   * @param dynamoDBClient
   *          the dynamoDBClient to set
   */
  public void setDynamoDBClient(AmazonDynamoDB dynamoDBClient) {
    this.dynamoDBClient = dynamoDBClient;
  }

  @Override
  public boolean exists(K key) throws GoraException {
    return exists(key);
  }


  private Object getHashKey(K key, T obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    // try to get the hashKey from 'key'
    Object hashKey = getHashFromKey(key);
    // if the key does not have these attributes then try to get them from the
    // object
    if (hashKey == null)
      hashKey = getHashFromObj(obj);
    // if no key has been found, then we try with the key
    if (hashKey == null)
      hashKey = key;
    return hashKey;
  }

  /**
   * Gets a hash key from a key of type K
   * 
   * @param obj
   *          Object from which we will get a hash key
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getHashFromKey(K obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    Object hashKey = null;
    // check if it is a DynamoDBKey
    if (obj instanceof DynamoDBKey) {
      hashKey = ((DynamoDBKey<?, ?>) obj).getHashKey();
    } else {
      // maybe the class has the method defined
      for (Method met : obj.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_HASH_KEY_METHOD)) {
          Object[] params = null;
          hashKey = met.invoke(obj, params);
          break;
        }
      }
    }
    return hashKey;
  }

  /**
   * Gets a hash key from an object of type T
   * @param <R>
   * 
   * @param obj
   *          Object from which we will get a hash key
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private <R> Object getHashFromObj(R obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    Object hashKey = null;
    // check if it is a DynamoDBKey
    if (obj instanceof DynamoDBKey) {
      hashKey = ((DynamoDBKey) obj).getHashKey();
    } else {
      // maybe the class has the method defined
      for (Method met : obj.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_HASH_KEY_METHOD)) {
          Object[] params = null;
          hashKey = met.invoke(obj, params);
          break;
        }
      }
    }
    return hashKey;
  }

  private Object getRangeKey(K key, T obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    Object rangeKey = getRangeKeyFromKey(key);
    if (rangeKey == null)
      rangeKey = getRangeKeyFromObj(obj);
    return rangeKey;
  }

  /**
   * Gets a range key from a key obj. This verifies if it is using a
   * {@link DynamoDBKey}
   * 
   * @param obj
   *          Object from which a range key will be extracted
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private Object getRangeKeyFromKey(K obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    Object rangeKey = null;
    // check if it is a DynamoDBKey
    if (obj instanceof DynamoDBKey) {
      rangeKey = ((DynamoDBKey<?, ?>) obj).getRangeKey();
    } else {
      // maybe the class has the method defined
      for (Method met : obj.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_RANGE_KEY_METHOD)) {
          Object[] params = null;
          rangeKey = met.invoke(obj, params);
          break;
        }
      }
    }
    return rangeKey;
  }

  /**
   * Gets a range key from an object T
   * @param <R>
   * 
   * @param obj
   *          Object from which a range key will be extracted
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private <R> Object getRangeKeyFromObj(R obj) throws IllegalArgumentException,
  IllegalAccessException, InvocationTargetException {
    Object rangeKey = null;
    // check if it is a DynamoDBKey
    if (obj instanceof DynamoDBKey) {
      rangeKey = ((DynamoDBKey<?, ?>) obj).getRangeKey();
    } else {
      // maybe the class has the method defined
      for (Method met : obj.getClass().getDeclaredMethods()) {
        if (met.getName().equals(GET_RANGE_KEY_METHOD)) {
          Object[] params = null;
          rangeKey = met.invoke(obj, params);
          break;
        }
      }
    }
    return rangeKey;
  }

  @Override
  public String getProvider() {
    return null;
  }

  @Override
  public void setProvider(String arg0) {}

  @Override
  public void write(DataOutput out) throws IOException {}

  @Override
  public void readFields(DataInput in) throws IOException {}
}
