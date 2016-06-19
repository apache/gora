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

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import java.util.concurrent.ConcurrentSkipListSet;

public class JCacheCacheEntryListener<K, T extends PersistentBase>
        implements CacheEntryCreatedListener<K, T>,
        CacheEntryRemovedListener<K, T> {

  private ConcurrentSkipListSet<K> cacheEntryList;

  public JCacheCacheEntryListener(ConcurrentSkipListSet cacheEntryList) {
    this.cacheEntryList = cacheEntryList;
  }

  @Override
  public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      cacheEntryList.add(event.getKey());
    }
  }

  @Override
  public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends T>> cacheEntryEvents)
          throws CacheEntryListenerException {
    for (CacheEntryEvent<? extends K, ? extends T> event : cacheEntryEvents) {
      cacheEntryList.remove(event.getKey());
    }
  }
}
