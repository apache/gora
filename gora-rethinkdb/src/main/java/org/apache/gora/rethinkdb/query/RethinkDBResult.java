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

package org.apache.gora.rethinkdb.query;

import java.io.IOException;

import org.apache.gora.rethinkdb.store.RethinkDBStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RethinkDB specific implementation of the {@link org.apache.gora.query.Result} interface.
 *
 */
public class RethinkDBResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private int size;
  private static final Logger log = LoggerFactory.getLogger(RethinkDBResult.class);

  public RethinkDBResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public RethinkDBStore<K, T> getDataStore() {
    return (RethinkDBStore<K, T>) super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException {
    return 0L;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  protected boolean nextInner() throws IOException {
    return true;
  }

  @Override
  public int size() {
    return 0;
  }

}
