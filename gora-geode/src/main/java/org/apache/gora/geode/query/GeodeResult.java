/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.geode.query;

import org.apache.gora.geode.store.GeodeStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.NavigableSet;

/**
 * {@link GeodeResult} is the primary class
 * responsible for representing result set of a cache manipulation query
 * {@link org.apache.gora.geode.query.GeodeQuery}
 */
public class GeodeResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(GeodeResult.class);
  private NavigableSet<K> cacheKeySet;
  private Iterator<K> iterator;
  private int current;

  public GeodeResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public GeodeResult(DataStore<K, T> dataStore, Query<K, T> query, NavigableSet<K> cacheKeySet) {
    super(dataStore, query);
    this.cacheKeySet = cacheKeySet;
    this.iterator = cacheKeySet.iterator();
    this.current = 0;
  }

  public GeodeStore<K, T> getDataStore() {
    return (GeodeStore<K, T>)  super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException {
    if (cacheKeySet.size() == 0) {
      return 1;
    }
    float progress = ((float) current / (float) cacheKeySet.size());
    return progress;
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  protected boolean nextInner() throws IOException {
    if (!iterator.hasNext()) {
      return false;
    }
    key = iterator.next();
    LOG.info("Results set pointer is now moved to key {}.", key);
    persistent = dataStore.get(key);
    this.current++;
    return true;
  }

  @Override
  public int size() {
    int totalSize = cacheKeySet.size();
    int intLimit = (int) this.limit;
    return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
  }
}
