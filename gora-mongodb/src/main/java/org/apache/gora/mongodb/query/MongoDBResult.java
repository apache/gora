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
package org.apache.gora.mongodb.query;

import java.io.IOException;

import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * MongoDB specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 * 
 * @author Fabien Poulard <fpoulard@dictanova.com>
 * @author Damien Raude-Morvan <draudemorvan@dictanova.com>
 */
public class MongoDBResult<K, T extends PersistentBase> extends
    ResultBase<K, T> {

  /**
   * Reference to the cursor pointing to the results
   */
  private DBCursor cursor;

  public MongoDBResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  @Override
  public float getProgress() throws IOException {
    if (cursor == null)
      return 0;
    else if (cursor.size() == 0)
      return 1;
    else
      return offset / cursor.size();
  }

  @Override
  public void close() throws IOException {
    if (cursor != null) {
      cursor.close();
    }
  }

  @Override
  protected boolean nextInner() throws IOException {
    if (!cursor.hasNext()) {
      return false;
    }

    DBObject obj = cursor.next();
    key = (K) obj.get("_id");
    persistent = ((MongoStore<K, T>) getDataStore()).newInstance(obj,
        getQuery().getFields());
    return persistent != null;
  }

  /**
   * Save the reference to the cursor that holds the actual results.
   * 
   * @param cursor
   *          {@link DBCursor} obtained from a query execution and that holds
   *          the actual results
   */
  public void setCursor(DBCursor cursor) {
    this.cursor = cursor;
  }

}
