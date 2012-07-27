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

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.QueryWSBase;
import org.apache.gora.store.DataStore;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;

public class DynamoDBQuery<K, T extends Persistent> extends QueryWSBase<K, T> {
	
	private boolean consistencyReadLevel;
	
	private static ComparisonOperator rangeCompOp;
	
	private static ComparisonOperator scanCompOp;
	
	private static final String RANGE_QUERY = "range";
	
	private static final String SCAN_QUERY = "scan";
	
	private String type;
	
	private Query<K, T> query;
	
	private Object dynamoDBExpression;
	
	private KeySchema keySchema;
	
	public DynamoDBQuery(){
		super(null);
	}
  
	public DynamoDBQuery(DataStore<K, T> dataStore) {
		super(dataStore);
	}
	public void buildExpression(){
		Condition newCondition = buildRangeCondition();
		AttributeValue hashAttrValue = buildKeyHashAttribute();
		if (hashAttrValue == null)
			throw new IllegalStateException("There is not a key schema defined.");
		if(newCondition != null){
			setType(RANGE_QUERY);
			buildQueryExpression(newCondition, hashAttrValue);
		}
		else{
			setType(SCAN_QUERY);
			buildScanExpression(hashAttrValue);
		}
	}
	public void buildScanExpression(AttributeValue pHashAttrValue){
		DynamoDBScanExpression newScanExpression = new DynamoDBScanExpression();
		// TODO right now we only support scanning using the key, but we should support other types of scans
		newScanExpression.addFilterCondition(getKeySchema().getHashKeyElement().getAttributeName(), buildKeyScanCondition());
		dynamoDBExpression = newScanExpression;
	}
	
	public void buildQueryExpression(Condition pNewCondition, AttributeValue pHashAttrValue) {
		DynamoDBQueryExpression newQueryExpression = new DynamoDBQueryExpression(pHashAttrValue); 
		newQueryExpression.setConsistentRead(getConsistencyReadLevel());
		newQueryExpression.setRangeKeyCondition(pNewCondition);
		dynamoDBExpression = newQueryExpression;
	}
	
	private AttributeValue buildKeyHashAttribute(){
		String pAttrType = getKeySchema().getHashKeyElement().getAttributeType();
		if(pAttrType.equals("S"))
			return new AttributeValue().withS(query.getKey().toString());
		else if(pAttrType.equals("N"))
			return new AttributeValue().withN(query.getKey().toString());
		return null;
	}
	
	private Condition buildKeyScanCondition(){
		Condition scanKeyCondition = new Condition();
		scanKeyCondition.setComparisonOperator(getScanCompOp());
		scanKeyCondition.withAttributeValueList(buildKeyHashAttribute());
		return scanKeyCondition;
	}
	private Condition buildRangeCondition(){
		KeySchemaElement kRangeSchema = getKeySchema().getRangeKeyElement();
		Condition rangeKeyCondition = null;
		if(kRangeSchema != null){
			rangeKeyCondition = new Condition();
			rangeKeyCondition.setComparisonOperator(ComparisonOperator.BETWEEN.toString());
			AttributeValue startVal, endVal = null;
			startVal = buildKeyHashAttribute();
			if(kRangeSchema.getAttributeType().equals("S"))
				endVal = new AttributeValue().withS(query.getStartKey().toString());
			else if (kRangeSchema.getAttributeType().equals("N"))
				endVal = new AttributeValue().withN(query.getStartKey().toString());
			rangeKeyCondition.withAttributeValueList(startVal, endVal);
		}
		return rangeKeyCondition;
	}
	
	public boolean getConsistencyReadLevel(){
		return consistencyReadLevel;
	}
	
	public void setConsistencyReadLevel(boolean pConsistencyReadLevel){
		this.consistencyReadLevel = pConsistencyReadLevel;
	}
	
	public KeySchema getKeySchema(){
		return keySchema;
	}
	
	public Object getQueryExpression(){
		return dynamoDBExpression;
	}
	
	public void setKeySchema(KeySchema pKeySchema){
		this.keySchema = pKeySchema;
	}
	
	public void setQuery(Query<K, T> pQuery){
		this.query = pQuery;
	}
	
	public Query<K, T> getQuery(){
		return this.query;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String pType) {
		this.type = pType;
	}

	public static ComparisonOperator getScanCompOp() {
		if (scanCompOp == null)
			scanCompOp = ComparisonOperator.GE;
		return scanCompOp;
	}

	public static void setScanCompOp(ComparisonOperator scanCompOp) {
		DynamoDBQuery.scanCompOp = scanCompOp;
	}
	
	public static ComparisonOperator getRangeCompOp(){
		if (rangeCompOp == null)
			rangeCompOp = ComparisonOperator.BETWEEN;
		return rangeCompOp;
	}
	
	public static void setRangeCompOp(ComparisonOperator pRangeCompOp){
		rangeCompOp = pRangeCompOp;
	}
}
