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
import java.util.Iterator;

import com.rethinkdb.model.MapObject;
import org.apache.gora.rethinkdb.store.RethinkDBStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RethinkDB specific implementation of the {@link org.apache.gora.query.Result} interface.
 */
public class RethinkDBResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private com.rethinkdb.net.Result<MapObject> resultSet;
  private int size;
  private Iterator<MapObject> resultSetIterator;
  private static final Logger log = LoggerFactory.getLogger(RethinkDBResult.class);

  public RethinkDBResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public RethinkDBResult(DataStore<K, T> dataStore,
                         Query<K, T> query,
                         com.rethinkdb.net.Result<MapObject> resultSet) {
    super(dataStore, query);
    this.resultSet = resultSet;
    this.resultSetIterator = resultSet.iterator();
    this.size = resultSet.bufferedCount();
  }

  public RethinkDBStore<K, T> getDataStore() {
    return (RethinkDBStore<K, T>) super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException {
    if (resultSet == null) {
      return 0;
    } else if (size == 0) {
      return 1;
    } else {
      return offset / (float) size;
    }
  }

  @Override
  public void close() throws IOException {
    resultSet.close();
  }

  @Override
  protected boolean nextInner() throws IOException {
    if (!resultSetIterator.hasNext()) {
      return false;
    }

    MapObject<String, Object> obj = resultSetIterator.next();
    key = (K) obj.get("id");
    persistent = ((RethinkDBStore<K, T>) getDataStore())
            .convertRethinkDBDocToAvroBean(obj, getQuery().getFields());
    return persistent != null;
  }

  @Override
  public int size() {
    int totalSize = size;
    int intLimit = (int) this.limit;
    return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
  }

}
