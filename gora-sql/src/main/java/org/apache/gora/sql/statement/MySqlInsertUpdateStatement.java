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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map.Entry;

import org.apache.avro.Schema;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.sql.store.Column;
import org.apache.gora.sql.store.SqlMapping;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.util.StringUtils;

public class MySqlInsertUpdateStatement<K, V extends PersistentBase> extends InsertUpdateStatement<K, V> {

  public MySqlInsertUpdateStatement(SqlStore<K, V> store, SqlMapping mapping, String tableName) {
    super(store, mapping, tableName);
  }

  @Override
  public PreparedStatement toStatement(Connection connection)
  throws SQLException {
    int i = 0;
    StringBuilder builder = new StringBuilder("INSERT INTO ");
    builder.append(tableName);
    StringUtils.join(builder.append(" ("), columnMap.keySet()).append(" )");

    builder.append("VALUES (");
    for(i = 0; i < columnMap.size(); i++) {
      if (i != 0) builder.append(",");
      builder.append("?");
    }

    builder.append(") ON DUPLICATE KEY UPDATE ");

    // TODO: Fix this stupid code. We need to make sure primary key field
    // is not in UPDATE part of sql query. This desperately needs a
    // better solution
    Column primaryColumn = mapping.getPrimaryColumn();
    Object key = columnMap.get(primaryColumn.getName()).object;
    i = 0;
    for(String s : columnMap.keySet()) {
      if (s.equals(primaryColumn.getName())) {
        continue;
      }
      if (i != 0) builder.append(",");
      builder.append(s).append("=").append("?");
      i++;
    }
    builder.append(";");

    PreparedStatement insert = connection.prepareStatement(builder.toString());

    int psIndex = 1;
    for (int count = 0; count < 2; count++) {
      for (Entry<String, ColumnData> e : columnMap.entrySet()) {
        ColumnData columnData = e.getValue();
        Column column = columnData.column;
        Schema fieldSchema = columnData.schema;
        Object fieldValue = columnData.object;

        // check if primary key
        if (column.getName().equals(primaryColumn.getName())) {
          if (count == 1) {
            continue;
          }
          if (primaryColumn.getScaleOrLength() > 0) {
            insert.setObject(psIndex++, key,
                primaryColumn.getJdbcType().getOrder(), primaryColumn.getScaleOrLength());
          } else {
            insert.setObject(psIndex++, key, primaryColumn.getJdbcType().getOrder());
          }
          continue;
        }

        try {
          store.setObject(insert, psIndex++, fieldValue, fieldSchema, column);
        } catch (IOException ex) {
          throw new SQLException(ex);
        }
      }
    }

    return insert;
  }

}
