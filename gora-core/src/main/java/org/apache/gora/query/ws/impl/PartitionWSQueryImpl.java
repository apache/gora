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

import java.util.Arrays;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;

/**
 * Implementation for {@link PartitionQuery}.
 */
//TODO this class should be reviewed when a web service backed datastore has the
// ability to write partition queries
public class PartitionWSQueryImpl<K, T extends Persistent>
  extends QueryWSBase<K, T> implements PartitionQuery<K, T> {

  /**
   * Base query
   */
  protected Query<K, T> baseQuery;
  
  /**
   * The places where this query will be executed
   */
  protected String[] locations;

  /**
   * Constructor
   */
  public PartitionWSQueryImpl() {
    super(null);
  }

  /**
   * Constructor
   */
  public PartitionWSQueryImpl(Query<K, T> baseQuery, String... locations) {
    this(baseQuery, null, null, locations);
  }

  /**
   * Constructor
   */
  public PartitionWSQueryImpl(Query<K, T> baseQuery, K startKey, K endKey,
      String... locations) {
    super(baseQuery.getDataStore());
    this.baseQuery = baseQuery;
    this.locations = locations;
    setStartKey(startKey);
    setEndKey(endKey);
    this.dataStore = baseQuery.getDataStore();
  }

  @Override
  /**
   * Gets the locations where this query will be executed
   */
  public String[] getLocations() {
    return locations;
  }

  /**
   * Gets the base query to be used
   * @return
   */
  public Query<K, T> getBaseQuery() {
    return baseQuery;
  }

  /* Override everything except start-key/end-key */
  @Override
  /**
   * Gets the fields used
   */
  public String[] getFields() {
    return baseQuery.getFields();
  }

  @Override
  /**
   * Gets the data store used
   */
  public DataStore<K, T> getDataStore() {
    return baseQuery.getDataStore();
  }

  @Override
  /**
   * Gets the timestamp used
   */
  public long getTimestamp() {
    return baseQuery.getTimestamp();
  }

  @Override
  /**
   * Gets the start time used
   */
  public long getStartTime() {
    return baseQuery.getStartTime();
  }

  @Override
  /**
   * Gets the end time used
   */
  public long getEndTime() {
    return baseQuery.getEndTime();
  }

  @Override
  /**
   * Gets the results limit number used
   */
  public long getLimit() {
    return baseQuery.getLimit();
  }

  @Override
  /**
   * Sets the fields to be retrieved
   */
  public void setFields(String... fields) {
    baseQuery.setFields(fields);
  }

  @Override
  /**
   * Sets the timestamp used
   */
  public void setTimestamp(long timestamp) {
    baseQuery.setTimestamp(timestamp);
  }

  @Override
  /**
   * Sets the start time used
   */
  public void setStartTime(long startTime) {
    baseQuery.setStartTime(startTime);
  }

  @Override
  /**
   * Sets the end time used
   */
  public void setEndTime(long endTime) {
    baseQuery.setEndTime(endTime);
  }

  @Override
  /**
   * Sets the time range used
   */
  public void setTimeRange(long startTime, long endTime) {
    baseQuery.setTimeRange(startTime, endTime);
  }

  @Override
  /**
   * Sets the maximum number of records to be retrieved
   */
  public void setLimit(long limit) {
    baseQuery.setLimit(limit);
  }

  @Override
  @SuppressWarnings({ "rawtypes" })
  /**
   * Determines if this object is equal to another one
   */
  public boolean equals(Object obj) {
    if(obj instanceof PartitionWSQueryImpl) {
      PartitionWSQueryImpl that = (PartitionWSQueryImpl) obj;
      return this.baseQuery.equals(that.baseQuery)
        && Arrays.equals(locations, that.locations);
    }
    return false;
  }
}
