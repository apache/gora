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

package org.apache.gora.store;

import static org.apache.gora.examples.WebPageDataCreator.ANCHORS;
import static org.apache.gora.examples.WebPageDataCreator.CONTENTS;
import static org.apache.gora.examples.WebPageDataCreator.LINKS;
import static org.apache.gora.examples.WebPageDataCreator.SORTED_URLS;
import static org.apache.gora.examples.WebPageDataCreator.URLS;
import static org.apache.gora.examples.WebPageDataCreator.URL_INDEXES;
import static org.apache.gora.examples.WebPageDataCreator.createWebPageData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.ByteUtils;
import org.apache.gora.util.StringUtils;

/**
 * Test utilities for DataStores. This utility class provides everything
 * necessary for convenience tests in {@link DataStoreTestBase} to execute cleanly.
 * The tests begin in a fairly trivial fashion getting progressively 
 * more complex as we begin testing some more advanced features within the 
 * Gora API. In addition to this class, the first place to look API
 * functionality is at the examples directories under various Gora modules. 
 * All the modules have a <gora-module>/src/examples/ directory under 
 * which some example classes can be found. Especially, there are some 
 * classes that are used for tests under <gora-core>/src/examples/
 */
public class DataStoreTestUtil {

  public static final long YEAR_IN_MS = 365L * 24L * 60L * 60L * 1000L;
  private static final int NUM_KEYS = 4;

  public static <K, T extends Persistent> void testNewPersistent(
      DataStore<K,T> dataStore) throws IOException, Exception {

    T obj1 = dataStore.newPersistent();
    T obj2 = dataStore.newPersistent();

    Assert.assertEquals(dataStore.getPersistentClass(),
        obj1.getClass());
    Assert.assertNotNull(obj1);
    Assert.assertNotNull(obj2);
    Assert.assertFalse( obj1 == obj2 );
  }

  public static <K> Employee createEmployee(
      DataStore<K, Employee> dataStore) throws IOException, Exception {

    Employee employee = dataStore.newPersistent();
    employee.setName(new Utf8("Random Joe"));
    employee.setDateOfBirth( System.currentTimeMillis() - 20L *  YEAR_IN_MS );
    employee.setSalary(100000);
    employee.setSsn(new Utf8("101010101010"));
    return employee;
  }

  public static void testAutoCreateSchema(DataStore<String,Employee> dataStore)
  throws IOException, Exception {
    //should not throw exception
    dataStore.put("foo", createEmployee(dataStore));
  }

  public static void testCreateEmployeeSchema(DataStore<String, Employee> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();

    //should not throw exception
    dataStore.createSchema();
  }

  public static void testTruncateSchema(DataStore<String, WebPage> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();
    WebPageDataCreator.createWebPageData(dataStore);
    dataStore.truncateSchema();

    assertEmptyResults(dataStore.newQuery());
  }

  public static void testDeleteSchema(DataStore<String, WebPage> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();
    WebPageDataCreator.createWebPageData(dataStore);
    dataStore.deleteSchema();
    dataStore.createSchema();

    assertEmptyResults(dataStore.newQuery());
  }

  public static<K, T extends Persistent> void testSchemaExists(
      DataStore<K, T> dataStore) throws IOException, Exception {
    dataStore.createSchema();

    Assert.assertTrue(dataStore.schemaExists());

    dataStore.deleteSchema();
    Assert.assertFalse(dataStore.schemaExists());
  }

  public static void testGetEmployee(DataStore<String, Employee> dataStore)
    throws IOException, Exception {
    dataStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee(dataStore);
    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();

    Employee after = dataStore.get(ssn, Employee._ALL_FIELDS);

    Assert.assertEquals(employee, after);
  }

  public static void testGetEmployeeNonExisting(DataStore<String, Employee> dataStore)
    throws IOException, Exception {
    Employee employee = dataStore.get("_NON_EXISTING_SSN_FOR_EMPLOYEE_");
    Assert.assertNull(employee);
  }

