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

package org.apache.gora.dynamodb.query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.QueryWSBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;

public class DynamoDBQuery<K, T extends Persistent> extends QueryWSBase<K, T> {
	
  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBQuery.class);
	
  /**
   * Reads consistency level
   */
  private boolean consistencyReadLevel;
	
  /**
   * Range comparator operator
   */
  private static ComparisonOperator rangeCompOp;
	
  /**
   * Scan comparator operator
   */
  private static ComparisonOperator scanCompOp;
	
  /**
   * Range query type property
   */
  public static final String RANGE_QUERY = "range";
	
  /**
   * Scan query type property
   */
  public static final String SCAN_QUERY = "scan";
	
  /**
   * Query type property
   */
  private static String type;
	
  /**
   * Generic query
   */
  private Query<K, T> query;
	
  /**
   * DynamoDB Expression to be used.
   * This could be a range or a scan DynamoDB Expression
   */
  private Object dynamoDBExpression;
	
  /**
   * Key schema used for the query
   */
  private KeySchema keySchema;
	
  /**
   * Hash key used for the query
   */
  private K hashKey;
	
  /**
   * Default Constructor
   */
  public DynamoDBQuery(){
  	super(null);
  }
  
  /**
   * Constructor
   * @param dataStore
   */
  public DynamoDBQuery(DataStore<K, T> dataStore) {
  	super(dataStore);
  }
	
  /**
   * Sets hash key
   */
  @Override
  public void setKey(K key) {
    this.hashKey = key;
  }
	
  /**
   * Gets hash key
   */
  @Override
  public K getKey() {
    return this.hashKey;
  }
	
  /**
   * Builds query expression depending on query type (range or scan) 
   */
  public void buildExpression(){
    AttributeValue hashAttrValue = buildKeyHashAttribute();
    if (hashAttrValue == null)
      throw new IllegalStateException("There is not a key schema defined.");
    if (DynamoDBQuery.getType().equals(RANGE_QUERY)){
      Condition newCondition = buildRangeCondition();
      buildQueryExpression(newCondition, hashAttrValue);
    }
    if (DynamoDBQuery.getType().equals(SCAN_QUERY))
      buildScanExpression(hashAttrValue);
  }
	
  /**
   * Builds scan query expression using a hash attribute value where to start
   * @param pHashAttrValue	Hash attribute value where to start scanning
   */
  public void buildScanExpression(AttributeValue pHashAttrValue){
    DynamoDBScanExpression newScanExpression = new DynamoDBScanExpression();
    // TODO right now we only support scanning using the key, but we should support other types of scans
    newScanExpression.addFilterCondition(getKeySchema().getHashKeyElement().getAttributeName(), buildKeyScanCondition());
    dynamoDBExpression = newScanExpression;
  }
	
  /**
   * Builds range query expression
   * @param pNewCondition		Condition for querying
   * @param pHashAttrValue	Hash attribute value where to start
   */
  public void buildQueryExpression(Condition pNewCondition, AttributeValue pHashAttrValue) {
    DynamoDBQueryExpression newQueryExpression = new DynamoDBQueryExpression(pHashAttrValue); 
    newQueryExpression.setConsistentRead(getConsistencyReadLevel());
    newQueryExpression.setRangeKeyCondition(pNewCondition);
    dynamoDBExpression = newQueryExpression;
  }
	
  /**
   * Builds hash key attribute from generic query received
   * @return	AttributeValue build from query
   */
  private AttributeValue buildKeyHashAttribute(){
    String pAttrType = getKeySchema().getHashKeyElement().getAttributeType();
    if(pAttrType.equals("S"))
      return new AttributeValue().withS(getHashKey(query.getKey()).toString());
    else if(pAttrType.equals("N"))
      return new AttributeValue().withN(getHashKey(query.getKey()).toString());
    return null;
  }
	
  /**
   * Gets hash key for querying
   * @param key
   * @return
   */
  private Object getHashKey(K key){
    Object hashKey = null;
    try {
    // Our key may be have hash and range keys
    for (Method met :key.getClass().getDeclaredMethods()){
      if(met.getName().equals("getHashKey")){
        Object [] params = null;
        hashKey = met.invoke(key, params);
        break;
      }
    }
  } catch (IllegalArgumentException e) {
    LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
  } catch (IllegalAccessException e) {
    LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
  } catch (InvocationTargetException e) {
    LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
  }
  return hashKey;
  }

  /**
   * Gets range key for querying from generic query object received
   * @param key
   * @return
   */
  private Object getRangeKey(K key){
    Object rangeKey = null;
    try {
        // Our key may be have hash and range keys
      for (Method met :key.getClass().getDeclaredMethods()){
      if(met.getName().equals("getRangeKey")){
        Object [] params = null;
        rangeKey = met.invoke(key, params);
        break;
        }
      }
    } catch (IllegalArgumentException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
    } catch (IllegalAccessException e) {
    LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
    } catch (InvocationTargetException e) {
    LOG.info("DynamoDBStore: Error while trying to fetch range key.");
    e.printStackTrace();
    }
    return rangeKey;
  }

  /**
   * Builds key scan condition using scan comparator, and hash key attribute
   * @return
   */
  private Condition buildKeyScanCondition(){
  Condition scanKeyCondition = new Condition();
  scanKeyCondition.setComparisonOperator(getScanCompOp());
  scanKeyCondition.withAttributeValueList(buildKeyHashAttribute());
  return scanKeyCondition;
  }

  /**
   * Builds range condition based on elements set 
   * @return
   */
  private Condition buildRangeCondition(){
  KeySchemaElement kRangeSchema = getKeySchema().getRangeKeyElement();
  Condition rangeKeyCondition = null;
  if(kRangeSchema != null){
    rangeKeyCondition = new Condition();
    rangeKeyCondition.setComparisonOperator(ComparisonOperator.BETWEEN.toString());
    AttributeValue startVal = null, endVal = null;
    //startVal = buildKeyHashAttribute();
    if(kRangeSchema.getAttributeType().equals("S")){
      startVal = new AttributeValue().withS(getRangeKey(query.getStartKey()).toString());
      endVal = new AttributeValue().withS(getRangeKey(query.getEndKey()).toString());
    }
    else if (kRangeSchema.getAttributeType().equals("N")){
      startVal = new AttributeValue().withN(getRangeKey(query.getStartKey()).toString());
      endVal = new AttributeValue().withN(getRangeKey(query.getEndKey()).toString());
    }
    rangeKeyCondition.withAttributeValueList(startVal, endVal);
  }
  return rangeKeyCondition;
  }

  /**
   * Gets read consistency level
   * @return
   */
  public boolean getConsistencyReadLevel(){
    return consistencyReadLevel;
  }
	
  /**
   * Sets read consistency level
   * @param pConsistencyReadLevel
   */
  public void setConsistencyReadLevel(boolean pConsistencyReadLevel){
    this.consistencyReadLevel = pConsistencyReadLevel;
  }
	
  /**
   * Gets key schema
   * @return
   */
  public KeySchema getKeySchema(){
    return keySchema;
  }

  /**
   * Gets query expression for query
   * @return
   */
  public Object getQueryExpression(){
    return dynamoDBExpression;
  }

  /**
   * Sets query key schema used for queying
   * @param pKeySchema
   */
  public void setKeySchema(KeySchema pKeySchema){
    this.keySchema = pKeySchema;
  }

  /**
   * Sets query to be performed
   * @param pQuery
   */
  public void setQuery(Query<K, T> pQuery){
    this.query = pQuery;
  }
	
  /**
   * Gets query performed
   * @return
   */
  public Query<K, T> getQuery(){
    return this.query;
  }

  /**
   * Gets query type
   * @return
   */
  public static String getType() {
    return type;
  }

  /**
   * Sets query type
   * @param pType
   */
  public static void setType(String pType) {
    type = pType;
  }

  /**
   * Gets scan comparator operator
   * @return
   */
  public static ComparisonOperator getScanCompOp() {
    if (scanCompOp == null)
      scanCompOp = ComparisonOperator.GE;
    return scanCompOp;
  }

  /**
   * Sets scan query comparator operator
   * @param scanCompOp
   */
  public static void setScanCompOp(ComparisonOperator scanCompOp) {
    DynamoDBQuery.scanCompOp = scanCompOp;
  }
	
  /**
   * Gets range query comparator operator
   * @return
   */
  public static ComparisonOperator getRangeCompOp(){
    if (rangeCompOp == null)
      rangeCompOp = ComparisonOperator.BETWEEN;
    return rangeCompOp;
  }
	
  /**
   * Sets range query comparator operator
   * @param pRangeCompOp
   */
  public static void setRangeCompOp(ComparisonOperator pRangeCompOp){
    rangeCompOp = pRangeCompOp;
  }
}
