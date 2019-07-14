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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@link org.apache.gora.jcache.store.JCacheCacheEntryListener} is the primary class
 * responsible for listening on {@link javax.cache.event.CacheEntryEvent} cache entry events
 * EG:- Creation, Removal, Expiry etc of entries on caches and trigger actions as specified.
 */
public class JCacheCacheEntryListener<K, T extends PersistentBase>
        implements CacheEntryCreatedListener<K, T>,
        CacheEntryRemovedListener<K, T>, CacheEntryUpdatedListener<K, T>,
        CacheEntryExpiredListener<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(JCacheCacheEntryListener.class);
  private ConcurrentSkipListSet<K> cacheEntryList;

  public JCacheCacheEntryListener(ConcurrentSkipListSet cacheEntryList) {
    this.cacheEntryList = cacheEntryList;
  }

  @Override
  public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      cacheEntryList.add(event.getKey());
      LOG.info("Cache entry added on key {}.", event.getKey().toString());
    }
  }

  @Override
  public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      cacheEntryList.remove(event.getKey());
      LOG.info("Cache entry removed on key {}.", event.getKey().toString());
    }
  }

  @Override
  public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      LOG.info("Cache entry updated set on key {}.", event.getKey().toString());
    }
  }

  @Override
  public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      LOG.warn("Cache entry expired on key {}.", event.getKey().toString());
    }
  }

}
