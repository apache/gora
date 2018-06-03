/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.infinispan.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.infinispan.avro.client.Marshaller;
import org.infinispan.avro.client.Support;
import org.infinispan.avro.hotrod.QueryBuilder;
import org.infinispan.avro.hotrod.QueryFactory;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.BasicCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @author Pierre Sutra, Valerio Schiavoni
 */
public class InfinispanClient<K, T extends PersistentBase> implements Configurable{

  public static final Logger LOG = LoggerFactory.getLogger(InfinispanClient.class);

  public static final String ISPN_CONNECTION_STRING_KEY = "infinispan.connectionstring";
  public static final String ISPN_CONNECTION_STRING_DEFAULT = "127.0.0.1:11222";

  private Configuration conf;

  private Class<K> keyClass;
  private Class<T> persistentClass;
  private RemoteCacheManager cacheManager;
  private QueryFactory qf;

  private RemoteCache<K, T> cache;
  private boolean cacheExists;

  private Map<K,T> toPut;

  public InfinispanClient() {
    conf = new Configuration();
  }

  public synchronized void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws Exception {

    if (cache!=null)
      return; // already initialized.

    this.keyClass = keyClass;
    this.persistentClass = persistentClass;

    String host = properties.getProperty(ISPN_CONNECTION_STRING_KEY,
        getConf().get(ISPN_CONNECTION_STRING_KEY, ISPN_CONNECTION_STRING_DEFAULT));
    conf.set(ISPN_CONNECTION_STRING_KEY, host);
    properties.setProperty(ISPN_CONNECTION_STRING_KEY, host);
    LOG.info("Connecting client to "+host);

    Marshaller<T> marshaller = new Marshaller<>(persistentClass);
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addServers(host);
    builder.marshaller(marshaller);
    cacheManager = new RemoteCacheManager(builder.build());
    cacheManager.start();

    cache = cacheManager.getCache(persistentClass.getSimpleName());
    qf = org.infinispan.avro.hotrod.Search.getQueryFactory(cache);
    createSchema();

    toPut = new HashMap<>();
  }

  public boolean cacheExists(){
    return cacheExists;
  }

  public synchronized void createSchema() throws GoraException {
    try {
      Support.registerSchema(cacheManager, persistentClass.getConstructor().newInstance().getSchema());
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public void createCache() throws GoraException {
    createSchema();
    cacheExists = true;
  }

  public void dropCache() throws GoraException {
    try {
      cache.clear();
      cacheExists = false;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public void deleteByKey(K key) {
    cache.remove(key);
  }

  public synchronized void put(K key, T val) {
    toPut.put(key, val);
  }

  public void putIfAbsent(K key, T obj) {
    this.cache.putIfAbsent(key,obj);
  }

  public T get(K key){
    return cache.get(key);
  }

  public boolean containsKey(K key) {
    return cache.containsKey(key);
  }

  public String getCacheName() {
    return this.persistentClass.getSimpleName();
  }

  public BasicCache<K, T> getCache() {
    return this.cache;
  }

  public QueryBuilder getQueryBuilder() {
    return (QueryBuilder) qf.from(persistentClass);
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf =conf;
  }

  @Override
  public Configuration getConf() {
    return conf;
  }


  public void flush(){
    LOG.debug("flush()");
    if (!toPut.isEmpty()) cache.putAll(toPut);
    toPut.clear();
  }

  public synchronized void close() {
    LOG.debug("close()");
    flush();
    getCache().stop();
    cacheManager.stop();
  }
}
