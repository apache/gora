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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.gora.filter.Filter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.QueryWSBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

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

  public static final ComparisonOperator DEFAULT_SCAN_OP = ComparisonOperator.GE;

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
  private ArrayList<KeySchemaElement> keySchema;

  /**
   * Hash key used for the query
   */
  private K hashKey;

  private Map<String, String> keyItems;

  /**
   * Default Constructor
   */
  public DynamoDBQuery(){
    super(null);
  }

  /**
   * Constructor
   * 
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

  private void defineQueryParams() {
    if ((query.getStartKey() != null || query.getKey() != null)
        && query.getEndKey() != null) {
      DynamoDBQuery.setType(RANGE_QUERY);
    } else if (query.getKey() != null || query.getStartKey() != null) {
      DynamoDBQuery.setType(SCAN_QUERY);
    }
  }

  /**
   * Builds query expression depending on query type (range or scan) 
   */
  public void buildExpression() {
    defineQueryParams();
    if (DynamoDBQuery.getType().equals(RANGE_QUERY)) {
      buildRangeExpression();
    } else if (DynamoDBQuery.getType().equals(SCAN_QUERY)) {
      buildScanExpression();
    } else {
      throw new IllegalArgumentException("Query type not supported");
    }
  }

  /**
   * Builds hash key attribute from generic query received.
   * 
   * @param qKey
   * 
   * @returnAttributeValue build from query
   */
  private Map<String, AttributeValue> buildHashKey(K qKey) {
    Map<String, AttributeValue> hashKey = new HashMap<>();
    for (KeySchemaElement key : getKeySchema()) {
      AttributeValue attr = new AttributeValue();
      if (key.getKeyType().equals(KeyType.HASH.toString())) {
        if (keyItems.get(key.getAttributeName()).equals("N")) {
          attr.withN(getHashKey(qKey).toString());
        } else if (keyItems.get(key.getAttributeName()).equals("S")) {
          attr.withS(getHashKey(qKey).toString());
        } else if (keyItems.get(key.getAttributeName()).equals("B")) {
          attr.withB(ByteBuffer.wrap(getHashKey(qKey).toString().getBytes(Charset.defaultCharset())));
        } else {
          throw new IllegalArgumentException("Data type not supported for "
              + key.getAttributeName());
        }
        hashKey.put(key.getAttributeName(), attr);
      }
    }
    if (hashKey.isEmpty()) {
      throw new IllegalStateException("No key value has been defined.");
    }
    return hashKey;
  }

  /**
   * Builds range key attribute from generic query received.
   * 
   * @param qKey
   * 
   * @return
   */
  private Map<String, AttributeValue> buildRangeKey(K qKey) {
    Map<String, AttributeValue> kAttrs = new HashMap<>();
    for (KeySchemaElement key : getKeySchema()) {
      AttributeValue attr = new AttributeValue();
      if (key.getKeyType().equals(KeyType.RANGE.toString())) {
        if (keyItems.get(key.getAttributeName()).equals("N")) {
          attr.withN(getRangeKey(qKey).toString());
        } else if (keyItems.get(key.getAttributeName()).equals("S")) {
          attr.withS(getRangeKey(qKey).toString());
        } else if (keyItems.get(key.getAttributeName()).equals("B")) {
          attr.withB(ByteBuffer.wrap(getRangeKey(qKey).toString().getBytes(Charset.defaultCharset())));
        } else {
          throw new IllegalArgumentException("Data type not supported for "
              + key.getAttributeName());
        }
        kAttrs.put(key.getAttributeName(), attr);
      }
    }
    return kAttrs;
  }

  /**
   * Builds scan query expression using a hash attribute value where to start
   * 
   * @param pHashAttrValueHash
   *          attribute value where to start scanning
   */
  public void buildScanExpression() {
    K qKey = getKey();
    if (qKey == null) {
      LOG.warn("No key defined. Trying with startKey.");
      qKey = query.getStartKey();
      if (qKey == null) {
        throw new IllegalStateException("No key has been defined please check");
      }
    }
    ComparisonOperator compOp = getScanCompOp() != null ? getScanCompOp()
        : DEFAULT_SCAN_OP;

    DynamoDBScanExpression newScanExpression = new DynamoDBScanExpression();
    // hash key condition
    Map<String, AttributeValue> hashAttrVals = buildHashKey(qKey);
    for (Entry<String, AttributeValue> en : hashAttrVals.entrySet()) {
      Condition scanFilterHashCondition = new Condition().withComparisonOperator(
          compOp.toString()).withAttributeValueList(en.getValue());
      newScanExpression.addFilterCondition(en.getKey(), scanFilterHashCondition);
    }
    // range key condition
    Map<String, AttributeValue> rangeAttrVals = buildRangeKey(qKey);
    for (Entry<String, AttributeValue> en : rangeAttrVals.entrySet()) {
      Condition scanFilterRangeCondition = new Condition().withComparisonOperator(
          compOp.toString()).withAttributeValueList(en.getValue());
      newScanExpression.addFilterCondition(en.getKey(), scanFilterRangeCondition);
    }
    dynamoDBExpression = newScanExpression;
  }

  /**
   * Builds range query expression
   * 
   */
  public void buildRangeExpression() {
    DynamoDBScanExpression queryExpression = new DynamoDBScanExpression();
    ComparisonOperator compOp = ComparisonOperator.BETWEEN;
    // hash key range
    Map<String, AttributeValue> hashAttrVals = buildHashKey(query.getStartKey());
    Map<String, AttributeValue> endHashAttrVals = buildHashKey(query.getEndKey());
    for (Entry<String, AttributeValue> en : hashAttrVals.entrySet()) {
      Condition scanFilterHashCondition = new Condition().withComparisonOperator(
          compOp.toString()).withAttributeValueList(en.getValue(), endHashAttrVals.get(en.getKey()));
      queryExpression.addFilterCondition(en.getKey(), scanFilterHashCondition);
    }
    // range key range
    Map<String, AttributeValue> rangeAttrVals = buildRangeKey(query.getStartKey());
    Map<String, AttributeValue> endRangeAttrVals = buildRangeKey(query.getEndKey());
    for (Entry<String, AttributeValue> en : rangeAttrVals.entrySet()) {
      Condition scanFilterRangeCondition = new Condition().withComparisonOperator(
          compOp.toString()).withAttributeValueList(en.getValue(), endRangeAttrVals.get(en.getKey()));
      queryExpression.addFilterCondition(en.getKey(), scanFilterRangeCondition);
    }
    dynamoDBExpression = queryExpression;
  }

  /**
   * Gets hash key for querying
   * 
   * @param key
   * @return
   */
  private Object getHashKey(K key){
    Object hashKey = null;
    try {
      // Our key may be have hash and range keys
      for (Method met : key.getClass().getDeclaredMethods()) {
        if (met.getName().equals("getHashKey")) {
          Object[] params = null;
          hashKey = met.invoke(key, params);
          break;
        }
      }
    } catch (IllegalArgumentException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (IllegalAccessException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new RuntimeException(e);
    }
    return hashKey;
  }

  /**
   * Gets range key for querying from generic query object received
   * 
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
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new IllegalArgumentException(e);
    } catch (IllegalAccessException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      LOG.info("DynamoDBStore: Error while trying to fetch range key.", e.getMessage());
      throw new RuntimeException(e);
    }
    return rangeKey;
  }

  /**
   * Gets read consistency level
   * 
   * @return
   */
  public boolean getConsistencyReadLevel(){
    return consistencyReadLevel;
  }

  /**
   * Sets read consistency level
   * 
   * @param pConsistencyReadLevel
   */
  public void setConsistencyReadLevel(boolean pConsistencyReadLevel){
    this.consistencyReadLevel = pConsistencyReadLevel;
  }

  /**
   * Gets key schema
   * 
   * @return
   */
  public ArrayList<KeySchemaElement> getKeySchema(){
    return keySchema;
  }

  /**
   * Gets query expression for query
   * 
   * @return
   */
  public Object getQueryExpression(){
    return dynamoDBExpression;
  }

  /**
   * Sets query key schema used for queying
   * 
   * @param arrayList
   */
  public void setKeySchema(ArrayList<KeySchemaElement> arrayList) {
    this.keySchema = arrayList;
  }

  /**
   * Sets query to be performed
   * 
   * @param pQuery
   */
  public void setQuery(Query<K, T> pQuery){
    this.setStartKey(query.getStartKey());
    this.setEndKey(query.getEndKey());
  }

  /**
   * Gets query performed
   * 
   * @return
   */
  public Query<K, T> getQuery(){
    return this.query;
  }

  /**
   * Gets query type
   * 
   * @return
   */
  public static String getType() {
    return type;
  }

  /**
   * Sets query type
   * 
   * @param pType
   */
  public static void setType(String pType) {
    type = pType;
  }

  /**
   * Gets scan comparator operator
   * 
   * @return
   */
  public static ComparisonOperator getScanCompOp() {
    return scanCompOp;
  }

  /**
   * Sets scan query comparator operator
   * 
   * @param scanCompOp
   */
  public static void setScanCompOp(ComparisonOperator scanCompOp) {
    DynamoDBQuery.scanCompOp = scanCompOp;
  }

  /**
   * Gets range query comparator operator
   * 
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

  /**
   * Sets the keyItems that could be used.
   * 
   * @param items
   */
  public void setKeyItems(Map<String, String> items) {
    keyItems = items;
  }

  @Override
  public void setFilter(Filter<K, T> filter) {
    // TODO Auto-generated method stub

  }

  @Override
  public Filter<K, T> getFilter() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setLocalFilterEnabled(boolean enable) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isLocalFilterEnabled() {
    // TODO Auto-generated method stub
    return false;
  }
}
