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
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.redis.store.RedisStore;
import org.apache.gora.store.DataStore;
import org.redisson.api.RedissonClient;

/**
 * Redis specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class RedisResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private RedissonClient connection;
  private Collection<String> range;
  private String[] fields;

  /**
   * Gets the data store used
   */
  public RedisStore<K, T> getDataStore() {
    return (RedisStore<K, T>) super.getDataStore();
  }

  /**
   * @param dataStore
   * @param query
   * @param scanner
   */
  public RedisResult(DataStore<K, T> dataStore, Query<K, T> query, RedissonClient con, Collection<String> rg, String[] fls) {//, Scanner scanner) {
    super(dataStore, query);

    this.connection = con;
    this.range = rg;
    this.fields = fls;
  }

  /**
   * Gets the items reading progress
   */
  @Override
  public float getProgress() throws IOException {
    if (this.limit != -1) {
      return (float) this.offset / (float) this.limit;
    } else {
      return 0;
    }
  }

  @Override
  public void close() throws IOException {
  }

  /**
   * Gets the next item
   */
  @Override
  protected boolean nextInner() throws IOException {
    return true;
  }

  @Override
  public int size() {
    return this.range.size();
  }

  public RedissonClient getConnection() {
    return connection;
  }

  public void setConnection(RedissonClient connection) {
    this.connection = connection;
  }

  public Collection<String> getRange() {
    return range;
  }

  public void setRange(Collection<String> range) {
    this.range = range;
  }

}