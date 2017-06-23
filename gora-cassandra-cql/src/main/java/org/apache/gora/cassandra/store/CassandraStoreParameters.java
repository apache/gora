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

/**
 * Configuration Properties.
 */
public class CassandraStoreParameters {

  /**
   * Property pointing to cassandra db contact points.
   * string (multiple values with comma separated)
   */
  public static final String CASSANDRA_SERVERS = "gora.cassandra.store.cassandraServers";
  /**
   *Property pointing to the Cassandra keyspace.
   * string
   */
  public static final String KEYSPACE = "gora.cassandra.store.keyspace";
  /**
   *  Property pointing to the port to use to connect to the Cassandra hosts.
   *  integer
   */
  public static final String PORT = "gora.cassandra.store.port";

  /**
   * Property pointing to the Cassandra cluster name.
   * string
   */
  public static final String CLUSTER_NAME = "gora.cassandra.store.clusterName";
  /**
   * Property pointing to set compression to use for the transport.
   * "LZ4", "SNAPPY", "NONE"
   */
  public static final String COMPRESSION = "gora.cassandra.store.compression";
  /**
   * Property pointing to the username to connect to the server.
   * string
   */
  public static final String USERNAME = "gora.cassandra.store.username";
  /**
   * Property pointing to the password to connect to the server.
   * string
   */
  public static final String PASSWORD = "gora.cassandra.store.password";
  /**
   * Property pointing to set load balancing policy.
   * "RoundRobinPolicy", "LatencyAwareRoundRobinPolicy", "TokenAwareRoundRobinPolicy"
   */
  public static final String LOAD_BALANCING_POLICY = "gora.cassandra.store.loadBalancingPolicy";
  /**
   * Property pointing to enable/disable JMX reporting.
   * boolean
   */
  public static final String ENABLE_JMX_REPORTING = "gora.cassandra.store.enableJMXReporting";
  /**
   * Property pointing to enable/disable metrics.
   * boolean
   */
  public static final String ENABLE_METRICS = "gora.cassandra.store.enableMetrics";
  /**
   * Property pointing to set local host core connections size.
   * integer
   */
  public static final String LOCAL_CORE_CONNECTIONS_PER_HOST = "gora.cassandra.store.localCoreConnectionsPerHost";
  /**
   * Property pointing to set remote host core connections size.
   * integer
   */
  public static final String REMOTE_CORE_CONNECTIONS_PER_HOST = "gora.cassandra.store.remoteCoreConnectionsPerHost";
  /**
   * Property pointing to set local host max connections size.
   * integer
   */
  public static final String LOCAL_MAX_CONNECTIONS_PER_HOST = "gora.cassandra.store.localMaxConnectionsPerHost";
  /**
   * Property pointing to set remote host max connections size.
   * integer
   */
  public static final String REMOTE_MAX_CONNECTIONS_PER_HOST = "gora.cassandra.store.remoteMaxConnectionsPerHost";
  /**
   * Property pointing to set local host new connection threshold.
   * integer
   */
  public static final String LOCAL_NEW_CONNECTION_THRESHOLD= "gora.cassandra.store.localNewConnectionThreshold";
  /**
   * Property pointing to set remote host new connection threshold.
   * integer
   */
  public static final String REMOTE_NEW_CONNECTION_THRESHOLD = "gora.cassandra.store.remoteNewConnectionThreshold";
  /**
   * Property pointing to set local host max requests per connection.
   * integer
   */
  public static final String LOCAL_MAX_REQUESTS_PER_CONNECTION = "gora.cassandra.store.localMaxRequestsPerConnection";
  /**
   * Property pointing to set remote host max requests per connection.
   * integer
   */
  public static final String REMOTE_MAX_REQUESTS_PER_CONNECTION = "gora.cassandra.store.remoteMaxRequestsPerConnection";
  /**
   * Property pointing to set CQL Protocol version.
   * integer
   */
  public static final String PROTOCOL_VERSION = "gora.cassandra.store.protocolVersion";
  /**
   * Property pointing to set consistency level in Cassandra Query Options.
   * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
   */
  public static final String CONSISTENCY_LEVEL = "gora.cassandra.store.consistencyLevel";
  /**
   * Property pointing to set fetchsize in Cassandra Query Options.
   * integer
   */
  public static final String FETCH_SIZE = "fetchSize";
  /**
   * Property pointing to set serial consistency level in Cassandra Query Options.
   * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
   */
  public static final String SERIAL_CONSISTENCY_LEVEL = "gora.cassandra.store.serialConsistencyLevel";
  /**
   * Property pointing to set reconnection policy
   * "ConstantReconnectionPolicy", "ExponentialReconnectionPolicy",
   */
  public static final String RECONNECTION_POLICY = "gora.cassandra.store.reconnectionPolicy";
  /**
   * Property pointing to set the delay in constant reconnection policy.
   * long
   */
  public static final String CONSTANT_RECONNECTION_POLICY_DELAY = "gora.cassandra.store.constantReconnectionPolicyDelay";
  /**
   * Property pointing to set the delay in exponential reconnection policy.
   * long
   */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY = "gora.cassandra.store.exponentialReconnectionPolicyBaseDelay";
  /**
   * Property pointing to set the max delay in exponential reconnection policy.
   * long
   */
  public static final String EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY = "gora.cassandra.store.exponentialReconnectionPolicyMaxDelay";
  /**
   * Property pointing to set the retry policy.
   * "DefaultRetryPolicy", "DowngradingConsistencyRetryPolicy", "FallthroughRetryPolicy",
   * "LoggingDefaultRetryPolicy", "LoggingDowngradingConsistencyRetryPolicy", "LoggingFallthroughRetryPolicy"
   */
  public static final String RETRY_POLICY = "gora.cassandra.store.retryPolicy";
  /**
   * Property pointing to set the connection time out in Cassandra Socket Options.
   * integer
   */
  public static final String CONNECTION_TIMEOUT_MILLIS = "gora.cassandra.store.connectionTimeoutMillis";
  /**
   * Property pointing to set the keep alive in Cassandra Socket Options.
   * boolean
   */
  public static final String KEEP_ALIVE = "gora.cassandra.store.keepAlive";
  /**
   * Property pointing to set the read time out in Cassandra Socket Options.
   * integer
   */
  public static final String READ_TIMEOUT_MILLIS = "gora.cassandra.store.readTimeoutMillis";
  /**
   * Property pointing to set the receiver buffer size in Cassandra Socket Options.
   * integer
   */
  public static final String RECEIVER_BUFFER_SIZE = "gora.cassandra.store.receiverBufferSize";
  /**
   * Property pointing to set the reuse address in Cassandra Socket Options.
   * boolean
   */
  public static final String REUSE_ADDRESS = "gora.cassandra.store.reuseAddress";
  /**
   * Property pointing to set the sender buffer size in Cassandra Socket Options.
   * integer
   */
  public static final String SEND_BUFFER_SIZE = "gora.cassandra.store.sendBufferSize";
  /**
   * Property pointing to set the soLinger in Cassandra Socket Options.
   * integer
   */
  public static final String SO_LINGER = "gora.cassandra.store.soLinger";
  /**
   * Property pointing to set the no tcp delay in Cassandra Socket Options.
   * boolean
   */
  public static final String TCP_NODELAY = "gora.cassandra.store.tcpNoDelay";
  /**
   * Property pointing to enable SSL.
   * boolean
   */
  public static final String ENABLE_SSL = "gora.cassandra.store.enableSSL";
  /**
   * Property pointing to set aware local data center.
   * string
   */
  public static final String DATA_CENTER = "gora.cassandra.store.dataCenter";
  /**
   * Property pointing to enable/disable remote data centers for local consistency level.
   * string
   */
  public static final String ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL = "gora.cassandra.store.allowRemoteDCsForLocalConsistencyLevel";
  /**
   * Property pointing to use Native Cassandra Native Serialization.
   * boolean
   */
  public static final String USE_CASSANDRA_NATIVE_SERIALIZATION = "gora.cassandra.store.useCassandraNativeSerialization";
}
