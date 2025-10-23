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

package org.apache.gora.hive.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.avro.util.Utf8;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.hive.GoraHiveTestDriver;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.StringUtils;
import org.apache.metamodel.query.parser.QueryParserException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * HiveStore Tests extending {@link DataStoreTestBase} which run the base JUnit test suite for
 * Gora.
 */
public class TestHiveStore extends DataStoreTestBase {

  static {
    try {
      setTestDriver(new GoraHiveTestDriver());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    assertTrue(employeeStore.schemaExists());
  }

  private void awaitWebPageSchema(String key) throws Exception {
    // wait until Hive exposes the schema and, if provided, the specific record
    for (int attempt = 0; attempt < 100; attempt++) {
      webPageStore.flush();
      if (!webPageStore.schemaExists()) {
        Thread.sleep(100L);
        continue;
      }
      if (key == null) return;
      try {
        // read the key to confirm parser metadata is in sync
        webPageStore.get(key, new String[] {"url"});
        return;
      } catch (QueryParserException e) {
        // recreate the store when Hive's parser cache is stale
        webPageStore.close();
        webPageStore = testDriver.createDataStore(String.class, WebPage.class);
      }
    }
    fail("Hive web page schema or record was not visible");
  }

  private void populateWebPages() throws Exception {
    // load deterministic WebPage fixtures and ensure their visibility
    webPageStore.createSchema();
    awaitWebPageSchema(null);
    WebPageDataCreator.createWebPageData(webPageStore);
    for (String url : WebPageDataCreator.URLS) {
      // block until each inserted record is queryable
      awaitWebPageSchema(url);
    }
  }

  private List<String> sortedWebPageUrls() {
    // copy and sort the static URL set to enforce deterministic order
    List<String> sorted = new ArrayList<>(Arrays.asList(WebPageDataCreator.URLS));
    Collections.sort(sorted);
    return sorted;
  }

  private void assertKeyRange(boolean setStartKey, boolean setEndKey) throws Exception {
    // verify range queries across all start/end key combinations
    populateWebPages();
    List<String> urls = sortedWebPageUrls();
    int n = urls.size();

    for (int i = 0, iLimit = setStartKey ? n : 1; i < iLimit; i++) {
      // derive the set of end indices allowed for the current start index
      int jStart = setEndKey ? i : n - 1;
      int jLimit = setEndKey ? n : jStart + 1;
      for (int j = jStart; j < jLimit; j++) {
        Query<String, WebPage> query = webPageStore.newQuery();
        if (setStartKey) query.setStartKey(urls.get(i));
        if (setEndKey)   query.setEndKey(urls.get(j));

        Result<String, WebPage> result = query.execute();
        int actual = 0;
        while (result.next()) {
          WebPage page = result.get();
          // enforce that each returned record matches the fixture expectations
          DataStoreTestUtil.assertWebPage(
              page, WebPageDataCreator.URL_INDEXES.get(page.getUrl().toString()));
          actual++;
        }
        result.close();

        // compare the observed result size with the analytical expectation
        int expected = (setEndKey ? j + 1 : n) - (setStartKey ? i : 0);
        assertEquals(expected, actual);
      }
    }
  }

  private void assertSingleKeyQuery(String[] fields) throws Exception {
    // run single-key lookups for each URL and validate the returned page
    populateWebPages();
    for (int i = 0; i < WebPageDataCreator.URLS.length; i++) {
      Query<String, WebPage> query = webPageStore.newQuery();
      query.setKey(WebPageDataCreator.URLS[i]);
      query.setFields(fields);
      Result<String, WebPage> result = query.execute();
      assertTrue(result.next());
      WebPage page = result.get();
      // the content must match the deterministic fixture by index
      DataStoreTestUtil.assertWebPage(page, i);
      assertFalse(result.next());
      result.close();
    }
  }

  @Override
  public void testQuery() throws Exception {
    assertKeyRange(false, false);
  }

  @Override
  public void testQueryEndKey() throws Exception {
    assertKeyRange(false, true);
  }

  @Override
  public void testQueryKeyRange() throws Exception {
    assertKeyRange(true, true);
  }

  @Override
  public void testQueryStartKey() throws Exception {
    assertKeyRange(true, false);
  }

  @Override
  public void testQueryWebPageQueryEmptyResults() throws Exception {
    populateWebPages();
    Query<String, WebPage> query = webPageStore.newQuery();
    query.setStartKey("aa");
    query.setEndKey("ab");
    DataStoreTestUtil.assertEmptyResults(query);

    query = webPageStore.newQuery();
    query.setKey("aa");
    DataStoreTestUtil.assertEmptyResults(query);
  }

  @Override
  public void testQueryWebPageSingleKey() throws Exception {
    assertSingleKeyQuery(((HiveStore<String, WebPage>) webPageStore).getFields());
  }

  @Override
  public void testQueryWebPageSingleKeyDefaultFields() throws Exception {
    assertSingleKeyQuery(null);
  }

  @Override
  public void assertPut(Employee employee) throws GoraException {
    employeeStore.put(employee.getSsn().toString(), employee);
  }

  @Override
  public void testGetWithFields() throws Exception {
    //Overrides DataStoreTestBase.testGetWithFields to avoid recursive field "boss"
    Employee employee = DataStoreTestUtil.createEmployee();
    WebPage webpage = DataStoreTestUtil.createWebPage();
    employee.setWebpage(webpage);
    String ssn = employee.getSsn().toString();
    employeeStore.put(ssn, employee);
    employeeStore.flush();

    String[] fields = ((HiveStore<String, Employee>) employeeStore).getFields();
    for (Set<String> subset : StringUtils.powerset(fields)) {
      if (subset.isEmpty()) {
        continue;
      }
      Employee after = employeeStore.get(ssn, subset.toArray(new String[subset.size()]));
      Employee expected = Employee.newBuilder().build();
      for (String field : subset) {
        int index = expected.getSchema().getField(field).pos();
        expected.put(index, employee.get(index));
      }

      DataStoreTestUtil.assertEqualEmployeeObjects(expected, after);
    }
  }

  @Override
  public void testGet() throws Exception {
    //Overrides DataStoreTestBase.testGet to avoid recursive field "boss"
    log.info("test method: testGet");
    employeeStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee();
    String ssn = employee.getSsn().toString();
    employeeStore.put(ssn, employee);
    employeeStore.flush();
    Employee after = employeeStore.get(ssn, null);
    DataStoreTestUtil.assertEqualEmployeeObjects(employee, after);
  }

  @Override
  public void testGetNested() throws Exception {
    //Overrides DataStoreTestBase.testGetNested to avoid recursive field "boss"
    Employee employee = DataStoreTestUtil.createEmployee();

    WebPage webpage = new BeanFactoryImpl<>(String.class, WebPage.class).newPersistent();
    webpage.setUrl(new Utf8("url.."));
    webpage.setContent(ByteBuffer.wrap("test content".getBytes(Charset.defaultCharset())));
    webpage.setParsedContent(new ArrayList<>());

    Metadata metadata = new BeanFactoryImpl<>(String.class, Metadata.class).newPersistent();
    webpage.setMetadata(metadata);
    employee.setWebpage(webpage);
    String ssn = employee.getSsn().toString();

    employeeStore.put(ssn, employee);
    employeeStore.flush();
    Employee after = employeeStore.get(ssn, null);
    DataStoreTestUtil.assertEqualEmployeeObjects(employee, after);
    DataStoreTestUtil.assertEqualWebPageObjects(webpage, after.getWebpage());
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testExists() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDelete() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDeleteByQuery() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDeleteByQueryFields() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testUpdate() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive datastore doesn't support recursive records")
  @Override
  public void testGetRecursive() throws Exception {
    //Hive datastore doesn't support recursive records
  }

  @Ignore("Hive datastore doesn't support recursive records")
  @Override
  public void testGetDoubleRecursive() throws Exception {
    //Hive datastore doesn't support recursive records
  }

  @Ignore("As recursive records are not supported, employee.boss field cannot be processed.")
  @Override
  public void testGet3UnionField() throws Exception {
    //As recursive records are not supported, employee.boss field cannot be processed.
  }
}
