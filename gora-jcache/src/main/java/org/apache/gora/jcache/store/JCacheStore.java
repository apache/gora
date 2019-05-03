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
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
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

/**
 * {@link org.apache.gora.jcache.store.JCacheStore} is the primary class
 * responsible for GORA CRUD operations on Hazelcast Caches. This class can be think
 * of as caching layer that can is wrapped over any persistency dataStore implementations
 * which extends {@link org.apache.gora.store.DataStore}.  This class delegates
 * most operations to it s persistency dataStore. Hazelcast cache implementation is based on
 * JCache JSR 107 specification.
 */
public class JCacheStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final String GORA_DEFAULT_JCACHE_PROVIDER_KEY = "gora.datastore.jcache.provider";
  private static final String JCACHE_READ_THROUGH_PROPERTY_KEY = "jcache.read.through.enable";
  private static final String JCACHE_WRITE_THROUGH_PROPERTY_KEY = "jcache.write.through.enable";
  private static final String JCACHE_STORE_BY_VALUE_PROPERTY_KEY = "jcache.store.by.value.enable";
  private static final String JCACHE_STATISTICS_PROPERTY_KEY = "jcache.statistics.enable";
  private static final String JCACHE_MANAGEMENT_PROPERTY_KEY = "jcache.management.enable";
  private static final String JCACHE_EXPIRE_POLICY_PROPERTY_KEY = "jcache.expire.policy";
  private static final String JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY = "jcache.expire.policy.duration";
  private static final String JCACHE_ACCESSED_EXPIRY_IDENTIFIER = "ACCESSED";
  private static final String JCACHE_CREATED_EXPIRY_IDENTIFIER = "CREATED";
  private static final String JCACHE_MODIFIED_EXPIRY_IDENTIFIER = "MODIFIED";
  private static final String JCACHE_TOUCHED_EXPIRY_IDENTIFIER = "TOUCHED";
  private static final String JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY = "jcache.auto.create.cache";
  private static final Logger LOG = LoggerFactory.getLogger(JCacheStore.class);
  private Cache<K, T> cache;
  private CacheManager manager;
  private CachingProvider cachingProvider;
  private ConcurrentSkipListSet<K> cacheEntryList;
  private DataStore<K, T> persistentDataStore;
  private CompleteConfiguration<K, T> cacheConfig;

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
    String cachingProviderKey = properties.getProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY);
    if (cachingProviderKey != null) {
      cachingProvider = Caching.getCachingProvider(cachingProviderKey);
    } else {
      cachingProvider = Caching.getCachingProvider();
    }

    try {
      this.persistentDataStore = DataStoreFactory.getDataStore(keyClass, persistentClass,
              new Configuration());
    } catch (GoraException ex) {
      LOG.error("Couldn't initialize persistent DataStore.", ex);
      throw ex;
    }

    manager = cachingProvider.getCacheManager();

    if (((properties.getProperty(JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY) != null) &&
            Boolean.valueOf(properties.getProperty(JCACHE_AUTO_CREATE_CACHE_PROPERTY_KEY)))
            || ((manager.getCache(super.getPersistentClass().getSimpleName(), keyClass, persistentClass) == null))) {
      cacheEntryList = new ConcurrentSkipListSet<>();
      MutableConfiguration mutableCacheConfig = new MutableConfiguration<>();
      mutableCacheConfig.setTypes(keyClass, persistentClass);
      if (properties.getProperty(JCACHE_READ_THROUGH_PROPERTY_KEY) != null) {
        mutableCacheConfig.setReadThrough(Boolean.valueOf(properties.getProperty(JCACHE_READ_THROUGH_PROPERTY_KEY)));
      } else {
        mutableCacheConfig.setReadThrough(true);
      }
      if (properties.getProperty(JCACHE_WRITE_THROUGH_PROPERTY_KEY) != null) {
        mutableCacheConfig.setWriteThrough(Boolean.valueOf(properties.getProperty(JCACHE_WRITE_THROUGH_PROPERTY_KEY)));
      } else {
        mutableCacheConfig.setWriteThrough(true);
      }
      if (properties.getProperty(JCACHE_STORE_BY_VALUE_PROPERTY_KEY) != null) {
        mutableCacheConfig.setStoreByValue(Boolean.valueOf(properties.getProperty(JCACHE_STORE_BY_VALUE_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_STATISTICS_PROPERTY_KEY) != null) {
        mutableCacheConfig.setStatisticsEnabled(Boolean.valueOf(properties.getProperty(JCACHE_STATISTICS_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_MANAGEMENT_PROPERTY_KEY) != null) {
        mutableCacheConfig.setStatisticsEnabled(Boolean.valueOf(properties.getProperty(JCACHE_MANAGEMENT_PROPERTY_KEY)));
      }
      if (properties.getProperty(JCACHE_EXPIRE_POLICY_PROPERTY_KEY) != null) {
        String expiryPolicyIdentifier = properties.getProperty(JCACHE_EXPIRE_POLICY_PROPERTY_KEY);
        if (expiryPolicyIdentifier.equals(JCACHE_ACCESSED_EXPIRY_IDENTIFIER)) {
          mutableCacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new AccessedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_CREATED_EXPIRY_IDENTIFIER)) {
          mutableCacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_MODIFIED_EXPIRY_IDENTIFIER)) {
          mutableCacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new ModifiedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        } else if (expiryPolicyIdentifier.equals(JCACHE_TOUCHED_EXPIRY_IDENTIFIER)) {
          mutableCacheConfig.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
                  new TouchedExpiryPolicy(new Duration(TimeUnit.SECONDS,
                          Integer.valueOf(properties.getProperty(JCACHE_EXPIRE_POLICY_DURATION_PROPERTY_KEY))))
          ));
        }
      }
      mutableCacheConfig.setCacheLoaderFactory(JCacheCacheFactoryBuilder
              .factoryOfCacheLoader(this.persistentDataStore, keyClass, persistentClass));
      mutableCacheConfig.setCacheWriterFactory(JCacheCacheFactoryBuilder
              .factoryOfCacheWriter(this.persistentDataStore, keyClass, persistentClass));
      cache = manager.createCache(persistentClass.getSimpleName(),
              mutableCacheConfig);
      cacheConfig = mutableCacheConfig;
    } else {
      cache = manager.getCache(super.getPersistentClass().getSimpleName(),
              keyClass, persistentClass);
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
                cacheConfig);
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
    // JCache api does not expose sufficient enough information to exploit data locality.
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query);
    partitionQuery.setConf(this.getConf());
    partitions.add(partitionQuery);
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
    if (!cache.isClosed() && !manager.isClosed()) {
      cache.close();
    }
    if (!manager.isClosed()) {
      manager.close();
    }
    cachingProvider.close();
    persistentDataStore.close();
    LOG.info("JCache Gora datastore destroyed successfully.");
  }

  private void populateLocalCacheEntrySet(Cache<K, T> cache) {
    cacheEntryList = new ConcurrentSkipListSet<>();
    Iterator<Cache.Entry<K, T>> cacheEntryIterator = cache.iterator();
    while (cacheEntryIterator.hasNext()) {
      cacheEntryList.add(cacheEntryIterator.next().getKey());
    }
    cacheConfig = cache.getConfiguration(CompleteConfiguration.class);
    LOG.info("Populated local cache entry set with respect to remote cache provider.");
  }

}
