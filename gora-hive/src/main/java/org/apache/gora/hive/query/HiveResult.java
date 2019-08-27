/*
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

package org.apache.gora.hive.query;

import java.io.IOException;
import java.util.List;
import org.apache.gora.hive.store.HiveStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;

/**
 * Hive Query Result implementation of the the {@link org.apache.gora.query.Result} interface.
 */
public class HiveResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private List<Row> results;
  private int currentRow= 0 ;

  public HiveResult(HiveStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public HiveResult(HiveStore<K, T> dataStore, Query<K, T> query, DataSet dataSet) {
    super(dataStore, query);
    results = dataSet.toRows();
    currentRow = 0;
  }

  @Override
  public HiveStore<K, T> getDataStore() {
    return (HiveStore<K, T>) super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return currentRow/(size()+1.0f);
  }

  @Override
  public int size() {
    return (results == null) ? 0 : results.size();
  }

  @Override
  protected boolean nextInner() throws IOException {
    try {
      if (results == null || currentRow == results.size()){
        return false;
      }
      HiveStore<K, T> hiveStore = ((HiveStore<K, T>) dataStore);
      Row nextRow = results.get(currentRow);
      key = hiveStore.readKey(nextRow);
      persistent = hiveStore.readObject(nextRow);
      currentRow++;
      return true;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
