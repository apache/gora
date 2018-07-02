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
package org.apache.gora.ignite.utils;

import avro.shaded.com.google.common.collect.Lists;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.gora.ignite.store.Column;
import org.apache.gora.ignite.store.IgniteMapping;

/**
 *
 * SQL Builder utility for Ignite.
 */
public class IgniteSQLBuilder {

  private static String format(String pattern, Object... args) {
    MessageFormat messageFormat = new MessageFormat(pattern, Locale.getDefault());
    return messageFormat.format(args);
  }

  public static String tableExists(String tableName) {
    return format("SELECT * FROM {0} LIMIT 0", tableName);
  }

  public static String createTable(IgniteMapping mapping) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("CREATE TABLE ");
    sqlBuilder.append(mapping.getTableName());
    sqlBuilder.append("(");
    ArrayList<Column> fieldsList = Lists.newArrayList(mapping.getPrimaryKey());
    fieldsList.addAll(Lists.newArrayList(mapping.getFields().values()));
    for (Column aColumn : fieldsList) {
      String name = aColumn.getName();
      Column.FieldType dataType = aColumn.getDataType();
      sqlBuilder.append(name).append(" ").append(dataType.toString()).append(",");
    }
    sqlBuilder.append("PRIMARY KEY ");
    sqlBuilder.append("(");
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      sqlBuilder.append(mapping.getPrimaryKey().get(i).getName());
      sqlBuilder.append(i == mapping.getPrimaryKey().size() - 1 ? "" : ",");
    }
    sqlBuilder.append(")");
    sqlBuilder.append(");");
    return sqlBuilder.toString();
  }

  public static String dropTable(String tableName) {
    return format("DROP TABLE IF EXISTS {0} ;", tableName);
  }

  public static String baseInsertStatement(IgniteMapping mapping, Map<Column, Object> data) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("MERGE INTO ");
    sqlBuilder.append(mapping.getTableName());
    sqlBuilder.append(" (");
    List<Entry<Column, Object>> list = new ArrayList<>(data.entrySet());
    for (int i = 0; i < list.size(); i++) {
      sqlBuilder.append(list.get(i).getKey().getName());
      sqlBuilder.append(i == list.size() - 1 ? "" : ",");
    }
    sqlBuilder.append(")");
    sqlBuilder.append(" VALUES ");
    sqlBuilder.append(" (");
    for (int i = 0; i < list.size(); i++) {
      sqlBuilder.append("?");
      sqlBuilder.append(i == list.size() - 1 ? "" : ",");
    }
    sqlBuilder.append(" )");
    return sqlBuilder.toString();
  }

  public static void fillInsertStatement(PreparedStatement st, Map<Column, Object> data) throws SQLException {
    List<Entry<Column, Object>> list = new ArrayList<>(data.entrySet());
    for (int i = 0; i < list.size(); i++) {
      int j = i + 1;
      st.setObject(j, list.get(i).getValue());
    }
  }

  public static String delete(IgniteMapping mapping) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("DELETE FROM ");
    sqlBuilder.append(mapping.getTableName());
    sqlBuilder.append(" WHERE ");
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      sqlBuilder.append(mapping.getPrimaryKey().get(i).getName());
      sqlBuilder.append("= ? ");
      sqlBuilder.append(i == mapping.getPrimaryKey().size() - 1 ? "" : " AND ");
    }
    return sqlBuilder.toString();
  }

  public static void fillDeleteStatement(PreparedStatement st, IgniteMapping mapping, Object... data) throws SQLException {
    assert mapping.getPrimaryKey().size() == data.length;
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      int j = i + 1;
      st.setObject(j, data[i]);
    }
  }

  public static String selectGet(IgniteMapping mapping, List<String> fields) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("SELECT ");
    for (int i = 0; i < fields.size(); i++) {
      sqlBuilder.append(fields.get(i));
      sqlBuilder.append(i == fields.size() - 1 ? "" : " , ");
    }
    sqlBuilder.append(" FROM ");
    sqlBuilder.append(mapping.getTableName());
    sqlBuilder.append(" WHERE ");
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      sqlBuilder.append(mapping.getPrimaryKey().get(i).getName());
      sqlBuilder.append("= ? ");
      sqlBuilder.append(i == mapping.getPrimaryKey().size() - 1 ? "" : " AND ");
    }
    return sqlBuilder.toString();
  }

  public static void fillSelectStatement(PreparedStatement st, IgniteMapping mapping, Object... data) throws SQLException {
    assert mapping.getPrimaryKey().size() == data.length;
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      int j = i + 1;
      st.setObject(j, data[i]);
    }
  }
}
