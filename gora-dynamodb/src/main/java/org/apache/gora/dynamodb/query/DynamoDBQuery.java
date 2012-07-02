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

import java.util.Collection;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.QueryWSBase;
import org.apache.gora.store.DataStore;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.dynamodb.model.QueryRequest;

public class DynamoDBQuery<K, T extends Persistent> extends QueryWSBase<K, T> {
	
	/**
	 * Query object to perform requests to the datastore
	 */
	//private Query<K, T> query;
	
	/**
	 * Maps fields to DynamoDB attributes.
	 */
	private Collection<String> attributesColl;
	
	/**
	 * Query object to perform requests to the datastore
	 */
	QueryRequest dynamoDBquery;

	public DynamoDBQuery(){
		super(null);
		this.dynamoDBquery = new QueryRequest();
	}
  
	public DynamoDBQuery(DataStore<K, T> dataStore) {
		super(dataStore);
	}
	
	public void setAttrCollection(Collection<String> attrsColl){
		this.attributesColl = attrsColl;
	}
	
	public Collection<String> getAttrCollection(){
		return attributesColl;
	}
	public void setTableName(String tableName){
		this.dynamoDBquery.setTableName(tableName);
	}
	
	public void setLimit(int limit){
		this.dynamoDBquery.setLimit(limit);
	}
	
	public void setConsistencyRead(boolean consistency){
		this.dynamoDBquery.setConsistentRead(consistency);
	}
	
	public void setCredentials(AWSCredentials credentials){
		this.dynamoDBquery.setRequestCredentials(credentials);
	}
  
  /**
   * @param family the family name
   * @return an array of the query column names belonging to the family
   */
  public String[] getColumns(String family) {
    
    //List<String> columnList = attributesMap.get(family);
    String[] columns = new String[2];
    //for (int i = 0; i < columns.length; ++i) {
    //  columns[i] = columnList.get(i);
    //}
    return columns;
  }
  
  public QueryRequest getQuery() {
    return dynamoDBquery;
  }
  
  public void setQuery(Query<K, T> query) {
    this.dynamoDBquery = (QueryRequest)query;
  }

}
