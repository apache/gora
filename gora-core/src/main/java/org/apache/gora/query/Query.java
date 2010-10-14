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

import java.io.IOException;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.Writable;

/**
 * A query to a data store to retrieve objects. Queries are constructed by 
 * the DataStore implementation via {@link DataStore#newQuery()}.
 */
public interface Query<K, T extends Persistent> extends Writable, Configurable {

  /**
   * Sets the dataStore of this query. Under normal operation, this call 
   * is not necessary and it is potentially dangerous. So use this 
   * method only if you know what you are doing.
   * @param dataStore the dataStore of the query
   */
  public abstract void setDataStore(DataStore<K,T> dataStore);
  
  /**
   * Returns the DataStore, that this Query is associated with.
   * @return the DataStore of the Query
   */
  public abstract DataStore<K,T> getDataStore();
  
  /**
   * Executes the Query on the DataStore and returns the results.
   * @return the {@link Result} for the query.
   */
  public abstract Result<K,T> execute() throws IOException;
  
//  /**
//   * Compiles the query for performance and error checking. This 
//   * method is an optional optimization for DataStore implementations.
//   */
//  public abstract void compile();
//  
//  /**
//   * Sets the query string
//   * @param queryString the query in String
//   */
//  public abstract void setQueryString(String queryString);
//  
//  /**
//   * Returns the query string
//   * @return the query as String
//   */
//  public abstract String getQueryString();

  /* Dimension : fields */
  public abstract void setFields(String... fieldNames);

  public abstract String[] getFields();

  /* Dimension : key */ 
  public abstract void setKey(K key);

  public abstract void setStartKey(K startKey);

  public abstract void setEndKey(K endKey);

  public abstract void setKeyRange(K startKey, K endKey);

  public abstract K getKey();

  public abstract K getStartKey();

  public abstract K getEndKey();
  
  /* Dimension : time */
  public abstract void setTimestamp(long timestamp);

  public abstract void setStartTime(long startTime);

  public abstract void setEndTime(long endTime);

  public abstract void setTimeRange(long startTime, long endTime);

  public abstract long getTimestamp();

  public abstract long getStartTime();

  public abstract long getEndTime();

//  public abstract void setFilter(String filter);
//  
//  public abstract String getFilter();
  
  /**
   * Sets the maximum number of results to return.
   */
  public abstract void setLimit(long limit);

  /**
   * Returns the maximum number of results
   * @return the limit if it is set, otherwise a negative number
   */
  public abstract long getLimit();

  /* parameters */
  /*
  public abstract void setParam(int paramIndex, int value);
  
  public abstract void setParam(String paramName, int value);
  
  public abstract void setParam(int paramIndex, long value);
  
  public abstract void setParam(String paramName, long value);
  
  public abstract void setParam(int paramIndex, String value);
  
  public abstract void setParam(String paramName, String value);
  
  public abstract void setParam(int paramIndex, boolean value);
  
  public abstract void setParam(String paramName, boolean value);
  
  public abstract void setParam(int paramIndex, double value);
  
  public abstract void setParam(String paramName, double value);
  
  public abstract void setParam(int paramIndex, char value);
  
  public abstract void setParam(String paramName, char value);
  
  public abstract void setParam(int paramIndex, Date value);
  
  public abstract void setParam(String paramName, Date value);
  */
    
}
