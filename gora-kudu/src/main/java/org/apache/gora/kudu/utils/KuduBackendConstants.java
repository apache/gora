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
package org.apache.gora.kudu.utils;

/**
 *
 * Constants file for Kudu.
 */
public class KuduBackendConstants {

  /*
  * Default configurations for Kudu
   */
  public static final String DEFAULT_KUDU_MASTERS = "localhost:123";

  /**
   * List of keys used in the configuration file of Kudu
   */
  public static final String PROP_MASTERADDRESSES = "gora.datastore.kudu.masterAddresses";
  public static final String PROP_BOSSCOUNT = "gora.datastore.kudu.bossCount";
  public static final String PROP_DEFAULTADMINOPERATIONTIMEOUTMS = "gora.datastore.kudu.defaultAdminOperationTimeoutMs";
  public static final String PROP_DEFAULTOPERATIONTIMEOUTMS = "gora.datastore.kudu.defaultOperationTimeoutMs";
  public static final String PROP_DEFAULTSOCKETREADTIMEOUTMS = "gora.datastore.kudu.defaultSocketReadTimeoutMs";
  public static final String PROP_CLIENTSTATISTICS = "gora.datastore.kudu.clientStatistics";
  public static final String PROP_WORKERCOUNT = "gora.datastore.kudu.workerCount";
  public static final String AS_PROP_OVERRIDING = "gora.kudu.override.hadoop.configuration";

}
