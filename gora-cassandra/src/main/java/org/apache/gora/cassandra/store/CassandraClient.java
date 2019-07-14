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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.datastax.driver.extras.codecs.arrays.DoubleArrayCodec;
import com.datastax.driver.extras.codecs.arrays.FloatArrayCodec;
import com.datastax.driver.extras.codecs.arrays.IntArrayCodec;
import com.datastax.driver.extras.codecs.arrays.LongArrayCodec;
import com.datastax.driver.extras.codecs.arrays.ObjectArrayCodec;
import com.datastax.driver.extras.codecs.date.SimpleDateCodec;
import com.datastax.driver.extras.codecs.date.SimpleTimestampCodec;
import com.datastax.driver.extras.codecs.jdk8.OptionalCodec;
import org.apache.gora.cassandra.bean.Field;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * This class provides the Cassandra Client Connection.
 * Initialize the Cassandra Connection according to the Properties.
 */
public class CassandraClient {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);

  private Cluster cluster;

  private Session session;

  private CassandraMapping mapping;

  private String readConsistencyLevel;

  private String writeConsistencyLevel;

  public Session getSession() {
    return session;
  }

  public Cluster getCluster() {
    return cluster;
  }

  void initialize(Properties properties, CassandraMapping mapping) throws Exception {
    Cluster.Builder builder = Cluster.builder();
    List<String> codecs = readCustomCodec(properties);
    builder = populateSettings(builder, properties);
    this.mapping = mapping;
    this.cluster = builder.build();
    if (codecs != null) {
      registerCustomCodecs(codecs);
    }
    readConsistencyLevel = properties.getProperty(CassandraStoreParameters.READ_CONSISTENCY_LEVEL);
    writeConsistencyLevel = properties.getProperty(CassandraStoreParameters.WRITE_CONSISTENCY_LEVEL);
    registerOptionalCodecs();
    this.session = this.cluster.connect();
  }

  private void registerOptionalCodecs() {
    // Optional Codecs for natives
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.ascii()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.bigint()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.blob()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.cboolean()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.cdouble()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.cfloat()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.cint()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.counter()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.date()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.decimal()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.inet()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.smallInt()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.time()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.timestamp()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.timeUUID()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.tinyInt()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.varint()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.varchar()));
    this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.uuid()));
    // Optional Array Codecs
    this.cluster.getConfiguration().getCodecRegistry().register(new IntArrayCodec());
    this.cluster.getConfiguration().getCodecRegistry().register(new DoubleArrayCodec());
    this.cluster.getConfiguration().getCodecRegistry().register(new FloatArrayCodec());
    this.cluster.getConfiguration().getCodecRegistry().register(new LongArrayCodec());
    this.cluster.getConfiguration().getCodecRegistry().register(new ObjectArrayCodec<>(
            DataType.list(DataType.varchar()),
            String[].class,
            TypeCodec.varchar()));
    // Optional Time Codecs
    this.cluster.getConfiguration().getCodecRegistry().register(new SimpleDateCodec());
    this.cluster.getConfiguration().getCodecRegistry().register(new SimpleTimestampCodec());

    for (Field field : this.mapping.getFieldList()) {
      String columnType = field.getType().toLowerCase(Locale.ENGLISH);
      //http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cql_data_types_c.html
      if (columnType.contains("list")) {
        columnType = columnType.substring(columnType.indexOf("<") + 1, columnType.indexOf(">"));
        this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.list(getTypeCodec(columnType))));
      } else if (columnType.contains("set")) {
        columnType = columnType.substring(columnType.indexOf("<") + 1, columnType.indexOf(">"));
        this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.set(getTypeCodec(columnType))));
      } else if (columnType.contains("map")) {
        String[] columnTypes = columnType.substring(columnType.indexOf("<") + 1, columnType.indexOf(">")).split(",");
        this.cluster.getConfiguration().getCodecRegistry().register(new OptionalCodec<>(TypeCodec.map(TypeCodec.set(getTypeCodec(columnTypes[0])), TypeCodec.set(getTypeCodec(columnTypes[1])))));
      }
    }
  }

  private TypeCodec getTypeCodec(String columnType) {
    TypeCodec typeCodec;
    switch (columnType) {
      case "ascii":
        typeCodec = TypeCodec.ascii();
        break;
      case "bigint":
        typeCodec = TypeCodec.bigint();
        break;
      case "blob":
        typeCodec = TypeCodec.blob();
        break;
      case "boolean":
        typeCodec = TypeCodec.cboolean();
        break;
      case "counter":
        typeCodec = TypeCodec.counter();
        break;
      case "date":
        typeCodec = TypeCodec.date();
        break;
      case "decimal":
        typeCodec = TypeCodec.decimal();
        break;
      case "double":
        typeCodec = TypeCodec.cdouble();
        break;
      case "float":
        typeCodec = TypeCodec.cfloat();
        break;
      case "inet":
        typeCodec = TypeCodec.inet();
        break;
      case "int":
        typeCodec = TypeCodec.cint();
        break;
      case "smallint":
        typeCodec = TypeCodec.smallInt();
        break;
      case "time":
        typeCodec = TypeCodec.time();
        break;
      case "timestamp":
        typeCodec = TypeCodec.timestamp();
        break;
      case "timeuuid":
        typeCodec = TypeCodec.timeUUID();
        break;
      case "tinyint":
        typeCodec = TypeCodec.tinyInt();
        break;
      case "uuid":
        typeCodec = TypeCodec.uuid();
        break;
      case "varint":
        typeCodec = TypeCodec.varint();
        break;
      case "varchar":
      case "text":
        typeCodec = TypeCodec.varchar();
        break;
      default:
        LOG.error("Unsupported Cassandra datatype: {} ", columnType);
        throw new RuntimeException("Unsupported Cassandra datatype: " + columnType);
    }
    return typeCodec;
  }

  private Cluster.Builder populateSettings(Cluster.Builder builder, Properties properties) {
    String serversParam = properties.getProperty(CassandraStoreParameters.CASSANDRA_SERVERS);
    String[] servers = serversParam.split(",");
    for (String server : servers) {
      builder = builder.addContactPoint(server);
    }
    String portProp = properties.getProperty(CassandraStoreParameters.PORT);
    if (portProp != null) {
      builder = builder.withPort(Integer.parseInt(portProp));
    }
    String clusterNameProp = properties.getProperty(CassandraStoreParameters.CLUSTER_NAME);
    if (clusterNameProp != null) {
      builder = builder.withClusterName(clusterNameProp);
    }
    String compressionProp = properties.getProperty(CassandraStoreParameters.COMPRESSION);
    if (compressionProp != null) {
      builder = builder.withCompression(ProtocolOptions.Compression.valueOf(compressionProp));
    }
    builder = this.populateCredentials(properties, builder);
    builder = this.populateLoadBalancingProp(properties, builder);
    String enableJMXProp = properties.getProperty(CassandraStoreParameters.ENABLE_JMX_REPORTING);
    if (!Boolean.parseBoolean(enableJMXProp)) {
      builder = builder.withoutJMXReporting();
    }
    String enableMetricsProp = properties.getProperty(CassandraStoreParameters.ENABLE_METRICS);
    if (!Boolean.parseBoolean(enableMetricsProp)) {
      builder = builder.withoutMetrics();
    }
    builder = this.populatePoolingSettings(properties, builder);
    String versionProp = properties.getProperty(CassandraStoreParameters.PROTOCOL_VERSION);
    if (versionProp != null) {
      builder = builder.withProtocolVersion(ProtocolVersion.fromInt(Integer.parseInt(versionProp)));
    }
    builder = this.populateQueryOptions(properties, builder);
    builder = this.populateReconnectPolicy(properties, builder);
    builder = this.populateRetrytPolicy(properties, builder);
    builder = this.populateSocketOptions(properties, builder);
    String enableSSLProp = properties.getProperty(CassandraStoreParameters.ENABLE_SSL);
    if (enableSSLProp != null) {
      if (Boolean.parseBoolean(enableSSLProp)) {
        builder = builder.withSSL();
      }
    }
    return builder;
  }

  private Cluster.Builder populateLoadBalancingProp(Properties properties, Cluster.Builder builder) {
    String loadBalancingProp = properties.getProperty(CassandraStoreParameters.LOAD_BALANCING_POLICY);
    if (loadBalancingProp != null) {
      switch (loadBalancingProp) {
        case "LatencyAwareRoundRobinPolicy":
          builder = builder.withLoadBalancingPolicy(LatencyAwarePolicy.builder(new RoundRobinPolicy()).build());
          break;
        case "RoundRobinPolicy":
          builder = builder.withLoadBalancingPolicy(new RoundRobinPolicy());
          break;
        case "DCAwareRoundRobinPolicy": {
          String dataCenter = properties.getProperty(CassandraStoreParameters.DATA_CENTER);
          boolean allowRemoteDCsForLocalConsistencyLevel = Boolean.parseBoolean(
                  properties.getProperty(CassandraStoreParameters.ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL));
          if (dataCenter != null && !dataCenter.isEmpty()) {
            if (allowRemoteDCsForLocalConsistencyLevel) {
              builder = builder.withLoadBalancingPolicy(
                      DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter)
                              .allowRemoteDCsForLocalConsistencyLevel().build());
            } else {
              builder = builder.withLoadBalancingPolicy(
                      DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter).build());
            }
          } else {
            if (allowRemoteDCsForLocalConsistencyLevel) {
              builder = builder.withLoadBalancingPolicy(
                      (DCAwareRoundRobinPolicy.builder().allowRemoteDCsForLocalConsistencyLevel().build()));
            } else {
              builder = builder.withLoadBalancingPolicy((DCAwareRoundRobinPolicy.builder().build()));
            }
          }
          break;
        }
        case "TokenAwareRoundRobinPolicy":
          builder = builder.withLoadBalancingPolicy(new TokenAwarePolicy(new RoundRobinPolicy()));
          break;
        case "TokenAwareDCAwareRoundRobinPolicy": {
          String dataCenter = properties.getProperty(CassandraStoreParameters.DATA_CENTER);
          boolean allowRemoteDCsForLocalConsistencyLevel = Boolean.parseBoolean(
                  properties.getProperty(CassandraStoreParameters.ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL));
          if (dataCenter != null && !dataCenter.isEmpty()) {
            if (allowRemoteDCsForLocalConsistencyLevel) {
              builder = builder.withLoadBalancingPolicy(new TokenAwarePolicy(
                      DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter)
                              .allowRemoteDCsForLocalConsistencyLevel().build()));
            } else {
              builder = builder.withLoadBalancingPolicy(new TokenAwarePolicy(
                      DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter).build()));
            }
          } else {
            if (allowRemoteDCsForLocalConsistencyLevel) {
              builder = builder.withLoadBalancingPolicy(new TokenAwarePolicy(
                      DCAwareRoundRobinPolicy.builder().allowRemoteDCsForLocalConsistencyLevel().build()));
            } else {
              builder = builder.withLoadBalancingPolicy(
                      new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build()));
            }
          }
          break;
        }
        default:
          LOG.error("Unsupported Cassandra load balancing  policy: {} ", loadBalancingProp);
          break;
      }
    }
    return builder;
  }

  private Cluster.Builder populateCredentials(Properties properties, Cluster.Builder builder) {
    String usernameProp = properties.getProperty(CassandraStoreParameters.USERNAME);
    String passwordProp = properties.getProperty(CassandraStoreParameters.PASSWORD);
    if (usernameProp != null) {
      builder = builder.withCredentials(usernameProp, passwordProp);
    }
    return builder;
  }

  private Cluster.Builder populatePoolingSettings(Properties properties, Cluster.Builder builder) {
    String localCoreConnectionsPerHost = properties.getProperty(CassandraStoreParameters.LOCAL_CORE_CONNECTIONS_PER_HOST);
    String remoteCoreConnectionsPerHost = properties.getProperty(CassandraStoreParameters.REMOTE_CORE_CONNECTIONS_PER_HOST);
    String localMaxConnectionsPerHost = properties.getProperty(CassandraStoreParameters.LOCAL_MAX_CONNECTIONS_PER_HOST);
    String remoteMaxConnectionsPerHost = properties.getProperty(CassandraStoreParameters.REMOTE_MAX_CONNECTIONS_PER_HOST);
    String localNewConnectionThreshold = properties.getProperty(CassandraStoreParameters.LOCAL_NEW_CONNECTION_THRESHOLD);
    String remoteNewConnectionThreshold = properties.getProperty(CassandraStoreParameters.REMOTE_NEW_CONNECTION_THRESHOLD);
    String localMaxRequestsPerConnection = properties.getProperty(CassandraStoreParameters.LOCAL_MAX_REQUESTS_PER_CONNECTION);
    String remoteMaxRequestsPerConnection = properties.getProperty(CassandraStoreParameters.REMOTE_MAX_REQUESTS_PER_CONNECTION);
    PoolingOptions options = new PoolingOptions();
    if (localCoreConnectionsPerHost != null) {
      options.setCoreConnectionsPerHost(HostDistance.LOCAL, Integer.parseInt(localCoreConnectionsPerHost));
    }
    if (remoteCoreConnectionsPerHost != null) {
      options.setCoreConnectionsPerHost(HostDistance.REMOTE, Integer.parseInt(remoteCoreConnectionsPerHost));
    }
    if (localMaxConnectionsPerHost != null) {
      options.setMaxConnectionsPerHost(HostDistance.LOCAL, Integer.parseInt(localMaxConnectionsPerHost));
    }
    if (remoteMaxConnectionsPerHost != null) {
      options.setMaxConnectionsPerHost(HostDistance.REMOTE, Integer.parseInt(remoteMaxConnectionsPerHost));
    }
    if (localNewConnectionThreshold != null) {
      options.setNewConnectionThreshold(HostDistance.LOCAL, Integer.parseInt(localNewConnectionThreshold));
    }
    if (remoteNewConnectionThreshold != null) {
      options.setNewConnectionThreshold(HostDistance.REMOTE, Integer.parseInt(remoteNewConnectionThreshold));
    }
    if (localMaxRequestsPerConnection != null) {
      options.setMaxRequestsPerConnection(HostDistance.LOCAL, Integer.parseInt(localMaxRequestsPerConnection));
    }
    if (remoteMaxRequestsPerConnection != null) {
      options.setMaxRequestsPerConnection(HostDistance.REMOTE, Integer.parseInt(remoteMaxRequestsPerConnection));
    }
    builder = builder.withPoolingOptions(options);
    return builder;
  }

  private Cluster.Builder populateQueryOptions(Properties properties, Cluster.Builder builder) {
    String consistencyLevelProp = properties.getProperty(CassandraStoreParameters.CONSISTENCY_LEVEL);
    String serialConsistencyLevelProp = properties.getProperty(CassandraStoreParameters.SERIAL_CONSISTENCY_LEVEL);
    String fetchSize = properties.getProperty(CassandraStoreParameters.FETCH_SIZE);
    QueryOptions options = new QueryOptions();
    if (consistencyLevelProp != null) {
      options.setConsistencyLevel(ConsistencyLevel.valueOf(consistencyLevelProp));
    }
    if (serialConsistencyLevelProp != null) {
      options.setSerialConsistencyLevel(ConsistencyLevel.valueOf(serialConsistencyLevelProp));
    }
    if (fetchSize != null) {
      options.setFetchSize(Integer.parseInt(fetchSize));
    }
    return builder.withQueryOptions(options);
  }

  private Cluster.Builder populateReconnectPolicy(Properties properties, Cluster.Builder builder) {
    String reconnectionPolicy = properties.getProperty(CassandraStoreParameters.RECONNECTION_POLICY);
    if (reconnectionPolicy != null) {
      switch (reconnectionPolicy) {
        case "ConstantReconnectionPolicy": {
          String constantReconnectionPolicyDelay = properties.getProperty(CassandraStoreParameters.CONSTANT_RECONNECTION_POLICY_DELAY);
          ConstantReconnectionPolicy policy = new ConstantReconnectionPolicy(Long.parseLong(constantReconnectionPolicyDelay));
          builder = builder.withReconnectionPolicy(policy);
          break;
        }
        case "ExponentialReconnectionPolicy": {
          String exponentialReconnectionPolicyBaseDelay = properties.getProperty(CassandraStoreParameters.EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY);
          String exponentialReconnectionPolicyMaxDelay = properties.getProperty(CassandraStoreParameters.EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY);

          ExponentialReconnectionPolicy policy = new ExponentialReconnectionPolicy(Long.parseLong(exponentialReconnectionPolicyBaseDelay),
                  Long.parseLong(exponentialReconnectionPolicyMaxDelay));
          builder = builder.withReconnectionPolicy(policy);
          break;
        }
        default:
          LOG.error("Unsupported reconnection policy : {} ", reconnectionPolicy);
      }
    }
    return builder;
  }

  private Cluster.Builder populateRetrytPolicy(Properties properties, Cluster.Builder builder) {
    String retryPolicy = properties.getProperty(CassandraStoreParameters.RETRY_POLICY);
    if (retryPolicy != null) {
      switch (retryPolicy) {
        case "DefaultRetryPolicy":
          builder = builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
          break;
        case "DowngradingConsistencyRetryPolicy":
          builder = builder.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE);
          break;
        case "FallthroughRetryPolicy":
          builder = builder.withRetryPolicy(FallthroughRetryPolicy.INSTANCE);
          break;
        case "LoggingDefaultRetryPolicy":
          builder = builder.withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE));
          break;
        case "LoggingDowngradingConsistencyRetryPolicy":
          builder = builder.withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE));
          break;
        case "LoggingFallthroughRetryPolicy":
          builder = builder.withRetryPolicy(new LoggingRetryPolicy(FallthroughRetryPolicy.INSTANCE));
          break;
        default:
          LOG.error("Unsupported retry policy : {} ", retryPolicy);
          break;
      }
    }
    return builder;
  }

  private Cluster.Builder populateSocketOptions(Properties properties, Cluster.Builder builder) {
    String connectionTimeoutMillisProp = properties.getProperty(CassandraStoreParameters.CONNECTION_TIMEOUT_MILLIS);
    String keepAliveProp = properties.getProperty(CassandraStoreParameters.KEEP_ALIVE);
    String readTimeoutMillisProp = properties.getProperty(CassandraStoreParameters.READ_TIMEOUT_MILLIS);
    String receiveBufferSizeProp = properties.getProperty(CassandraStoreParameters.RECEIVER_BUFFER_SIZE);
    String reuseAddress = properties.getProperty(CassandraStoreParameters.REUSE_ADDRESS);
    String sendBufferSize = properties.getProperty(CassandraStoreParameters.SEND_BUFFER_SIZE);
    String soLinger = properties.getProperty(CassandraStoreParameters.SO_LINGER);
    String tcpNoDelay = properties.getProperty(CassandraStoreParameters.TCP_NODELAY);
    SocketOptions options = new SocketOptions();
    if (connectionTimeoutMillisProp != null) {
      options.setConnectTimeoutMillis(Integer.parseInt(connectionTimeoutMillisProp));
    }
    if (keepAliveProp != null) {
      options.setKeepAlive(Boolean.parseBoolean(keepAliveProp));
    }
    if (readTimeoutMillisProp != null) {
      options.setReadTimeoutMillis(Integer.parseInt(readTimeoutMillisProp));
    }
    if (receiveBufferSizeProp != null) {
      options.setReceiveBufferSize(Integer.parseInt(receiveBufferSizeProp));
    }
    if (reuseAddress != null) {
      options.setReuseAddress(Boolean.parseBoolean(reuseAddress));
    }
    if (sendBufferSize != null) {
      options.setSendBufferSize(Integer.parseInt(sendBufferSize));
    }
    if (soLinger != null) {
      options.setSoLinger(Integer.parseInt(soLinger));
    }
    if (tcpNoDelay != null) {
      options.setTcpNoDelay(Boolean.parseBoolean(tcpNoDelay));
    }
    return builder.withSocketOptions(options);
  }

  private List<String> readCustomCodec(Properties properties) throws JDOMException, IOException {
    String filename = properties.getProperty(CassandraStoreParameters.CUSTOM_CODEC_FILE);
    if (filename != null) {
      List<String> codecs = new ArrayList<>();
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader().getResourceAsStream(filename));
      List<Element> codecElementList = doc.getRootElement().getChildren("codec");
      for (Element codec : codecElementList) {
        codecs.add(codec.getValue());
      }
      return codecs;
    }
    return null;
  }

  /**
   * This method returns configured read consistency level.
   * @return read Consistency level
   */
  public String getReadConsistencyLevel() {
    return readConsistencyLevel;
  }

  /**
   * This method returns configured write consistency level.
   * @return write Consistency level
   */
  public String getWriteConsistencyLevel() {
    return writeConsistencyLevel;
  }


  public void close() {
    this.session.close();
    this.cluster.close();
  }

  private void registerCustomCodecs(List<String> codecs) throws Exception {
    for (String codec : codecs) {
      this.cluster.getConfiguration().getCodecRegistry().register((TypeCodec<?>) Class.forName(codec).getDeclaredConstructor().newInstance());
    }
  }

}
