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

package org.apache.gora.query.ws.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;

/**
 * Base class for Query implementations.
 */
public abstract class QueryWSBase<K, T extends Persistent> implements Query<K,T>{
	
  /**
   * Data store used for this query
   */
  protected DataStore<K,T> dataStore;

  /**
   * Query represented in a string
   */
  protected String queryString;
  
  /**
   * Fields to be retrieved
   */
  protected String[] fields;

  /**
   * Range key parameters
   */
  protected K startKey;
  protected K endKey;

  /**
   * Query time parameters
   */
  protected long startTime = -1;
  protected long endTime = -1;

  /**
   * Query filter
   */
  protected String filter;

  /**
   * Max number of results to be retrieved
   */
  protected long limit = -1;

  /**
   * Flag to determine whether a query is compiled or not
   */
  protected boolean isCompiled = false;

  /** Object that will hold user's authentication tokens for webservice database */
  private Object authentication;

  /**
   * Constructor
   * @param dataStore
   */
  public QueryWSBase(DataStore<K,T> dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  /**
   * Executes the query
   */
  public Result<K,T> execute() throws Exception {
    //compile();
    return dataStore.execute(this);
  }

  @Override
  /**
   * Sets the data store to be used
   */
  public void setDataStore(DataStore<K, T> dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  /**
   * Gets the data store used for querying
   */
  public DataStore<K, T> getDataStore() {
    return dataStore;
  }

  @Override
  /**
   * Sets the fields to be retrieved
   */
  public void setFields(String... fields) {
    this.fields = fields;
  }

  @Override
  /**
   * Gets the fields to be retrieved
   */
  public String[] getFields() {
    return fields;
  }

  @Override
  /**
   * Sets the key to be used for querying
   */
  public void setKey(K key) {
    setKeyRange(key, key);
  }

  @Override
  /**
   * Sets the start key to be used for querying
   */
  public void setStartKey(K startKey) {
    this.startKey = startKey;
  }

  @Override
  /**
   * Sets the end key to be used for querying
   */
  public void setEndKey(K endKey) {
    this.endKey = endKey;
  }

  @Override
  /**
   * Sets the range key to be used for querying
   */
  public void setKeyRange(K startKey, K endKey) {
    this.startKey = startKey;
    this.endKey = endKey;
  }

  @Override
  /**
   * Gets the key to be used for querying
   */
  public K getKey() {
    if(startKey == endKey) {
      return startKey; //address comparison
    }
    return null;
  }

  @Override
  /**
   * Gets the start key to be used for querying
   */
  public K getStartKey() {
    return startKey;
  }

  @Override
  /**
   * Gets the end key to be used for querying
   */
  public K getEndKey() {
    return endKey;
  }

  @Override
  /**
   * Sets the timestamp to be used for querying
   */
  public void setTimestamp(long timestamp) {
    setTimeRange(timestamp, timestamp);
  }

  @Override
  /**
   * Sets the start time for querying
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @Override
  /**
   * Sets the end time for querying
   */
  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  @Override
  /**
   * Sets the range time for querying
   */
  public void setTimeRange(long startTime, long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  /**
   * Gets the timestamp set
   */
  public long getTimestamp() {
    return startTime == endTime ? startTime : -1;
  }

  @Override
  /**
   * Gets the start time
   */
  public long getStartTime() {
    return startTime;
  }

  @Override
  /**
   * Gets the end time
   */
  public long getEndTime() {
    return endTime;
  }

  @Override
  /**
   * Sets the limit of results to be gotten
   */
  public void setLimit(long limit) {
    this.limit = limit;
  }

  @Override
  /**
   * Gets the number limit of this query
   */
  public long getLimit() {
    return limit;
  }

  /**
   * Gets the configuration object
   * @return
   */
  public Object getConf() {
    return authentication;
  }

  /**
   * Sets the configuration object
   * @param auth
   */
  public void setConf(Object auth) {
    this.authentication = auth;
  }
 
  @SuppressWarnings({ "rawtypes" })
  @Override
  /**
   * Determines if this object is equal to a different one
   */
  public boolean equals(Object obj) {
    if(obj instanceof QueryWSBase) {
      QueryWSBase that = (QueryWSBase) obj;
      EqualsBuilder builder = new EqualsBuilder();
      builder.append(dataStore, that.dataStore);
      builder.append(queryString, that.queryString);
      builder.append(fields, that.fields);
      builder.append(startKey, that.startKey);
      builder.append(endKey, that.endKey);
      builder.append(filter, that.filter);
      builder.append(limit, that.limit);
      return builder.isEquals();
    }
    return false;
  }

  @Override
  /**
   * Retrieves the object as hash code
   */
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(dataStore);
    builder.append(queryString);
    builder.append(fields);
    builder.append(startKey);
    builder.append(endKey);
    builder.append(filter);
    builder.append(limit);
    return builder.toHashCode();
  }

  @Override
  /**
   * Convets an object to string
   */
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("dataStore", dataStore);
    builder.append("fields", fields);
    builder.append("startKey", startKey);
    builder.append("endKey", endKey);
    builder.append("filter", filter);
    builder.append("limit", limit);

    return builder.toString();
  }
}
