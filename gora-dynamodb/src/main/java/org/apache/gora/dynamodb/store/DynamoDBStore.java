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
import static org.apache.gora.dynamodb.store.DynamoDBUtils.SERIALIZATION_TYPE;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.SLEEP_DELETE_TIME;
import static org.apache.gora.dynamodb.store.DynamoDBUtils.WAIT_TIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.gora.dynamodb.store.DynamoDBMapping.DynamoDBMappingBuilder;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
public class DynamoDBStore<K, T extends Persistent> implements DataStore<K, T> {

  /** Handler for different serialization modes. */
  private IDynamoDB<K, T> dynamoDbStore;

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
  public void close() {
    dynamoDbStore.close();
  }

  /**
   * Creates the table within the data store for a preferred schema or for a
   * group of schemas defined within the mapping file
   */
  @Override
  public void createSchema() throws GoraException {
    dynamoDbStore.createSchema();
  }

  @Override
  public boolean delete(K key) throws GoraException {
    return dynamoDbStore.delete(key);
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    return dynamoDbStore.deleteByQuery(query);
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
    return dynamoDbStore.execute(query);
  }

  @Override
  public void flush() throws GoraException {
    dynamoDbStore.flush();
  }

  @Override
  public T get(K key) throws GoraException {
    return dynamoDbStore.get(key);
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    return dynamoDbStore.get(key, fields);
  }

  @Override
  public BeanFactory<K, T> getBeanFactory() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Class<K> getKeyClass() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> arg0)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Class<T> getPersistentClass() {
    // TODO Auto-generated method stub
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

      dynamoDbStore = DynamoDBFactory.buildDynamoDBStore(getSerializationType());
      dynamoDbStore.setDynamoDBStoreHandler(this);
      dynamoDbStore.initialize(keyClass, persistentClass, properties);
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  private void setDynamoDBProperties(Properties properties) throws IOException {
    setSerializationType(properties.getProperty(SERIALIZATION_TYPE));
    PropertiesCredentials creds = DynamoDBUtils.getCredentials(this.getClass());
    setPreferredSchema(properties.getProperty(PREF_SCH_NAME));
    setDynamoDBClient(DynamoDBUtils.getClient(
        properties.getProperty(CLI_TYP_PROP), creds));
    getDynamoDBClient().setEndpoint(properties.getProperty(ENDPOINT_PROP));
    setDynamoDbMapping(readMapping());
    setConsistency(properties.getProperty(CONSISTENCY_READS));
  }

  @Override
  public K newKey() throws GoraException {
    return dynamoDbStore.newKey();
  }

  @Override
  public T newPersistent() throws GoraException {
    return dynamoDbStore.newPersistent();
  }

  @Override
  public Query<K, T> newQuery() {
    return dynamoDbStore.newQuery();
  }

  @Override
  public void put(K key, T value) throws GoraException {
    dynamoDbStore.put(key, value);
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
    // TODO Auto-generated method stub
  }

  @Override
  public void setKeyClass(Class<K> arg0) {
    dynamoDbStore.setKeyClass(arg0);
  }

  @Override
  public void setPersistentClass(Class<T> arg0) {
    dynamoDbStore.setPersistentClass(arg0);
  }

  @Override
  public void truncateSchema() throws GoraException {
    // TODO Auto-generated method stub
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
   * Set DynamoDBStore to be used.
   * 
   * @param iDynamoDB
   */
  public void setDynamoDbStore(IDynamoDB<K, T> iDynamoDB) {
    this.dynamoDbStore = iDynamoDB;
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
   * Gets serialization type used inside DynamoDB module.
   * 
   * @return
   */
  private DynamoDBUtils.DynamoDBType getSerializationType() {
    return serializationType;
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
    return dynamoDbStore.exists(key);
  }
}
