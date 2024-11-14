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
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CreateTableQuery;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.DeleteQuery;
import com.healthmarketscience.sqlbuilder.DropQuery;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import com.healthmarketscience.sqlbuilder.custom.mysql.MysLimitClause;
import com.healthmarketscience.sqlbuilder.dbspec.Constraint;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbConstraint;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.gora.ignite.store.Column;
import org.apache.gora.ignite.store.IgniteMapping;

/**
 *
 * SQL Builder utility for Ignite.
 */
public class IgniteSQLBuilder {

  /**
   * Returns a SQL query for determine whether a table exists or not.
   *
   * @param tableName The name of the table to be check.
   * @return SQL query
   */
  public static String tableExists(String tableName) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(tableName);
    return new SelectQuery().addAllColumns().addFromTable(aTable)
        .addCustomization(new MysLimitClause(0)).validate().toString();
  }

  /**
   * Returns a SQL create table statement for initializing a datastore based
   * upon a Ignite Mapping definition.
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL create query (DDL).
   */
  public static String createTable(IgniteMapping mapping) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    ArrayList<Column> fieldsList = Lists.newArrayList(mapping.getPrimaryKey());
    fieldsList.addAll(Lists.newArrayList(mapping.getFields().values()));
    for (Column aColumn : fieldsList) {
      String name = aColumn.getName();
      Column.FieldType dataType = aColumn.getDataType();
      aTable.addColumn(name, dataType.toString(), null);
    }
    String[] keys = new String[mapping.getPrimaryKey().size()];
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      keys[i] = mapping.getPrimaryKey().get(i).getName();
    }
    aTable.addConstraint(new DbConstraint(aTable,
        mapping.getTableName() + "_PRIMARY_KEY",
        Constraint.Type.PRIMARY_KEY, keys));
    return new CreateTableQuery(aTable, true).validate().toString();
  }

  /**
   * Returns a SQL drop table statement for deleting a datastore instance within
   * ignite.
   *
   * @param tableName The name of the table to be dropped.
   * @return SQL drop query (DDL).
   */
  public static String dropTable(String tableName) {
    String statement = DropQuery.dropTable(tableName).validate().toString();
    return statement.substring(0, 11) + "IF EXISTS " + statement.substring(11);
  }

  /**
   * Returns a bare SQL insert statement for adding a new record on a Ignite
   * data store.
   *
   * @param mapping The ignite mapping definition of the data store
   * @param data A map containing the Column-Value pairs of the new record.
   * @return SQL insert statement
   */
  public static String createInsertQuery(IgniteMapping mapping, List<Column> dataKeyList) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    InsertQuery insertQuery = new InsertQuery(aTable);
    // List<Entry<Column, Object>> list = new ArrayList<>(data.entrySet());
    String[] columns = new String[dataKeyList.size()];
    for (int i = 0; i < dataKeyList.size(); i++) {
      columns[i] = dataKeyList.get(i).getName();
    }
    return insertQuery.addCustomPreparedColumns(columns).validate().toString()
        .replaceFirst("INSERT", "MERGE");
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
  public static void fillInsertQuery(PreparedStatement statement, List<Object> insertData) throws SQLException {
    // List<Entry<Column, Object>> list = new ArrayList<>(insertData.entrySet());
    for (int i = 0; i < insertData.size(); i++) {
      int j = i + 1;
      statement.setObject(j, insertData.get(i));
    }
  }

  /**
   * Returns a bare SQL statement for deleting a record from the Ignite data
   * store.
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL delete statement
   */
  public static String createDeleteQuery(IgniteMapping mapping) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    DeleteQuery statement = new DeleteQuery(aTable);
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      statement.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
          new DbColumn(aTable, mapping.getPrimaryKey().get(i).getName(), null),
          SqlObject.QUESTION_MARK));
    }
    return statement.validate().toString();
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
  public static void fillDeleteQuery(PreparedStatement statement, IgniteMapping mapping, Object... deleteData) throws SQLException {
    assert mapping.getPrimaryKey().size() == deleteData.length;
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      int j = i + 1;
      statement.setObject(j, deleteData[i]);
    }
  }

  /**
   * Returns a bare SQL statement for checking if a key exists
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL select statement
   */
  public static String createSelectQueryExists(IgniteMapping mapping) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomColumns(FunctionCall.countAll());
    selectQuery.addFromTable(aTable);
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
          new DbColumn(aTable, mapping.getPrimaryKey().get(i).getName(), null),
          SqlObject.QUESTION_MARK));
    }
    return selectQuery.validate().toString();
  }

  /**
   * Returns a bare SQL statement for retrieving a record from the ignite data
   * store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param columns A list of columns to be retrieved within the select query
   * @return SQL select statement
   */
  public static String createSelectQueryGet(IgniteMapping mapping, List<String> columns) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addFromTable(aTable);
    DbColumn[] lsColumns = new DbColumn[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      lsColumns[i] = aTable.addColumn(columns.get(i));
    }
    selectQuery.addColumns(lsColumns);
    for (int i = 0; i < mapping.getPrimaryKey().size(); i++) {
      selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
          new DbColumn(aTable, mapping.getPrimaryKey().get(i).getName(), null),
          SqlObject.QUESTION_MARK));
    }
    return selectQuery.validate().toString();
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
  public static void fillSelectQuery(PreparedStatement statement, IgniteMapping mapping, Object... selectData) throws SQLException {
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
  public static String createSelectQuery(IgniteMapping mapping, List<String> selectFields) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addFromTable(aTable);
    List<String> fields = new ArrayList<>();
    for (Column c : mapping.getPrimaryKey()) {
      fields.add(c.getName());
    }
    fields.addAll(selectFields);
    DbColumn[] lsColumns = new DbColumn[fields.size()];
    for (int i = 0; i < fields.size(); i++) {
      lsColumns[i] = aTable.addColumn(fields.get(i));
    }
    selectQuery.addColumns(lsColumns);
    return selectQuery.validate().toString();
  }

  /**
   * Returns a base SQL statement for deleting multiple records from the ignite
   * data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @return SQL delete statement
   */
  public static String createDeleteQueryMultipleRecords(IgniteMapping mapping) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    DeleteQuery deleteQuery = new DeleteQuery(aTable);
    return deleteQuery.validate().toString();
  }

  /**
   * Returns a base SQL statement for deleting fields from records of the ignite
   * data store
   *
   * @param mapping The ignite mapping definition of the data store
   * @param deleteFields A list of columns to be deleted (set to null)
   * @return SQL update statement
   */
  public static String createDeleteQueryWithFields(IgniteMapping mapping, List<String> deleteFields) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    UpdateQuery updateQuery = new UpdateQuery(aTable);
    for (int i = 0; i < deleteFields.size(); i++) {
      updateQuery.addSetClause(new DbColumn(aTable, deleteFields.get(i), null), SqlObject.NULL_VALUE);
    }
    return updateQuery.validate().toString();
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
  public static String createWhereClause(IgniteMapping mapping, Object startKey, Object endKey, long limit) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable(mapping.getTableName());
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addFromTable(aTable);
    String fisrtPart = selectQuery.validate().toString();
    String keycolumn = mapping.getPrimaryKey().get(0).getName();
    if (startKey != null || endKey != null) {
      if (startKey != null && endKey != null && startKey.equals(endKey)) {
        selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
            new CustomSql(keycolumn), SqlObject.QUESTION_MARK));
      } else {
        if (startKey != null) {
          selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.GREATER_THAN_OR_EQUAL_TO,
              new CustomSql(keycolumn), SqlObject.QUESTION_MARK));
        }
        if (endKey != null) {
          selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.LESS_THAN_OR_EQUAL_TO,
              new CustomSql(keycolumn), SqlObject.QUESTION_MARK));
        }
      }
    }
    if (limit > 0) {
      selectQuery.addCustomization(new MysLimitClause(limit));
    }
    String completeQuery = selectQuery.validate().toString();
    return completeQuery.substring(fisrtPart.length());
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
  public static void fillWhereClause(PreparedStatement statement, Object startKey, Object endKey) throws SQLException {
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

  /**
   * Returns a SQL statement for returning a list of tables
   */
  public static String createSelectAllTablesNames(String schemaIgnite) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable("SYS.TABLES");
    SelectQuery selectQuery = new SelectQuery();
    DbColumn[] lsColumns = new DbColumn[]{aTable.addColumn("TABLE_NAME")};
    selectQuery.addColumns(lsColumns);
    selectQuery.addFromTable(aTable);
    return selectQuery.validate().toString();
  }

  /**
   * Returns a SQL statement for metadata of a table
   */
  public static String createSelectAllColumnsOfTable(String schemaIgnite, String table) {
    DbSpec spec = new DbSpec();
    DbSchema schema = spec.addDefaultSchema();
    DbTable aTable = schema.addTable("INFORMATION_SCHEMA.COLUMNS");
    DbTable aTable2 = schema.addTable("SYS.TABLE_COLUMNS");
    SelectQuery selectQuery = new SelectQuery();
    DbColumn[] lsColumns = new DbColumn[]{aTable.addColumn("COLUMN_NAME"),
      aTable.addColumn("TYPE_NAME"), aTable2.addColumn("PK")};
    selectQuery.addColumns(lsColumns);
    selectQuery.addFromTable(aTable);
    selectQuery.addFromTable(aTable2);
    selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
            new DbColumn(aTable, "TABLE_SCHEMA", null),
            schemaIgnite));
    selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
            new DbColumn(aTable, "TABLE_NAME", null),
            table));
    selectQuery.addCondition(new BinaryCondition(BinaryCondition.Op.EQUAL_TO,
            new DbColumn(aTable, "COLUMN_NAME", null),
            new DbColumn(aTable2, "COLUMN_NAME", null)));
    return selectQuery.validate().toString();
  }
}
