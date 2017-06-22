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

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.*;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.hadoop.conf.Configuration;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class CassandraStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final String DEFAULT_MAPPING_FILE = "gora-cassandra-mapping.xml";

  public static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  private Cluster cluster;

  private CassandraMapping mapping;

  private boolean isUseCassandraMappingManager;

  private Mapper<T> mapper;

  private Session session;

  public CassandraStore() {
    super();
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    LOG.debug("Initializing Cassandra store");
    super.initialize(keyClass, persistentClass, properties);
    try {
      String mappingFile = DataStoreFactory.getMappingFile(properties, this,
              DEFAULT_MAPPING_FILE);
      mapping = readMapping(mappingFile);
      isUseCassandraMappingManager = Boolean.parseBoolean(properties.getProperty(Constants.USE_CASSANDRA_MAPPING_MANAGER));
      Cluster.Builder builder = Cluster.builder();
      populateSettings(builder, properties);
      this.cluster = builder.build();
      this.session = this.cluster.connect();
      if (isUseCassandraMappingManager) {
        MappingManager mappingManager = new MappingManager(session);
        mapper = mappingManager.mapper(persistentClass);
      }

    } catch (Exception e) {
      LOG.error("Error while initializing Cassandra store: {}",
              new Object[]{e.getMessage()});
      throw new RuntimeException(e);
    }
  }

  private CassandraMapping readMapping(String filename) throws IOException {
    CassandraMapping map = new CassandraMapping();
    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader()
              .getResourceAsStream(filename));

      List<Element> keyspaces = doc.getRootElement().getChildren("keyspace");

      List<Element> classes = doc.getRootElement().getChildren("class");

      boolean classMatched = false;
      for (Element classElement : classes) {
        if (classElement.getAttributeValue("keyClass").equals(
                keyClass.getCanonicalName())
                && classElement.getAttributeValue("name").equals(
                persistentClass.getCanonicalName())) {

          classMatched = true;
          String tableName = getSchemaName(
                  classElement.getAttributeValue("table"), persistentClass);
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
            for (Object anAttributeList : fieldAttributes) {
              Attribute attribute = (Attribute) anAttributeList;
              String attributeName = attribute.getName();
              String attributeValue = attribute.getValue();
              switch (attributeName) {
                case "name":
                  cassandraField.setFieldName(attributeValue);
                  break;
                case "column":
                  cassandraField.setColumnName(attributeValue);
                  break;
                default:
                  cassandraField.addProperty(attributeName, attributeValue);
                  break;
              }
            }
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
        KeySpace keyspace = null;
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
            Element placementStrategy =  keyspaceElement.getChild("placementStrategy");
            switch (KeySpace.PlacementStrategy.valueOf(placementStrategy.getAttributeValue("name"))) {
              case SimpleStrategy:
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.SimpleStrategy);
                keyspace.setReplicationFactor(Integer.parseInt(placementStrategy.getAttributeValue("replication_factor")));
                break;
              case NetworkTopologyStrategy:
                List<Element> dataCenters =  placementStrategy.getChildren("datacenter");
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.NetworkTopologyStrategy);
                for(Element dataCenter : dataCenters) {
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

    } catch (Exception ex) {
      throw new IOException(ex);
    }

    return map;
  }

  private void populateSettings(Cluster.Builder builder, Properties properties) {
    String serversParam = properties.getProperty(Constants.CASSANDRA_SERVERS);
    String[] servers = serversParam.split(",");
    for (String server : servers) {
      builder = builder.addContactPoint(server);
    }
    String portProp = properties.getProperty(Constants.PORT);
    if (portProp != null) {
      builder = builder.withPort(Integer.parseInt(portProp));
    }
    String clusterNameProp = properties.getProperty(Constants.CLUSTER_NAME);
    if (clusterNameProp != null) {
      builder = builder.withClusterName(clusterNameProp);
    }
    String compressionProp = properties.getProperty(Constants.COMPRESSION);
    if (compressionProp != null) {
      builder = builder.withCompression(ProtocolOptions.Compression.valueOf(compressionProp));
    }
    builder = this.populateCredentials(properties, builder);
    builder = this.populateLoadBalancingProp(properties, builder);
    String enableJMXProp = properties.getProperty(Constants.ENABLE_JMX_REPORTING);
    if (enableJMXProp != null) {
      if (!Boolean.parseBoolean(enableJMXProp)) {
        builder = builder.withoutJMXReporting();
      }
    }
    String enableMetricsProp = properties.getProperty(Constants.ENABLE_METRICS);
    if (enableMetricsProp != null) {
      if (!Boolean.parseBoolean(enableMetricsProp)) {
        builder = builder.withoutMetrics();
      }
    }
    builder = this.populatePoolingSettings(properties, builder);
    String versionProp = properties.getProperty(Constants.PROTOCOL_VERSION);
    if (versionProp != null) {
      builder = builder.withProtocolVersion(ProtocolVersion.fromInt(Integer.parseInt(versionProp)));
    }
    builder = this.populateQueryOptions(properties, builder);
    builder = this.populateReconnectPolicy(properties, builder);
    builder = this.populateRetrytPolicy(properties, builder);
    builder = this.populateSocketOptions(properties, builder);
    String enableSSLProp = properties.getProperty(Constants.ENABLE_SSL);
    if (enableSSLProp != null) {
      if (Boolean.parseBoolean(enableSSLProp)) {
        builder = builder.withSSL();
      }
    }
  }


  private Cluster.Builder populateLoadBalancingProp(Properties properties, Cluster.Builder builder) {
    String loadBalancingProp = properties.getProperty(Constants.LOAD_BALANCING_POLICY);
    if (loadBalancingProp != null) {
      switch (loadBalancingProp) {
        case "LatencyAwareRoundRobinPolicy":
          builder = builder.withLoadBalancingPolicy(LatencyAwarePolicy.builder(new RoundRobinPolicy()).build());
          break;
        case "RoundRobinPolicy":
          builder = builder.withLoadBalancingPolicy(new RoundRobinPolicy());
          break;
        case "DCAwareRoundRobinPolicy": {
          String dataCenter = properties.getProperty(Constants.DATA_CENTER);
          boolean allowRemoteDCsForLocalConsistencyLevel = Boolean.parseBoolean(
                  properties.getProperty(Constants.ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL));
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
          String dataCenter = properties.getProperty(Constants.DATA_CENTER);
          boolean allowRemoteDCsForLocalConsistencyLevel = Boolean.parseBoolean(
                  properties.getProperty(Constants.ALLOW_REMOTE_DCS_FOR_LOCAL_CONSISTENCY_LEVEL));
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
          LOG.error("Unsupported Cassandra load balancing " + "policy: " + loadBalancingProp);
          break;
      }
    }
    return builder;
  }

  private Cluster.Builder populateCredentials(Properties properties, Cluster.Builder builder) {
    String usernameProp = properties.getProperty(Constants.USERNAME);
    String passwordProp = properties.getProperty(Constants.PASSWORD);
    if (usernameProp != null) {
      builder = builder.withCredentials(usernameProp, passwordProp);
    }
    return builder;
  }

  private Cluster.Builder populatePoolingSettings(Properties properties, Cluster.Builder builder) {
    String localCoreConnectionsPerHost = properties.getProperty(Constants.LOCAL_CORE_CONNECTIONS_PER_HOST);
    String remoteCoreConnectionsPerHost = properties.getProperty(Constants.REMOTE_CORE_CONNECTIONS_PER_HOST);
    String localMaxConnectionsPerHost = properties.getProperty(Constants.LOCAL_MAX_CONNECTIONS_PER_HOST);
    String remoteMaxConnectionsPerHost = properties.getProperty(Constants.REMOTE_MAX_CONNECTIONS_PER_HOST);
    String localNewConnectionThreshold = properties.getProperty(Constants.LOCAL_NEW_CONNECTION_THRESHOLD);
    String remoteNewConnectionThreshold = properties.getProperty(Constants.REMOTE_NEW_CONNECTION_THRESHOLD);
    String localMaxRequestsPerConnection = properties.getProperty(Constants.LOCAL_MAX_REQUESTS_PER_CONNECTION);
    String remoteMaxRequestsPerConnection = properties.getProperty(Constants.REMOTE_MAX_REQUESTS_PER_CONNECTION);
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
    String consistencyLevelProp = properties.getProperty(Constants.CONSISTENCY_LEVEL);
    String serialConsistencyLevelProp = properties.getProperty(Constants.SERIAL_CONSISTENCY_LEVEL);
    String fetchSize = properties.getProperty(Constants.FETCH_SIZE);
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
    String reconnectionPolicy = properties.getProperty(Constants.RECONNECTION_POLICY);
    if (reconnectionPolicy != null) {
      switch (reconnectionPolicy) {
        case "ConstantReconnectionPolicy": {
          String constantReconnectionPolicyDelay = properties.getProperty(Constants.CONSTANT_RECONNECTION_POLICY_DELAY);

          ConstantReconnectionPolicy policy = new ConstantReconnectionPolicy(Long.parseLong(constantReconnectionPolicyDelay));
          builder = builder.withReconnectionPolicy(policy);
          break;
        }
        case "ExponentialReconnectionPolicy": {
          String exponentialReconnectionPolicyBaseDelay = properties.getProperty(Constants.EXPONENTIAL_RECONNECTION_POLICY_BASE_DELAY);
          String exponentialReconnectionPolicyMaxDelay = properties.getProperty(Constants.EXPONENTIAL_RECONNECTION_POLICY_MAX_DELAY);

          ExponentialReconnectionPolicy policy = new ExponentialReconnectionPolicy(Long.parseLong(exponentialReconnectionPolicyBaseDelay),
                  Long.parseLong(exponentialReconnectionPolicyMaxDelay));
          builder = builder.withReconnectionPolicy(policy);
          break;
        }
        default:
          LOG.error("Unsupported reconnection policy " + reconnectionPolicy);
      }
    }
    return builder;
  }

  private Cluster.Builder populateRetrytPolicy(Properties properties, Cluster.Builder builder) {
    String retryPolicy = properties.getProperty(Constants.RETRY_POLICY);
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
          LOG.error("Unsupported retry policy " + retryPolicy);
          break;
      }
    }
    return builder;
  }

  private Cluster.Builder populateSocketOptions(Properties properties, Cluster.Builder builder) {
    String connectionTimeoutMillisProp = properties.getProperty(Constants.CONNECTION_TIMEOUT_MILLIS);
    String keepAliveProp = properties.getProperty(Constants.KEEP_ALIVE);
    String readTimeoutMillisProp = properties.getProperty(Constants.READ_TIMEOUT_MILLIS);
    String receiveBufferSizeProp = properties.getProperty(Constants.RECEIVER_BUFFER_SIZE);
    String reuseAddress = properties.getProperty(Constants.REUSE_ADDRESS);
    String sendBufferSize = properties.getProperty(Constants.SEND_BUFFER_SIZE);
    String soLinger = properties.getProperty(Constants.SO_LINGER);
    String tcpNoDelay = properties.getProperty(Constants.TCP_NODELAY);
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

  @Override
  public void setPersistentClass(Class<T> persistentClass) {
    super.setPersistentClass(persistentClass);
  }

  @Override
  public Class<T> getPersistentClass() {
    return super.getPersistentClass();
  }

  @Override
  public String getSchemaName() {
    return null;
  }

  @Override
  public void createSchema() {

  }

  @Override
  public void deleteSchema() {

  }

  @Override
  public Class<K> getKeyClass() {
    return super.getKeyClass();
  }

  @Override
  public void setKeyClass(Class<K> keyClass) {
    super.setKeyClass(keyClass);
  }

  @Override
  public K newKey() {
    return super.newKey();
  }

  @Override
  public T newPersistent() {
    return super.newPersistent();
  }

  @Override
  public void setBeanFactory(BeanFactory<K, T> beanFactory) {
    super.setBeanFactory(beanFactory);
  }

  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return super.getBeanFactory();
  }

  @Override
  public void close() {

  }

  @Override
  public T get(K key) {
    if (isUseCassandraMappingManager) {
      return mapper.get(key);
    } else {
      return super.get(key);
    }
  }

  @Override
  public T get(K key, String[] fields) {
    return null;
  }

  @Override
  public void put(K key, T obj) {
    if (isUseCassandraMappingManager) {
      mapper.save(obj);
    } else {
      super.get(key);
    }
  }

  @Override
  public boolean delete(K key) {
    if (isUseCassandraMappingManager) {
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
  protected String[] getFieldsToQuery(String[] fields) {
    return super.getFieldsToQuery(fields);
  }

  @Override
  protected String[] getFields() {
    return super.getFields();
  }

  @Override
  public Configuration getConf() {
    return super.getConf();
  }

  @Override
  public void setConf(Configuration conf) {
    super.setConf(conf);
  }

  @Override
  protected Configuration getOrCreateConf() {
    return super.getOrCreateConf();
  }

  @Override
  public void readFields(DataInput in) {
    super.readFields(in);
  }

  @Override
  public void write(DataOutput out) {
    super.write(out);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public void truncateSchema() {
    super.truncateSchema();
  }

  @Override
  public boolean schemaExists() {
    return false;
  }

  @Override
  protected String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }
}