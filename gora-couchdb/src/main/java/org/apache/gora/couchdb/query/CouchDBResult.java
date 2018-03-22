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
package org.apache.gora.couchdb.query;

import org.apache.gora.couchdb.store.CouchDBStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * CouchDB specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class CouchDBResult<K, T extends Persistent> extends ResultBase<K, T> {

  /**
   * Result set containing query results
   */
  private List<Map> result;

  protected CouchDBStore dataStore;
  int position = 0;

  /**
   * Constructor for the result set
   *
   * @param dataStore Data store used
   * @param query     Query used
   * @param result    Result obtained from querying
   */
  public CouchDBResult(DataStore<K, T> dataStore, Query<K, T> query, List<Map> result) {
    super(dataStore, query);
    this.result = result;
    this.dataStore = (CouchDBStore) dataStore;
  }

  /**
   * Gets the next item
   */
  @Override
  protected boolean nextInner() throws IOException {
    if (result == null || result.size() <= 0 || position >= result.size()) {
      return false;
    }
    key = (K) result.get(position).get("_id");
    persistent = (T) dataStore.newInstance(result.get(position++), query.getFields());
    return persistent != null;
  }

  /**
   * Gets the items reading progress
   */
  @Override
  public float getProgress() throws IOException, InterruptedException {
    if (result != null && result.size() > 0) {
      return (float) position / (float) result.size();
    } else {
      return 0;
    }
  }

  /**
   * Result set containing query results
   *
   * @return Result set containing query results
   */
  public List<Map> getResultData() {
    return result;
  }

    @Override
    public int size() {
        return result.size();
    }
}
