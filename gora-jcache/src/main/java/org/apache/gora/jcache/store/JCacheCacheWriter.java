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
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import java.util.Collection;
import java.util.Iterator;

public class JCacheCacheWriter<K, T extends PersistentBase> implements CacheWriter<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(JCacheCacheWriter.class);
  private DataStore<K, T> dataStore;

  public JCacheCacheWriter(Class<K> keyClass,
                           Class<T> persistent) throws GoraException {
    dataStore = DataStoreFactory.getDataStore(keyClass, persistent,
            new Configuration());
  }

  @Override
  public void write(Cache.Entry<? extends K,
          ? extends T> entry) throws CacheWriterException {
    dataStore.put(entry.getKey(), entry.getValue());
  }

  @Override
  public void writeAll(Collection<Cache.Entry<? extends K,
          ? extends T>> entries) throws CacheWriterException {
    Iterator<Cache.Entry<? extends K, ? extends T>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      write(iterator.next());
      iterator.remove();
    }
  }

  @Override
  public void delete(Object key) throws CacheWriterException {
    dataStore.delete((K) key);
  }

  @Override
  public void deleteAll(Collection<?> keys) throws CacheWriterException {
    Iterator<?> iterator = keys.iterator();
    while (iterator.hasNext()) {
      delete(iterator.next());
      iterator.remove();
    }
  }

}
