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
package org.apache.gora.scylladb.store;

/**
 * Configuration Properties.
 */
public class ScyllaDBStoreParameters {

    /**
     * Property pointing to scylla db contact points.
     * string (multiple values with comma separated)
     */
    public static final String SCYLLADB_SERVERS = "gora.scylladbstore.servers";
    /**
     * Property pointing to the scylladb keyspace.
     * string
     */
    public static final String KEYSPACE = "gora.scylladbstore.keyspace";
    /**
     * Property pointing to the port to use to connect to the scylladb hosts.
     * integer
     */
    public static final String PORT = "gora.scylladbstore.port";

    /**
     * Property pointing to the scylladb cluster name.
     * string
     */
    public static final String CLUSTER_NAME = "gora.scylladbstore.clusterName";
    /**
     * Property pointing to set compression to use for the transport.
     * "LZ4", "SNAPPY", "NONE"
     */
    public static final String COMPRESSION = "gora.scylladbstore.compression";
    /**
     * Property pointing to the username to connect to the server.
     * string
     */
    public static final String USERNAME = "gora.scylladbstore.username";
    /**
     * Property pointing to the password to connect to the server.
     * string
     */
    public static final String PASSWORD = "gora.scylladbstore.password";
    /**
     * Property pointing to set load balancing policy.
     * "RoundRobinPolicy", "LatencyAwareRoundRobinPolicy", "TokenAwareRoundRobinPolicy"
     */
    public static final String LOAD_BALANCING_POLICY = "gora.scylladbstore.loadBalancingPolicy";
    /**
     * Property pointing to enable/disable JMX reporting.
     * boolean
     */
    public static final String ENABLE_JMX_REPORTING = "gora.scylladbstore.enableJMXReporting";
    /**
     * Property pointing to enable/disable metrics.
     * boolean
     */
    public static final String ENABLE_METRICS = "gora.scylladbstore.enableMetrics";
    /**
     * Property pointing to set local host core connections size.
     * integer
     */
    public static final String LOCAL_CORE_CONNECTIONS_PER_HOST = "gora.scylladbstore.localCoreConnectionsPerHost";
    /**
     * Property pointing to set remote host core connections size.
     * integer
     */
    public static final String REMOTE_CORE_CONNECTIONS_PER_HOST = "gora.scylladbstore.remoteCoreConnectionsPerHost";
    /**
     * Property pointing to set local host max connections size.
     * integer
     */
    public static final String LOCAL_MAX_CONNECTIONS_PER_HOST = "gora.scylladbstore.localMaxConnectionsPerHost";
    /**
     * Property pointing to set remote host max connections size.
     * integer
     */
    public static final String REMOTE_MAX_CONNECTIONS_PER_HOST = "gora.scylladbstore.remoteMaxConnectionsPerHost";
    /**
     * Property pointing to set local host new connection threshold.
     * integer
     */
    public static final String LOCAL_NEW_CONNECTION_THRESHOLD = "gora.scylladbstore.localNewConnectionThreshold";
    /**
     * Property pointing to set remote host new connection threshold.
     * integer
     */
    public static final String REMOTE_NEW_CONNECTION_THRESHOLD = "gora.scylladbstore.remoteNewConnectionThreshold";
    /**
     * Property pointing to set local host max requests per connection.
     * integer
     */
    public static final String LOCAL_MAX_REQUESTS_PER_CONNECTION = "gora.scylladbstore.localMaxRequestsPerConnection";
    /**
     * Property pointing to set remote host max requests per connection.
     * integer
     */
    public static final String REMOTE_MAX_REQUESTS_PER_CONNECTION = "gora.scylladbstore.remoteMaxRequestsPerConnection";
    /**
     * Property pointing to set CQL Protocol version.
     * integer
     */
    public static final String PROTOCOL_VERSION = "gora.scylladbstore.protocolVersion";
    /**
     * Property pointing to set consistency level in scylladb Query Options.
     * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
     */
    public static final String CONSISTENCY_LEVEL = "gora.scylladbstore.consistencyLevel";
    /**
     * Property pointing to set fetch size in scylladb Query Options.
     * integer
     */
    public static final String FETCH_SIZE = "fetchSize";
    /**
     * Property pointing to set serial consistency level in scylladb Query Options.
     * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
     */
    public static final String SERIAL_CONSISTENCY_LEVEL = "gora.scylladbstore.serialConsistencyLevel";
    /**
     * Property pointing to set reconnection policy
     * "ConstantReconnectionPolicy", "ExponentialReconnectionPolicy",
     */
    public static final String RECONNECTION_POLICY = "gora.scylladbstore.reconnectionPolicy";
    /**
     * Property pointing to set the delay in constant reconnection policy.
     * long
     */
    public static final String CONSTANT_RECONNECTION_POLICY_DELAY = "gora.scylladbstore.constantReconnectionPolicyDelay";
    /**
     * Property pointing to set the delay in exponential reconnection policy.
     * long
     */
    public static final String EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY = "gora.scylladbstore.exponentialReconnectionPolicyBaseDelay";
    /**
     * Property pointing to set the max delay in exponential reconnection policy.
     * long
     */
    public static final String EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY = "gora.scylladbstore.exponentialReconnectionPolicyMaxDelay";
    /**
     * Property pointing to set the retry policy.
     * "DefaultRetryPolicy", "DowngradingConsistencyRetryPolicy", "FallthroughRetryPolicy",
     * "LoggingDefaultRetryPolicy", "LoggingDowngradingConsistencyRetryPolicy", "LoggingFallthroughRetryPolicy"
     */
    public static final String RETRY_POLICY = "gora.scylladbstore.retryPolicy";
    /**
     * Property pointing to set the connection time out in scylladb Socket Options.
     * integer
     */
    public static final String CONNECTION_TIMEOUT_MILLIS = "gora.scylladbstore.connectionTimeoutMillis";
    /**
     * Property pointing to set the keep alive in scylladb Socket Options.
     * boolean
     */
    public static final String KEEP_ALIVE = "gora.scylladbstore.keepAlive";
    /**
     * Property pointing to set the read time out in scylladb Socket Options.
     * integer
     */
    public static final String READ_TIMEOUT_MILLIS = "gora.scylladbstore.readTimeoutMillis";
    /**
     * Property pointing to set the receiver buffer size in scylladb Socket Options.
     * integer
     */
    public static final String RECEIVER_BUFFER_SIZE = "gora.scylladbstore.receiverBufferSize";
    /**
     * Property pointing to set the reuse address in scylladb Socket Options.
     * boolean
     */
    public static final String REUSE_ADDRESS = "gora.scylladbstore.reuseAddress";
    /**
     * Property pointing to set the sender buffer size in scylladb Socket Options.
     * integer
     */
    public static final String SEND_BUFFER_SIZE = "gora.scylladbstore.sendBufferSize";
    /**
     * Property pointing to set the soLinger in scylladb Socket Options.
     * integer
     */
    public static final String SO_LINGER = "gora.scylladbstore.soLinger";
    /**
     * Property pointing to set the no tcp delay in scylladb Socket Options.
     * boolean
     */
    public static final String TCP_NODELAY = "gora.scylladbstore.tcpNoDelay";
    /**
     * Property pointing to enable SSL.
     * boolean
     */
    public static final String ENABLE_SSL = "gora.scylladbstore.enableSSL";
    /**
     * Property pointing to set aware local data center.
     * string
     */
    public static final String DATA_CENTER = "gora.scylladbstore.dataCenter";
    /**
     * Property pointing to enable/disable remote data centers for local consistency level.
     * string
     */
    public static final String ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL = "gora.scylladbstore.allowRemoteDCsForLocalConsistencyLevel";
    /**
     * Property pointing to use Native scylladb Native Serialization.
     * avro/ native
     */
    public static final String SCYLLADB_SERIALIZATION_TYPE = "gora.scylladbstore.scylladbSerializationType";
    /**
     * Property pointing to the custom codec file.
     * string
     */
    public static final String CUSTOM_CODEC_FILE = "gora.scylladbstore.custom.codec.file";
    /**
     * Property pointing to set consistency level for read queries
     * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
     */
    public static final String READ_CONSISTENCY_LEVEL = "gora.scylladbstore.read.consistencyLevel";
    /**
     * Property pointing to set consistency level for write queries
     * "ALL", "ANY", "EACH_QUORUM", "LOCAL_ONE", "LOCAL_QUORUM", "LOCAL_SERIAL", "ONE", "QUORUM", "SERIAL", "THREE", "TWO"
     */
    public static final String WRITE_CONSISTENCY_LEVEL = "gora.scylladbstore.write.consistencyLevel";
}
