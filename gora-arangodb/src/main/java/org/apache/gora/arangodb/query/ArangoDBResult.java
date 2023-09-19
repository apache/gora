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

package org.apache.gora.arangodb.query;

import java.io.IOException;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoIterator;
import com.arangodb.entity.BaseDocument;
import org.apache.gora.arangodb.store.ArangoDBStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

/**
 * ArangoDB specific implementation of the {@link org.apache.gora.query.Result} interface.
 *
 */
public class ArangoDBResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private int size;
  private ArangoCursor<BaseDocument> cursor;
  private ArangoIterator<BaseDocument> resultSetIterator;

  public ArangoDBResult(DataStore<K, T> dataStore,
                        Query<K, T> query,
                        ArangoCursor<BaseDocument> cursor) {
    super(dataStore, query);
    this.cursor = cursor;
    this.resultSetIterator = cursor.iterator();
    this.size = cursor.getStats().getFullCount().intValue();
  }

  public ArangoDBResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public ArangoDBStore<K, T> getDataStore() {
    return (ArangoDBStore<K, T>) super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException {
    if (cursor == null) {
      return 0;
    } else if (size == 0) {
      return 1;
    } else {
      return offset / (float) size;
    }
  }

  @Override
  public void close() throws IOException {
    cursor.close();
  }

  @Override
  protected boolean nextInner() throws IOException {
    if (!resultSetIterator.hasNext()) {
      return false;
    }

    BaseDocument obj = resultSetIterator.next();
    key = (K) obj.getKey();
    persistent = ((ArangoDBStore<K, T>) getDataStore())
            .convertArangoDBDocToAvroBean(obj, getQuery().getFields());
    return persistent != null;
  }

  @Override
  public int size() {
    int totalSize = size;
    int intLimit = (int) this.limit;
    return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
  }

}
