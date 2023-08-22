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

package org.apache.gora.arangodb.store;

import org.apache.hadoop.conf.Configuration;

import java.util.Properties;

/**
 * Maintains ArangoDB client related properties parsed from gora.properties.
 */
public class ArangoDBStoreParameters {

  public static final String ARANGO_DB_MAPPING_FILE = "gora.arangodb.mapping.file";
  public static final String ARANGO_DB_SERVER_HOST = "gora.arangodb.server.host";
  public static final String ARANGO_DB_SERVER_PORT = "gora.arangodb.server.port";
  public static final String ARANGO_DB_USER_USERNAME = "gora.arangodb.user.username";
  public static final String ARANGO_DB_USER_PASSWORD = "gora.arangodb.user.password";
  public static final String ARANGO_DB_DB_NAME = "gora.arangodb.database.name";
  public static final String ARANGO_DB_CONNECTION_POOL_SIZE = "gora.arangodb.con.pool.size";
  public static final String ARANGO_PROP_OVERRIDING = "gora.arangodb.override.hadoop.configuration";

  public static final String DEFAULT_ARANGO_DB_SERVER_HOST = "localhost";
  public static final String DEFAULT_ARANGO_DB_SERVER_PORT = "8529";
  public static final String DEFAULT_ARANGO_DB_USER_USERNAME = "root";
  public static final String DEFAULT_ARANGO_DB_USER_PASSWORD = "root";

  private String mappingFile;
  private String serverHost;
  private String serverPort;
  private String userName;
  private String userPassword;
  private String databaseName;
  private String connPoolSize;

  public String getMappingFile() {
    return this.mappingFile;
  }

  public String getServerHost() {
    return this.serverHost;
  }

  public String getServerPort() {
    return this.serverPort;
  }

  public String getUserName() {
    return this.userName;
  }

  public String getUserPassword() {
    return this.userPassword;
  }

  public String getDatabaseName() {
    return this.databaseName;
  }

  public String getConnectionPoolSize() {
    return this.connPoolSize;
  }


  public ArangoDBStoreParameters(String mappingFile,
                                 String serverHost,
                                 String serverPort,
                                 String userName,
                                 String userPassword,
                                 String databaseName,
                                 String connPoolSize) {
    this.mappingFile = mappingFile;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.userName = userName;
    this.userPassword = userPassword;
    this.databaseName = databaseName;
    this.connPoolSize = connPoolSize;
  }

  public static ArangoDBStoreParameters load(Properties properties, Configuration conf) {
    String propMappingFile = properties.getProperty(ARANGO_DB_MAPPING_FILE,
            ArangoDBStore.DEFAULT_MAPPING_FILE);
    String propServerHost = properties.getProperty(ARANGO_DB_SERVER_HOST);
    String propServerPort = properties.getProperty(ARANGO_DB_SERVER_PORT);
    String propUserName = properties.getProperty(ARANGO_DB_USER_USERNAME);
    String propUserPassword = properties.getProperty(ARANGO_DB_USER_PASSWORD);
    String propDatabaseName = properties.getProperty(ARANGO_DB_DB_NAME);
    String propConnPoolSize = properties.getProperty(ARANGO_DB_CONNECTION_POOL_SIZE);
    String overrideHadoop = properties.getProperty(ARANGO_PROP_OVERRIDING);
    if (!Boolean.parseBoolean(overrideHadoop)) {
      propServerHost = conf.get(ARANGO_DB_SERVER_HOST, DEFAULT_ARANGO_DB_SERVER_HOST);
      propServerPort = conf.get(ARANGO_DB_SERVER_PORT, DEFAULT_ARANGO_DB_SERVER_PORT);
      propUserName = conf.get(ARANGO_DB_USER_USERNAME, DEFAULT_ARANGO_DB_USER_USERNAME);
      propUserPassword = conf.get(ARANGO_DB_USER_PASSWORD, DEFAULT_ARANGO_DB_USER_PASSWORD);
    }

    return new ArangoDBStoreParameters(propMappingFile,
            propServerHost, propServerPort, propUserName,
            propUserPassword, propDatabaseName, propConnPoolSize);
  }

}
