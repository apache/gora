/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.couchdb.store;

import java.util.Properties;

/**
 * Configuration properties
 */
public class CouchDBParameters {

  /**
   * Property pointing to the host where the server is running
   */
  public static final String PROP_COUCHDB_SERVER = "gora.datastore.couchdb.server";

  /**
   * Property pointing to the port where the server is running
   */
  public static final String PROP_COUCHDB_PORT = "gora.datastore.couchdb.port";

  private final String server;
  private final String port;

  /**
   *
   *  Initializing for configuration properties
   *
   * @param server      server domain or ip for couchDB connection
   * @param port        port for couchDB connection
   */
  private CouchDBParameters(String server, String port) {
    this.server = server;
    this.port = port;
  }

  /**
   * Get server domain
   *
   * @return
   */
  public String getServer() {
    return server;
  }

  /**
   * Get server port
   *
   * @return
   */
  public int getPort() {
    return Integer.parseInt(port);
  }

  /**
   * load configuration values
   *
   * @param properties
   * @return
   */
  public static CouchDBParameters load(Properties properties) {
    String couchDBServer = properties.getProperty(PROP_COUCHDB_SERVER);
    String couchDBPort = properties.getProperty(PROP_COUCHDB_PORT);

    return new CouchDBParameters(couchDBServer, couchDBPort);
  }
}
