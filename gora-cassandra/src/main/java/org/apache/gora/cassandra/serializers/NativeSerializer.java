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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.query.CassandraResultSet;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;

/**
 * This Class contains the operation relates to Native Serialization.
 */
class NativeSerializer<K, T extends Persistent> extends CassandraSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(NativeSerializer.class);

  private Mapper<T> mapper;

  NativeSerializer(CassandraClient cassandraClient, Class<K> keyClass, Class<T> persistentClass, CassandraMapping mapping) throws GoraException {
    super(cassandraClient, keyClass, persistentClass, mapping);
    try {
      analyzePersistent();
    } catch (Exception e) {
      throw new GoraException("Error occurred while analyzing the persistent class, :" + e.getMessage(), e);
    }
    this.createSchema();
    MappingManager mappingManager = new MappingManager(cassandraClient.getSession());
    mapper = mappingManager.mapper(persistentClass);
    if (cassandraClient.getWriteConsistencyLevel() != null) {
      mapper.setDefaultDeleteOptions(Mapper.Option.consistencyLevel(ConsistencyLevel.valueOf(cassandraClient.getWriteConsistencyLevel())));
      mapper.setDefaultSaveOptions(Mapper.Option.consistencyLevel(ConsistencyLevel.valueOf(cassandraClient.getWriteConsistencyLevel())));
    }
    if (cassandraClient.getReadConsistencyLevel() != null) {
      mapper.setDefaultGetOptions(Mapper.Option.consistencyLevel(ConsistencyLevel.valueOf(cassandraClient.getReadConsistencyLevel())));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @param value
   */
  @Override
  public void put(Object key, Persistent value) throws GoraException {
    try {
      LOG.debug("Object is saved with key : {} and value : {}", key, value);
      mapper.save((T) value);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @return
   */
  @Override
  public T get(Object key) throws GoraException {
    try {
      T object = mapper.get(key);
      if (object != null) {
        LOG.debug("Object is found for key : {}", key);
      } else {
        LOG.debug("Object is not found for key : {}", key);
      }      
      return object;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @return
   */
  @Override
  public boolean delete(Object key) throws GoraException {
    LOG.debug("Object is deleted for key : {}", key);
    try {
      mapper.delete(key);
      return true;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @param fields
   * @return
   */
  @Override
  public Persistent get(Object key, String[] fields) {
    if (fields == null) {
      fields = getFields();
    }
    String cqlQuery = CassandraQueryFactory.getSelectObjectWithFieldsQuery(mapping, fields);
    SimpleStatement statement = new SimpleStatement(cqlQuery, key);
    if (readConsistencyLevel != null) {
      statement.setConsistencyLevel(ConsistencyLevel.valueOf(readConsistencyLevel));
    }
    ResultSet results = client.getSession().execute(statement);
    Result<T> objects = mapper.map(results);
    List<T> objectList = objects.all();
    if (objectList != null) {
      LOG.debug("Object is found for key : {}", key);
      return objectList.get(0);
    }
    LOG.debug("Object is not found for key : {}", key);
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @throws Exception
   */
  @Override
  protected void analyzePersistent() throws Exception {
    userDefineTypeMaps = new HashMap<>();
    for (Field field : mapping.getFieldList()) {
      String fieldType = field.getType();
      if (fieldType.contains("frozen")) {
        String udtType = fieldType.substring(fieldType.indexOf("<") + 1, fieldType.indexOf(">"));
        String createQuery = CassandraQueryFactory.getCreateUDTTypeForNative(mapping, persistentClass, udtType, field.getFieldName());
        userDefineTypeMaps.put(udtType, createQuery);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param query
   * @return
   */
  @Override
  public boolean updateByQuery(Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    String cqlQuery = CassandraQueryFactory.getUpdateByQueryForNative(mapping, query, objectArrayList);
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
    return results.wasApplied();
  }

  /**
   * {@inheritDoc}
   *
   * @param dataStore
   * @param query
   * @return
   */
  @Override
  public org.apache.gora.query.Result execute(DataStore dataStore, Query query) throws GoraException {
    try {
      List<Object> objectArrayList = new ArrayList<>();
      String[] fields = query.getFields();
      if (fields != null) {
        fields = (String[]) ArrayUtils.addAll(fields, mapping.getAllKeys());
      } else {
        fields = mapping.getAllFieldsIncludingKeys();
      }
      CassandraResultSet<K, T> cassandraResult = new CassandraResultSet<>(dataStore, query);
      String cqlQuery = CassandraQueryFactory.getExecuteQuery(mapping, query, objectArrayList, fields);
      ResultSet results;
      if (objectArrayList.size() == 0) {
        results = client.getSession().execute(cqlQuery);
      } else {
        results = client.getSession().execute(cqlQuery, objectArrayList.toArray());
      }
      Result<T> objects = mapper.map(results);
      Iterator iterator = objects.iterator();
      while (iterator.hasNext()) {
        T result = (T) iterator.next();
        K key = getKey(result);
        cassandraResult.addResultElement(key, result);
      }
      return cassandraResult;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  private K getKey(T object) {
    String keyField = null;
    for (Field field : mapping.getFieldList()) {
      boolean isPrimaryKey = Boolean.parseBoolean(field.getProperty("primarykey"));
      if (isPrimaryKey) {
        keyField = field.getFieldName();
        break;
      }
    }
    K key;
    Method keyMethod = null;
    try {
      for (Method method : this.persistentClass.getMethods()) {
        if (method.getName().equalsIgnoreCase("get" + keyField)) {
          keyMethod = method;
        }
      }
      key = (K) keyMethod.invoke(object);
    } catch (Exception e) {
      try {
        key = (K) this.persistentClass.getField(keyField).get(object);
      } catch (Exception e1) {
        throw new RuntimeException("Field" + keyField + " is not accessible in " + persistentClass + " : " + e1.getMessage());
      }
    }
    return key;
  }
}
