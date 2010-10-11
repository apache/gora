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
package org.apache.gora.sql.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.sql.store.Column;
import org.apache.gora.sql.store.SqlMapping;
import org.apache.gora.sql.store.SqlStore;

public abstract class InsertUpdateStatement<K, V extends Persistent> {

  protected class ColumnData {
    protected Object object;
    protected Schema schema;
    protected Column column;

    protected ColumnData(Object object, Schema schema, Column column) {
      this.object = object;
      this.schema = schema;
      this.column = column;
    }
  }

  protected SortedMap<String, ColumnData> columnMap = new TreeMap<String, ColumnData>();

  protected String tableName;

  protected SqlMapping mapping;

  protected SqlStore<K, V> store;

  public InsertUpdateStatement(SqlStore<K, V> store, SqlMapping mapping, String tableName) {
    this.store = store;
    this.mapping = mapping;
    this.tableName = tableName;
  }

  public void setObject(Object object, Schema schema, Column column) {
    columnMap.put(column.getName(), new ColumnData(object, schema, column));
  }

  public abstract PreparedStatement toStatement(Connection connection)
  throws SQLException;
}
