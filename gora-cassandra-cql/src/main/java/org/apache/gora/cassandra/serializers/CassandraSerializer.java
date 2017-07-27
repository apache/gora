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

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.TableMetadata;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is the abstract Cassandra Serializer class.
 */
public abstract class CassandraSerializer<K, T extends Persistent> {
  CassandraClient client;

  protected Class<K> keyClass;

  protected Class<T> persistentClass;

  protected CassandraMapping mapping;

  private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  CassandraSerializer(CassandraClient cc, Class<K> keyClass, Class<T> persistantClass, CassandraMapping mapping) {
    this.keyClass = keyClass;
    this.persistentClass = persistantClass;
    this.client = cc;
    this.mapping = mapping;
  }

  public void createSchema() {
    LOG.debug("creating Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.client.getSession().execute(CassandraQueryFactory.getCreateKeySpaceQuery(mapping));
    LOG.debug("creating Cassandra column family / table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getCreateTableQuery(mapping));
  }

  public void deleteSchema() {
    LOG.debug("dropping Cassandra table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getDropTableQuery(mapping));
    LOG.debug("dropping Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.client.getSession().execute(CassandraQueryFactory.getDropKeySpaceQuery(mapping));
  }

  public void close() {
    this.client.close();
  }

  public void truncateSchema() {
    LOG.debug("truncating Cassandra table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getTruncateTableQuery(mapping));
  }

  public boolean schemaExists() {
    KeyspaceMetadata keyspace = this.client.getCluster().getMetadata().getKeyspace(mapping.getKeySpace().getName());
    if (keyspace != null) {
      TableMetadata table = keyspace.getTable(mapping.getCoreName());
      return table != null;
    } else {
      return false;
    }
  }

  /**
   * This method returns the Cassandra Serializer according the Cassandra serializer property.
   *
   * @param cc              Cassandra Client
   * @param type            Serialization type
   * @param dataStore        Cassandra DataStore
   * @param mapping         Cassandra Mapping
   * @param <K>             key class
   * @param <T>             persistent class
   * @return Serializer
   */
  public static <K, T extends Persistent> CassandraSerializer getSerializer(CassandraClient cc, String type, final DataStore<K,T> dataStore, CassandraMapping mapping) {
    CassandraStore.SerializerType serType = type.isEmpty() ? CassandraStore.SerializerType.NATIVE : CassandraStore.SerializerType.valueOf(type.toUpperCase(Locale.ENGLISH));
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

  protected String[] getFields() {
    List<String> fields = new ArrayList<>();
    for (Field field : mapping.getFieldList()) {
      fields.add(field.getFieldName());
    }
    return fields.toArray(new String[0]);
  }

  public abstract void put(K key, T value);

  public abstract T get(K key);

  public abstract boolean delete(K key);

  public abstract T get(K key, String[] fields);

  public abstract Result<K, T> execute(DataStore<K, T> dataStore, Query<K, T> query);

  public boolean updateByQuery(Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    String cqlQuery = CassandraQueryFactory.getUpdateByQuery(mapping, query, objectArrayList);
    ResultSet results;
    if (objectArrayList.size() == 0) {
      results = client.getSession().execute(cqlQuery);
    } else {
      results = client.getSession().execute(cqlQuery, objectArrayList.toArray());
    }
    return results.wasApplied();
  }

  public long deleteByQuery(Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    String cqlQuery = CassandraQueryFactory.getDeleteByQuery(mapping, query, objectArrayList);
    ResultSet results;
    if (objectArrayList.size() == 0) {
      results = client.getSession().execute(cqlQuery);
    } else {
      results = client.getSession().execute(cqlQuery, objectArrayList.toArray());
    }
    LOG.debug("Delete by Query was applied : " + results.wasApplied());
    LOG.info("Delete By Query method doesn't return the deleted element count.");
    return 0;
  }

}