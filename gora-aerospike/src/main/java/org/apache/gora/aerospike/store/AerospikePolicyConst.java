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

package org.apache.gora.aerospike.store;

/**
 * Class containing the configurable policy names
 */
public class AerospikePolicyConst {

  public static final String WRITE_POLICY_NAME = "write";

  public static final String GENERATION_POLICY_NAME = "gen";

  public static final String RECORD_EXISTS_ACTION_NAME = "recordExists";

  public static final String COMMIT_LEVEL_NAME = "commitLevel";

  public static final String DURABLE_DELETE_NAME = "durableDelete";

  public static final String EXPIRATION_NAME = "expiration";

  public static final String READ_POLICY_NAME = "read";

  public static final String PRIORITY_NAME = "priority";

  public static final String CONSISTENCY_LEVEL_NAME = "consistencyLevel";

  public static final String REPLICA_POLICY_NAME = "replica";

  public static final String SOCKET_TIMEOUT_NAME = "socketTimeout";

  public static final String TOTAL_TIMEOUT_NAME = "totalTimeout";

  public static final String TIMEOUT_DELAY_NAME = "timeoutDelay";

  public static final String MAX_RETRIES_NAME = "maxRetries";

}
