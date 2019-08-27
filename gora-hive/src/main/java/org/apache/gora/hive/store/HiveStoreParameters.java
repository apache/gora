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

import java.util.Properties;

/**
 * This class is to hold parameters which need to initiate a connection to hive backend service
 */
public class HiveStoreParameters {
  /**
   * Hive server url can be, jdbc:hive2://<host>:<port>/<db>;initFile=<file>,
   * jdbc:hive2:///;initFile=<file>, jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>,
   * jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>,
   * jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2
   */
  private static final String HIVE_SERVER_URL_PROPERTY = "gora.hive.server.url";

  private static final String HIVE_DATABASE_NAME_PROPERTY = "gora.hive.database.name";
  private static final String HIVE_DRIVER_NAME_PROPERTY = "gora.hive.driver.name";

  //hive default values
  private static final String HIVE_DEFAULT_DATABASE_NAME = "default";
  public static final String HIVE_DEFAULT_DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
  public static final String HIVE_DATA_CONTEXT_TYPE = "jdbc";

  private String databaseName;
  private String serverUrl;
  private String driverName;

  /**
   * This shouldn't be initiated directly
   */
  private HiveStoreParameters() {
  }

  /**
   * Getter for hive server url
   *
   * @return server url
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * Setter for hive server url
   *
   * @param serverUrl Sever url
   */
  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  /**
   * Getter for hive driver class name.
   *
   * @return driver class name
   */
  public String getDriverName() {
    return driverName;
  }

  /**
   * Setter for hive driver name
   *
   * @param driverName driver class name
   */
  public void setDriverName(String driverName) {
    this.driverName = driverName;
  }

  /**
   * Getter for hive database name
   *
   * @return database name
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Setter for hive database name
   *
   * @param databaseName database name
   */
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * Derive hive store parameters from the properties
   *
   * @param properties hive properties
   * @return HiveStoreParameters object
   */
  public static HiveStoreParameters load(Properties properties) {
    HiveStoreParameters storeParameters = new HiveStoreParameters();
    storeParameters
        .setDatabaseName(
            properties.getProperty(HIVE_DATABASE_NAME_PROPERTY, HIVE_DEFAULT_DATABASE_NAME));
    storeParameters.setServerUrl(properties.getProperty(HIVE_SERVER_URL_PROPERTY));
    storeParameters.setDriverName(properties.getProperty(HIVE_DRIVER_NAME_PROPERTY,
        HIVE_DEFAULT_DRIVER_NAME));
    return storeParameters;
  }
}
