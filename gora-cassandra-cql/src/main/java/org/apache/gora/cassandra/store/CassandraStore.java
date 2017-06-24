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
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.TableMetadata;
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
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.ClusterKeyField;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;
import org.apache.gora.cassandra.bean.PartitionKeyField;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.jdom.Attribute;
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
 * Implementation of Cassandra Store.
 *
 * @param <K> key class
 * @param <T> persistent class
 */
public class CassandraStore<K, T extends Persistent> implements DataStore<K, T> {

  private static final String DEFAULT_MAPPING_FILE = "gora-cassandra-mapping.xml";

  public static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  private BeanFactory<K, T> beanFactory;

  private Cluster cluster;

  private Class keyClass;

  private Class persistentClass;

  private CassandraMapping mapping;

  private boolean isUseNativeSerialization;

  private Mapper<T> mapper;

  private Session session;

  public CassandraStore() {
    super();
  }

  /**
   * In initializing the cassandra datastore, read the mapping file, creates the basic connection to cassandra cluster,
   * according to the gora properties
   *
   * @param keyClass        key class
   * @param persistentClass persistent class
   * @param properties      properties
   */
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    LOG.debug("Initializing Cassandra store");
    try {
      this.keyClass = keyClass;
      this.persistentClass = persistentClass;
      String mappingFile = DataStoreFactory.getMappingFile(properties, this, DEFAULT_MAPPING_FILE);
      List<String> codecs = readCustomCodec(properties);
      mapping = readMapping(mappingFile);
      isUseNativeSerialization = Boolean.parseBoolean(properties.getProperty(CassandraStoreParameters.USE_CASSANDRA_NATIVE_SERIALIZATION));
      Cluster.Builder builder = Cluster.builder();
      builder = populateSettings(builder, properties);
      this.cluster = builder.build();
      if (codecs != null) {
        registerCustomCodecs(codecs);
      }
      this.session = this.cluster.connect();
      if (isUseNativeSerialization) {
        this.createSchema();
        MappingManager mappingManager = new MappingManager(session);
        mapper = mappingManager.mapper(persistentClass);
      }

    } catch (Exception e) {
      LOG.error("Error while initializing Cassandra store: {}",
              new Object[]{e.getMessage()});
      throw new RuntimeException(e);
    }
  }

  private void registerCustomCodecs(List<String> codecs) throws Exception {
    for (String codec : codecs) {
      this.cluster.getConfiguration().getCodecRegistry().register((TypeCodec<?>) Class.forName(codec).newInstance());
    }
  }

  /**
   * In this method we reads the mapping file and creates the Cassandra Mapping.
   *
   * @param filename mapping file name
   * @return @{@link CassandraMapping}
   * @throws IOException
   */
  private CassandraMapping readMapping(String filename) throws IOException {
    CassandraMapping map = new CassandraMapping();
    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader().getResourceAsStream(filename));

      List<Element> keyspaces = doc.getRootElement().getChildren("keyspace");
      List<Element> classes = doc.getRootElement().getChildren("class");
      List<Element> keys = doc.getRootElement().getChildren("cassandraKey");

      boolean classMatched = false;
      for (Element classElement : classes) {
        if (classElement.getAttributeValue("keyClass").equals(
                keyClass.getCanonicalName())
                && classElement.getAttributeValue("name").equals(
                persistentClass.getCanonicalName())) {

          classMatched = true;
          String tableName = classElement.getAttributeValue("table");
          map.setCoreName(tableName);

          List classAttributes = classElement.getAttributes();
          for (Object anAttributeList : classAttributes) {
            Attribute attribute = (Attribute) anAttributeList;
            String attributeName = attribute.getName();
            String attributeValue = attribute.getValue();
            map.addProperty(attributeName, attributeValue);
          }

          List<Element> fields = classElement.getChildren("field");

          for (Element field : fields) {
            Field cassandraField = new Field();

            List fieldAttributes = field.getAttributes();
            processAttributes(fieldAttributes, cassandraField);
            map.addCassandraField(cassandraField);
          }
          break;
        }
        LOG.warn("Check that 'keyClass' and 'name' parameters in gora-solr-mapping.xml "
                + "match with intended values. A mapping mismatch has been found therefore "
                + "no mapping has been initialized for class mapping at position "
                + " {} in mapping file.", classes.indexOf(classElement));
      }
      if (!classMatched) {
        LOG.error("Check that 'keyClass' and 'name' parameters in {} no mapping has been initialized for {} class mapping", filename, persistentClass);
      }

      String keyspaceName = map.getProperty("keyspace");
      if (keyspaceName != null) {
        KeySpace keyspace;
        for (Element keyspaceElement : keyspaces) {
          if (keyspaceName.equals(keyspaceElement.getAttributeValue("name"))) {
            keyspace = new KeySpace();
            List fieldAttributes = keyspaceElement.getAttributes();
            for (Object attributeObject : fieldAttributes) {
              Attribute attribute = (Attribute) attributeObject;
              String attributeName = attribute.getName();
              String attributeValue = attribute.getValue();
              switch (attributeName) {
                case "name":
                  keyspace.setName(attributeValue);
                  break;
                case "durableWrite":
                  keyspace.setDurableWritesEnabled(Boolean.parseBoolean(attributeValue));
                  break;
                default:
                  keyspace.addProperty(attributeName, attributeValue);
                  break;
              }
            }
            Element placementStrategy = keyspaceElement.getChild("placementStrategy");
            switch (KeySpace.PlacementStrategy.valueOf(placementStrategy.getAttributeValue("name"))) {
              case SimpleStrategy:
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.SimpleStrategy);
                keyspace.setReplicationFactor(Integer.parseInt(placementStrategy.getAttributeValue("replication_factor")));
                break;
              case NetworkTopologyStrategy:
                List<Element> dataCenters = placementStrategy.getChildren("datacenter");
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.NetworkTopologyStrategy);
                for (Element dataCenter : dataCenters) {
                  String dataCenterName = dataCenter.getAttributeValue("name");
                  Integer dataCenterReplicationFactor = Integer.valueOf(dataCenter.getAttributeValue("replication_factor"));
                  keyspace.addDataCenter(dataCenterName, dataCenterReplicationFactor);
                }
                break;
            }
            map.setKeySpace(keyspace);
            break;
          }

        }

      }

      for (Element key : keys) {
        if (keyClass.getName().equals(key.getAttributeValue("name"))) {
          CassandraKey cassandraKey = new CassandraKey(keyClass.getName());
          Element partitionKeys = key.getChild("partitionKey");
          Element clusterKeys = key.getChild("clusterKey");
          List<Element> partitionKeyFields = partitionKeys.getChildren("field");
          List<Element> partitionCompositeKeyFields = partitionKeys.getChildren("compositeKey");
          // process non composite partition keys
          for (Element partitionKeyField : partitionKeyFields) {
            PartitionKeyField fieldKey = new PartitionKeyField();
            List fieldAttributes = partitionKeyField.getAttributes();
            processAttributes(fieldAttributes, fieldKey);
            cassandraKey.addPartitionKeyField(fieldKey);
          }
          // process composite partitions keys
          for (Element partitionCompositeKeyField : partitionCompositeKeyFields) {
            PartitionKeyField compositeFieldKey = new PartitionKeyField();
            compositeFieldKey.setComposite(true);
            List<Element> compositeKeyFields = partitionCompositeKeyField.getChildren("field");
            for (Element partitionKeyField : compositeKeyFields) {
              PartitionKeyField fieldKey = new PartitionKeyField();
              List fieldAttributes = partitionKeyField.getAttributes();
              processAttributes(fieldAttributes, fieldKey);
              compositeFieldKey.addField(fieldKey);
            }
            cassandraKey.addPartitionKeyField(compositeFieldKey);
          }

          //process cluster keys
          List<Element> clusterKeyFields = clusterKeys.getChildren("field");
          for (Element clusterKeyField : clusterKeyFields) {
            ClusterKeyField keyField = new ClusterKeyField();
            List fieldAttributes = clusterKeyField.getAttributes();
            for (Object anAttributeList : fieldAttributes) {
              Attribute attribute = (Attribute) anAttributeList;
              String attributeName = attribute.getName();
              String attributeValue = attribute.getValue();
              switch (attributeName) {
                case "name":
                  keyField.setFieldName(attributeValue);
                  break;
                case "column":
                  keyField.setColumnName(attributeValue);
                  break;
                case "type":
                  keyField.setType(attributeValue);
                  break;
                case "order":
                  keyField.setOrder(ClusterKeyField.Order.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                  break;
                default:
                  keyField.addProperty(attributeName, attributeValue);
                  break;
              }
            }
            cassandraKey.addClusterKeyField(keyField);
          }
          map.setCassandraKey(cassandraKey);
        }
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return map;
  }

  private void processAttributes(List<Element> attributes, Field fieldKey) {
    for (Object anAttributeList : attributes) {
      Attribute attribute = (Attribute) anAttributeList;
      String attributeName = attribute.getName();
      String attributeValue = attribute.getValue();
      switch (attributeName) {
        case "name":
          fieldKey.setFieldName(attributeValue);
          break;
        case "column":
          fieldKey.setColumnName(attributeValue);
          break;
        case "type":
          fieldKey.setType(attributeValue);
          break;
        default:
          fieldKey.addProperty(attributeName, attributeValue);
          break;
      }
    }
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
    if (enableJMXProp != null) {
      if (!Boolean.parseBoolean(enableJMXProp)) {
        builder = builder.withoutJMXReporting();
      }
    }
    String enableMetricsProp = properties.getProperty(CassandraStoreParameters.ENABLE_METRICS);
    if (enableMetricsProp != null) {
      if (!Boolean.parseBoolean(enableMetricsProp)) {
        builder = builder.withoutMetrics();
      }
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

  /**
   * {@inheritDoc}
   * <p>
   * This is a setter method to set the class of persistent objects.
   *
   * @param persistentClass class of persistent objects
   *                        {@link org.apache.gora.cassandra.serializers.CassandraNativePersistent}
   *                        {@link  org.apache.gora.persistency.Persistent}
   */
  @Override
  public void setPersistentClass(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  @Override
  public Class<T> getPersistentClass() {
    return this.persistentClass;
  }

  @Override
  public String getSchemaName() {
    return mapping.getCoreName();
  }

  @Override
  public void createSchema() {
    LOG.debug("creating Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.session.execute(CassandraQueryFactory.getCreateKeySpaceQuery(mapping));
    LOG.debug("creating Cassandra column family / table {}", mapping.getCoreName());
    this.session.execute(CassandraQueryFactory.getCreateTableQuery(mapping));
  }

  @Override
  public void deleteSchema() {
    LOG.debug("dropping Cassandra table {}", mapping.getCoreName());
    this.session.execute(CassandraQueryFactory.getDropTableQuery(mapping));
    LOG.debug("dropping Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.session.execute(CassandraQueryFactory.getDropKeySpaceQuery(mapping));
  }

  @Override
  public Class<K> getKeyClass() {
    return this.keyClass;
  }

  @Override
  public void setKeyClass(Class<K> keyClass) {
    this.keyClass = keyClass;
  }

  @Override
  public K newKey() {
    try {
      return beanFactory.newKey();
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return null;
    }
  }

  @Override
  public T newPersistent() {
    return this.beanFactory.newPersistent();
  }

  @Override
  public void setBeanFactory(BeanFactory<K, T> beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return this.beanFactory;
  }

  @Override
  public void close() {
    this.session.close();
    this.cluster.close();
  }

  @Override
  public T get(K key) {
    if (isUseNativeSerialization) {
      return mapper.get(key);
    } else {
      return null;
    }
  }

  @Override
  public T get(K key, String[] fields) {
    return null;
  }

  @Override
  public void put(K key, T obj) {
    if (isUseNativeSerialization) {
      mapper.save(obj);
    } else {

    }
  }

  @Override
  public boolean delete(K key) {
    if (isUseNativeSerialization) {
      mapper.delete(key);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    return null;
  }

  @Override
  public Query<K, T> newQuery() {
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    return null;
  }

  @Override
  public void flush() {

  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public void truncateSchema() {
    LOG.debug("truncating Cassandra table {}", mapping.getCoreName());
    this.session.execute(CassandraQueryFactory.getTruncateTableQuery(mapping));
  }

  @Override
  public boolean schemaExists() {
    KeyspaceMetadata keyspace = cluster.getMetadata().getKeyspace(mapping.getKeySpace().getName());
    if (keyspace != null) {
      TableMetadata table = keyspace.getTable(mapping.getCoreName());
      return table != null;
    } else {
      return false;
    }
  }

}