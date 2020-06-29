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

package org.apache.gora.rethinkdb.store;

import java.util.Properties;

/**
 * Maintains RethinkDB client related properties parsed from gora.properties.
 */
public class RethinkDBStoreParameters {

  public static final String RETHINK_DB_MAPPING_FILE = "gora.rethinkdb.mapping.file";
  public static final String RETHINK_DB_SERVER_HOST = "gora.rethinkdb.server.host";
  public static final String RETHINK_DB_SERVER_PORT = "gora.rethinkdb.server.port";
  public static final String RETHINK_DB_USER_USERNAME = "gora.rethinkdb.user.username";
  public static final String RETHINK_DB_USER_PASSWORD = "gora.rethinkdb.user.password";
  public static final String RETHINK_DB_DB_NAME = "gora.rethinkdb.database.name";

  private String mappingFile;
  private String serverHost;
  private String serverPort;
  private String userName;
  private String userPassword;
  private String databaseName;

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

  public RethinkDBStoreParameters(String mappingFile,
                                  String serverHost,
                                  String serverPort,
                                  String userName,
                                  String userPassword,
                                  String databaseName) {
    this.mappingFile = mappingFile;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.userName = userName;
    this.userPassword = userPassword;
    this.databaseName = databaseName;
  }


  public static RethinkDBStoreParameters load(Properties properties) {
    String propMappingFile = properties.getProperty(RETHINK_DB_MAPPING_FILE,
            RethinkDBStore.DEFAULT_MAPPING_FILE);
    String propServerHost = properties.getProperty(RETHINK_DB_SERVER_HOST);
    String propServerPort = properties.getProperty(RETHINK_DB_SERVER_PORT);
    String propUserName = properties.getProperty(RETHINK_DB_USER_USERNAME);
    String propUserPassword = properties.getProperty(RETHINK_DB_USER_PASSWORD, "");
    String propDatabaseName = properties.getProperty(RETHINK_DB_DB_NAME);
    return new RethinkDBStoreParameters(propMappingFile,
            propServerHost, propServerPort, propUserName,
            propUserPassword, propDatabaseName);
  }

}
