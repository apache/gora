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
package org.apache.gora.dynamodb.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

public class DynamoDBUtils {

  public enum DynamoDBType {
    DYNAMO("native"), AVRO("avro");
    private String value;

    DynamoDBType(String val) {
      this.value = val;
    }

    @Override
    public String toString() {
      return this.value;
    }
  }

  public static final String DYNAMO_KEY_HASHRANGE = "hashrange";
  public static final String DYNAMO_KEY_HASHR = "hash";

  /** AWS Credential file name. */
  public static final String AWS_CREDENTIALS_PROPERTIES = "awscredentials.properties";

  /** Name of the cloud database provider. */
  public static final String WS_PROVIDER = "amazon.web.services";

  /** Parameter to decide what type of Amazon DynamoDB client to use */
  public static final String CLI_TYP_PROP = "gora.dynamodb.client";

  /** Parameter to decide where the data store will make its computations */
  public static final String ENDPOINT_PROP = "gora.dynamodb.endpoint";

  /** Parameter to decide which AWS region to use */
  public static final String REGION_PROP = "gora.dynamodb.region";

  /** Parameter to decide which schema will be used */
  public static final String PREF_SCH_NAME = "preferred.schema.name";

  /**
   * Parameter to decide how reads will be made i.e. using strong consistency or
   * eventual consistency.
   */
  public static final String CONSISTENCY_READS = "gora.dynamodb.consistent.reads";
  public static final String CONSISTENCY_READS_TRUE = "true";

  /**
   * Parameter to decide how serialization will be made i.e. using Dynamodb's or
   * Avro serialization.
   */
  public static final String SERIALIZATION_TYPE = "gora.dynamodb.serialization.type";
  public static final String DYNAMO_SERIALIZATION = "dynamo";
  public static final String AVRO_SERIALIZATION = "avro";

  /** DynamoDB client types. */
  public static final String SYNC_CLIENT_PROP = "sync";
  public static final String ASYNC_CLIENT_PROP = "async";
  public static final String STREAMS_CLIENT_PROP = "streams";
  public static final String STREAMS_ASYNC_CLIENT_PROP = "streamsasync";

  /** The mapping file to create the tables from. */
  public static final String MAPPING_FILE = "gora-dynamodb-mapping.xml";

