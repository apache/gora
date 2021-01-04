/**
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
package org.apache.gora.neo4j.store;

import java.util.Properties;
import org.apache.hadoop.conf.Configuration;

/**
 * Parameters for Neo4j.
 */
public class Neo4jParameters {

  private String host;
  private String port;
  private String username;
  private String password;
  private String protocol;

  public Neo4jParameters(String host, String port, String username, String password, String protocol) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.protocol = protocol;
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

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * Reads parameters from a properties list
   *
   * @param properties Properties list
   * @return Neo4j parameters instance
   */
  public static Neo4jParameters load(Properties properties, Configuration conf) {
    return new Neo4jParameters(conf.get(Neo4jConstants.PROPERTY_HOST, properties.getProperty(Neo4jConstants.PROPERTY_HOST)),
            conf.get(Neo4jConstants.PROPERTY_PORT, properties.getProperty(Neo4jConstants.PROPERTY_PORT)),
            conf.get(Neo4jConstants.PROPERTY_USERNAME, properties.getProperty(Neo4jConstants.PROPERTY_USERNAME)),
            conf.get(Neo4jConstants.PROPERTY_PASSWORD, properties.getProperty(Neo4jConstants.PROPERTY_PASSWORD)),
            conf.get(Neo4jConstants.PROPERTY_PROTOCOL, properties.getProperty(Neo4jConstants.PROPERTY_PROTOCOL)));
  }

}
