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
package org.apache.gora.cassandra.serializers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.TableMetadata;

/**
 * This is the abstract Cassandra Serializer class.
 */
public abstract class CassandraSerializer<K, T extends Persistent> {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  protected Class<K> keyClass;

  protected Class<T> persistentClass;

  protected CassandraMapping mapping;

  protected CassandraClient client;

  protected String readConsistencyLevel;

  protected String writeConsistencyLevel;

  protected Map<String, String> userDefineTypeMaps;

  CassandraSerializer(CassandraClient cc, Class<K> keyClass, Class<T> persistantClass, CassandraMapping mapping) {
    this.keyClass = keyClass;
    this.persistentClass = persistantClass;
    this.client = cc;
    this.mapping = mapping;
    this.readConsistencyLevel = client.getReadConsistencyLevel();
    this.writeConsistencyLevel = client.getWriteConsistencyLevel();
  }

  /**
   * This method returns the Cassandra Serializer according the Cassandra serializer property.
   *
   * @param cc        Cassandra Client
   * @param type      Serialization type
   * @param dataStore Cassandra DataStore
   * @param mapping   Cassandra Mapping
   * @param <K>       key class
   * @param <T>       persistent class
   * @return Serializer
   * @throws GoraException 
   */
  public static <K, T extends Persistent> CassandraSerializer getSerializer(CassandraClient cc, String type, final DataStore<K, T> dataStore, CassandraMapping mapping) throws GoraException {
    CassandraStore.SerializerType serType = type == null || type.isEmpty() ? CassandraStore.SerializerType.NATIVE : CassandraStore.SerializerType.valueOf(type.toUpperCase(Locale.ENGLISH));
    CassandraSerializer serializer;
    switch (serType) {
      case AVRO:
        serializer = new AvroSerializer(cc, dataStore, mapping);
        break;
      case NATIVE:
      default:
        serializer = new NativeSerializer(cc, dataStore.getKeyClass(), dataStore.getPersistentClass(), mapping);
    }
    return serializer;
  }

  /**
   * In this method persistent class been analyzed to find inner records with UDT type, this method should call in every Cassandra serialization Constructor.
   *
   * @throws Exception exception
   */
  protected abstract void analyzePersistent() throws Exception;


  public void createSchema() throws GoraException {
    try {
      LOG.debug("creating Cassandra keyspace {}", mapping.getKeySpace().getName());
      this.client.getSession().execute(CassandraQueryFactory.getCreateKeySpaceQuery(mapping));
      for (Map.Entry udtType : userDefineTypeMaps.entrySet()) {
        LOG.debug("creating Cassandra User Define Type {}", udtType.getKey());
        this.client.getSession().execute((String) udtType.getValue());
      }
      LOG.debug("creating Cassandra column family / table {}", mapping.getCoreName());
      this.client.getSession().execute(CassandraQueryFactory.getCreateTableQuery(mapping));
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public void deleteSchema() throws GoraException {
    try {
      LOG.debug("dropping Cassandra table {}", mapping.getCoreName());
      this.client.getSession().execute(CassandraQueryFactory.getDropTableQuery(mapping));
      LOG.debug("dropping Cassandra keyspace {}", mapping.getKeySpace().getName());
      this.client.getSession().execute(CassandraQueryFactory.getDropKeySpaceQuery(mapping));
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public void close() {
    this.client.close();
  }

  public void truncateSchema() throws GoraException {
    try {
      LOG.debug("truncating Cassandra table {}", mapping.getCoreName());
      this.client.getSession().execute(CassandraQueryFactory.getTruncateTableQuery(mapping));
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public boolean schemaExists() throws GoraException {
    try {
      KeyspaceMetadata keyspace = this.client.getCluster().getMetadata().getKeyspace(mapping.getKeySpace().getName());
      if (keyspace != null) {
        TableMetadata table = keyspace.getTable(mapping.getCoreName());
        return table != null;
      } else {
        return false;
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  protected String[] getFields() {
    List<String> fields = new ArrayList<>();
    for (Field field : mapping.getFieldList()) {
      fields.add(field.getFieldName());
    }
    return fields.toArray(new String[0]);
  }

  /**
   * Inserts the persistent Object
   *
   * @param key   key value
   * @param value persistent value
   */
  public abstract void put(K key, T value) throws GoraException;

  /**
   * Retrieves the persistent value according to the key
   *
   * @param key key value
   * @return persistent value
   */
  public abstract T get(K key) throws GoraException;
	
  /**
   * Check if key exists
   *
   * @param key key value
   * @return true/false
   */
  public boolean exists(Object key) throws GoraException {
    try {
      ArrayList<String> cassandraKeys = new ArrayList<>();
      ArrayList<Object> cassandraValues = new ArrayList<>();
      AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
      String cqlQuery = CassandraQueryFactory.getCheckExistsQuery(mapping, cassandraKeys);
      SimpleStatement statement = new SimpleStatement(cqlQuery, cassandraValues.toArray());
      if (readConsistencyLevel != null) {
        statement.setConsistencyLevel(ConsistencyLevel.valueOf(readConsistencyLevel));
      }
      ResultSet resultSet = client.getSession().execute(statement);
      Iterator<Row> iterator = resultSet.iterator();
      Row next = iterator.next();
      long count = next.getLong(0);
      return count != 0;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Deletes persistent value according to the key
   *
   * @param key key value
   * @return isDeleted
   */
  public abstract boolean delete(K key) throws GoraException;

  /**
   * Retrieves the persistent value according to the key and fields
   *
   * @param key    key value
   * @param fields fields
   * @return persistent value
   */
  public abstract T get(K key, String[] fields) throws GoraException;

  /**
   * Executes the given query and returns the results.
   *
   * @param dataStore Cassandra data store
   * @param query     Cassandra Query
   * @return Cassandra Result
   */
  public abstract Result<K, T> execute(DataStore<K, T> dataStore, Query<K, T> query) throws GoraException ;

  /**
   * Update the persistent objects
   *
   * @param query Cassandra Query
   * @return isUpdated
   */
  public abstract boolean updateByQuery(Query query);

  public long deleteByQuery(Query query) throws GoraException {
    try {
      List<Object> objectArrayList = new ArrayList<>();
      if (query.getKey() == null && query.getEndKey() == null && query.getStartKey() == null) {
        if (query.getFields() == null) {
          client.getSession().execute(CassandraQueryFactory.getTruncateTableQuery(mapping));
        } else {
          LOG.error("Delete by Query is not supported for the Queries which didn't specify Query keys with fields.");
        }
      } else {
        String cqlQuery = CassandraQueryFactory.getDeleteByQuery(mapping, query, objectArrayList);
        ResultSet results;
        SimpleStatement statement;
        if (objectArrayList.size() == 0) {
          statement = new SimpleStatement(cqlQuery);
        } else {
          statement = new SimpleStatement(cqlQuery, objectArrayList.toArray());
        }
        if (writeConsistencyLevel != null) {
          statement.setConsistencyLevel(ConsistencyLevel.valueOf(writeConsistencyLevel));
        }
        results = client.getSession().execute(statement);
        LOG.debug("Delete by Query was applied : " + results.wasApplied());
      }
      LOG.info("Delete By Query method doesn't return the deleted element count.");
      return 0;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

}