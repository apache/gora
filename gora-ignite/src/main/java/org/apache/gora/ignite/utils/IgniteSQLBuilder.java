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

  /**
   * Returns a SQL query for determine whether a table exists or not.
   *
   * @param tableName The name of the table to be check.
   * @return SQL query
   */
  public static String tableExists(String tableName) {
    return format("SELECT * FROM {0} LIMIT 0", tableName);
  }

  /**
   * Returns a SQL create table statement for initializing a datastore based
   * upon a Ignite Mapping definition.
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL create query (DDL).
   */
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

  /**
   * Returns a SQL drop table statement for deleting a datastore instance within
   * ignite.
   *
   * @param tableName The name of the table to be dropped.
   * @return SQL drop query (DDL).
   */
  public static String dropTable(String tableName) {
    return format("DROP TABLE IF EXISTS {0} ;", tableName);
  }

  /**
   * Returns a bare SQL insert statement for adding a new record on a Ignite
   * data store.
   *
   * @param mapping The ignite mapping definition of the data store
   * @param data A map containing the Column-Value pairs of the new record.
   * @return SQL insert statement
   */
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

  /**
   * Fills a SQL PreparedStatement of a insert operation with the actual data to
   * be inserted.
   *
   * @param statement The insertion PreparedStatement to be filled.
   * @param insertData A map containing the Column-Value pairs of the new
   * record.
   * @throws SQLException When invalid values are provided as parameters for the
   * insert statement.
   */
  public static void fillInsertStatement(PreparedStatement statement, Map<Column, Object> insertData) throws SQLException {
    List<Entry<Column, Object>> list = new ArrayList<>(insertData.entrySet());
    for (int i = 0; i < list.size(); i++) {
      int j = i + 1;
      statement.setObject(j, list.get(i).getValue());
    }
  }

  /**
   * Returns a bare SQL statement for deleting a record from the Ignite data
   * store.
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL delete statement
   */
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

  /**
   * Fills a SQL PreparedStatement of a delete operation with the actual key of
   * the record to be deleted
   *
   * @param statement The deletion PreparedStatement to be filled.
   * @param mapping The ignite mapping definition of the data store
   * @param deleteData An Object array containing the primary key values of the
   * record to be deleted
   * @throws SQLException When invalid keys' values are provided as parameters
   */
  public static void fillDeleteStatement(PreparedStatement statement, IgniteMapping mapping, Object... deleteData) throws SQLException {
    assert mapping.getPrimaryKey().size() == deleteData.length;
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      int j = i + 1;
      statement.setObject(j, deleteData[i]);
    }
  }

  /**
   * Returns a bare SQL statement for retrieving a record from the ignite data
   * store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param columns A list of columns to be retrieved within the select query
   * @return SQL select statement
   */
  public static String selectGet(IgniteMapping mapping, List<String> columns) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("SELECT ");
    for (int i = 0; i < columns.size(); i++) {
      sqlBuilder.append(columns.get(i));
      sqlBuilder.append(i == columns.size() - 1 ? "" : " , ");
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

  /**
   * Fills a SQL PreparedStatement of a select operation with the actual keys of
   * the record to be retrieved
   *
   * @param statement The select PreparedStatement to be filled.
   * @param mapping The ignite mapping definition of the data store
   * @param selectData An Object array containing the primary key values of the
   * record to be retrieved
   * @throws SQLException When invalid keys' values are provided as parameters
   */
  public static void fillSelectStatement(PreparedStatement statement, IgniteMapping mapping, Object... selectData) throws SQLException {
    assert mapping.getPrimaryKey().size() == selectData.length;
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      int j = i + 1;
      statement.setObject(j, selectData[i]);
    }
  }

  /**
   * Returns a base SQL statement for retrieving multiple records from the
   * ignite data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param selectFields A list of columns to be retrieved within the select
   * query
   * @return SQL select statement
   */
  public static String selectQuery(IgniteMapping mapping, List<String> selectFields) {
    List<String> fields = new ArrayList<>();
    for (Column c : mapping.getPrimaryKey()) {
      fields.add(c.getName());
    }
    fields.addAll(selectFields);
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("SELECT ");
    for (int i = 0; i < fields.size(); i++) {
      sqlBuilder.append(fields.get(i));
      sqlBuilder.append(i == fields.size() - 1 ? "" : " , ");
    }
    sqlBuilder.append(" FROM ");
    sqlBuilder.append(mapping.getTableName());
    return sqlBuilder.toString();
  }

  /**
   * Returns a base SQL statement for deleting multiple records from the ignite
   * data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL delete statement
   */
  public static String deleteQuery(IgniteMapping mapping) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("DELETE FROM ");
    sqlBuilder.append(mapping.getTableName());
    return sqlBuilder.toString();
  }

  /**
   * Returns a base SQL statement for deleting fields from records of the ignite
   * data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param deleteFields A list of columns to be deleted (set to null)
   * @return SQL update statement
   */
  public static String deleteQueryFields(IgniteMapping mapping, List<String> deleteFields) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("UPDATE ");
    sqlBuilder.append(mapping.getTableName());
    if (!deleteFields.isEmpty()) {
      sqlBuilder.append(" SET ");
    }
    for (int i = 0; i < deleteFields.size(); i++) {
      sqlBuilder.append(deleteFields.get(i));
      sqlBuilder.append(" = null");
      sqlBuilder.append(i == deleteFields.size() - 1 ? "" : " , ");
    }
    return sqlBuilder.toString();
  }

  /**
   * Returns a SQL's WHERE segment with proper conditions set for
   * Querying/Deleting/Updating multiple records of a ignite data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param startKey Start key of the WHERE condition
   * @param endKey End key of the WHERE condition
   * @param limit The maximum number of records to be consider
   * @return SQL WHERE segment
   */
  public static String queryWhere(IgniteMapping mapping, Object startKey, Object endKey, long limit) {
    //composite keys pending
    assert mapping.getPrimaryKey().size() == 1;
    String keycolumn = mapping.getPrimaryKey().get(0).getName();
    StringBuilder sqlBuilder = new StringBuilder();
    if (startKey != null || endKey != null) {
      sqlBuilder.append(" WHERE ");
      if (startKey != null && endKey != null && startKey.equals(endKey)) {
        sqlBuilder.append(keycolumn);
        sqlBuilder.append("= ?");
      } else {
        if (startKey != null) {
          sqlBuilder.append(keycolumn);
          sqlBuilder.append(">= ?");
        }
        if (startKey != null && endKey != null) {
          sqlBuilder.append(" AND ");
        }
        if (endKey != null) {
          sqlBuilder.append(keycolumn);
          sqlBuilder.append("<= ?");
        }
      }
    }
    if (limit > 0) {
      sqlBuilder.append(" LIMIT ").append(limit);
    }
    return sqlBuilder.toString();
  }

  /**
   * Fills a SQL PreparedStatement's WHERE segment of a select/delete/update
   * operation with proper key values
   *
   * @param statement The select PreparedStatement to be filled.
   * @param startKey Start key of the WHERE condition
   * @param endKey End key of the WHERE condition
   * @throws SQLException When invalid keys' values are provided as parameters
   */
  public static void fillSelectQuery(PreparedStatement statement, Object startKey, Object endKey) throws SQLException {
    if (startKey != null || endKey != null) {
      if (startKey != null && endKey != null && startKey.equals(endKey)) {
        statement.setObject(1, startKey);
      } else {
        if (startKey != null && endKey != null) {
          statement.setObject(1, startKey);
          statement.setObject(2, endKey);
        } else {
          if (startKey != null) {
            statement.setObject(1, startKey);
          } else {
            statement.setObject(1, endKey);
          }
        }
      }
    }
  }

}
