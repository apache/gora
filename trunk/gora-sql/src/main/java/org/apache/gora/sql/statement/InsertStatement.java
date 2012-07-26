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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.gora.sql.store.SqlMapping;
import org.apache.gora.util.StringUtils;

/**
 * An SQL INSERT statement, for generating a Prepared Statement
 */
public class InsertStatement {

  private SqlMapping mapping;
  private String tableName;
  private List<String> columnNames;

  public InsertStatement(SqlMapping mapping, String tableName) {
    this.mapping = mapping;
    this.tableName = tableName;
    this.columnNames = new ArrayList<String>();
  }

  public InsertStatement(SqlMapping mapping, String tableName, String... columnNames) {
    this.mapping = mapping;
    this.tableName = tableName;
    this.columnNames = Arrays.asList(columnNames);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("INSERT INTO ");
    builder.append(tableName);

    StringUtils.join(builder.append(" ("), columnNames).append(" )");

    builder.append("VALUES (");
    for(int i = 0; i < columnNames.size(); i++) {
      if (i != 0) builder.append(",");
      builder.append("?");
    }

    builder.append(") ON DUPLICATE KEY UPDATE ");
    columnNames.remove(mapping.getPrimaryColumnName());
    for(int i = 0; i < columnNames.size(); i++) {
      if (i != 0) builder.append(",");
      builder.append(columnNames.get(i));
      builder.append("=");
      builder.append("?");
    }
    builder.append(";");

    return builder.toString();
  }

  /**
   * @return the tableName
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * @param tableName the tableName to set
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * @return the columnNames
   */
  public List<String> getColumnNames() {
    return columnNames;
  }

  /**
   * @param columnNames the columnNames to set
   */
  public void setColumnNames(String... columnNames) {
    this.columnNames = Arrays.asList(columnNames);
  }

  public void addColumnName(String columnName) {
    this.columnNames.add(columnName);
  }

  public void clear() {
    this.columnNames.clear();
  }
}