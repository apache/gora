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
package org.apache.gora.redis.query;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.redis.store.RedisStore;
import org.apache.gora.store.DataStore;

/**
 * Redis specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class RedisResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private Iterator<K> range;
  private final int size;

  /**
   * Constructor of RedisResult
   *
   * @param dataStore Query's data store
   * @param query Query
   * @param idsRange Collection of found keys
   */
  public RedisResult(DataStore<K, T> dataStore, Query<K, T> query, Collection<K> idsRange) {
    super(dataStore, query);
    this.size = idsRange.size();
    this.range = idsRange.iterator();
  }

  /**
   * Gets the items reading progress
   *
   * @return a float value representing progress of the job
   * @throws java.io.IOException if there is an error obtaining progress
   */
  @Override
  public float getProgress() throws IOException {
    if (this.limit != -1) {
      return (float) this.offset / (float) this.limit;
    } else {
      return 0;
    }
  }

  /**
   * Gets the next item
   *
   * @return true if another result exists
   * @throws java.io.IOException if for some reason we reach a result which does
   * not exist
   */
  @Override
  protected boolean nextInner() throws IOException {
    if (range == null) {
      return false;
    }
    boolean next = range.hasNext();
    if (next) {
      key = (K) range.next();
      persistent = ((RedisStore<K, T>) getDataStore()).get(key, query.getFields());
    }

    return next;
  }

  @Override
  public int size() {
    return this.size;
  }

}
