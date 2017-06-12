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
  public static final String CASSANDRA_SERVERS = "gora.cassandrastore.cassandraServers";
  /* string */
  public static final String KEYSPACE = "gora.cassandrastore.keyspace";
  /* integer */
  public static final String PORT = "gora.cassandrastore.port";
  /* string */
  public static final String CLUSTER_NAME = "gora.cassandrastore.clusterName";
  /* "LZ4", "SNAPPY", "NONE" */
  public static final String COMPRESSION = "gora.cassandrastore.compression";
  /* string */
  public static final String USERNAME = "gora.cassandrastore.username";
  /* string */
  public static final String PASSWORD = "gora.cassandrastore.password";
  /* "RoundRobinPolicy", "LatencyAwareRoundRobinPolicy", "TokenAwareRoundRobinPolicy" */
  public static final String LOAD_BALANCING_POLICY = "gora.cassandrastore.loadBalancingPolicy";
  /* boolean */
  public static final String ENABLE_JMX_REPORTING = "gora.cassandrastore.enableJMXReporting";
  /* boolean */
  public static final String ENABLE_METRICS = "gora.cassandrastore.enableMetrics";
  /* inter */
  public static final String LOCAL_CORE_CONNECTIONS_PER_HOST = "gora.cassandrastore.localCoreConnectionsPerHost";
  /* integer */
  public static final String REMOTE_CORE_CONNECTIONS_PER_HOST = "gora.cassandrastore.remoteCoreConnectionsPerHost";
  /* integer */
  public static final String LOCAL_MAX_CONNECTIONS_PER_HOST = "gora.cassandrastore.localMaxConnectionsPerHost";
  /* integer */
  public static final String REMOTE_MAX_CONNECTIONS_PER_HOST = "gora.cassandrastore.remoteMaxConnectionsPerHost";
  /* integer */
  public static final String LOCAL_NEW_CONNECTION_THRESHOLD= "gora.cassandrastore.localNewConnectionThreshold";
  /* integer */
  public static final String REMOTE_NEW_CONNECTION_THRESHOLD = "gora.cassandrastore.remoteNewConnectionThreshold";
  /* integer */
  public static final String LOCAL_MAX_REQUESTS_PER_CONNECTION = "gora.cassandrastore.localMaxRequestsPerConnection";
  /* integer */
  public static final String REMOTE_MAX_REQUESTS_PER_CONNECTION = "gora.cassandrastore.remoteMaxRequestsPerConnection";
  /* integer */
  public static final String PROTOCOL_VERSION = "gora.cassandrastore.protocolVersion";
  /* "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO" */
  public static final String CONSISTENCY_LEVEL = "gora.cassandrastore.consistencyLevel";
  /* integer */
  public static final String FETCH_SIZE = "fetchSize";
  /* "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO" */
  public static final String SERIAL_CONSISTENCY_LEVEL = "gora.cassandrastore.serialConsistencyLevel";
  /* "ConstantReconnectionPolicy", "ExponentialReconnectionPolicy",  */
  public static final String RECONNECTION_POLICY = "gora.cassandrastore.reconnectionPolicy";
  /* long */
  public static final String CONSTANT_RECONNECTION_POLICY_DELAY = "gora.cassandrastore.constantReconnectionPolicyDelay";
  /* long */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY = "gora.cassandrastore.exponentialReconnectionPolicyBaseDelay";
  /* long */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY = "gora.cassandrastore.exponentialReconnectionPolicyMaxDelay";
  /* "DefaultRetryPolicy", "DowngradingConsistencyRetryPolicy", "FallthroughRetryPolicy",
   * "LoggingDefaultRetryPolicy", "LoggingDowngradingConsistencyRetryPolicy", "LoggingFallthroughRetryPolicy" */
  public static final String RETRY_POLICY = "gora.cassandrastore.retryPolicy";
  /* integer */
  public static final String CONNECTION_TIMEOUT_MILLIS = "gora.cassandrastore.connectionTimeoutMillis";
  /* boolean */
  public static final String KEEP_ALIVE = "gora.cassandrastore.keepAlive";
  /* integer */
  public static final String READ_TIMEOUT_MILLIS = "gora.cassandrastore.readTimeoutMillis";
  /* integer */
  public static final String RECEIVER_BUFFER_SIZE = "gora.cassandrastore.receiverBufferSize";
  /* boolean */
  public static final String REUSE_ADDRESS = "gora.cassandrastore.reuseAddress";
  /* integer */
  public static final String SEND_BUFFER_SIZE = "gora.cassandrastore.sendBufferSize";
  /* integer */
  public static final String SO_LINGER = "gora.cassandrastore.soLinger";
  /* boolean */
  public static final String TCP_NODELAY = "gora.cassandrastore.tcpNoDelay";
  /* boolean */
  public static final String ENABLE_SSL = "gora.cassandrastore.enableSSL";
  /* string */
  public static final String DATA_CENTER = "gora.cassandrastore.dataCenter";
  /* boolean */
  public static final String ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL = "gora.cassandrastore.allowRemoteDCsForLocalConsistencyLevel";

}
