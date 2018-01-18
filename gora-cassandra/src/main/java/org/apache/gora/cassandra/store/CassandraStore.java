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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
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
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException("Error while initializing Cassandra store: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
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
   */
  @Override
  public String getSchemaName() {
    return mapping.getCoreName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createSchema() throws GoraException {
    cassandraSerializer.createSchema();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteSchema() throws GoraException {
    cassandraSerializer.deleteSchema();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("all")
  @Override
  public Class<K> getKeyClass() {
    return this.keyClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setKeyClass(Class<K> keyClass) {
    this.keyClass = keyClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public K newKey() throws GoraException {
    try {
      if (beanFactory != null) {
        return beanFactory.newKey();
      } else {
        return keyClass.newInstance();
      }
    } catch (Exception e) {
      LOG.error("Error while instantiating a key: " + e.getMessage(), e);
      throw new GoraException("Error while instantiating a key: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("all")
  @Override
  public T newPersistent() throws GoraException {
    try {
      if (beanFactory != null) {
        return this.beanFactory.newPersistent();
      } else {
        return persistentClass.newInstance();
      }
    } catch (Exception e) {
      LOG.error("Error while instantiating a persistent: " + e.getMessage(), e);
      throw new GoraException("Error while instantiating a key: " + e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return this.beanFactory;
  }

  /**
   * {@inheritDoc}
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
   */
  @Override
  public T get(K key) throws GoraException {
    return (T) cassandraSerializer.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(K key, String[] fields) throws GoraException {
    return (T) cassandraSerializer.get(key, fields);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(K key, T obj) throws GoraException {
    cassandraSerializer.put(key, obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean delete(K key) throws GoraException {
    return cassandraSerializer.delete(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    return cassandraSerializer.deleteByQuery(query);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    try {
        return (Result<K, T>) cassandraSerializer.execute(this, query);
    } catch (Exception e) {
        this.LOG.error(e.getMessage(), e);
        throw new GoraException(e);
    }
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
   */
  @Override
  public Query<K, T> newQuery() {
    Query<K, T> query = new CassandraQuery(this);
    return query;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws GoraException {
    try {
      List<PartitionQuery<K, T>> partitions = new ArrayList<>();
      PartitionWSQueryImpl<K, T> pqi = new PartitionWSQueryImpl<>(query);
      pqi.setDataStore(this);
      partitions.add(pqi);
      return partitions;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() throws GoraException {
    // ignore since caching has been disabled
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void truncateSchema() throws GoraException {
    cassandraSerializer.truncateSchema();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean schemaExists() throws GoraException{
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