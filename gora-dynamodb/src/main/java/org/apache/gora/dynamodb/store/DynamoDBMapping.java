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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;

public class DynamoDBMapping {
  
  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBMapping.class);
  
  /**
   *  a map from field name to attribute value
   */
  private final Map<String, List<Map<String, String>>> tablesToItems;
  
  /**
   * Maps tables to their own key schemas
   */
  private final Map<String, KeySchema> tablesToKeySchemas;
  
  /**
   * Maps tables to their provisioned throughput
   */
  private final Map<String, ProvisionedThroughput> tablesToPrTh;
  
  /**
   * Constructor for DynamoDBMapping 
   * @param tables	Tables mapped.
   * @param tablesToKeySchemas	KeySchemas used within tables mapped.
   * @param provisionedThroughput	Provisioned throughput used within tables mapped.
   */
  public DynamoDBMapping(Map<String, List<Map<String, String>>> tables,
    Map<String, KeySchema> tablesToKeySchemas,
    Map<String, ProvisionedThroughput> provisionedThroughput) {
    
    this.tablesToItems = tables;
    this.tablesToKeySchemas = tablesToKeySchemas;
    this.tablesToPrTh = provisionedThroughput;
  }

  /**
   * Gets the tables with their own items
   * @return tablesToItem HashMap 
   */
  public Map<String,List<Map<String, String>>> getTables(){
    return tablesToItems;
  }
  
  /**
   * Gets items or attributes from a specific table
   * @param tableName	Table name to determine which attributes to get 
   * @return
   */
  public List<Map<String, String>> getItems(String tableName){
    return tablesToItems.get(tableName);
  }

  /**
   * Gets the key schema from a specific table
   * @param tableName	Table name to determine which key schema to get
   * @return
   */
  public KeySchema getKeySchema(String tableName) {
    return tablesToKeySchemas.get(tableName);
  }
  
  /**
   * Gets the provisioned throughput from a specific table
   * @param tableName	Table name to determine which provisioned throughput to get
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
     * Table name to be used to build the DynamoDBMapping object 
     */
    private String tableName;
	  
    /**
     * This data structure can hold several tables, with their own items.
     * Map<TableName, List<Map<AttributeName,AttributeType>>
     */
    private Map<String, List<Map<String, String>>> tablesToItems = 
      new HashMap<String, List<Map<String, String>>>();
	
    /**
     * Maps tables to key schemas
     */
    private Map<String, KeySchema> tablesToKeySchemas = new HashMap<String, KeySchema>();
	
    /**
     * Maps tables to provisioned throughput
     */
    private Map<String, ProvisionedThroughput> tablesToPrTh = new HashMap<String, ProvisionedThroughput>();
	  
    /**
     * Sets table name
     * @param tabName
     */
    public void setTableName(String tabName){
      tableName = tabName;
    }
	  
    /**
     * Gets the table name for which the table is being mapped
     * @param tableName
     * @return
     */
    public String getTableName(String tableName){
      return tableName;
    }
	  
    /**
     * Sets the provisioned throughput for the specified table
     * @param tableName
     * @param readCapUnits
     * @param writeCapUnits
     */
    public void setProvisionedThroughput(String tableName, long readCapUnits, long writeCapUnits){
      ProvisionedThroughput ptDesc = 
      new ProvisionedThroughput().withReadCapacityUnits(readCapUnits).withWriteCapacityUnits(writeCapUnits);
      tablesToPrTh.put(tableName, ptDesc);
    }
	  
    /**
     * Sets the hash range key schema for the specified table
     * @param tableName
     * @param rangeKeyName
     * @param rangeKeyType
     */
    public void setHashRangeKeySchema(String tableName, String rangeKeyName, String rangeKeyType){
      KeySchema kSchema = tablesToKeySchemas.get(tableName);
      if ( kSchema == null)
        kSchema = new KeySchema();
   
	KeySchemaElement rangeKeyElement = 
	new KeySchemaElement().withAttributeName(rangeKeyName).withAttributeType(rangeKeyType);
	kSchema.setRangeKeyElement(rangeKeyElement);
	tablesToKeySchemas.put(tableName, kSchema);
    }
	  
    /**
     * Sets the hash key schema for the specified table
     * @param tableName
     * @param keyName
     * @param keyType
     */
    public void setHashKeySchema(String tableName, String keyName, String keyType){
      KeySchema kSchema = tablesToKeySchemas.get(tableName);
        if ( kSchema == null)
	  kSchema = new KeySchema();
	  KeySchemaElement hashKey = 
	  new KeySchemaElement().withAttributeName(keyName).withAttributeType(keyType);
          kSchema.setHashKeyElement(hashKey);
	  tablesToKeySchemas.put(tableName, kSchema);
    }
	  
    /**
     * Checks if a table exists, and if doesn't exist it creates the new table. 
     * @param tableName
     * @return The table identified by the parameter
     */
    private List<Map<String, String>> getOrCreateTable(String tableName) {
      
      List<Map<String, String>> items = tablesToItems.get(tableName);
      if (items == null) {
        items = new ArrayList<Map<String, String>>();
        tablesToItems.put(tableName, items);
      }
      return items;
    }
	  
    /**
     * Gets the attribute for a specific item. The idea is to be able to get different items with different attributes.
     * TODO This method is incomplete because the itemNumber might not be present and this would be a problem
     * @param items
     * @param itemNumber
     * @return
     */
    private HashMap<String, String> getOrCreateItemAttribs(List<Map<String, String>> items, int itemNumber){
      HashMap<String, String> itemAttribs;
   	  
      if (items.isEmpty())
        items.add(new HashMap<String, String>());
   	  
   	itemAttribs = (HashMap<String, String>) items.get(itemNumber);
   	if (itemAttribs == null)
   	  items.add(new HashMap<String, String>());
   	  return (HashMap<String, String>) items.get(itemNumber);
    }
      
    /**
     * Adds an attribute to an specific item
     * @param tableName
     * @param attributeName
     * @param attrType
     * @param itemNumber
     */
     public void addAttribute(String tableName, String attributeName, String attrType, int itemNumber) {
       // selecting table
       List<Map<String, String>> items = getOrCreateTable(tableName);
       // add attribute to item
       HashMap<String, String> itemAttribs = getOrCreateItemAttribs(items, itemNumber);
       itemAttribs.put(attributeName, attrType);
       //items.add(itemAttribs);
       // add item to table
       //tablesToItems.put(tableName, items);
     }
	  
    /**
     * Method to verify whether or not the schemas have been initialized
     * @return
     */
    private String verifyAllKeySchemas(){
	  
      String wrongTable = "";
      // if there are not tables defined
      if (tablesToItems.isEmpty()) return "";
        for(String tableName : tablesToItems.keySet()){
	  // if there are not schemas defined
	  if (tablesToKeySchemas.isEmpty()) return "";
	    if (!verifyKeySchema(tableName)) return "";
        }
      return wrongTable;
    }
	  
    /**
     * Verifies is a table has a key schema defined
     * @param tableName	Table name to determine which key schema to obtain 
     * @return
     */
    private boolean verifyKeySchema(String tableName){
      KeySchema kSchema = tablesToKeySchemas.get(tableName);
	  
      if (kSchema == null) 
        return false;
			  
	KeySchemaElement rangeKey = kSchema.getRangeKeyElement();
	KeySchemaElement hashKey = kSchema.getHashKeyElement();
	// A range key must have a hash key as well
	if (rangeKey != null){
	  if (hashKey != null)	
	    return true;
	  else 	  
	    return false;
	}
	// A hash key may exist by itself
	if (hashKey != null)	  
	  return true;
	  return false;
    }
	  
    /**
     * Constructs the DynamoDBMapping object
     * @return A newly constructed mapping.
     */
    public DynamoDBMapping build() {
	  
      if (tableName == null) throw new IllegalStateException("tableName is not specified");
    
        // verifying items for at least a table
        if (tablesToItems.isEmpty()) throw new IllegalStateException("No tables");
      
	  // verifying if key schemas have been properly defined
	  String wrongTableName = verifyAllKeySchemas();  
	  if (!wrongTableName.equals("")) throw new IllegalStateException("no key schemas defined for table " + wrongTableName);
     
	    // Return the tableDescription and all the attributes needed
            return new DynamoDBMapping(tablesToItems,tablesToKeySchemas, tablesToPrTh);
    }
  }
}
