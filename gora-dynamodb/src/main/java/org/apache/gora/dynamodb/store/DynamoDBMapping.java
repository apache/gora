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

import static org.apache.gora.dynamodb.store.DynamoDBUtils.DYNAMO_KEY_HASHRANGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class DynamoDBMapping {

  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBMapping.class);

  /**
   *  a map from field name to attribute value
   */
  private final Map<String, Map<String, String>> tablesToItems;

  /**
   * Maps tables to their own key schemas
   */
  private final Map<String, ArrayList<KeySchemaElement>> tablesToKeySchemas;

  /**
   * Maps tables to their provisioned throughput
   */
  private final Map<String, ProvisionedThroughput> tablesToPrTh;

  /**
   * Constructor for DynamoDBMapping
   * 
   * @param tablesToItems2
   *          Tables mapped.
   * @param tablesToKeySchemas
   *          KeySchemas used within tables mapped.
   * @param provisionedThroughput
   *          Provisioned throughput used within tables mapped.
   */
  public DynamoDBMapping(Map<String, Map<String, String>> tablesToItems2,
      Map<String, ArrayList<KeySchemaElement>> tablesToKeySchemas,
      Map<String, ProvisionedThroughput> provisionedThroughput) {

    this.tablesToItems = tablesToItems2;
    this.tablesToKeySchemas = tablesToKeySchemas;
    this.tablesToPrTh = provisionedThroughput;
  }

  /**
   * Gets the tables with their own items
   * 
   * @return tablesToItem 
   *          HashMap 
   */
  public Map<String, Map<String, String>> getTables() {
    return tablesToItems;
  }

  /**
   * Gets items or attributes from a specific table
   * 
   * @param tableName
   *          table name to determine which attributes to get 
   * @return
   */
  public Map<String, String> getItems(String tableName) {
    return tablesToItems.get(tableName);
  }

  /**
   * Gets the key schema from a specific table
   * @param tableName
   *          Table name to determine which key schema to get
   * @return
   */
  public ArrayList<KeySchemaElement> getKeySchema(String tableName) {
    return tablesToKeySchemas.get(tableName);
  }

  /**
   * Gets the provisioned throughput from a specific table
   * 
   * @param tableName
   *          Table name to determine which provisioned throughput to get
   * @return
   */
  public ProvisionedThroughput getProvisionedThroughput(String tableName){
    return tablesToPrTh.get(tableName);
  }

  /**
   * A builder for creating the mapper. This will allow building a thread safe
   * {@link DynamoDBMapping} using simple immutabilty.
   *
   */
  public static class DynamoDBMappingBuilder {

    /**
     * This data structure can hold several tables, with their own items.
     * Map<TableName, List<Map<AttributeName,AttributeType>>
     */
    private Map<String, Map<String, String>> tablesToItems = 
        new HashMap<String, Map<String, String>>();

    /**
     * Maps tables to key schemas
     */
    private Map<String, ArrayList<KeySchemaElement>> tablesToKeySchemas = 
        new HashMap<String, ArrayList<KeySchemaElement>>();

    /**
     * Maps tables to provisioned throughput
     */
    private Map<String, ProvisionedThroughput> tablesToPrTh = 
        new HashMap<String, ProvisionedThroughput>();

    /**
     * Gets the table name for which the table is being mapped
     * 
     * @param tableName
     * @return
     */
    public String getTableName(String tableName){
      return tableName;
    }

    /**
     * Sets the provisioned throughput for the specified table
     * 
     * @param tableName
     * @param readCapUnits
     * @param writeCapUnits
     */
    public void setProvisionedThroughput(String tableName, long readCapUnits,
        long writeCapUnits) {
      @SuppressWarnings("unused")
      ProvisionedThroughput ptDesc = new ProvisionedThroughput()
          .withReadCapacityUnits(readCapUnits).withWriteCapacityUnits(
              writeCapUnits);
    }

    /**
     * Sets the hash range key schema for the specified table
     * 
     * @param tableName
     * @param rangeKeyName
     * @param rangeKeyType
     */
    // public void setHashRangeKeySchema(String tableName, String rangeKeyName,
    // String rangeKeyType){
    // KeySchemaElement kSchema = tablesToKeySchemas.get(tableName);
    // if ( kSchema == null)
    // kSchema = new KeySchemaElement();

    // KeySchemaElement rangeKeyElement = new
    // KeySchemaElement().withAttributeName(rangeKeyName).withKeyType(KeyType.RANGE).withKeyType(rangeKeyType);
    // kSchema.
    // kSchema.setRangeKeyElement(rangeKeyElement);
    // tablesToKeySchemas.put(tableName, kSchema);
    // }

    /**
     * Sets the hash key schema for the specified table
     *
     * @param tableName
     * @param keyName
     * @param keyType
     */
    public void setKeySchema(String tableName, String keyName, String keyType) {
      ArrayList<KeySchemaElement> kSchema = tablesToKeySchemas.get(tableName);
      if (kSchema == null) {
        kSchema = new ArrayList<KeySchemaElement>();
        tablesToKeySchemas.put(tableName, kSchema);
      }
      KeyType type = keyType.equals(DYNAMO_KEY_HASHRANGE) ? KeyType.RANGE : KeyType.HASH;
      kSchema.add(new KeySchemaElement().withAttributeName(keyName)
          .withKeyType(type));
    }

    /**
     * Checks if a table exists, and if doesn't exist it creates the new table.
     * 
     * @param tableName
     * @return The table identified by the parameter
     */
    private Map<String, String> getOrCreateTable(String tableName) {
      Map<String, String> items = tablesToItems.get(tableName);
      if (items == null) {
        items = new HashMap<String, String>();
        tablesToItems.put(tableName, items);
      }
      return items;
    }

    /**
     * Gets the attribute for a specific item. The idea is to be able to get 
     * different items with different attributes.
     * TODO This method is incomplete because the itemNumber might not 
     * be present and this would be a problem
     * 
     * @param items
     * @param itemNumber
     * @return
     */
    /*private HashMap<String, String> getOrCreateItemAttribs(
        Map<String, String> items) {
      HashMap<String, String> itemAttribs;

      if (items.isEmpty())
        items.add(new HashMap<String, String>());

      itemAttribs = (HashMap<String, String>) items.get(itemNumber);
      if (itemAttribs == null) {
        itemAttribs = new HashMap<String, String>();
      }

      items.add(itemAttribs);
      return null;
    }*/

    /**
     * Adds an attribute to an specific item
     *
     * @param tableName
     * @param attributeName
     * @param attrType
     */
    public void addAttribute(String tableName, String attributeName,
        String attrType) {
      // selecting table
      Map<String, String> items = getOrCreateTable(tableName);
      // add attribute to item
      //HashMap<String, String> itemAttribs = getOrCreateItemAttribs(items);
      //itemAttribs.put(attributeName, attrType);
      // add item to table
      items.put(attributeName, attrType);
      // tablesToItems.put(tableName, items);
    }

    /**
     * Method to verify whether or not the schemas have been initialized
     * 
     * @return
     */
    private boolean verifyAllKeySchemas() {
      boolean rsl = true;
      if (tablesToItems.isEmpty() || tablesToKeySchemas.isEmpty())
        rsl = false;
      for (String tableName : tablesToItems.keySet()) {
        // if there are not schemas defined
        if (tablesToKeySchemas.get(tableName) == null) {
          LOG.error("No schema defined for DynamoDB table '" + tableName + '\'');
          rsl = false;
        }
        rsl = verifyKeySchema(tableName);
      }
      return rsl;
    }

    /**
     * Verifies is a table has a key schema defined
     * 
     * @param tableName	Table name to determine which key schema to obtain 
     * @return
     */
    private boolean verifyKeySchema(String tableName) {
      ArrayList<KeySchemaElement> kSchema = tablesToKeySchemas.get(tableName);
      boolean hashPk = false;
      if (kSchema == null) {
        LOG.error("No keys defined for '{}'. Please check your schema!", tableName);
        return hashPk;
      }
      for (KeySchemaElement ks : kSchema) {
        if (ks.getKeyType().equals(KeyType.HASH.toString())) {
          hashPk = true;
        }
      }
      return hashPk;
    }

    /**
     * Constructs the DynamoDBMapping object
     * 
     * @return A newly constructed mapping.
     */
    public DynamoDBMapping build() {

      // verifying items for at least a table
      if (tablesToItems.isEmpty())
        throw new IllegalStateException("No tables were defined.");

      // verifying if key schemas have been properly defined
      if (!verifyAllKeySchemas())
        throw new IllegalStateException("no key schemas defined for table ");

      // Return the tableDescription and all the attributes needed
      return new DynamoDBMapping(tablesToItems, tablesToKeySchemas,
          tablesToPrTh);
    }
  }
}
