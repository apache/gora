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

package org.apache.gora.query.impl;

import org.apache.gora.filter.Filter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;

import java.io.IOException;

/**
 * Base class for {@link Result} implementations.
 */
public abstract class ResultBase<K, T extends Persistent> 
implements Result<K, T> {

  protected final DataStore<K,T> dataStore;

  protected final Query<K, T> query;

  protected K key;

  protected T persistent;

  /** Query limit */
  protected long limit;

  /** How far we have proceeded*/
  protected long offset = 0;

  public ResultBase(DataStore<K,T> dataStore, Query<K,T> query) {
    this.dataStore = dataStore;
    this.query = query;
    this.limit = query.getLimit();
  }

  @Override
  public DataStore<K, T> getDataStore() {
    return dataStore;
  }

  @Override
  public Query<K, T> getQuery() {
    return query;
  }

  @Override
  public T get() {
    return persistent;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public Class<K> getKeyClass() {
    return getDataStore().getKeyClass();
  }

  @Override
  public Class<T> getPersistentClass() {
    return getDataStore().getPersistentClass();
  }

  /**
   * Returns whether the limit for the query is reached.
   * @return true if result limit is reached
   */
  protected boolean isLimitReached() {
    return limit > 0 && offset >= limit;
  }

  protected void clear() {
    if(persistent != null) {
      persistent.clear();
    }
    if(key != null && key instanceof Persistent) {
      ((Persistent)key).clear();
    }
  }

  @Override
  public final boolean next() throws Exception {
    if(isLimitReached()) {
      return false;
    }

    boolean ret;
    do {
      clear();
      persistent = getOrCreatePersistent(persistent);
      ret = nextInner();
      if (!ret) {
        //this is the end
        break;
      }
      //we keep looping until we get a row that is not filtered out
    } while (filter(key, persistent));

    if(ret) ++offset;
    return ret;
  }

  protected boolean filter(K key, T persistent) {
    if (!query.isLocalFilterEnabled()) {
      return false;
    }

    Filter<K, T> filter = query.getFilter();
    if (filter == null) {
      return false;
    }

    return filter.filter(key, persistent);
  }

  @Override
  public long getOffset() {
    return offset;
  }

  /**
   * {@link ResultBase#next()} calls this function to read the 
   * actual results.
   * @return true if another result exists
   * @throws IOException if for some reason we reach a result which does not exist
   */
  protected abstract boolean nextInner() throws IOException; 

  protected T getOrCreatePersistent(T persistent) throws Exception {
    if(persistent != null) {
      return persistent;
    }
    return dataStore.newPersistent();
  }

  @Override
  public void close() throws IOException{
  }
}
