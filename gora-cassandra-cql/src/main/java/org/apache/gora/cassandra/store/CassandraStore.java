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

import org.apache.gora.cassandra.persistent.CassandraNativePersistent;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.serializers.CassandraSerializer;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

  public static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  private BeanFactory<K, T> beanFactory;

  private Class<K> keyClass;

  private Class<T> persistentClass;

  private CassandraMapping mapping;

  private CassandraSerializer cassandraSerializer;

  public enum SerializerType {
    AVRO("AVRO"), NATIVE("NATIVE");
    String val;

    SerializerType(String v) {
      this.val = v;
    }
  }


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
      CassandraMappingBuilder mappingBuilder = new CassandraMappingBuilder(this);
      mapping = mappingBuilder.readMapping(mappingFile);
      CassandraClient cassandraClient = new CassandraClient();
      cassandraClient.initialize(properties);
      cassandraSerializer = CassandraSerializer.getSerializer(cassandraClient, properties.getProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE), keyClass, persistentClass, mapping);
    } catch (Exception e) {
      LOG.error("Error while initializing Cassandra store: {}",
              new Object[]{e.getMessage()});
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * This is a setter method to set the class of persistent objects.
   *
   * @param persistentClass class of persistent objects
   *                        {@link CassandraNativePersistent}
   *                        {@link  org.apache.gora.persistency.Persistent}
   */
  @Override
  public void setPersistentClass(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  @SuppressWarnings("all")
  @Override
  public Class<T> getPersistentClass() {
    return (Class<T>) this.persistentClass;
  }

  @Override
  public String getSchemaName() {
    return mapping.getCoreName();
  }

  @Override
  public void createSchema() {
    cassandraSerializer.createSchema();
  }

  @Override
  public void deleteSchema() {
    cassandraSerializer.deleteSchema();
  }

  @SuppressWarnings("all")
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
      if (beanFactory != null) {
        return beanFactory.newKey();
      } else {
        LOG.warn("beanFactory is hasn't been initialized. It's recommended to initialize beanFactory.");
        return keyClass.newInstance();
      }
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return null;
    }
  }

  @SuppressWarnings("all")
  @Override
  public T newPersistent() {
    try {
      if (beanFactory != null) {
        return this.beanFactory.newPersistent();
      } else {
        LOG.warn("beanFactory is hasn't been initialized. It's recommended to initialize beanFactory.");
        return persistentClass.newInstance();
      }
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      return null;
    }
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
    this.cassandraSerializer.close();
  }

  @Override
  public T get(K key) {
    return (T) cassandraSerializer.get(key);
  }

  @Override
  public T get(K key, String[] fields) {
    return (T) cassandraSerializer.get(key, fields);
  }

  @Override
  public void put(K key, T obj) {
    cassandraSerializer.put(key, obj);
  }

  @Override
  public boolean delete(K key) {
    return cassandraSerializer.delete(key);
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    return (Result<K,T>) cassandraSerializer.execute(this, query);
  }

  public void updateByQuery(Query<K,T> query) {

  }

  @Override
  public Query<K, T> newQuery() {
    Query<K,T> query = new CassandraQuery(this);
    query.setFields(mapping.getFieldNames());
    return query;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    return null;
  }

  @Override
  public void flush() {
 // ignore since caching has been disabled
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public void truncateSchema() {
    cassandraSerializer.truncateSchema();
  }

  @Override
  public boolean schemaExists() {
    return cassandraSerializer.schemaExists();
  }

}