  /** Default times to wait while requests are performed. */
  public static long WAIT_TIME = 10L * 60L * 1000L;
  public static long SLEEP_TIME = 1000L * 20L;
  public static long SLEEP_DELETE_TIME = 1000L * 10L;

  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBUtils.class);

  /**
   * Method to create the specific client to be used
   * 
   * @param clientType
   * @param credentials
   * @return
   */
  public static AmazonDynamoDB getClient(String clientType,
          AWSCredentials credentials, String endpoint, String region) {
    if (clientType.equals(SYNC_CLIENT_PROP))
      return AmazonDynamoDBClientBuilder
              .standard()
              .withEndpointConfiguration(
                      new AwsClientBuilder.EndpointConfiguration(endpoint, region))
              .withCredentials(
                      new AWSStaticCredentialsProvider(credentials))
              .build();
    if (clientType.equals(ASYNC_CLIENT_PROP))
      return AmazonDynamoDBAsyncClientBuilder
              .standard()
              .withEndpointConfiguration(
                      new AwsClientBuilder.EndpointConfiguration(endpoint, region))
              .withCredentials(
                      new AWSStaticCredentialsProvider(credentials))
              .build();
    if (clientType.equals(STREAMS_CLIENT_PROP))
      return (AmazonDynamoDB) AmazonDynamoDBStreamsClientBuilder
              .standard()
              .withEndpointConfiguration(
                      new AwsClientBuilder.EndpointConfiguration(endpoint, region))
              .withCredentials(
                      new AWSStaticCredentialsProvider(
                              credentials))
              .build();
    if (clientType.equals(STREAMS_ASYNC_CLIENT_PROP))
      return (AmazonDynamoDB) AmazonDynamoDBStreamsAsyncClientBuilder
              .standard()
              .withEndpointConfiguration(
                      new AwsClientBuilder.EndpointConfiguration(endpoint, region))
              .withCredentials(
                      new AWSStaticCredentialsProvider(credentials))
              .build();
    return null;
  }

  /**
   * Creates the AWSCredentials object based on the properties file.
   *
   * @param clazz
   * @return
   */
  public static PropertiesCredentials getCredentials(Class<?> clazz) {
    PropertiesCredentials awsCredentials = null;
    try {
      InputStream awsCredInpStr = clazz.getClassLoader().getResourceAsStream(
              AWS_CREDENTIALS_PROPERTIES);
      if (awsCredInpStr == null)
        LOG.error("AWS Credentials File was not found on the classpath!");
      awsCredentials = new PropertiesCredentials(awsCredInpStr);
    } catch (IOException e) {
      LOG.error("Error loading AWS Credentials File from the classpath!", e.getMessage());
      throw new RuntimeException(e);
    }
    return awsCredentials;
  }

  /**
   * Executes a create table request using the DynamoDB client and waits the
   * default time until it's been created.
   * 
   * @param awsClient
   * @param keySchema
   * @param tableName
   * @param proThrou
   */
  public static void executeCreateTableRequest(AmazonDynamoDB awsClient, String tableName,
          ArrayList<KeySchemaElement> keySchema, Map<String, String> attrs, ProvisionedThroughput proThrou) {
    CreateTableRequest createTableRequest = buildCreateTableRequest(tableName,
            keySchema, proThrou, attrs);
    // use the client to perform the request
    try {
      awsClient.createTable(createTableRequest).getTableDescription();
      // wait for table to become active
      waitForTableToBecomeAvailable(awsClient, tableName);
    } catch (ResourceInUseException ex) {
      LOG.warn("Table '{}' already exists.", tableName);
    } finally {
      LOG.info("Table '{}' is available.", tableName);
    }
  }

  /**
   * Builds the necessary requests to create tables
   * 
   * @param tableName
   * @param keySchema
   * @param proThrou
   * @param attrs 
   * @return
   */
  public static CreateTableRequest buildCreateTableRequest(String tableName,
          ArrayList<KeySchemaElement> keySchema, ProvisionedThroughput proThrou, Map<String, String> attrs) {
    CreateTableRequest createTableRequest = new CreateTableRequest();
    createTableRequest.setTableName(tableName);
    createTableRequest.setKeySchema(keySchema);
    ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
    for (KeySchemaElement kEle : keySchema) {
      AttributeDefinition attrDef = new AttributeDefinition();
      attrDef.setAttributeName(kEle.getAttributeName());
      attrDef.setAttributeType(attrs.get(kEle.getAttributeName()));
      attributeDefinitions.add(attrDef);
    }
    createTableRequest.setAttributeDefinitions(attributeDefinitions);
    createTableRequest.setProvisionedThroughput(proThrou);
    return createTableRequest;
  }

  /**
   * Waits up to 6 minutes to confirm if a table has been created or not
   * 
   * @param awsClient
   * @param tableName
   */
  public static void waitForTableToBecomeAvailable(AmazonDynamoDB awsClient,
          String tableName) {
    LOG.debug("Waiting for {} to become available", tableName);
    long startTime = System.currentTimeMillis();
    long endTime = startTime + WAIT_TIME;
    while (System.currentTimeMillis() < endTime) {
      try {
        Thread.sleep(SLEEP_TIME);
      } catch (Exception e) {
      }
      try {
        DescribeTableRequest request = new DescribeTableRequest()
                .withTableName(tableName);
        TableDescription tableDescription = awsClient.describeTable(request)
                .getTable();
        String tableStatus = tableDescription.getTableStatus();
        LOG.debug("{} - current state: {}", tableName, tableStatus);
        if (tableStatus.equals(TableStatus.ACTIVE.toString()))
          return;
      } catch (AmazonServiceException ase) {
        if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false)
          throw ase;
      }
    }
    throw new RuntimeException("Table " + tableName + " never became active");
  }
}