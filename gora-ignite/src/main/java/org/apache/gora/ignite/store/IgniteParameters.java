/*
 * Copyright 2018 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.ignite.store;

import java.util.Properties;
import org.apache.hadoop.conf.Configuration;

/**
 * Parameters definitions for Ignite.
 */
public class IgniteParameters {

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

  private String host;
  private String port;
  private String schema;
  private String user;
  private String password;
  private String additionalConfigurations;

  /**
   *
   * @param host
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

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAdditionalConfigurations() {
    return additionalConfigurations;
  }

  public void setAdditionalConfigurations(String additionalConfigurations) {
    this.additionalConfigurations = additionalConfigurations;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public static IgniteParameters load(Properties properties, Configuration conf) {
    return new IgniteParameters(
        properties.getProperty(PROP_HOST, "localhost"),
        properties.getProperty(PROP_PORT, "10800"),
        properties.getProperty(PROP_SCHEMA, null),
        properties.getProperty(PROP_USER, null),
        properties.getProperty(PROP_PASSWORD, null),
        properties.getProperty(PROP_ADDITIONALS, null));
  }

}
