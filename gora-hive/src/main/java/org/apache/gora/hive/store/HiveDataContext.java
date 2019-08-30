/*
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

package org.apache.gora.hive.store;


import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.gora.util.GoraException;
import org.apache.metamodel.DataContext;
import org.apache.metamodel.MetaModelException;
import org.apache.metamodel.UpdateScript;
import org.apache.metamodel.UpdateSummary;
import org.apache.metamodel.UpdateableDataContext;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.factory.DataContextPropertiesImpl;
import org.apache.metamodel.jdbc.JdbcDataContext;
import org.apache.metamodel.jdbc.JdbcDataContextFactory;
import org.apache.metamodel.query.CompiledQuery;
import org.apache.metamodel.query.Query;
import org.apache.metamodel.query.builder.InitFromBuilder;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread safe implementation to query on hive data context This implements all methods of
 * {@link org.apache.metamodel.DataContext} and {@link org.apache.metamodel.UpdateableDataContext}
 * and methods to establish a HiveConnection and execute row hive sql strings on that connection
 */
public class HiveDataContext implements DataContext, UpdateableDataContext {

  private static final Logger LOG = LoggerFactory.getLogger((MethodHandles.lookup().lookupClass()));

  private final ThreadLocal<JdbcDataContext> dataContext;
  private final DataContextPropertiesImpl dataContextProperties;
  private final JdbcDataContextFactory jdbcDataContextFactory;
  private final BlockingQueue<JdbcDataContext> dataContextPool = new LinkedBlockingQueue<>();
  // provide if the hive data context is closed and this is used to synchronise creating
  // and closing jdbc data contexts
  private Boolean isClosed;

  public HiveDataContext(HiveStoreParameters hiveStoreParameters) {
    jdbcDataContextFactory = new JdbcDataContextFactory();
    dataContextProperties = generateDataContextProperties(hiveStoreParameters);
    this.dataContext = new ThreadLocal<>();
    this.isClosed = false;
  }

  /**
   * Return jdbc data context assigned to the current thread.
   *
   * @return {@link org.apache.metamodel.jdbc.JdbcDataContext} Jdbc data context
   */
  public JdbcDataContext getDataContext() {
    // If the data context is not found or the connection is closed,
    // a new data context will be created assuming the current thread accesses
    // the data context for the first time or the previous connection closed due to some exceptions
    // thrown during the last query

    JdbcDataContext jdbcDataContext = dataContext.get();
    boolean connectionClosed = false;
    if (jdbcDataContext != null) {
      try {
        connectionClosed = jdbcDataContext.getConnection().isClosed();
      } catch (SQLException e) {
        LOG.error("Checking connection status failed", e);
      }
    }
    synchronized (isClosed) {
      if ((jdbcDataContext == null || connectionClosed) && !isClosed) {
        jdbcDataContext = (JdbcDataContext) jdbcDataContextFactory
            .create(dataContextProperties, null);
        dataContext.set(jdbcDataContext);
        dataContextPool.add(jdbcDataContext);
      }
    }
    return jdbcDataContext;
  }

  @Override
  public DataContext refreshSchemas() {
    try {
      return getDataContext().refreshSchemas();
    } catch (MetaModelException e) {
      LOG.error("Refreshing schema failed", e);
      return null;
    }
  }

  @Override
  public List<Schema> getSchemas() {
    return getDataContext().getSchemas();
  }

  @Override
  public List<String> getSchemaNames() {
    return getDataContext().getSchemaNames();
  }

  @Override
  public Schema getDefaultSchema() {
    return getDataContext().getDefaultSchema();
  }

  @Override
  public Schema getSchemaByName(String s) {
    return getDataContext().getSchemaByName(s);
  }

  @Override
  public InitFromBuilder query() {
    try {
      return getDataContext().query();
    } catch (MetaModelException e) {
      LOG.error("Initiating a query failed", e);
      return null;
    }
  }

  @Override
  public Query parseQuery(String s) {
    return getDataContext().parseQuery(s);
  }

  @Override
  public DataSet executeQuery(Query query) {
    return getDataContext().executeQuery(query);
  }

  @Override
  public CompiledQuery compileQuery(Query query) {
    return getDataContext().compileQuery(query);
  }

  @Override
  public DataSet executeQuery(CompiledQuery compiledQuery, Object... objects) {
    return getDataContext().executeQuery(compiledQuery, objects);
  }

  @Override
  public DataSet executeQuery(String s) {
    return getDataContext().executeQuery(s);
  }

  @Override
  public UpdateSummary executeUpdate(UpdateScript updateScript) {
    return getDataContext().executeUpdate(updateScript);
  }

  @Override
  public Column getColumnByQualifiedLabel(String s) {
    return getDataContext().getColumnByQualifiedLabel(s);
  }

  @Override
  public Table getTableByQualifiedLabel(String s) {
    return getDataContext().getTableByQualifiedLabel(s);
  }

  /**
   * Close hive data context and clear all its subsequent jdbc data contexts
   */
  public void close() {
    synchronized (isClosed) {
      if (!isClosed) {
        for (JdbcDataContext jdbcDataContext : dataContextPool) {
          Connection connection = jdbcDataContext.getConnection();
          jdbcDataContext.close(connection);
        }
        dataContextPool.clear();
        isClosed = true;
      }
    }
  }

  /**
   * Execute hive query sql
   *
   * @param hiveQuery query to be executed
   * @throws GoraException throw if a SQLException is thrown
   */
  public void executeHiveQL(String hiveQuery) throws GoraException {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Executing the query : {}", hiveQuery);
      }
      Connection connection = getDataContext().getConnection();
      Statement statement = connection.createStatement();
      statement.execute(hiveQuery);
      statement.close();
    } catch (SQLException e) {
      throw new GoraException(e);
    }
  }

  /**
   * Create a prepared statement using the given hive sql query
   *
   * @param hiveQuery query to be executed
   * @return {@link org.apache.hive.jdbc.HivePreparedStatement}  hive prepared statement
   * @throws GoraException throw if a SQLException is thrown
   */
  public PreparedStatement getPreparedStatement(String hiveQuery) throws GoraException {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Initiating prepared statement for the query : {}", hiveQuery);
      }
      Connection connection = getDataContext().getConnection();
      return connection.prepareStatement(hiveQuery);
    } catch (SQLException e) {
      throw new GoraException(e);
    }
  }

  /**
   * Generate DataContextPropertiesImpl using basic properties to establish a connection to Hive
   * backend service
   *
   * @param hiveStoreParameters hive store parameters including at least server url
   * @return DataContextPropertiesImpl connection properties
   */
  private DataContextPropertiesImpl generateDataContextProperties(
      HiveStoreParameters hiveStoreParameters) {
    final DataContextPropertiesImpl properties = new DataContextPropertiesImpl();
    properties.put(DataContextPropertiesImpl.PROPERTY_DATA_CONTEXT_TYPE,
        HiveStoreParameters.HIVE_DATA_CONTEXT_TYPE);
    properties.put(DataContextPropertiesImpl.PROPERTY_URL, hiveStoreParameters.getServerUrl());
    properties
        .put(DataContextPropertiesImpl.PROPERTY_DRIVER_CLASS, hiveStoreParameters.getDriverName());
    return properties;
  }
}