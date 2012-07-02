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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.ipc.ByteBufferInputStream;
import org.apache.avro.ipc.ByteBufferOutputStream;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.sql.query.SqlQuery;
import org.apache.gora.sql.query.SqlResult;
import org.apache.gora.sql.statement.Delete;
import org.apache.gora.sql.statement.InsertUpdateStatement;
import org.apache.gora.sql.statement.InsertUpdateStatementFactory;
import org.apache.gora.sql.statement.SelectStatement;
import org.apache.gora.sql.statement.Where;
import org.apache.gora.sql.store.SqlTypeInterface.JdbcType;
import org.apache.gora.sql.util.SqlUtils;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.IOUtils;
import org.apache.gora.util.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * A DataStore implementation for RDBMS with a SQL interface. SqlStore
 * uses the JOOQ API and various JDBC drivers to communicate with the DB. 
 * Through use of the JOOQ API this SqlStore aims to support numerous SQL 
 * database stores namely;
 * DB2 9.7
 * Derby 10.8
 * H2 1.3.161
 * HSQLDB 2.2.5
 * Ingres 10.1.0
 * MySQL 5.1.41 and 5.5.8
 * Oracle XE 10.2.0.1.0 and 11g
 * PostgreSQL 9.0
 * SQLite with inofficial JDBC driver v056
 * SQL Server 2008 R8
 * Sybase Adaptive Server Enterprise 15.5
 * Sybase SQL Anywhere 12
 *
 * This DataStore is currently in development, and requires a complete
 * re-write as per GORA-86
 * Please see https://issues.apache.org/jira/browse/GORA-86
 */
public class SqlStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  /** The vendor of the DB */
  public static enum DBVendor {
    MYSQL,
    HSQL,
    GENERIC;

    static DBVendor getVendor(String dbProductName) {
      String name = dbProductName.toLowerCase();
      if(name.contains("mysql"))
        return MYSQL;
      else if(name.contains("hsql"))
        return HSQL;
      return GENERIC;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(SqlStore.class);

  /** The JDBC Driver class name */
  protected static final String DRIVER_CLASS_PROPERTY = "jdbc.driver";

  /** JDBC Database access URL */
  protected static final String URL_PROPERTY = "jdbc.url";

  /** User name to access the database */
  protected static final String USERNAME_PROPERTY = "jdbc.user";

  /** Password to access the database */
  protected static final String PASSWORD_PROPERTY = "jdbc.password";

  protected static final String DEFAULT_MAPPING_FILE = "gora-sql-mapping.xml";

  private String jdbcDriverClass;
  private String jdbcUrl;
  private String jdbcUsername;
  private String jdbcPassword;

  private SqlMapping mapping;

  private Connection connection; //no connection pooling yet

  private DatabaseMetaData metadata;
  private boolean dbMixedCaseIdentifiers, dbLowerCaseIdentifiers, dbUpperCaseIdentifiers;
  private HashMap<String, JdbcType> dbTypeMap;

  private HashSet<PreparedStatement> writeCache;

  private int keySqlType;

  // TODO implement DataBaseTable sqlTable
  //private DataBaseTable sqlTable;

  private Column primaryColumn;

  private String dbProductName;

  private DBVendor dbVendor;

  public void initialize() throws IOException {
      //TODO
  }

  @Override
  public String getSchemaName() {
    return mapping.getTableName();
  }

  @Override
  public void close() throws IOException {
  //TODO
  }

  
  private void setColumnConstraintForQuery() throws IOException {
  //TODO
  }
  
  
  @Override
  public void createSchema() throws IOException {
  //TODO
  }

  private void getColumnConstraint() throws IOException {
  //TODO
  }

  @Override
  public void deleteSchema() throws IOException {
  //TODO
  }

  @Override
  public boolean schemaExists() throws IOException {
  //TODO
  return false;
  }

  @Override
  public boolean delete(K key) throws IOException {
  //TODO
  return false;
  }
  
  @Override
  public long deleteByQuery(Query<K, T> query) throws IOException {
  //TODO
  return 0;
  }

  public void flush() throws IOException {
  //TODO
  }

  @Override
  public T get(K key, String[] requestFields) throws IOException {
  //TODO
  return null;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws IOException {
  //TODO
  return null;
  }

  private void constructWhereClause() throws IOException {
  //TODO
  }

  private void setParametersForPreparedStatement() throws SQLException, IOException {
  //TODO
  }

  @SuppressWarnings("unchecked")
  public K readPrimaryKey(ResultSet resultSet) throws SQLException {
    return (K) resultSet.getObject(primaryColumn.getName());
  }

  public T readObject(ResultSet rs, T persistent
      , String[] requestFields) throws SQLException, IOException {
  //TODO
  return null;
  }

  protected byte[] getBytes() throws SQLException, IOException {
    return null;
  }

  protected Object readField() throws SQLException, IOException {
  //TODO
  return null;
  }

  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
  throws IOException {
  //TODO Implement this using Hadoop support
  return null;
  }

  @Override
  public Query<K, T> newQuery() {
    return new SqlQuery<K, T>(this);
  }

  @Override
  public void put(K key, T persistent) throws IOException {
  //TODO
  }

  /**
   * Sets the object to the preparedStatement by it's schema
   */
  public void setObject(PreparedStatement statement, int index, Object object
      , Schema schema, Column column) throws SQLException, IOException {
  //TODO
  }
  
  protected <V> void setObject(PreparedStatement statement, int index, V object
      , int objectType, Column column) throws SQLException, IOException {
    statement.setObject(index, object, objectType, column.getScaleOrLength());
  }

  protected void setBytes() throws SQLException   {
  //TODO
  }

  /** Serializes the field using Avro to a BLOB field */
  protected void setField() throws IOException, SQLException {
  //TODO
  }

  protected Connection getConnection() throws IOException {
  //TODO
  return null;
  }

  protected void initDbMetadata() throws IOException {
  //TODO
  }

  protected String getIdentifier() {
  //TODO
  return null;
  }

  private void addColumn() {
  //TODO
  }

  
  protected void createSqlTable() {
  //TODO
  }
  
  private void addField() throws IOException {
  //TODO
  }

  @SuppressWarnings("unchecked")
  protected SqlMapping readMapping() throws IOException {
  //TODO
  return null;
  }
}
