/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.cassandra.query;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CassandraResult specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class CassandraResultSet<K, T extends Persistent> extends ResultBase<K, T> {

  private List<T> persistentObject = new ArrayList<T>();

  private List<K> persistentKey = new ArrayList<K>();

  private int size = 0;

  private int position = 0;

  /**
   * Constructor of the Cassandra Result
   * @param dataStore Cassandra Data Store
   * @param query Cassandra Query
   */
  public CassandraResultSet(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean nextInner() throws IOException {
    if (offset < size) {
      persistent = persistentObject.get(position);
      key = persistentKey.get(position);
      position++;
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float getProgress() throws IOException, InterruptedException {
    return ((float) position) / size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get() {
    return super.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public K getKey() {
    return super.getKey();
  }

  /**
   * This method adds Result Element into result lists, So when user retrieves values from the Result these objects will be passed.
   *
   * @param key   key
   * @param token persistent Object
   */
  public void addResultElement(K key, T token) {
    this.persistentKey.add(key);
    this.persistentObject.add(token);
    this.size++;
  }

  @Override
  /**
   * Returns whether the limit for the query is reached.
   * @return true if result limit is reached
   */
  protected boolean isLimitReached() {
    return (limit > 0 && offset >= limit) || (offset >= size);
  }

  @Override
  public int size() {
    return size;
  }
}
