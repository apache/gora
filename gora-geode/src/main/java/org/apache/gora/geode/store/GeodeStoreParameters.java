/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.gora.geode.store;

/**
 * Configuration Properties.
 */
public class GeodeStoreParameters {

  /**
   * Property pointing to geode server contact points.
   * string (multiple values with comma separated)
   */
  public static final String GEODE_SERVER_HOST = "gora.geode.server.hostname";

  /**
   * Property pointing to the port to use to connect to the geode hosts.
   * integer
   */
  public static final String GEODE_SERVER_PORT = "gora.geode.server.port";

  /**
   * Property pointing to the gora schemaName.
   * integer
   */
  public static final String PREFERRED_SCHEMA_NAME = "gora.geode.preferred.schemaName";

  /**
   * Property pointing to the geode region shortcut.
   * integer
   */
  public static final String GEODE_REGION_SHORTCUT = "gora.geode.region.shortcut";

  /**
   * Property pointing to the username to connect to the server.
   * string
   */
  public static final String GEODE_USERNAME = "gora.geode.username";

  /**
   * Property pointing to the password to connect to the server.
   * string
   */
  public static final String GEODE_PASSWORD = "gora.geode.password";

}
