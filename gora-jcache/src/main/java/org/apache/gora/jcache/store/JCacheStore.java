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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.spi.CachingProvider;

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

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.cache.ICache;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.CacheConfig;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;

/**
 * {@link org.apache.gora.jcache.store.JCacheStore} is the primary class
 * responsible for GORA CRUD operations on Hazelcast Caches. This class can be think
 * of as caching layer that can is wrapped over any persistency dataStore implementations
 * which extends {@link org.apache.gora.store.DataStore}.  This class delegates
 * most operations to it s persistency dataStore. Hazelcast cache implementation is based on
 * JCache JSR 107 specification.
 */
public class JCacheStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final String GORA_DEFAULT_JCACHE_NAMESPACE = "gora.jcache.namespace";
  private static final String GORA_DEFAULT_JCACHE_PROVIDER_KEY = "gora.datastore.jcache.provider";
  private static final String GORA_DEFAULT_JCACHE_HAZELCAST_CONFIG_KEY = "gora.datastore.jcache.hazelcast.config";
  private static final String JCACHE_READ_THROUGH_PROPERTY_KEY = "jcache.read.through.enable";
  private static final String JCACHE_WRITE_THROUGH_PROPERTY_KEY = "jcache.write.through.enable";
  private static final String JCACHE_STORE_BY_VALUE_PROPERTY_KEY = "jcache.store.by.value.enable";
  private static final String JCACHE_STATISTICS_PROPERTY_KEY = "jcache.statistics.enable";
  private static final String JCACHE_MANAGEMENT_PROPERTY_KEY = "jcache.management.enable";
  private static final String JCACHE_CACHE_NAMESPACE_PROPERTY_KEY = "jcache.cache.namespace";
  private static final String JCACHE_EVICTION_POLICY_PROPERTY_KEY = "jcache.eviction.policy";
  private static final String JCACHE_EVICTION_MAX_SIZE_POLICY_PROPERTY_KEY = "jcache.eviction.max.size.policy";
  private static final String JCACHE_EVICTION_SIZE_PROPERTY_KEY = "jcache.eviction.size";
  private static final String JCACHE_EXPIRE_POLICY_PROPERTY_KEY = "jcache.expire.policy";
  private static final String JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY = "jcache.expire.policy.duration";
  private static final String JCACHE_ACCESSED_EXPIRY_IDENTIFIER = "ACCESSED";
  private static final String JCACHE_CREATED_EXPIRY_IDENTIFIER = "CREATED";
  private static final String JCACHE_MODIFIED_EXPIRY_IDENTIFIER = "MODIFIED";
  private static final String JCACHE_TOUCHED_EXPIRY_IDENTIFIER = "TOUCHED";
  private static final String HAZELCAST_CACHE_IN_MEMORY_FORMAT_PROPERTY_KEY = "jcache.cache.inmemory.format";
  private static final String HAZELCAST_CACHE_BINARY_IN_MEMORY_FORMAT_IDENTIFIER = "BINARY";
  private static final String HAZELCAST_CACHE_OBJECT_IN_MEMORY_FORMAT_IDENTIFIER = "OBJECT";
  private static final String HAZELCAST_CACHE_NATIVE_IN_MEMORY_FORMAT_IDENTIFIER = "NATIVE";
  private static final String JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY = "jcache.auto.create.cache";
  private static final String HAZELCAST_SERVER_CACHE_PROVIDER_IDENTIFIER = "Server";
  private static final Logger LOG = LoggerFactory.getLogger(JCacheStore.class);
  private ICache<K, T> cache;
  private CacheManager manager;
  private ConcurrentSkipListSet<K> cacheEntryList;
  private String goraCacheNamespace = GORA_DEFAULT_JCACHE_NAMESPACE;
  private DataStore<K, T> persistentDataStore;
  private CacheConfig<K, T> cacheConfig;
  private HazelcastInstance hazelcastInstance;

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
    if (fields != null && fields.length > 0) {
      for (String field : fields) {
        Schema.Field otherField = persitent.getSchema().getField(field);
        int index = otherField.pos();
        clonedPersistent.put(index, persitent.get(index));
      }
    } else {
      for (String field : otherFieldStrings) {
        Schema.Field otherField = persitent.getSchema().getField(field);
        int index = otherField.pos();
        clonedPersistent.put(index, persitent.get(index));
      }
    }
    return clonedPersistent;
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);
    CachingProvider cachingProvider = Caching.getCachingProvider
            (properties.getProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY));
    if (properties.getProperty(JCACHE_CACHE_NAMESPACE_PROPERTY_KEY) != null) {
      goraCacheNamespace = properties.getProperty(JCACHE_CACHE_NAMESPACE_PROPERTY_KEY);
    }
    try {
      this.persistentDataStore = DataStoreFactory.getDataStore(keyClass, persistentClass,
              new Configuration());
    } catch (GoraException ex) {
      LOG.error("Couldn't initialize persistent DataStore.", ex);
      throw ex;
    }
    if (properties.getProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY)
            .contains(HAZELCAST_SERVER_CACHE_PROVIDER_IDENTIFIER)) {
      Config config = new ClasspathXmlConfig(properties.getProperty(GORA_DEFAULT_JCACHE_HAZELCAST_CONFIG_KEY));
      hazelcastInstance = Hazelcast.newHazelcastInstance(config);
    } else {
      try {
        ClientConfig config =
                new XmlClientConfigBuilder(properties.getProperty(GORA_DEFAULT_JCACHE_HAZELCAST_CONFIG_KEY)).build();
        hazelcastInstance = HazelcastClient.newHazelcastClient(config);
      } catch (IOException ex) {
        LOG.error("Couldn't locate the client side cache provider configuration.", ex);
        throw new GoraException (ex);
      }
    }
    Properties providerProperties = new Properties();
    providerProperties.setProperty(HazelcastCachingProvider.HAZELCAST_INSTANCE_NAME,
            hazelcastInstance.getName());
    try {
      manager = cachingProvider.getCacheManager(new URI(goraCacheNamespace), null, providerProperties);
    } catch (URISyntaxException ex) {
      LOG.error("Couldn't initialize cache manager to bounded hazelcast instance.", ex);
      manager = cachingProvider.getCacheManager();
    }
    if (((properties.getProperty(JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY) != null) &&
            Boolean.valueOf(properties.getProperty(JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY)))
            || ((manager.getCache(super.getPersistentClass().getSimpleName(), keyClass, persistentClass) == null))) {
      cacheEntryList = new ConcurrentSkipListSet<>();
      cacheConfig = new CacheConfig<K, T>();
      cacheConfig.setTypes(keyClass, persistentClass);
      if (properties.getProperty(JCACHE_READ_THROUGH_PROPERTY_KEY) != null) {
        cacheConfig.setReadThrough(Boolean.valueOf(properties.getProperty(JCACHE_READ_THROUGH_PROPERTY_KEY)));
      } else {
        cacheConfig.setReadThrough(true);
      }
      if (properties.getProperty(JCACHE_WRITE_THROUGH_PROPERTY_KEY) != null) {
        cacheConfig.setWriteThrough(Boolean.valueOf(properties.getProperty(JCACHE_WRITE_THROUGH_PROPERTY_KEY)));
      } else {
        cacheConfig.setWriteThrough(true);
      }
      if (properties.getProperty(JCACHE_STORE_BY_VALUE_PROPERTY_KEY) != null) {
        cacheConfig.setStoreByValue(Boolean.valueOf(properties.getProperty(JCACHE_STORE_BY_VALUE_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_STATISTICS_PROPERTY_KEY) != null) {
        cacheConfig.setStatisticsEnabled(Boolean.valueOf(properties.getProperty(JCACHE_STATISTICS_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_MANAGEMENT_PROPERTY_KEY) != null) {
        cacheConfig.setStatisticsEnabled(Boolean.valueOf(properties.getProperty(JCACHE_MANAGEMENT_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_EVICTION_POLICY_PROPERTY_KEY) != null) {
        cacheConfig.getEvictionConfig()
                .setEvictionPolicy(EvictionPolicy.valueOf(properties.getProperty(JCACHE_EVICTION_POLICY_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_EVICTION_MAX_SIZE_POLICY_PROPERTY_KEY) != null) {
        cacheConfig.getEvictionConfig()
                .setMaximumSizePolicy(EvictionConfig.MaxSizePolicy
                        .valueOf(properties.getProperty(JCACHE_EVICTION_MAX_SIZE_POLICY_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_EVICTION_SIZE_PROPERTY_KEY) != null) {
        cacheConfig.getEvictionConfig()
                .setSize(Integer.valueOf(properties.getProperty(JCACHE_EVICTION_SIZE_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_EXPIRE_POLICY_PROPERTY_KEY) != null) {
        String expiryPolicyIdentifier = properties.getProperty(JCACHE_EXPIRE_POLICY_PROPERTY_KEY);
        if (expiryPolicyIdentifier.equals(JCACHE_ACCESSED_EXPIRY_IDENTIFIER)) {
          cacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new AccessedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_CREATED_EXPIRY_IDENTIFIER)) {
          cacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_MODIFIED_EXPIRY_IDENTIFIER)) {
          cacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new ModifiedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_TOUCHED_EXPIRY_IDENTIFIER)) {
          cacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new TouchedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        }
      }
      if (properties.getProperty(HAZELCAST_CACHE_IN_MEMORY_FORMAT_PROPERTY_KEY) != null) {
        String inMemoryFormat = properties.getProperty(HAZELCAST_CACHE_IN_MEMORY_FORMAT_PROPERTY_KEY);
        if (inMemoryFormat.equals(HAZELCAST_CACHE_BINARY_IN_MEMORY_FORMAT_IDENTIFIER) ||
                inMemoryFormat.equals(HAZELCAST_CACHE_OBJECT_IN_MEMORY_FORMAT_IDENTIFIER) ||
                inMemoryFormat.equals(HAZELCAST_CACHE_NATIVE_IN_MEMORY_FORMAT_IDENTIFIER)) {
          cacheConfig.setInMemoryFormat(InMemoryFormat.valueOf(inMemoryFormat));
        }
      }
      cacheConfig.setCacheLoaderFactory(JCacheCacheFactoryBuilder
              .factoryOfCacheLoader(this.persistentDataStore, keyClass, persistentClass));
      cacheConfig.setCacheWriterFactory(JCacheCacheFactoryBuilder
              .factoryOfCacheWriter(this.persistentDataStore, keyClass, persistentClass));
      cache = manager.createCache(persistentClass.getSimpleName(),
              cacheConfig).unwrap(ICache.class);
    } else {
      cache = manager.getCache(super.getPersistentClass().getSimpleName(),
              keyClass, persistentClass).unwrap(ICache.class);
      this.populateLocalCacheEntrySet(cache);
    }
    cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<>(
            JCacheCacheFactoryBuilder
                    .factoryOfEntryListener(new JCacheCacheEntryListener<K, T>(cacheEntryList)),
            null, true, true));
    LOG.info("JCache Gora datastore initialized successfully.");
  }

  @Override
  public String getSchemaName() {
    return super.persistentClass.getSimpleName();
  }

  @Override
  public void createSchema() throws GoraException {
    try {
      if (manager.getCache(super.getPersistentClass().getSimpleName(), keyClass, persistentClass) == null) {
        cacheEntryList.clear();
        cache = manager.createCache(persistentClass.getSimpleName(),
                cacheConfig).unwrap(ICache.class);
      }
      cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<>(
              JCacheCacheFactoryBuilder
                      .factoryOfEntryListener(new JCacheCacheEntryListener<K, T>(cacheEntryList)),
              null, true, true));
      persistentDataStore.createSchema();
      LOG.info("Created schema on persistent store and initialized cache for persistent bean {}."
              , super.getPersistentClass().getSimpleName());
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    try {
      cache.removeAll();
      manager.destroyCache(super.getPersistentClass().getSimpleName());
      persistentDataStore.deleteSchema();
      LOG.info("Deleted schema on persistent store and destroyed cache for persistent bean {}."
              , super.getPersistentClass().getSimpleName());
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    try {
      return (manager.getCache(super.getPersistentClass().getSimpleName(), keyClass, persistentClass) != null);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    try {
      T persitent = (T) cache.get(key);
      if (persitent == null) {
        return null;
      }
      return getPersistent(persitent, fields);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean exists(K key) throws GoraException {
    try {
      return cache.containsKey(key);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key) throws GoraException {
    try {
      return cache.get(key);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void put(K key, T val) throws GoraException {
    try {
      cache.put(key, val);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    try {
      return cache.remove(key);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
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
      LOG.info("JCache Gora datastore deleled {} rows from Persistent datastore.", deletedRows);
      return deletedRows;
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }    
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
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
    
    NavigableSet<K> cacheEntrySubList = null;
    if (startKey != null && endKey != null) {
      try {
        cacheEntrySubList =  cacheEntryList.subSet(startKey, true, endKey, true);
      } catch (Exception e) {
        throw new GoraException(e);
      }
    } else {
      // Empty
      cacheEntrySubList = Collections.emptyNavigableSet() ;
    }
    
    return new JCacheResult<>(this, query, cacheEntrySubList);
  }

  @Override
  public Query<K, T> newQuery() {
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
        partition.setConf(this.getConf());
        partitions.add(partition);
      }
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      LOG.error("Exception occurred while partitioning the query based on Hazelcast partitions.", ex);
      throw new IOException(ex.getMessage(), ex) ;
    }
    LOG.info("Query is partitioned to {} number of partitions.", partitions.size());
    return partitions;
  }

  @Override
  public void flush() throws GoraException {
    persistentDataStore.flush();
    LOG.info("JCache Gora datastore flushed successfully.");
  }

  @Override
  public void close() {
    try{
      flush();
    } catch (GoraException e) {
      LOG.error(e.getMessage(), e);
      if (e.getCause() != null) {
        LOG.error(e.getCause().getMessage());
      }
      // At this point, the exception is ignored...
    }
    cacheEntryList.clear();
    if (!cache.isDestroyed() && !manager.isClosed()) {
      cache.close();
    }
    if (!manager.isClosed()) {
      manager.close();
    }
    hazelcastInstance.shutdown();
    persistentDataStore.close();
    LOG.info("JCache Gora datastore destroyed successfully.");
  }

  private void populateLocalCacheEntrySet(ICache<K, T> cache) {
    cacheEntryList = new ConcurrentSkipListSet<>();
    Iterator<Cache.Entry<K, T>> cacheEntryIterator = cache.iterator();
    while (cacheEntryIterator.hasNext()) {
      cacheEntryList.add(cacheEntryIterator.next().getKey());
    }
    cacheConfig = cache.getConfiguration(CacheConfig.class);
    LOG.info("Populated local cache entry set with respect to remote cache provider.");
  }

}
