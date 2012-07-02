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

import org.apache.gora.store.DataStore;

/**
 * A result to a {@link Query}. Objects in the result set can be 
 * iterated by calling {@link #next()}, {@link #get()} 
 * and {@link #getKey()}. 
 */
public interface Result<K,T> {

  /**
   * Returns the DataStore, that this Result is associated with.
   * @return the DataStore of the Result
   */
  DataStore<K,T> getDataStore();
  
  /**
   * Returns the Query object for this Result.
   * @return the Query object for this Result.
   */
  Query<K, T> getQuery();
  
  /**
   * Advances to the next element and returns false if at end.
   * @return true if end is not reached yet
   */
  boolean next() throws Exception, IOException;
  
  /**
   * Returns the current key.
   * @return current key
   */
  K getKey();
  
  /**
   * Returns the current object.
   * @return current object
   */
  T get();
  
  /**
   * Returns the class of the keys
   * @return class of the keys
   */
  Class<K> getKeyClass();
    
  /**
   * Returns the class of the persistent objects
   * @return class of the persistent objects
   */
  Class<T> getPersistentClass();
  
  /**
   * Returns the number of times next() is called with return value true.
   * @return the number of results so far
   */
  long getOffset();
  
  /**
   * Returns how far along the result has iterated, a value between 0 and 1.
   */
  float getProgress() throws IOException, InterruptedException, Exception;

  void close() throws IOException;
  
}
