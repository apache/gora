/*
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
package org.apache.gora.neo4j.query;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.gora.neo4j.store.Neo4jStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

/**
 * Neo4jResult specific implementation of the
 * {@link org.apache.gora.query.Result} interface.
 */
public class Neo4jResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private int size = -1;
  private Iterator<Map.Entry<K, T>> iterator;

  public Neo4jResult(DataStore<K, T> dataStore, Query<K, T> query, ResultSet resultSet) throws SQLException, IOException {
    super(dataStore, query);
    Map<K, T> data = new LinkedHashMap();
    this.size = 0;
    while (resultSet.next()) {
      this.size++;
      K keyTemp = ((Neo4jStore<K, T>) getDataStore()).extractKey(resultSet);
      T persistentTemp = ((Neo4jStore<K, T>) getDataStore()).newInstance(resultSet, getQuery().getFields());
      data.put(keyTemp, persistentTemp);
    }
    this.iterator = data.entrySet().iterator();
  }

  @Override
  protected boolean nextInner() throws IOException {
    if (!iterator.hasNext()) {
      return false;
    }
    Map.Entry<K, T> next = iterator.next();
    key = next.getKey();
    persistent = next.getValue();
    return persistent != null;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    if (iterator == null) {
      return 0;
    } else if (size == 0) {
      return 1;
    } else {
      return offset / (float) size;
    }
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void close() throws IOException {
  }

}