  public static void testGetEmployeeWithFields(DataStore<String, Employee> dataStore)
    throws IOException, Exception {
    Employee employee = DataStoreTestUtil.createEmployee(dataStore);
    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();

    String[] fields = employee.getFields();
    for(Set<String> subset : StringUtils.powerset(fields)) {
      if(subset.isEmpty())
        continue;
      Employee after = dataStore.get(ssn, subset.toArray(new String[subset.size()]));
      Employee expected = new Employee();
      for(String field:subset) {
        int index = expected.getFieldIndex(field);
        expected.put(index, employee.get(index));
      }

      Assert.assertEquals(expected, after);
    }
  }

  public static Employee testPutEmployee(DataStore<String, Employee> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee(dataStore);
    return employee;
  }

  public static void testEmptyUpdateEmployee(DataStore<String, Employee> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();
    long ssn = 1234567890L;
    String ssnStr = Long.toString(ssn);
    long now = System.currentTimeMillis();

    Employee employee = dataStore.newPersistent();
    employee.setName(new Utf8("John Doe"));
    employee.setDateOfBirth(now - 20L *  YEAR_IN_MS);
    employee.setSalary(100000);
    employee.setSsn(new Utf8(ssnStr));
    dataStore.put(employee.getSsn().toString(), employee);

    dataStore.flush();

    employee = dataStore.get(ssnStr);
    dataStore.put(ssnStr, employee);

    dataStore.flush();

    employee = dataStore.newPersistent();
    dataStore.put(Long.toString(ssn + 1), employee);

    dataStore.flush();

    employee = dataStore.get(Long.toString(ssn + 1));
    Assert.assertNull(employee);
  }

  public static void testUpdateEmployee(DataStore<String, Employee> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();
    long ssn = 1234567890L;
    long now = System.currentTimeMillis();

    for (int i = 0; i < 5; i++) {
      Employee employee = dataStore.newPersistent();
      employee.setName(new Utf8("John Doe " + i));
      employee.setDateOfBirth(now - 20L *  YEAR_IN_MS);
      employee.setSalary(100000);
      employee.setSsn(new Utf8(Long.toString(ssn + i)));
      dataStore.put(employee.getSsn().toString(), employee);
    }

    dataStore.flush();

    for (int i = 0; i < 1; i++) {
      Employee employee = dataStore.newPersistent();
      employee.setName(new Utf8("John Doe " + (i + 5)));
      employee.setDateOfBirth(now - 18L *  YEAR_IN_MS);
      employee.setSalary(120000);
      employee.setSsn(new Utf8(Long.toString(ssn + i)));
      dataStore.put(employee.getSsn().toString(), employee);
    }

    dataStore.flush();

    for (int i = 0; i < 1; i++) {
      String key = Long.toString(ssn + i);
      Employee employee = dataStore.get(key);
      Assert.assertEquals(now - 18L * YEAR_IN_MS, employee.getDateOfBirth());
      Assert.assertEquals("John Doe " + (i + 5), employee.getName().toString());
      Assert.assertEquals(120000, employee.getSalary());
    }
  }

  public static void testUpdateWebPage(DataStore<String, WebPage> dataStore)
  throws IOException, Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g"};
    String content = "content";
    String parsedContent = "parsedContent";
    String anchor = "anchor";

