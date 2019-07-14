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

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.configuration.Factory;

/**
 * {@link org.apache.gora.jcache.store.JCacheCacheLoaderFactory} is the primary class
 * responsible for creating cache loader {@link javax.cache.integration.CacheLoader} instances which itself
 * loads data beans from persistency dataStore to in memory cache.
 */
public class JCacheCacheLoaderFactory<K, T extends PersistentBase>
        implements Factory<JCacheCacheLoader<K, T>> {

  public static final long serialVersionUID = 201305101626L;
  private static final Logger LOG = LoggerFactory.getLogger(JCacheCacheLoaderFactory.class);
  private transient JCacheCacheLoader<K, T> instance;
  private Class<K> keyClass;
  private Class<T> persistentClass;

  public JCacheCacheLoaderFactory(JCacheCacheLoader<K, T> instance,
                                  Class<K> keyClass,
                                  Class<T> persistentClass) {
    this.keyClass = keyClass;
    this.persistentClass = persistentClass;
    LOG.info("JCache cache entry loader factory initialized successfully.");
    this.instance = instance;
  }

  public JCacheCacheLoader<K, T> create() {
    if (this.instance != null) {
      return (JCacheCacheLoader<K, T>) this.instance;
    } else {
      try {
        this.instance = new JCacheCacheLoader<>(DataStoreFactory
                .getDataStore(keyClass, persistentClass, new Configuration()));
      } catch (GoraException ex) {
        LOG.error("Couldn't initialize persistent dataStore for cache loader.", ex);
        return null;
      }
      return (JCacheCacheLoader<K, T>) this.instance;
    }
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return true;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return this.hashCode();
  }

}