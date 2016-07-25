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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.cache.ICache;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import org.apache.avro.Schema;
import org.apache.gora.jcache.query.JCacheQuery;
import org.apache.gora.jcache.query.JCacheResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

public class JCacheStore<K,T extends PersistentBase> extends DataStoreBase<K,T> {

  private ICache<K, T> cache;
  private CacheManager manager;
  private ConcurrentSkipListSet<K> cacheEntryList;
  private static final String GORA_DEFAULT_JCACHE_PROVIDER_KEY = "gora.datastore.jcache.provider";
  private static final String GORA_DEFAULT_JCACHE_NAMESPACE = "gora.jcache.namespace";
  private static final Logger LOG = LoggerFactory.getLogger(JCacheStore.class);
  private DataStore<K, T> persistentDataStore;
  private MutableConfiguration<K, T> cacheConfig;
  private HazelcastInstance hazelcastInstance;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
    CachingProvider cachingProvider = Caching.getCachingProvider(
           properties.getProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY)
    );
    try {
      this.persistentDataStore = DataStoreFactory.getDataStore(keyClass, persistentClass,
              new Configuration());
    } catch (GoraException ex) {
      LOG.error("Couldn't initialize persistent DataStore");
    }
    hazelcastInstance = Hazelcast.newHazelcastInstance();
    Properties providerProperties = new Properties();
    providerProperties.setProperty( HazelcastCachingProvider.HAZELCAST_INSTANCE_NAME,
            hazelcastInstance.getName());
    try {
      manager = cachingProvider.getCacheManager(new URI(GORA_DEFAULT_JCACHE_NAMESPACE), null, providerProperties);
    } catch (URISyntaxException ex) {
      LOG.error("Couldn't initialize cache manager to a bounded hazelcast instance");
      manager = cachingProvider.getCacheManager();
    }
    cacheEntryList = new ConcurrentSkipListSet<>();
    cacheConfig = new MutableConfiguration<K, T>();
    cacheConfig.setTypes(keyClass, persistentClass);
    cacheConfig.setReadThrough(true);
    cacheConfig.setWriteThrough(true);
    cacheConfig.setStoreByValue(true);
    cacheConfig.setCacheLoaderFactory(JCacheCacheFactoryBuilder
            .factoryOfCacheLoader(this.persistentDataStore));
    cacheConfig.setCacheWriterFactory(JCacheCacheFactoryBuilder
            .factoryOfCacheWriter(this.persistentDataStore));
    cacheConfig.addCacheEntryListenerConfiguration(
            new MutableCacheEntryListenerConfiguration<>(
                    JCacheCacheFactoryBuilder
                            .factoryOfEntryListener(new JCacheCacheEntryListener<K,T>(cacheEntryList)),
                    null, true, true
            )
    );
    cache = manager.createCache(persistentClass.getSimpleName(),
            cacheConfig).unwrap(ICache.class);
  }

  @Override
  public String getSchemaName() {
    return super.persistentClass.getSimpleName();
  }

  @Override
  public void createSchema() {
    if (manager.getCache(super.getPersistentClass().getSimpleName()) == null) {
      cache = manager.createCache(persistentClass.getSimpleName(),
              cacheConfig).unwrap(ICache.class);
    }
    persistentDataStore.createSchema();
  }

  @Override
  public void deleteSchema() {
    manager.destroyCache(super.getPersistentClass().getSimpleName());
    persistentDataStore.deleteSchema();
  }

  @Override
  public boolean schemaExists() {
    return (manager.getCache(super.getPersistentClass().getSimpleName()) != null)
            && persistentDataStore.schemaExists();
  }

  @Override
  public T get(K key, String[] fields) {
    T persitent = (T) cache.get(key);
    if (persitent == null) {
      return null;
    }
    return getPersistent(persitent, fields);
  }

  private static <T extends PersistentBase> T getPersistent(T persitent, String[] fields) {
    List<Schema.Field> otherFields = persitent.getSchema().getFields();
    String[] otherFieldStrings = new String[otherFields.size()];
    for (int i = 0; i < otherFields.size(); i++) {
      otherFieldStrings[i] = otherFields.get(i).name();
    }
    if (Arrays.equals(fields, otherFieldStrings)) {
      return persitent;
    }
    T clonedPersistent = AvroUtils.deepClonePersistent(persitent);
    clonedPersistent.clear();
    for (String field : fields) {
      Schema.Field otherField = persitent.getSchema().getField(field);
      int index = otherField.pos();
      clonedPersistent.put(index, persitent.get(index));
    }
    return clonedPersistent;
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
  public long deleteByQuery(Query<K, T> query) {
    try {
      long deletedRows = 0;
      Result<K, T> result = query.execute();
      String[] fields = getFieldsToQuery(query.getFields());
      boolean isAllFields = Arrays.equals(fields, getFields());
      while (result.next()) {
        if (isAllFields) {
          if (delete(result.getKey())) {
            deletedRows++;
          }
        } else {
          ArrayList<String> excludedFields = new ArrayList<>();
          for (String field : getFields()) {
            if (!Arrays.asList(fields).contains(field)) {
              excludedFields.add(field);
            }
          }
          T newClonedObj = getPersistent(result.get(),
                  excludedFields.toArray(new String[excludedFields.size()]));
          if (delete(result.getKey())) {
            put(result.getKey(), newClonedObj);
            deletedRows++;
          }
        }
      }
      return deletedRows;
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    if (startKey == null) {
      if (!cacheEntryList.isEmpty()) {
        startKey = (K) cacheEntryList.first();
      }
    }
    if (endKey == null) {
      if (!cacheEntryList.isEmpty()) {
        endKey = (K) cacheEntryList.last();
      }
    }
    query.setFields(getFieldsToQuery(query.getFields()));
    ConcurrentSkipListSet<K> cacheEntrySubList = null;
    try {
      cacheEntrySubList = (ConcurrentSkipListSet<K>) cacheEntryList.subSet(startKey, true, endKey, true);
    } catch (NullPointerException npe) {
      LOG.error("NPE occurred while executing the query for JCacheStore");
      return new JCacheResult<>(this, query, new ConcurrentSkipListSet<K>());
    }
    return new JCacheResult<>(this, query, cacheEntrySubList);
  }

  @Override
  public Query<K,T> newQuery() {
    return new JCacheQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    try {
      Member[] clusterMembers = new Member[hazelcastInstance.getCluster().getMembers().size()];
      this.hazelcastInstance.getCluster().getMembers().toArray(clusterMembers);
      for (Member member : clusterMembers) {
        JCacheResult<K, T> result = ((JCacheResult<K, T>) query.execute());
        ConcurrentSkipListSet<K> memberOwnedCacheEntries = new ConcurrentSkipListSet<>();
        while (result.next()) {
          K key = result.getKey();
          Partition partition = hazelcastInstance.getPartitionService().getPartition(key);
          if (partition.getOwner().getUuid().equals(member.getUuid())) {
            memberOwnedCacheEntries.add(key);
          }
        }
        PartitionQueryImpl<K, T> partition = new PartitionQueryImpl<>(
                query, memberOwnedCacheEntries.first(),
                memberOwnedCacheEntries.last(), member.getSocketAddress().getHostString());
        partitions.add(partition);
      }
    } catch (java.lang.Exception ex) {
      LOG.error("Exception occurred while partitioning the query based on Hazelcast partitions.");
      return null;
    }
    return partitions;
  }

  @Override
  public void flush() {
    persistentDataStore.flush();
  }

  @Override
  public void close() {
    flush();
    if (!cache.isDestroyed()) {
      cache.destroy();
    }
    if (!manager.isClosed()) {
      manager.close();
    }
    persistentDataStore.close();
  }

}
