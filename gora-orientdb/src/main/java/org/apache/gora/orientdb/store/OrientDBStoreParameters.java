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

package org.apache.gora.orientdb.store;

import java.util.Properties;

/**
 * Maintains OrientDB client related properties parsed from gora.properties.
 */
public class OrientDBStoreParameters {

  public static final String ORIENT_DB_MAPPING_FILE = "gora.orientdb.mapping.file";
  public static final String ORIENT_DB_SERVER_HOST = "gora.orientdb.server.host";
  public static final String ORIENT_DB_SERVER_PORT = "gora.orientdb.server.port";
  public static final String ORIENT_DB_USER_USERNAME = "gora.orientdb.user.username";
  public static final String ORIENT_DB_USER_PASSWORD = "gora.orientdb.user.password";
  public static final String ORIENT_DB_DB_NAME = "gora.orientdb.database.name";
  public static final String ORIENT_DB_CONNECTION_POOL_MIN_SIZE = "gora.orientdb.con.pool.min.size";
  public static final String ORIENT_DB_CONNECTION_POOL_MAX_SIZE = "gora.orientdb.con.pool.max.size";
  public static final String ORIENT_DB_STORAGE_TYPE = "gora.orientdb.storage.type";


  private String mappingFile;
  private String serverHost;
  private String serverPort;
  private String userName;
  private String userPassword;
  private String databaseName;
  private String connPoolMinSize;
  private String connPoolMaxSize;
  private String storageType;


  /**
   * Return classpath or file system location for OrientDB mapping file. Eg:- /gora-orientdb-mapping.xml
   *
   * @return OrientDB Mapping file Location as string.
   */
  public String getMappingFile() {
    return this.mappingFile;
  }

  /**
   * Return remote OrientDB server host name. Eg:- localhost
   *
   * @return OrientDB remote server host as string.
   */
  public String getServerHost() {
    return this.serverHost;
  }

  /**
   * Return remote OrientDB server port number. Eg:- 2424
   *
   * @return OrientDB remote server port number as string.
   */
  public String getServerPort() {
    return this.serverPort;
  }

  /**
   * Return remote OrientDB server client connecting user username. Eg:- admin
   *
   * @return OrientDB remote server client connecting user username as string.
   */
  public String getUserName() {
    return this.userName;
  }

  /**
   * Return remote OrientDB server client connecting user password. Eg:- admin
   *
   * @return OrientDB remote server client connecting user pass as string.
   */
  public String getUserPassword() {
    return this.userPassword;
  }

  /**
   * Return remote OrientDB server pointing database name. Eg:- gora
   *
   * @return OrientDB remote server pointing database name as string.
   */
  public String getDatabaseName() {
    return this.databaseName;
  }

  /**
   * Return remote OrientDB client connections pool min size. Eg:- 80
   *
   * @return OrientDB remote server client connections pool min size as string.
   */
  public String getConnectionPoolMinSize() {
    return this.connPoolMinSize;
  }

  /**
   * Return remote OrientDB client connections pool max size. Eg:- 100
   *
   * @return OrientDB remote server client connections pool max size as string.
   */
  public String getConnectionPoolMaxSize() {
    return this.connPoolMaxSize;
  }

  /**
   * Return remote OrientDB server storage type of pointing database. Eg:- plocal, memory
   *
   * @return OrientDB remote server storage type of pointing database.
   */
  public String getStorageType() {
    return this.storageType;
  }

  public OrientDBStoreParameters(String mappingFile,
                                 String serverHost,
                                 String serverPort,
                                 String userName,
                                 String userPassword,
                                 String databaseName,
                                 String connPoolMinSize,
                                 String connPoolMaxSize,
                                 String storageType) {
    this.mappingFile = mappingFile;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
    this.userName = userName;
    this.userPassword = userPassword;
    this.databaseName = databaseName;
    this.connPoolMinSize = connPoolMinSize;
    this.connPoolMaxSize = connPoolMaxSize;
    this.storageType = storageType;
  }

  /**
   * Extraction OrientDB dataStore properties from {@link Properties} gora.properties file.
   *
   * @param properties gora.properties properties related to datastore client.
   * @return OrientDB client properties encapsulated inside instance of {@link OrientDBStoreParameters}
   */
  public static OrientDBStoreParameters load(Properties properties) {
    String propMappingFile = properties.getProperty(ORIENT_DB_MAPPING_FILE,
            OrientDBStore.DEFAULT_MAPPING_FILE);
    String propServerHost = properties.getProperty(ORIENT_DB_SERVER_HOST);
    String propServerPort = properties.getProperty(ORIENT_DB_SERVER_PORT);
    String propUserName = properties.getProperty(ORIENT_DB_USER_USERNAME);
    String propUserPassword = properties.getProperty(ORIENT_DB_USER_PASSWORD);
    String propDatabaseName = properties.getProperty(ORIENT_DB_DB_NAME);
    String propConnPoolMinSize = properties.getProperty(ORIENT_DB_CONNECTION_POOL_MIN_SIZE);
    String propConnPoolMaxSize = properties.getProperty(ORIENT_DB_CONNECTION_POOL_MAX_SIZE);
    String propStorageType = properties.getProperty(ORIENT_DB_STORAGE_TYPE);
    return new OrientDBStoreParameters(propMappingFile,
            propServerHost, propServerPort, propUserName,
            propUserPassword, propDatabaseName, propConnPoolMinSize, propConnPoolMaxSize, propStorageType);
  }
}
