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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.persistent.CassandraNativePersistent;
import org.apache.gora.cassandra.query.CassandraResultSet;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This Class contains the operation relates to Native Serialization.
 */
public class NativeSerializer<K, T extends CassandraNativePersistent> extends CassandraSerializer {

  private Mapper<T> mapper;

  @Override
  public void put(Object key, Persistent value) {
    mapper.save((T) value);
  }

  @Override
  public T get(Object key) {
    return mapper.get(key);
  }

  @Override
  public boolean delete(Object key) {
    mapper.delete(key);
    return true;
  }

  @Override
  public Persistent get(Object key, String[] fields) {
    List<Object> objectArrayList = new ArrayList<>();
    String cqlQuery = CassandraQueryFactory.getObjectWithFieldsQuery(mapping, fields, key, objectArrayList);
    ResultSet results = client.getSession().execute(cqlQuery, objectArrayList.toArray());
    Result<T> objects = mapper.map(results);
    List<T> objectList = objects.all();
    if (objectList != null) {
      return objectList.get(0);
    }
    return null;
  }

  @Override
  public org.apache.gora.query.Result execute(DataStore dataStore, Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    CassandraResultSet<K, T> cassandraResult = new CassandraResultSet<K, T>(dataStore, query);
    String cqlQuery = CassandraQueryFactory.getExecuteQuery(mapping, query, objectArrayList);
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
  }

  public NativeSerializer(CassandraClient cassandraClient, Class<K> keyClass, Class<T> persistentClass, CassandraMapping mapping) {
    super(cassandraClient, keyClass, persistentClass, mapping);
    this.createSchema();
    MappingManager mappingManager = new MappingManager(cassandraClient.getSession());
    mapper = mappingManager.mapper(persistentClass);
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
    K key = null;
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
