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

import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.serializers.CassandraSerializer;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.ws.impl.PartitionWSQueryImpl;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Implementation of Cassandra Store.
 *
 * @param <K> key class
 * @param <T> persistent class
 */
public class CassandraStore<K, T extends Persistent> implements DataStore<K, T> {

  private static final String DEFAULT_MAPPING_FILE = "gora-cassandra-mapping.xml";

  private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  private BeanFactory<K, T> beanFactory;

  private Class<K> keyClass;

  private Class<T> persistentClass;

  private CassandraMapping mapping;

  private CassandraSerializer cassandraSerializer;

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
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    LOG.debug("Initializing Cassandra store");
    String serializationType;
    try {
      this.keyClass = keyClass;
      this.persistentClass = persistentClass;
      if (this.beanFactory == null) {
        this.beanFactory = new BeanFactoryImpl<>(keyClass, persistentClass);
      }
      String mappingFile = DataStoreFactory.getMappingFile(properties, this, DEFAULT_MAPPING_FILE);
      serializationType = properties.getProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE);
      CassandraMappingBuilder mappingBuilder = new CassandraMappingBuilder(this);
      mapping = mappingBuilder.readMapping(mappingFile);
      CassandraClient cassandraClient = new CassandraClient();
      cassandraClient.initialize(properties, mapping);
      cassandraSerializer = CassandraSerializer.getSerializer(cassandraClient, serializationType, this, mapping);
    } catch (Exception e) {
      throw new RuntimeException("Error while initializing Cassandra store: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @SuppressWarnings("all")
  @Override
  public Class<T> getPersistentClass() {
    return (Class<T>) this.persistentClass;
  }

  /**
   * {@inheritDoc}
   * <p>
   * This is a setter method to set the class of persistent objects.
   *
   * @param persistentClass class of persistent objects
   *                        {@link  org.apache.gora.persistency.Persistent}
   */
  @Override
  public void setPersistentClass(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public String getSchemaName() {
    return mapping.getCoreName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createSchema() {
    cassandraSerializer.createSchema();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteSchema() {
    cassandraSerializer.deleteSchema();
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @SuppressWarnings("all")
  @Override
  public Class<K> getKeyClass() {
    return this.keyClass;
  }

  /**
   * {@inheritDoc}
   *
   * @param keyClass the class of keys
   */
  @Override
  public void setKeyClass(Class<K> keyClass) {
    this.keyClass = keyClass;
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public K newKey() {
    try {
      if (beanFactory != null) {
        return beanFactory.newKey();
      } else {
        return keyClass.newInstance();
      }
    } catch (Exception ex) {
      throw new RuntimeException("Error while instantiating a key: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @SuppressWarnings("all")
  @Override
  public T newPersistent() {
    try {
      if (beanFactory != null) {
        return this.beanFactory.newPersistent();
      } else {
        return persistentClass.newInstance();
      }
    } catch (Exception ex) {
      throw new RuntimeException("Error while instantiating a persistent: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return this.beanFactory;
  }

  /**
   * {@inheritDoc}
   *
   * @param beanFactory the BeanFactory to use
   */
  @Override
  public void setBeanFactory(BeanFactory<K, T> beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    this.cassandraSerializer.close();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key of the object
   * @return
   */
  @Override
  public T get(K key) {
    return (T) cassandraSerializer.get(key);
  }

  /**
   * {@inheritDoc}
   *
   * @param key    the key of the object
   * @param fields the fields required in the object. Pass null, to retrieve all fields
   * @return
   */
  @Override
  public T get(K key, String[] fields) {
    return (T) cassandraSerializer.get(key, fields);
  }

  /**
   * {@inheritDoc}
   *
   * @param key key value
   * @param obj object value
   */
  @Override
  public void put(K key, T obj) {
    cassandraSerializer.put(key, obj);
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key of the object
   * @return
   */
  @Override
  public boolean delete(K key) {
    return cassandraSerializer.delete(key);
  }

  /**
   * {@inheritDoc}
   *
   * @param query matching records to this query will be deleted
   * @return
   */
  @Override
  public long deleteByQuery(Query<K, T> query) {
    return cassandraSerializer.deleteByQuery(query);
  }

  /**
   * {@inheritDoc}
   *
   * @param query the query to execute.
   * @return
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) {
    return (Result<K, T>) cassandraSerializer.execute(this, query);
  }

  /**
   * This method is used to update multiple objects in the table.
   *
   * @param query Query
   * @return isQuery applied or not
   */
  public boolean updateByQuery(Query<K, T> query) {
    return cassandraSerializer.updateByQuery(query);
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public Query<K, T> newQuery() {
    Query<K, T> query = new CassandraQuery(this);
    return query;
  }

  /**
   * {@inheritDoc}
   *
   * @param query cassandra Query
   * @return
   * @throws IOException
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionWSQueryImpl<K, T> pqi = new PartitionWSQueryImpl<>(query);
    pqi.setDataStore(this);
    partitions.add(pqi);
    return partitions;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() {
    // ignore since caching has been disabled
  }

  /**
   * {@inheritDoc}
   *
   * @param obj
   * @return
   */
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void truncateSchema() {
    cassandraSerializer.truncateSchema();
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public boolean schemaExists() {
    return cassandraSerializer.schemaExists();
  }

  public enum SerializerType {
    AVRO("AVRO"), NATIVE("NATIVE");
    String val;

    SerializerType(String v) {
      this.val = v;
    }
  }

}