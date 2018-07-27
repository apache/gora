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
}
