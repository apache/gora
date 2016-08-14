/**
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

package org.apache.gora.jcache;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.jcache.store.JCacheStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class GoraHazelcastTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(GoraHazelcastTestDriver.class);
  private JCacheStore<String, WebPage> serverCacheProvider;
  private static final String GORA_DEFAULT_JCACHE_PROVIDER_KEY = "gora.datastore.jcache.provider";
  private static final String PROVIDER = "com.hazelcast.cache.impl.HazelcastServerCachingProvider";
  private static final String GORA_DEFAULT_JCACHE_HAZELCAST_CONFIG_KEY = "gora.datastore.jcache.hazelcast.config";
  private static final String CONFIG = "hazelcast.xml";
  public static final String GORA_DEFAULT_DATASTORE_KEY = "gora.datastore.default";
  public static final String MEMSTORE = "org.apache.gora.memory.store.MemStore";
  private static final String JCACHE_READ_THROUGH_PROPERTY_KEY = "jcache.read.through.enable";
  private static final String JCACHE_WRITE_THROUGH_PROPERTY_KEY = "jcache.write.through.enable";
  private static final String FALSE = "false";

  public GoraHazelcastTestDriver() {
    super(JCacheStore.class);
  }

  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Starting Hazelcast server side cache provider.");
    Properties properties = new Properties();
    properties.setProperty(GORA_DEFAULT_JCACHE_PROVIDER_KEY, PROVIDER);
    properties.setProperty(GORA_DEFAULT_JCACHE_HAZELCAST_CONFIG_KEY, CONFIG);
    properties.setProperty(GORA_DEFAULT_DATASTORE_KEY, MEMSTORE);
    properties.setProperty(JCACHE_READ_THROUGH_PROPERTY_KEY, FALSE);
    properties.setProperty(JCACHE_WRITE_THROUGH_PROPERTY_KEY, FALSE);
    serverCacheProvider = new JCacheStore();
    serverCacheProvider.initialize(String.class, WebPage.class, properties);
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Stopping Hazelcast server side cache provider.");
    serverCacheProvider.close();
  }

  @Override
  public <K, T extends Persistent> DataStore<K, T>
  createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    JCacheStore store = (JCacheStore) super.createDataStore(keyClass, persistentClass);
    return store;
  }

}
