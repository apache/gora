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

package org.apache.gora.sql.query;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.sql.util.SqlUtils;
import org.apache.gora.store.DataStore;

public class SqlResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private ResultSet resultSet;
  private PreparedStatement statement;
  
  public SqlResult(DataStore<K, T> dataStore, Query<K, T> query
      , ResultSet resultSet, PreparedStatement statement) {
    super(dataStore, query);
    this.resultSet = resultSet;
    this.statement = statement;
  }

  @Override
  protected boolean nextInner() throws IOException {
    try {
      if(!resultSet.next()) { //no matching result
        close();
        return false;
      }

      SqlStore<K, T> sqlStore = ((SqlStore<K,T>)dataStore);
      key = sqlStore.readPrimaryKey(resultSet);
      persistent = sqlStore.readObject(resultSet, persistent, query.getFields());

      return true;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void close() throws IOException {
    SqlUtils.close(resultSet);
    SqlUtils.close(statement);
  }

  @Override
  public float getProgress() throws IOException {
    return 0;
  }
}
