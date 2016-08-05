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
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import java.util.HashMap;
import java.util.Map;


public class JCacheCacheLoader<K, T extends PersistentBase> implements CacheLoader<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(JCacheCacheLoader.class);
  private DataStore<K, T> dataStore;

  public JCacheCacheLoader(DataStore<K, T> dataStore) {
      this.dataStore = dataStore;
  }

  @Override
  public T load(K key) throws CacheLoaderException {
    T persistent = null;
    try {
      persistent = dataStore.get(key);
      LOG.info("Loaded data bean from persistent datastore on key " + key.toString());
    } catch (CacheLoaderException ex) {
      throw ex;
    }
    return persistent;
  }

  @Override
  public Map<K, T> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
    Map<K, T> loaded = new HashMap<K, T>();
    for (K key : keys) {
      T persistent = dataStore.get(key);
      LOG.info("Loaded data bean from persistent datastore on key " + key.toString());
      if (persistent != null) {
        loaded.put(key, persistent);
      }
    }
    return loaded;
  }

}
