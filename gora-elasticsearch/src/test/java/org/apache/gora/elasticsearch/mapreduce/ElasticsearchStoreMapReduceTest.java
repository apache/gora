/*
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
package org.apache.gora.elasticsearch.mapreduce;

import org.apache.gora.elasticsearch.GoraElasticsearchTestDriver;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.io.serializer.DataStoreMapReduceTestBase;
import org.apache.gora.io.serializer.MapReduceTestUtils;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Executes tests for MR jobs over Elasticsearch dataStore.
 */
public class ElasticsearchStoreMapReduceTest extends DataStoreMapReduceTestBase {

  private GoraElasticsearchTestDriver driver;

  public ElasticsearchStoreMapReduceTest() throws IOException {
    super();
    driver = new GoraElasticsearchTestDriver();
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
    return DataStoreFactory.getDataStore(String.class, WebPage.class, driver.getConfiguration());
  }

  @Test
  @Override
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(createWebPageDataStore(), driver.getConfiguration());
  }
}
