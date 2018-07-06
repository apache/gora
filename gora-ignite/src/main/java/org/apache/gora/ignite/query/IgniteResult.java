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
package org.apache.gora.ignite.query;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.gora.ignite.store.IgniteStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

/**
 * IgniteResult specific implementation of the
 * {@link org.apache.gora.query.Result} interface.
 */
public class IgniteResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private ResultSet resultSet;
  private Statement st;
  private int size;

  public IgniteResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  @Override
  protected boolean nextInner() throws IOException {
    try {
      if (!resultSet.next()) {
        return false;
      }
      key = ((IgniteStore<K, T>) getDataStore()).extractKey(resultSet);
      persistent = ((IgniteStore<K, T>) getDataStore()).newInstance(resultSet, getQuery().getFields());
      return persistent != null;
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    if (resultSet == null) {
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
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException ex) {
        throw new IOException(ex);
      }
    }
    if (st != null) {
      try {
        st.close();
      } catch (SQLException ex) {
        throw new IOException(ex);
      }
    }
  }

  public void setResultSet(ResultSet resultSet) throws SQLException {
    this.resultSet = resultSet;
    if (resultSet.last()) {
      size = resultSet.getRow();
    } else {
      size = 0;
    }
    resultSet.beforeFirst();
  }

}
