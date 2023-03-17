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

/**
 *
 * Constants for Neo4j
 */
public class Neo4jConstants {

  /**
   * IP address or hostname of the neo4j server.
   */
  public static final String PROPERTY_HOST = "gora.datastore.neo4j.host";
  /**
   * Port number of the neo4j server.
   */
  public static final String PROPERTY_PORT = "gora.datastore.neo4j.port";
  /**
   * Username for the JDBC connection.
   */
  public static final String PROPERTY_USERNAME = "gora.datastore.neo4j.username";
  /**
   * Password for the JDBC connection.
   */
  public static final String PROPERTY_PASSWORD = "gora.datastore.neo4j.password";
  /**
   * Access protocol used in the Neo4j JDBC Driver.
   */
  public static final String PROPERTY_PROTOCOL = "gora.datastore.neo4j.protocol";

  /**
   * JDBC Driver used by Apache Gora.
   */
  public static final String DRIVER_NAME = "org.neo4j.jdbc.Driver";
}
