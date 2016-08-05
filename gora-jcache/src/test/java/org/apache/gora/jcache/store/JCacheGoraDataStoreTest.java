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

package org.apache.gora.jcache.store;

import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.gora.examples.WebPageDataCreator.SORTED_URLS;
import static org.apache.gora.examples.WebPageDataCreator.URLS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class JCacheGoraDataStoreTest extends DataStoreTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(JCacheGoraDataStoreTest.class);
  private static final int NUM_KEYS = 4;
  private Configuration conf = new Configuration();

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
    //mandatory to clean up hazelcast instances
    //this is not handled at super class level
    super.employeeStore.close();
    super.webPageStore.close();
  }

  @SuppressWarnings("unchecked")
  protected DataStore<String, Employee> createEmployeeDataStore() throws IOException {
    return DataStoreFactory.getDataStore(String.class, Employee.class, conf, true);
  }

  @SuppressWarnings("unchecked")
  protected DataStore<String, WebPage> createWebPageDataStore() throws IOException {
    return DataStoreFactory.getDataStore(String.class, WebPage.class, conf, true);
  }

  @Test
  public void testGetMissingValue() throws IOException {
    DataStore<String, WebPage> store = super.webPageStore;
    WebPage nullWebPage = store.get("missing", new String[0]);
    assertNull(nullWebPage);
  }

  @Test
  public void testDeleteSchema() throws Exception {
    DataStore<String, WebPage> store = super.webPageStore;
    assertTrue(store.schemaExists());
    store.deleteSchema();
    assertTrue(!store.schemaExists());
  }

  @Test
  public void testSchemaExists() throws Exception {
    DataStore<String, WebPage> store = super.webPageStore;
    assertTrue(store.schemaExists());
  }

  @Test
  public void testStorePutGet() throws Exception {
    String key = "org.apache.gora:http:/";
    DataStore<String, WebPage> store = super.webPageStore;
    assumeTrue(store.get(key, new String[0]) == null);
    store.put(key, WebPage.newBuilder().build());
    assertNotNull(store.get(key, new String[0]));
  }

  @Test
  public void testGetWithFields() throws Exception {

    DataStore<String, WebPage> store = super.webPageStore;
    BeanFactory<String, WebPage> beanFactory = new BeanFactoryImpl<>(String.class, WebPage.class);
    store.setBeanFactory(beanFactory);
    WebPageDataCreator.createWebPageData(store);
    String[] interestFields = new String[2];
    interestFields[0] = "url";
    interestFields[1] = "content";
    WebPage page = store.get(URLS[1], interestFields);
    assertNotNull(page);
    assertNotNull(page.getUrl());
    assertEquals(page.getUrl().toString(), URLS[1]);
    assertNotNull(page.getContent());
    assertEquals("Map of Outlinks should have a size of '0' as it is omitted at retrieval",
            0, page.getOutlinks().size());
    assertEquals("Map of Parsed Content should have a size of '0' as it is omitted at retrieval",
            0, page.getParsedContent().size());
  }

  @Test
  public void testDeleteByQueryFields() throws Exception {

    DataStore<String, WebPage> store = super.webPageStore;
    BeanFactory<String, WebPage> beanFactory = new BeanFactoryImpl<>(String.class, WebPage.class);
    store.setBeanFactory(beanFactory);
    Query<String, WebPage> query;
    WebPageDataCreator.createWebPageData(store);
    query = store.newQuery();
    query.setFields("outlinks", "parsedContent", "content");
    Query<String, WebPage> newQuery = store.newQuery();
    newQuery.setStartKey(SORTED_URLS[0]);
    newQuery.setEndKey(SORTED_URLS[9]);
    newQuery.setFields("outlinks", "parsedContent", "content");
    DataStoreTestUtil.assertNumResults(newQuery, URLS.length);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.flush();
    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);
    for (String SORTED_URL : SORTED_URLS) {
      WebPage page = store.get(SORTED_URL);
      assertNotNull(page);

      assertNotNull(page.getUrl());
      assertEquals(page.getUrl().toString(), SORTED_URL);
      assertEquals("Map of Outlinks should have a size of '0' as the deleteByQuery "
              + "not only removes the data but also the data structure.", 0, page.getOutlinks().size());
      assertEquals(0, page.getParsedContent().size());
      if (page.getContent() != null) {
        LOG.info("url:" + page.getUrl().toString());
        LOG.info("limit:" + page.getContent().limit());
      } else {
        assertNull(page.getContent());
      }
    }
    WebPageDataCreator.createWebPageData(store);
    query = store.newQuery();
    query.setFields("url");
    String startKey = SORTED_URLS[NUM_KEYS];
    String endKey = SORTED_URLS[SORTED_URLS.length - NUM_KEYS];
    query.setStartKey(startKey);
    query.setEndKey(endKey);
    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.flush();
    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);
    for (int i = 0; i < URLS.length; i++) {
      WebPage page = store.get(URLS[i]);
      assertNotNull(page);
      if (URLS[i].compareTo(startKey) < 0 || URLS[i].compareTo(endKey) > 0) {
        DataStoreTestUtil.assertWebPage(page, i);
      } else {
        assertNull(page.getUrl());
        assertNotNull(page.getOutlinks());
        assertNotNull(page.getParsedContent());
        assertNotNull(page.getContent());
        assertTrue(page.getOutlinks().size() > 0);
        assertTrue(page.getParsedContent().size() > 0);
      }
    }

  }
}
