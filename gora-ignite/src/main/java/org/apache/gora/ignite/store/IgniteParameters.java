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
package org.apache.gora.ignite.store;

import java.util.Properties;
import org.apache.gora.ignite.utils.IgniteBackendConstants;

/**
 * Parameters definitions for Ignite.
 */
public class IgniteParameters {

  private String host;
  private String port;
  private String schema;
  private String user;
  private String password;
  private String additionalConfigurations;

  /**
   * @param host Hostname/IP of the Ignite Server
   * @param port Optional port for Ignite Server
   * @param user Optional username for Ignite
   * @param password Optional password for Ignite
   * @param additionalConfigurations Optional additional configurations for
   * Ignite
   */
  private IgniteParameters(String host, String port, String schema, String user, String password, String additionalConfigurations) {
    this.host = host;
    this.port = port;
    this.schema = schema;
    this.user = user;
    this.password = password;
    this.additionalConfigurations = additionalConfigurations;
  }

  /**
   * Returns the ignite hostname
   *
   * @return IP/domain of the ignite server
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the ignite hostname
   *
   * @param host IP/domain of the ignite server
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Returns the ignite port
   *
   * @return Port of the ignite server eg. 10800
   */
  public String getPort() {
    return port;
  }

  /**
   * Sets the ignite port
   *
   * @param port Port of the ignite server eg. 10800
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Returns the username used for the ignite connection
   *
   * @return Username of ignite
   */
  public String getUser() {
    return user;
  }

  /**
   * Sets the username used for the ignite connection
   *
   * @param user Username of ignite
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Returns the secrets used for the ignite connection
   *
   * @return Password of the ignite user
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the secrets used for the ignite connection
   *
   * @param password Password of the ignite user
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns additional configurations used for the JDBC connection For more
   * details refer to https://apacheignite-sql.readme.io/docs/jdbc-driver
   *
   * @return String containing JDBC configurations
   */
  public String getAdditionalConfigurations() {
    return additionalConfigurations;
  }

  /**
   * Sets additional configurations used for the JDBC connection. For more
   * details refer to https://apacheignite-sql.readme.io/docs/jdbc-driver
   *
   * @param additionalConfigurations String containing JDBC configurations
   */
  public void setAdditionalConfigurations(String additionalConfigurations) {
    this.additionalConfigurations = additionalConfigurations;
  }

  /**
   * Returns the ignite schema for the JDBC connection
   *
   * @return Ignite schema e.g. PUBLIC
   */
  public String getSchema() {
    return schema;
  }

  /**
   * Sets the ignite schema for the JDBC connection
   *
   * @param schema Ignite schema e.g. PUBLIC
   */
  public void setSchema(String schema) {
    this.schema = schema;
  }

  /**
   * Reads Ignite parameters from a properties list
   *
   * @param properties Properties list
   * @return Ignite parameters instance
   */
  public static IgniteParameters load(Properties properties) {
    return new IgniteParameters(
        properties.getProperty(IgniteBackendConstants.PROP_HOST, IgniteBackendConstants.DEFAULT_IGNITE_HOST),
        properties.getProperty(IgniteBackendConstants.PROP_PORT, IgniteBackendConstants.DEFAULT_IGNITE_PORT),
        properties.getProperty(IgniteBackendConstants.PROP_SCHEMA),
        properties.getProperty(IgniteBackendConstants.PROP_USER),
        properties.getProperty(IgniteBackendConstants.PROP_PASSWORD),
        properties.getProperty(IgniteBackendConstants.PROP_ADDITIONALS));
  }

}
