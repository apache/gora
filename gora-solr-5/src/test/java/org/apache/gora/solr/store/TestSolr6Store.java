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
package org.apache.gora.solr.store;

import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.solr.GoraSolr6TestDriver;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static org.apache.gora.examples.WebPageDataCreator.SORTED_URLS;
import static org.apache.gora.examples.WebPageDataCreator.URLS;
import static org.apache.gora.store.DataStoreTestUtil.assertNumResults;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestSolr6Store extends DataStoreTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(TestSolr6Store.class);
  private static final int NUM_KEYS = 4;
  
  static {
    setTestDriver(new GoraSolr6TestDriver());
  }

  @Override
  protected DataStore<String, Employee> createEmployeeDataStore()
      throws IOException {
    SolrStore<String, Employee> store = new SolrStore<>();
    store.initialize(String.class, Employee.class, DataStoreFactory.createProps());
    return store;
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore()
      throws IOException {
    SolrStore<String, WebPage> store = new SolrStore<>();
    store.initialize(String.class, WebPage.class, DataStoreFactory.createProps());
    return store;
  }

  @Override
  public void testDeleteByQueryFields()
          throws Exception {
    Query<String, WebPage> query;

    //test 5 - delete all with some fields
    WebPageDataCreator.createWebPageData(this.webPageStore);

    query = this.webPageStore.newQuery();
    query.setFields("outlinks"
            , "parsedContent", "content");

    assertNumResults(this.webPageStore.newQuery(), URLS.length);
    this.webPageStore.deleteByQuery(query);

    this.webPageStore.flush();

    assertNumResults(this.webPageStore.newQuery(), URLS.length);

    //assert that data is deleted
    for (String SORTED_URL : SORTED_URLS) {
      WebPage page = this.webPageStore.get(SORTED_URL);
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

    //test 6 - delete some with some fields
    WebPageDataCreator.createWebPageData(this.webPageStore);

    query = this.webPageStore.newQuery();
    query.setFields("url");
    String startKey = SORTED_URLS[NUM_KEYS];
    String endKey = SORTED_URLS[SORTED_URLS.length - NUM_KEYS];
    query.setStartKey(startKey);
    query.setEndKey(endKey);

    assertNumResults(this.webPageStore.newQuery(), URLS.length);
    this.webPageStore.deleteByQuery(query);

    this.webPageStore.flush();

    assertNumResults(query,0);

  }
}
