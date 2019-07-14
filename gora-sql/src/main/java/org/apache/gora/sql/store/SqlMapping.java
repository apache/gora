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

package org.apache.gora.sql.store;

import java.util.HashMap;

import org.apache.gora.sql.store.SqlTypeInterface.JdbcType;

public class SqlMapping {

  private String tableName;
  private HashMap<String, Column> fields;
  private Column primaryColumn;

  public SqlMapping() {
    fields = new HashMap<String, Column>();
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public void addField(String fieldname, String column) {
    fields.put(fieldname, new Column(column));
  }

  public void addField(String fieldName, String columnName, JdbcType jdbcType,
      String sqlType, int length, int scale) {
    fields.put(fieldName, new Column(columnName, false, jdbcType, sqlType, length, scale));
  }

  public Column getColumn(String fieldname) {
    return fields.get(fieldname);
  }

  public void setPrimaryKey(String columnName, JdbcType jdbcType,
      int length, int scale) {
    primaryColumn = new Column(columnName, true, jdbcType, length, scale);
  }

  public Column getPrimaryColumn() {
    return primaryColumn;
  }

  public String getPrimaryColumnName() {
    return primaryColumn.getName();
  }

  public HashMap<String, Column> getFields() {
    return fields;
  }
}
