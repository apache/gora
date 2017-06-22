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

package org.apache.gora.cassandra.store;

public class Constants {

  /* string (multiple values with comma separated) */
  public static final String CASSANDRA_SERVERS = "gora.cassandra.store.cassandraServers";
  /* string */
  public static final String KEYSPACE = "gora.cassandra.store.keyspace";
  /* integer */
  public static final String PORT = "gora.cassandra.store.port";
  /* string */
  public static final String CLUSTER_NAME = "gora.cassandra.store.clusterName";
  /* "LZ4", "SNAPPY", "NONE" */
  public static final String COMPRESSION = "gora.cassandra.store.compression";
  /* string */
  public static final String USERNAME = "gora.cassandra.store.username";
  /* string */
  public static final String PASSWORD = "gora.cassandra.store.password";
  /* "RoundRobinPolicy", "LatencyAwareRoundRobinPolicy", "TokenAwareRoundRobinPolicy" */
  public static final String LOAD_BALANCING_POLICY = "gora.cassandra.store.loadBalancingPolicy";
  /* boolean */
  public static final String ENABLE_JMX_REPORTING = "gora.cassandra.store.enableJMXReporting";
  /* boolean */
  public static final String ENABLE_METRICS = "gora.cassandra.store.enableMetrics";
  /* inter */
  public static final String LOCAL_CORE_CONNECTIONS_PER_HOST = "gora.cassandra.store.localCoreConnectionsPerHost";
  /* integer */
  public static final String REMOTE_CORE_CONNECTIONS_PER_HOST = "gora.cassandra.store.remoteCoreConnectionsPerHost";
  /* integer */
  public static final String LOCAL_MAX_CONNECTIONS_PER_HOST = "gora.cassandra.store.localMaxConnectionsPerHost";
  /* integer */
  public static final String REMOTE_MAX_CONNECTIONS_PER_HOST = "gora.cassandra.store.remoteMaxConnectionsPerHost";
  /* integer */
  public static final String LOCAL_NEW_CONNECTION_THRESHOLD= "gora.cassandra.store.localNewConnectionThreshold";
  /* integer */
  public static final String REMOTE_NEW_CONNECTION_THRESHOLD = "gora.cassandra.store.remoteNewConnectionThreshold";
  /* integer */
  public static final String LOCAL_MAX_REQUESTS_PER_CONNECTION = "gora.cassandra.store.localMaxRequestsPerConnection";
  /* integer */
  public static final String REMOTE_MAX_REQUESTS_PER_CONNECTION = "gora.cassandra.store.remoteMaxRequestsPerConnection";
  /* integer */
  public static final String PROTOCOL_VERSION = "gora.cassandra.store.protocolVersion";
  /* "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO" */
  public static final String CONSISTENCY_LEVEL = "gora.cassandra.store.consistencyLevel";
  /* integer */
  public static final String FETCH_SIZE = "fetchSize";
  /* "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO" */
  public static final String SERIAL_CONSISTENCY_LEVEL = "gora.cassandra.store.serialConsistencyLevel";
  /* "ConstantReconnectionPolicy", "ExponentialReconnectionPolicy",  */
  public static final String RECONNECTION_POLICY = "gora.cassandra.store.reconnectionPolicy";
  /* long */
  public static final String CONSTANT_RECONNECTION_POLICY_DELAY = "gora.cassandra.store.constantReconnectionPolicyDelay";
  /* long */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY = "gora.cassandra.store.exponentialReconnectionPolicyBaseDelay";
  /* long */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY = "gora.cassandra.store.exponentialReconnectionPolicyMaxDelay";
  /* "DefaultRetryPolicy", "DowngradingConsistencyRetryPolicy", "FallthroughRetryPolicy",
   * "LoggingDefaultRetryPolicy", "LoggingDowngradingConsistencyRetryPolicy", "LoggingFallthroughRetryPolicy" */
  public static final String RETRY_POLICY = "gora.cassandra.store.retryPolicy";
  /* integer */
  public static final String CONNECTION_TIMEOUT_MILLIS = "gora.cassandra.store.connectionTimeoutMillis";
  /* boolean */
  public static final String KEEP_ALIVE = "gora.cassandra.store.keepAlive";
  /* integer */
  public static final String READ_TIMEOUT_MILLIS = "gora.cassandra.store.readTimeoutMillis";
  /* integer */
  public static final String RECEIVER_BUFFER_SIZE = "gora.cassandra.store.receiverBufferSize";
  /* boolean */
  public static final String REUSE_ADDRESS = "gora.cassandra.store.reuseAddress";
  /* integer */
  public static final String SEND_BUFFER_SIZE = "gora.cassandra.store.sendBufferSize";
  /* integer */
  public static final String SO_LINGER = "gora.cassandra.store.soLinger";
  /* boolean */
  public static final String TCP_NODELAY = "gora.cassandra.store.tcpNoDelay";
  /* boolean */
  public static final String ENABLE_SSL = "gora.cassandra.store.enableSSL";
  /* string */
  public static final String DATA_CENTER = "gora.cassandra.store.dataCenter";
  /* boolean */
  public static final String ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL = "gora.cassandra.store.allowRemoteDCsForLocalConsistencyLevel";

  public static final String USE_CASSANDRA_MAPPING_MANAGER = "gora.cassandra.store.useCassandraMappingManager";
}
