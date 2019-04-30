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
package org.apache.gora.lucene.store;

import org.apache.gora.examples.WebPageDataCreator;
import static org.apache.gora.examples.WebPageDataCreator.SORTED_URLS;
import static org.apache.gora.examples.WebPageDataCreator.URLS;
import org.apache.gora.examples.generated.EmployeeInt;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreTestBase;
import static org.apache.gora.store.DataStoreTestBase.log;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.OperationNotSupportedException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestLuceneStore class executes tests for
 * {@link org.apache.gora.lucene.store.LuceneStore}
 */
public class TestLuceneStore extends DataStoreTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(DataStoreTestUtil.class);

  @BeforeClass
  public static void setUpClass() throws Exception {
    setTestDriver(new TestLuceneStoreDriver());
    DataStoreTestBase.setUpClass();
  }

  @Test(expected = AssertionError.class)
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }

  @Test(expected = OperationNotSupportedException.class)
  public void testGetPartitions() throws Exception {
    super.testGetPartitions();
  }

  @Ignore("3 types union field is not supported by LuceneStore.")
  @Override
  public void testGet3UnionField() throws Exception {
    //3 types union field is not supported by LuceneStore.
  }

  public static void createDummySimplifiedEmployees(DataStore<Integer, EmployeeInt> dataStore) throws GoraException {
    for (int i = 0; i < 10; i++) {
      EmployeeInt employee = EmployeeInt.newBuilder().build();
      employee.setSsn(i);
      dataStore.put(i, employee);
    }
  }

  @Test
  public void testInferDataType() throws GoraException {
    DataStore<Integer, EmployeeInt> employeeIntStore;
    employeeIntStore = testDriver.createDataStore(Integer.class, EmployeeInt.class);

    Query<Integer, EmployeeInt> query = employeeIntStore.newQuery();

    createDummySimplifiedEmployees(employeeIntStore);

    query.setStartKey(0);
    query.setEndKey(10);
    Result<Integer, EmployeeInt> results = query.execute();

    assertEquals(10, results.size());

  }

  @Test
  public void testDeleteByQueryFields() throws Exception {
    log.info("test method: testQueryByQueryFields");
    testDeleteByQueryFields(webPageStore);
  }

  private void testDeleteByQueryFields(DataStore<String, WebPage> store)
          throws Exception {
    int NUM_KEYS = 4;

    Query<String, WebPage> query;

    //test 5 - delete all with some fields
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields("outlinks",
            "parsedContent", "content");

    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);

    store.flush();

    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);

    //assert that data is deleted
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

    //test 6 - delete some with some fields
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields("url");
    String startKey = SORTED_URLS[NUM_KEYS];
    String endKey = SORTED_URLS[SORTED_URLS.length - NUM_KEYS];
    query.setStartKey(startKey);
    query.setEndKey(endKey);

    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);

    store.flush();

    DataStoreTestUtil.assertNumResults(store.newQuery(), URLS.length - 3);

    //assert that data is deleted
    for (int i = 0; i < URLS.length; i++) {
      WebPage page = store.get(URLS[i]);
      //assertNotNull(page);
      if (URLS[i].compareTo(startKey) < 0 || URLS[i].compareTo(endKey) > 0) {
        //not deleted
        DataStoreTestUtil.assertWebPage(page, i);
      } else {
        //deleted
        assertNull(page);
      }
    }
  }

}
