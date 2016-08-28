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

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.jcache.store.JCacheStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoraHazelcastTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(GoraHazelcastTestDriver.class);
  private static final String CONFIG = "hazelcast.xml";
  private HazelcastInstance hazelcastInstance;

  public GoraHazelcastTestDriver() {
    super(JCacheStore.class);
  }

  @Override
  public void setUpClass() {
    log.info("Starting Hazelcast server side cache provider.");
    Config config = new ClasspathXmlConfig(CONFIG);
    hazelcastInstance = Hazelcast.newHazelcastInstance(config);
  }

  @Override
  public void tearDownClass() {
    log.info("Stopping Hazelcast server side cache provider.");
    hazelcastInstance.shutdown();
  }

  @Override
  public <K, T extends Persistent> DataStore<K, T>
  createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    JCacheStore store = (JCacheStore) super.createDataStore(keyClass, persistentClass);
    return store;
  }

}
