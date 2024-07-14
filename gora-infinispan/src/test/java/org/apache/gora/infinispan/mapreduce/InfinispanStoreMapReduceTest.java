/*
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
package org.apache.gora.infinispan.mapreduce;

import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.infinispan.GoraInfinispanTestDriver;
import org.apache.gora.infinispan.store.InfinispanStore;
import org.apache.gora.io.serializer.DataStoreMapReduceTestBase;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.gora.infinispan.store.InfinispanClient.ISPN_CONNECTION_STRING_KEY;

/**
 * @author Pierre Sutra
 */
public class InfinispanStoreMapReduceTest extends DataStoreMapReduceTestBase {

  private GoraInfinispanTestDriver driver;
  private Configuration conf;

  public InfinispanStoreMapReduceTest() throws IOException {
    super();
    List<String> cacheNames = new ArrayList<>();
    cacheNames.add(WebPage.class.getSimpleName());
    driver = new GoraInfinispanTestDriver(3, cacheNames);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    driver.setUpClass();
    super.setUp();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    driver.tearDownClass();
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() throws IOException {
    conf = driver.getConfiguration();
    conf.set(ISPN_CONNECTION_STRING_KEY,driver.connectionString());
    try {
      InfinispanStore<String,WebPage> store = new InfinispanStore<>();
      store.setConf(conf);
      store.initialize(String.class, WebPage.class, new Properties());
      return store;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
