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

package org.apache.gora.query;

import org.apache.gora.filter.Filter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;

/**
 * A query to a data store to retrieve objects. Queries are constructed by 
 * the DataStore implementation via {@link DataStore#newQuery()}.
 */
public interface Query<K, T extends Persistent> {

  /**
   * Sets the dataStore of this query. Under normal operation, this call 
   * is not necessary and it is potentially dangerous. So use this 
   * method only if you know what you are doing.
   * @param dataStore the dataStore of the query
   */
  void setDataStore(DataStore<K, T> dataStore);
  
  /**
   * Returns the DataStore, that this Query is associated with.
   * @return the DataStore of the Query
   */
  DataStore<K, T> getDataStore();
  
  /**
   * Executes the Query on the DataStore and returns the results.
   * @return the {@link Result} for the query.
   */
  Result<K, T> execute();
  
//  /**
//   * Compiles the query for performance and error checking. This 
//   * method is an optional optimization for DataStore implementations.
//   */
//  void compile();
//  
//  /**
//   * Sets the query string
//   * @param queryString the query in String
//   */
//  void setQueryString(String queryString);
//  
//  /**
//   * Returns the query string
//   * @return the query as String
//   */
//  String getQueryString();

  /* Dimension : fields */
  void setFields(String... fieldNames);

  String[] getFields();
  
  /**
   * @param filter Set a filter on this query.
   */
  public void setFilter(Filter<K, T> filter);
  
  /**
   * @return The filter on this query, or <code>null</code> if none.
   */
  public Filter<K, T> getFilter();
  
  /**
   * Set whether the local filter is enabled. This is usually called by
   * data store implementations that install the filter remotely
   * (for efficiency reasons) and therefore disable the local filter.
   * @param enable
   *      true to enable local {@link org.apache.gora.filter.Filter}
   */
  void setLocalFilterEnabled(boolean enable);
  
  /**
   * @return Whether the local filter is enabled.
   * See {@link #setLocalFilterEnabled(boolean)}.
   */
  boolean isLocalFilterEnabled();

  /* Dimension : key */ 
  void setKey(K key);

  /**
   * 
   * @param startKey
   *          an inclusive start key
   */
  void setStartKey(K startKey);

  /**
   * 
   * @param endKey
   *          an inclusive end key
   */
  void setEndKey(K endKey);

  /**
   * Set the range of keys over which the query will execute.
   * 
   * @param startKey
   *          an inclusive start key
   * @param endKey
   *          an inclusive end key
   */
  void setKeyRange(K startKey, K endKey);

  K getKey();

  K getStartKey();

  K getEndKey();
  
  /* Dimension : time */
  void setTimestamp(long timestamp);

  void setStartTime(long startTime);

  void setEndTime(long endTime);

  void setTimeRange(long startTime, long endTime);

  long getTimestamp();

  long getStartTime();

  long getEndTime();
  
  /**
   * Sets the maximum number of results to return.
   * @param limit
   *      long value for the limit permitted for a given query
   */
  void setLimit(long limit);

  /**
   * Returns the maximum number of results
   * @return the limit if it is set, otherwise a negative number
   */
  long getLimit();
}