    int parsedContentCount = 0;


    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.newPersistent();
      webPage.setUrl(new Utf8(urls[i]));
      for (parsedContentCount = 0; parsedContentCount < 5; parsedContentCount++) {
        webPage.addToParsedContent(new Utf8(parsedContent + i + "," + parsedContentCount));
      }
      for (int j = 0; j < urls.length; j += 2) {
        webPage.putToOutlinks(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      webPage.setContent(ByteBuffer.wrap(ByteUtils.toBytes(content + i)));
      for (parsedContentCount = 5; parsedContentCount < 10; parsedContentCount++) {
        webPage.addToParsedContent(new Utf8(parsedContent + i + "," + parsedContentCount));
      }
      webPage.getOutlinks().clear();
      for (int j = 1; j < urls.length; j += 2) {
        webPage.putToOutlinks(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      Assert.assertEquals(content + i, ByteUtils.toString(webPage.getContent().array()));
      Assert.assertEquals(10, webPage.getParsedContent().size());
      int j = 0;
      for (Utf8 pc : webPage.getParsedContent()) {
        Assert.assertEquals(parsedContent + i + "," + j, pc.toString());
        j++;
      }
      int count = 0;
      for (j = 1; j < urls.length; j += 2) {
        Utf8 link = webPage.getOutlinks().get(new Utf8(anchor + j));
        Assert.assertNotNull(link);
        Assert.assertEquals(urls[j], link.toString());
        count++;
      }
      Assert.assertEquals(count, webPage.getOutlinks().size());
    }

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      for (int j = 0; j < urls.length; j += 2) {
        webPage.putToOutlinks(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      int count = 0;
      for (int j = 0; j < urls.length; j++) {
        Utf8 link = webPage.getOutlinks().get(new Utf8(anchor + j));
        Assert.assertNotNull(link);
        Assert.assertEquals(urls[j], link.toString());
        count++;
      }
    }
  }

  public static void assertWebPage(WebPage page, int i) throws Exception{
    Assert.assertNotNull(page);

    Assert.assertEquals(URLS[i], page.getUrl().toString());
    Assert.assertTrue("content error:" + new String(page.getContent().array()) +
        " actual=" + CONTENTS[i] + " i=" + i
    , Arrays.equals(page.getContent().array()
        , CONTENTS[i].getBytes()));

    GenericArray<Utf8> parsedContent = page.getParsedContent();
    Assert.assertNotNull(parsedContent);
    Assert.assertTrue(parsedContent.size() > 0);

    int j=0;
    String[] tokens = CONTENTS[i].split(" ");
    for(Utf8 token : parsedContent) {
      Assert.assertEquals(tokens[j++], token.toString());
    }

    if(LINKS[i].length > 0) {
      Assert.assertNotNull(page.getOutlinks());
      Assert.assertTrue(page.getOutlinks().size() > 0);
      for(j=0; j<LINKS[i].length; j++) {
        Assert.assertEquals(ANCHORS[i][j],
            page.getFromOutlinks(new Utf8(URLS[LINKS[i][j]])).toString());
      }
    } else {
      Assert.assertTrue(page.getOutlinks() == null || page.getOutlinks().isEmpty());
    }
  }

  private static void testGetWebPage(DataStore<String, WebPage> store, String[] fields)
    throws IOException, Exception {
    createWebPageData(store);

    for(int i=0; i<URLS.length; i++) {
      WebPage page = store.get(URLS[i], fields);
      assertWebPage(page, i);
    }
  }

  public static void testGetWebPage(DataStore<String, WebPage> store) throws IOException, Exception {
    testGetWebPage(store, WebPage._ALL_FIELDS);
  }

  public static void testGetWebPageDefaultFields(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testGetWebPage(store, null);
  }

  private static void testQueryWebPageSingleKey(DataStore<String, WebPage> store
      , String[] fields) throws IOException, Exception {

    createWebPageData(store);

    for(int i=0; i<URLS.length; i++) {
      Query<String, WebPage> query = store.newQuery();
      query.setFields(fields);
      query.setKey(URLS[i]);
      Result<String, WebPage> result = query.execute();
      Assert.assertTrue(result.next());
      WebPage page = result.get();
      assertWebPage(page, i);
      Assert.assertFalse(result.next());
    }
  }

  public static void testQueryWebPageSingleKey(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testQueryWebPageSingleKey(store, WebPage._ALL_FIELDS);
  }

  public static void testQueryWebPageSingleKeyDefaultFields(
      DataStore<String, WebPage> store) throws IOException, Exception {
    testQueryWebPageSingleKey(store, null);
  }

  public static void testQueryWebPageKeyRange(DataStore<String, WebPage> store,
      boolean setStartKeys, boolean setEndKeys)
  throws IOException, Exception {
    createWebPageData(store);

    //create sorted set of urls
    List<String> sortedUrls = new ArrayList<String>();
    for(String url: URLS) {
      sortedUrls.add(url);
    }
    Collections.sort(sortedUrls);

    //try all ranges
    for(int i=0; i<sortedUrls.size(); i++) {
      for(int j=i; j<sortedUrls.size(); j++) {
        Query<String, WebPage> query = store.newQuery();
        if(setStartKeys)
          query.setStartKey(sortedUrls.get(i));
        if(setEndKeys)
          query.setEndKey(sortedUrls.get(j));
        Result<String, WebPage> result = query.execute();

        int r=0;
        while(result.next()) {
          WebPage page = result.get();
          assertWebPage(page, URL_INDEXES.get(page.getUrl().toString()));
          r++;
        }

        int expectedLength = (setEndKeys ? j+1: sortedUrls.size()) -
                             (setStartKeys ? i: 0);
        Assert.assertEquals(expectedLength, r);
        if(!setEndKeys)
          break;
      }
      if(!setStartKeys)
        break;
    }
  }

  public static void testQueryWebPages(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testQueryWebPageKeyRange(store, false, false);
  }

  public static void testQueryWebPageStartKey(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testQueryWebPageKeyRange(store, true, false);
  }

  public static void testQueryWebPageEndKey(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testQueryWebPageKeyRange(store, false, true);
  }

  public static void testQueryWebPageKeyRange(DataStore<String, WebPage> store)
  throws IOException, Exception {
    testQueryWebPageKeyRange(store, true, true);
  }

  public static void testQueryWebPageEmptyResults(DataStore<String, WebPage> store)
    throws IOException, Exception {
    createWebPageData(store);

    //query empty results
    Query<String, WebPage> query = store.newQuery();
    query.setStartKey("aa");
    query.setEndKey("ab");
    assertEmptyResults(query);

    //query empty results for one key
    query = store.newQuery();
    query.setKey("aa");
    assertEmptyResults(query);
  }

  public static<K,T extends Persistent> void assertEmptyResults(Query<K, T> query)
    throws IOException, Exception {
    assertNumResults(query, 0);
  }

  public static<K,T extends Persistent> void assertNumResults(Query<K, T>query
      , long numResults) throws IOException, Exception {
    Result<K, T> result = query.execute();
    int actualNumResults = 0;
    while(result.next()) {
      actualNumResults++;
    }
    result.close();
    Assert.assertEquals(numResults, actualNumResults);
  }

  public static void testGetPartitions(DataStore<String, WebPage> store)
  throws IOException, Exception {
    createWebPageData(store);
    testGetPartitions(store, store.newQuery());
  }

  public static void testGetPartitions(DataStore<String, WebPage> store
      , Query<String, WebPage> query) throws IOException, Exception {
    List<PartitionQuery<String, WebPage>> partitions = store.getPartitions(query);

    Assert.assertNotNull(partitions);
    Assert.assertTrue(partitions.size() > 0);

    for(PartitionQuery<String, WebPage> partition:partitions) {
      Assert.assertNotNull(partition);
    }

    assertPartitions(store, query, partitions);
  }

  public static void assertPartitions(DataStore<String, WebPage> store,
      Query<String, WebPage> query, List<PartitionQuery<String,WebPage>> partitions)
  throws IOException, Exception {

    int count = 0, partitionsCount = 0;
    Map<String, Integer> results = new HashMap<String, Integer>();
    Map<String, Integer> partitionResults = new HashMap<String, Integer>();

    //execute query and count results
    Result<String, WebPage> result = store.execute(query);
    Assert.assertNotNull(result);

    while(result.next()) {
      Assert.assertNotNull(result.getKey());
      Assert.assertNotNull(result.get());
      results.put(result.getKey(), result.get().hashCode()); //keys are not reused, so this is safe
      count++;
    }
    result.close();

    Assert.assertTrue(count > 0); //assert that results is not empty
    Assert.assertEquals(count, results.size()); //assert that keys are unique

    for(PartitionQuery<String, WebPage> partition:partitions) {
      Assert.assertNotNull(partition);

      result = store.execute(partition);
      Assert.assertNotNull(result);

      while(result.next()) {
        Assert.assertNotNull(result.getKey());
        Assert.assertNotNull(result.get());
        partitionResults.put(result.getKey(), result.get().hashCode());
        partitionsCount++;
      }
      result.close();

      Assert.assertEquals(partitionsCount, partitionResults.size()); //assert that keys are unique
    }

    Assert.assertTrue(partitionsCount > 0);
    Assert.assertEquals(count, partitionsCount);

    for(Map.Entry<String, Integer> r : results.entrySet()) {
      Integer p = partitionResults.get(r.getKey());
      Assert.assertNotNull(p);
      Assert.assertEquals(r.getValue(), p);
    }
  }

  public static void testDelete(DataStore<String, WebPage> store) throws IOException, Exception {
    WebPageDataCreator.createWebPageData(store);
    //delete one by one

    int deletedSoFar = 0;
    for(String url : URLS) {
      Assert.assertTrue(store.delete(url));
      store.flush();

      //assert that it is actually deleted
      Assert.assertNull(store.get(url));

      //assert that other records are not deleted
      assertNumResults(store.newQuery(), URLS.length - ++deletedSoFar);
    }
  }

  public static void testDeleteByQuery(DataStore<String, WebPage> store)
    throws IOException, Exception {

    Query<String, WebPage> query;

    //test 1 - delete all
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.flush();
    assertEmptyResults(store.newQuery());


    //test 2 - delete all
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields(WebPage._ALL_FIELDS);

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.flush();
    assertEmptyResults(store.newQuery());


    //test 3 - delete all
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setKeyRange("a", "z"); //all start with "http://"

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.flush();
    assertEmptyResults(store.newQuery());


    //test 4 - delete some
    WebPageDataCreator.createWebPageData(store);
    query = store.newQuery();
    query.setEndKey(SORTED_URLS[NUM_KEYS]);

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.flush();
    assertNumResults(store.newQuery(), URLS.length - (NUM_KEYS+1));

    store.truncateSchema();

  }

  public static void testDeleteByQueryFields(DataStore<String, WebPage> store)
  throws IOException, Exception {

    Query<String, WebPage> query;

    //test 5 - delete all with some fields
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields(WebPage.Field.OUTLINKS.getName()
        , WebPage.Field.PARSED_CONTENT.getName(), WebPage.Field.CONTENT.getName());

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.deleteByQuery(query);//don't you love that HBase sometimes does not delete arbitrarily
    
    store.flush();
    
    assertNumResults(store.newQuery(), URLS.length);

    //assert that data is deleted
    for (int i = 0; i < SORTED_URLS.length; i++) {
      WebPage page = store.get(SORTED_URLS[i]);
      Assert.assertNotNull(page);

      Assert.assertNotNull(page.getUrl());
      Assert.assertEquals(page.getUrl().toString(), SORTED_URLS[i]);
      Assert.assertEquals(0, page.getOutlinks().size());
      Assert.assertEquals(0, page.getParsedContent().size());
      if(page.getContent() != null) {
        System.out.println("url:" + page.getUrl().toString());
        System.out.println( "limit:" + page.getContent().limit());
      } else {
        Assert.assertNull(page.getContent());
      }
    }

    //test 6 - delete some with some fields
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields(WebPage.Field.URL.getName());
    String startKey = SORTED_URLS[NUM_KEYS];
    String endKey = SORTED_URLS[SORTED_URLS.length - NUM_KEYS];
    query.setStartKey(startKey);
    query.setEndKey(endKey);

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);
    store.deleteByQuery(query);
    store.deleteByQuery(query);//don't you love that HBase sometimes does not delete arbitrarily
    
    store.flush();

    assertNumResults(store.newQuery(), URLS.length);

    //assert that data is deleted
    for (int i = 0; i < URLS.length; i++) {
      WebPage page = store.get(URLS[i]);
      Assert.assertNotNull(page);
      if( URLS[i].compareTo(startKey) < 0 || URLS[i].compareTo(endKey) >= 0) {
        //not deleted
        assertWebPage(page, i);
      } else {
        //deleted
        Assert.assertNull(page.getUrl());
        Assert.assertNotNull(page.getOutlinks());
        Assert.assertNotNull(page.getParsedContent());
        Assert.assertNotNull(page.getContent());
        Assert.assertTrue(page.getOutlinks().size() > 0);
        Assert.assertTrue(page.getParsedContent().size() > 0);
      }
    }

  }
}
