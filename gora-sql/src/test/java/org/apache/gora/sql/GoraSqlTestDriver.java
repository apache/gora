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

package org.apache.gora.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.sql.store.SqlStore;
import org.apache.hadoop.util.StringUtils;
import org.hsqldb.Server;

/**
 * Helper class for third part tests using gora-sql backend. 
 * @see GoraTestDriver
 */
public class GoraSqlTestDriver extends GoraTestDriver {

  public GoraSqlTestDriver() {
    super(SqlStore.class);
  }

  /** The JDBC Driver class name */
  protected static final String DRIVER_CLASS_PROPERTY = "jdbc.driver";
  /** JDBC Database access URL */
  protected static final String URL_PROPERTY = "jdbc.url";
  /** User name to access the database */
  protected static final String USERNAME_PROPERTY = "jdbc.user";
  /** Password to access the database */
  protected static final String PASSWORD_PROPERTY = "jdbc.password";


  private static final String JDBC_URL = "jdbc:hsqldb:hsql://localhost/goratest";
  private static final String JDBC_DRIVER_CLASS = "org.hsqldb.jdbcDriver";

  private Server server;

  private boolean initialized = false;

  private boolean startHsqldb = true;

  private void startHsqldbServer() {
    log.info("Starting HSQLDB server");
    server = new Server();
    server.setDatabasePath(0,
        System.getProperty("test.build.data", "/tmp") + "/goratest");
    server.setDatabaseName(0, "goratest");
    server.setDaemon(true);
    server.start();
  }

  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();

    if(!this.initialized && startHsqldb) {
      startHsqldbServer();
      this.initialized = true;
    }
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    try {
      if(server != null) {
        server.shutdown();
      }
    }catch (Throwable ex) {
      log.warn("Exception occurred while shutting down HSQLDB :"
          + StringUtils.stringifyException(ex));
    }
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @SuppressWarnings("unused")
  private Connection createConnection(String driverClassName
      , String url) throws Exception {

    Class.forName(driverClassName);
    Connection connection = DriverManager.getConnection(url);
    connection.setAutoCommit(false);
    return connection;
  }


  @Override
  protected void setProperties(Properties properties) {
    super.setProperties(properties);
    properties.setProperty("gora.sqlstore." + DRIVER_CLASS_PROPERTY, JDBC_DRIVER_CLASS);
    properties.setProperty("gora.sqlstore." + URL_PROPERTY, JDBC_URL);
    properties.remove("gora.sqlstore." + USERNAME_PROPERTY);
    properties.remove("gora.sqlstore." + PASSWORD_PROPERTY);
  }

}
