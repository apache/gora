/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.jcache.store;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.gora.jcache.query.JCacheQuery;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

public class JCacheStore<K,T extends PersistentBase> extends DataStoreBase<K,T> {

  private Cache<K, T> cache;
  private CacheManager manager;
  private ConcurrentSkipListSet<K> cacheEntryList;
  private static final String GORA_DEFAULT_JCACHE_PROVIDER_KEY = "gora.datastore.jcache.provider";
  private static final Logger LOG = LoggerFactory.getLogger(JCacheStore.class);

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
    CachingProvider cachingProvider = Caching.getCachingProvider(
           properties.getProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY)
    );
    manager = cachingProvider.getCacheManager();
    cacheEntryList = new ConcurrentSkipListSet<>();
    MutableConfiguration<K, T> config = new MutableConfiguration<K, T>();
    config.setTypes(keyClass, persistentClass);
    config.setReadThrough(true);
    config.setWriteThrough(true);
    config.setCacheLoaderFactory(JCacheCacheFactoryBuilder.factoryOfCacheLoader(keyClass,persistentClass));
    config.setCacheWriterFactory(JCacheCacheFactoryBuilder.factoryOfCacheWriter(keyClass,persistentClass));
    config.addCacheEntryListenerConfiguration(
            new MutableCacheEntryListenerConfiguration<>(
                    JCacheCacheFactoryBuilder.factoryOfEntryListener(new JCacheCacheEntryListener<K,T>(cacheEntryList)),
                    null, true, true
            )
    );
    cache = manager.createCache(persistentClass.getSimpleName(),config);
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
  public boolean schemaExists() {
    return false;
  }


  @Override
  public T get(K key, String[] fields) {
    return null;
  }

  @Override
  public T get(K key){
    return cache.get(key);
  }

  @Override
  public void put(K key, T val) {
    cache.put(key,val);
  }

  @Override
  public boolean delete(K key) {
    return cache.remove(key);
  }

  @Override
  public long deleteByQuery(Query<K,T> query) {
    return 0;
  }

  @Override
  public Result<K,T> execute(Query<K,T> query) {
    return null;
  }

  @Override
  public Query<K,T> newQuery() {
    return new JCacheQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K,T>> getPartitions(Query<K,T> query) throws IOException {
    return null;
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }

}
