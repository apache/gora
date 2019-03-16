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

/**
 *
 * Constants file for Ignite.
 */
public class IgniteBackendConstants {

  /**
   * Ignite JDBC constants
   */
  public static final String DRIVER_NAME = "org.apache.ignite.IgniteJdbcThinDriver";
  public static final String JDBC_PREFIX = "jdbc:ignite:thin://";

  /*
  * Default configurations for Ignite
   */
  public static final String DEFAULT_IGNITE_HOST = "localhost";
  public static final String DEFAULT_IGNITE_PORT = "10800";

  /*
   * A '42000' error code is thrown by Ignite when a non-existent table is queried.
   * More details: https://apacheignite-sql.readme.io/docs/jdbc-error-codes
   */
  public static final String DEFAULT_IGNITE_TABLE_NOT_EXISTS_CODE = "42000";

  /**
   * Property indicating the Ignite Schema to be used
   */
  public static final String PROP_SCHEMA = "gora.datastore.ignite.schema";

  /**
   * Property indicating the Ignite Cluster Node IP
   */
  public static final String PROP_HOST = "gora.datastore.ignite.host";

  /**
   * Property indicating the port used by the Ignite Server
   */
  public static final String PROP_PORT = "gora.datastore.ignite.port";

  /**
   * Property indicating the username to connect to the server
   */
  public static final String PROP_USER = "gora.datastore.ignite.user";

  /**
   * Property indicating the password to connect to the server
   */
  public static final String PROP_PASSWORD = "gora.datastore.ignite.password";

  /**
   * Property indicating additional JDBC options
   */
  public static final String PROP_ADDITIONALS = "gora.datastore.ignite.additionalConfigurations";

}
