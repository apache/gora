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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.apache.avro.Schema.Field;
import org.apache.avro.util.Utf8;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ByteUtils;
import org.apache.gora.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(DataStoreTestUtil.class);

  public static final long YEAR_IN_MS = 365L * 24L * 60L * 60L * 1000L;
  private static final int NUM_KEYS = 4;

  public static <K, T extends Persistent> void testNewPersistent(
      DataStore<K,T> dataStore) throws Exception {

    T obj1 = dataStore.newPersistent();
    T obj2 = dataStore.newPersistent();

    assertEquals(dataStore.getPersistentClass(),
        obj1.getClass());
    assertNotNull(obj1);
    assertNotNull(obj2);
    assertFalse( obj1 == obj2 );
  }

  public static <K> Employee createEmployee() throws Exception {

    Employee employee = Employee.newBuilder().build();
    employee.setName(new Utf8("Random Joe"));
    employee.setDateOfBirth( System.currentTimeMillis() - 20L *  YEAR_IN_MS );
    employee.setSalary(100000);
    employee.setSsn(new Utf8("101010101010"));
    return employee;
  }

  private static <K> WebPage createWebPage() {
    WebPage webpage = WebPage.newBuilder().build();
    webpage.setUrl(new Utf8("url.."));
    webpage.setContent(ByteBuffer.wrap("test content".getBytes(Charset.defaultCharset())));
    webpage.setParsedContent(new ArrayList<CharSequence>());
    Metadata metadata = Metadata.newBuilder().build();
    webpage.setMetadata(metadata);
    
    return webpage;
  }

  public static <K> Employee createBoss()
      throws Exception {

    Employee employee = Employee.newBuilder().build();
    employee.setName(new Utf8("Random boss"));
    employee.setDateOfBirth(System.currentTimeMillis() - 22L * YEAR_IN_MS);
    employee.setSalary(1000000);
    employee.setSsn(new Utf8("202020202020"));
    return employee;
  }
  
  public static void testAutoCreateSchema(DataStore<String,Employee> dataStore)
  throws Exception {
    //should not throw exception
    dataStore.put("foo", createEmployee());
  }

  public static void testCreateEmployeeSchema(DataStore<String, Employee> dataStore)
  throws Exception {
    dataStore.createSchema();

    //should not throw exception
    dataStore.createSchema();
  }

  public static void testTruncateSchema(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();
    WebPageDataCreator.createWebPageData(dataStore);
    dataStore.truncateSchema();

    assertEmptyResults(dataStore.newQuery());
  }

  public static void testDeleteSchema(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();
    WebPageDataCreator.createWebPageData(dataStore);
    dataStore.deleteSchema();
    dataStore.createSchema();

    assertEmptyResults(dataStore.newQuery());
  }

  public static<K, T extends Persistent> void testSchemaExists(
      DataStore<K, T> dataStore) throws Exception {
    dataStore.createSchema();

    assertTrue(dataStore.schemaExists());

    dataStore.deleteSchema();
    assertFalse(dataStore.schemaExists());
  }

  public static void testGetEmployee(DataStore<String, Employee> dataStore)
    throws Exception {
    dataStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee();
    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();

    Employee after = dataStore.get(ssn, AvroUtils.getSchemaFieldNames(Employee.SCHEMA$));

    assertEqualEmployeeObjects(employee, after);
  }

  public static void testGetEmployeeRecursive(DataStore<String, Employee> dataStore)
    throws Exception {

    Employee employee = DataStoreTestUtil.createEmployee();
    Employee boss = DataStoreTestUtil.createBoss();
    employee.setBoss(boss);
    
    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();
    Employee after = dataStore.get(ssn, AvroUtils.getSchemaFieldNames(Employee.SCHEMA$));
    assertEqualEmployeeObjects(employee, after);
  }

  public static void testGetEmployeeDoubleRecursive(DataStore<String, Employee> dataStore)
      throws Exception {

      Employee employee = DataStoreTestUtil.createEmployee();
      Employee boss = DataStoreTestUtil.createBoss();
      Employee uberBoss = DataStoreTestUtil.createBoss();
      uberBoss.setName(new Utf8("Überboss")) ;
      boss.setBoss(uberBoss) ;
      employee.setBoss(boss) ;
      
      String ssn = employee.getSsn().toString();
      dataStore.put(ssn, employee);
      dataStore.flush();
      Employee after = dataStore.get(ssn, AvroUtils.getSchemaFieldNames(Employee.SCHEMA$));
      assertEqualEmployeeObjects(employee, after);
    }
  
  public static void testGetEmployeeNested(DataStore<String, Employee> dataStore)
    throws Exception {

    Employee employee = DataStoreTestUtil.createEmployee();
    WebPage webpage = new BeanFactoryImpl<>(String.class,WebPage.class).newPersistent() ;
    
    webpage.setUrl(new Utf8("url..")) ;
    webpage.setContent(ByteBuffer.wrap("test content".getBytes(Charset.defaultCharset()))) ;
    webpage.setParsedContent(new ArrayList<CharSequence>());
    Metadata metadata = new BeanFactoryImpl<>(String.class,Metadata.class).newPersistent();
    webpage.setMetadata(metadata) ;
    employee.setWebpage(webpage) ;
    
    String ssn = employee.getSsn().toString();
   
    dataStore.put(ssn, employee);
    dataStore.flush();
    Employee after = dataStore.get(ssn, AvroUtils.getSchemaFieldNames(Employee.SCHEMA$));
    assertEqualEmployeeObjects(employee, after);
    assertEqualWebPageObjects(webpage, after.getWebpage());
  }
  
  public static void testGetEmployee3UnionField(DataStore<String, Employee> dataStore)
    throws Exception {

    Employee employee = DataStoreTestUtil.createEmployee();
    employee.setBoss(new Utf8("Real boss")) ;

    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();
    Employee after = dataStore.get(ssn, AvroUtils.getSchemaFieldNames(Employee.SCHEMA$));
    assertEqualEmployeeObjects(employee, after);
    assertEquals("Real boss", after.getBoss().toString()) ;
  }
  
  public static void testGetEmployeeNonExisting(DataStore<String, Employee> dataStore)
    throws Exception {
    Employee employee = dataStore.get("_NON_EXISTING_SSN_FOR_EMPLOYEE_");
    assertNull(employee);
  }

  public static void testGetEmployeeWithFields(DataStore<String, Employee> dataStore)
    throws Exception {
    Employee employee = DataStoreTestUtil.createEmployee();
    WebPage webpage = createWebPage();
    employee.setWebpage(webpage);
    Employee boss = createBoss();
    employee.setBoss(boss);
    String ssn = employee.getSsn().toString();
    dataStore.put(ssn, employee);
    dataStore.flush();

    String[] fields = AvroUtils.getPersistentFieldNames(employee);
    for(Set<String> subset : StringUtils.powerset(fields)) {
      if(subset.isEmpty())
        continue;
      Employee after = dataStore.get(ssn, subset.toArray(new String[subset.size()]));
      Employee expected = Employee.newBuilder().build();
      for(String field:subset) {
        int index = expected.getSchema().getField(field).pos();
        expected.put(index, employee.get(index));
      }

      assertEqualEmployeeObjects(expected, after);
    }
  }
  
  /**
   * Simple function which iterates through a before (put) and after (get) object
   * in an attempt to verify if the same field's and values have been obtained.
   * Within the original employee object we iterate from 1 instead of 0 due to the 
   * removal of the '__g__' field at position 0 when we put objects into the datastore. 
   * This field is used to identify whether fields within the object, and 
   * consequently the object itself, are/is dirty however this field is not 
   * required when persisting the object.
   * We explicitly get values from each field as this makes it easier to debug 
   * if tests go wrong.
   * @param employee
   * @param after
   */
  private static void assertEqualEmployeeObjects(Employee employee, Employee after) {
    //for (int i = 1; i < employee.SCHEMA$.getFields().size(); i++) {
    //  for (int j = 1; j < after.SCHEMA$.getFields().size(); j++) {
    //    assertEquals(employee.SCHEMA$.getFields().get(i), after.SCHEMA$.getFields().get(j));
    //  }
    //}
    //check name field
    CharSequence beforeName = employee.getName();
    CharSequence afterName = after.getName();
    assertEquals(beforeName, afterName);
    //check dateOfBirth field
    Long beforeDOB = employee.getDateOfBirth();
    Long afterDOB = after.getDateOfBirth();
    assertEquals(beforeDOB, afterDOB);
    //check ssn field
    CharSequence beforeSsn = employee.getSsn();
    CharSequence afterSsn = after.getSsn();
    assertEquals(beforeSsn, afterSsn);
    //check salary field
    Integer beforeSalary = employee.getSalary();
    Integer afterSalary = after.getSalary();
    assertEquals(beforeSalary, afterSalary);
    //check boss field
    if (employee.getBoss() != null) {
      if (employee.getBoss() instanceof Utf8) {
        String beforeBoss = employee.getBoss().toString();
        String afterBoss = after.getBoss().toString();
        assertEquals("Boss String field values in UNION should be the same",
            beforeBoss, afterBoss);
      } else {
        Employee beforeBoss = (Employee) employee.getBoss();
        Employee afterBoss = (Employee) after.getBoss();
        assertEqualEmployeeObjects(beforeBoss, afterBoss);
      }
    }
    //check webpage field
    if (employee.getWebpage() != null) {
      WebPage beforeWebPage = employee.getWebpage();
      WebPage afterWebPage = after.getWebpage();
      assertEqualWebPageObjects(beforeWebPage, afterWebPage);
    }
  }

  /**
   * Mimics {@link org.apache.gora.store.DataStoreTestUtil#assertEqualEmployeeObjects(Employee, Employee)}
   * in that we pick our way through fields within before and after 
   * {@link org.apache.gora.examples.generated.WebPage} objects comparing field values.
   * @param beforeWebPage
   * @param afterWebPage
   */
  private static void assertEqualWebPageObjects(WebPage beforeWebPage, WebPage afterWebPage) {
    //check url field
    CharSequence beforeUrl = beforeWebPage.getUrl();
    CharSequence afterUrl = afterWebPage.getUrl();
    assertEquals(beforeUrl, afterUrl);
    //check content field
    ByteBuffer beforeContent = beforeWebPage.getContent();
    ByteBuffer afterContent = afterWebPage.getContent();
    assertEquals(beforeContent, afterContent);
    //check parsedContent field
    List<CharSequence> beforeParsedContent =
            beforeWebPage.getParsedContent();
    List<CharSequence> afterParsedContent =
            afterWebPage.getParsedContent();
    assertEquals(beforeParsedContent, afterParsedContent);
    //check outlinks field
    Map<CharSequence, CharSequence> beforeOutlinks =
            beforeWebPage.getOutlinks();
    Map<CharSequence, CharSequence> afterOutlinks =
            afterWebPage.getOutlinks();
    assertEquals(beforeOutlinks, afterOutlinks);
    //check metadata field
    if (beforeWebPage.get(5) != null) {
      Metadata beforeMetadata = beforeWebPage.getMetadata();
      Metadata afterMetadata = afterWebPage.getMetadata();
      assertEqualMetadataObjects(beforeMetadata, afterMetadata);
    }
  }

  /**
   * Mimics {@link org.apache.gora.store.DataStoreTestUtil#assertEqualEmployeeObjects(Employee, Employee)}
   * in that we pick our way through fields within before and after 
   * {@link org.apache.gora.examples.generated.Metadata} objects comparing field values.
   * @param beforeMetadata
   * @param afterMetadata
   */
  private static void assertEqualMetadataObjects(Metadata beforeMetadata, Metadata afterMetadata) {
    //check version field
    int beforeVersion = beforeMetadata.getVersion();
    int afterVersion = afterMetadata.getVersion();
    assertEquals(beforeVersion, afterVersion);
    //check data field
    Map<CharSequence, CharSequence> beforeData = beforeMetadata.getData();
    Map<CharSequence, CharSequence> afterData =  afterMetadata.getData();
    assertEquals(beforeData, afterData);
  }

  public static Employee testPutEmployee(DataStore<String, Employee> dataStore)
  throws Exception {
    dataStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee();
    return employee;
  }

  public static void testEmptyUpdateEmployee(DataStore<String, Employee> dataStore)
  throws Exception {
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
    assertNull(employee);
  }

  /**
   * Here we create 5 {@link org.apache.gora.examples.generated.Employee} objects
   * before populating fields with data and flushing them to the datastore.
   * We then update the 1st of the {@link org.apache.gora.examples.generated.Employee}'s
   * with more data and flush this data. Assertions are then made over the updated
   * {@link org.apache.gora.examples.generated.Employee} object.
   * @param dataStore
   * @throws IOException
   * @throws Exception
   */
  public static void testUpdateEmployee(DataStore<String, Employee> dataStore)
  throws Exception {
    dataStore.createSchema();
    long ssn = 1234567890L;
    long now = System.currentTimeMillis();

    for (int i = 0; i < 5; i++) {
      Employee employee = Employee.newBuilder().build();
      employee.setName(new Utf8("John Doe " + i));
      employee.setDateOfBirth(now - 20L *  YEAR_IN_MS);
      employee.setSalary(100000);
      employee.setSsn(new Utf8(Long.toString(ssn + i)));
      dataStore.put(employee.getSsn().toString(), employee);
    }

    dataStore.flush();

    for (int i = 0; i < 1; i++) {
      Employee employee = Employee.newBuilder().build();
      employee.setName(new Utf8("John Doe " + (i + 5)));
      employee.setDateOfBirth(now - 18L * YEAR_IN_MS);
      employee.setSalary(120000);
      employee.setSsn(new Utf8(Long.toString(ssn + i)));
      dataStore.put(employee.getSsn().toString(), employee);
    }

    dataStore.flush();

    for (int i = 0; i < 1; i++) {
      String key = Long.toString(ssn + i);
      Employee employee = dataStore.get(key);
      assertEquals(now - 18L * YEAR_IN_MS, employee.getDateOfBirth().longValue()); 
      assertEquals("John Doe " + (i + 5), employee.getName().toString());
      assertEquals(120000, employee.getSalary().intValue()); 
    }
  }

  /**
   * Here we create 7 {@link org.apache.gora.examples.generated.WebPage}
   * objects and populate field data before flushing the objects to the 
   * datastore. We then get the objects, adding data to the 'content' and
   * 'parsedContent' fields before clearing the 'outlinks' field and 
   * re-populating it. This data is then flushed to the datastore. 
   * Finally we get the {@link org.apache.gora.examples.generated.WebPage}
   * objects and make various assertions over verious fields. This tests 
   * that we can update fields and that data can be written and read correctly.
   * @param dataStore
   * @throws IOException
   * @throws Exception
   */
  public static void testUpdateWebPagePutToArray(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g" };
    String content = "content";
    String parsedContent = "parsedContent";

    int parsedContentCount = 0;


    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = WebPage.newBuilder().build();
      webPage.setUrl(new Utf8(urls[i]));
      for (parsedContentCount = 0; parsedContentCount < 5; parsedContentCount++) {
        webPage.getParsedContent().add(new Utf8(parsedContent + i + "," + parsedContentCount));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      webPage.setContent(ByteBuffer.wrap(ByteUtils.toBytes(content + i)));
      for (parsedContentCount = 5; parsedContentCount < 10; parsedContentCount++) {
        webPage.getParsedContent().add(new Utf8(parsedContent + i + "," + parsedContentCount));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (int i = 0; i < urls.length; i++) {
      WebPage webPage = dataStore.get(urls[i]);
      assertEquals(content + i, ByteUtils.toString( toByteArray(webPage.getContent()) ));
      assertEquals(10, webPage.getParsedContent().size());
      int j = 0;
      for (CharSequence pc : webPage.getParsedContent()) {
        assertEquals(parsedContent + i + "," + j, pc.toString());
        j++;
      }
    }
  }

  public static void testUpdateWebPagePutToNotNullableMap(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g" };
    String anchor = "anchor";

    // putting evens
    for (String url : urls) {
      WebPage webPage = WebPage.newBuilder().build();
      webPage.setUrl(new Utf8(url));
      for (int j = 0; j < urls.length; j += 2) {
        webPage.getOutlinks().put(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }
    dataStore.flush();

    // putting odds
    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      webPage.getOutlinks().clear();
      for (int j = 1; j < urls.length; j += 2) {
        webPage.getOutlinks().put(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      // test for double put of same entries
      for (int j = 1; j < urls.length; j += 2) {
        webPage.getOutlinks().put(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }
    dataStore.flush();

    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      int count = 0;
      for (int j = 1; j < urls.length; j += 2) {
        CharSequence link = webPage.getOutlinks().get(new Utf8(anchor + j));
        assertNotNull(link);
        assertEquals(urls[j], link.toString());
        count++;
      }
      assertEquals(count, webPage.getOutlinks().size());
    }
  }

  public static void testUpdateWebPagePutToNullableMap(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g" };
    String header = "header";
    String[] headers = { "firstHeader", "secondHeader", "thirdHeader",
        "fourthHeader", "fifthHeader", "sixthHeader" };

    for (String url : urls) {
      WebPage webPage = WebPage.newBuilder().build();
      webPage.setUrl(new Utf8(url));
      //test put for nullable map field
      // we put data to the 'headers' field which is a Map with default value of 'null'
      webPage.setHeaders(new HashMap<CharSequence, CharSequence>());
      for (int j = 0; j < headers.length; j += 2) {
        webPage.getHeaders().put(new Utf8(header + j), new Utf8(headers[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      //webPage.getHeaders().clear(); //TODO clear method does not work
      webPage.setHeaders(new HashMap<CharSequence, CharSequence>());
      for (int j = 1; j < headers.length; j += 2) {
        webPage.getHeaders().put(new Utf8(header + j), new Utf8(headers[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      int count = 0;
      for (int j = 1; j < headers.length; j += 2) {
        CharSequence headerSample = webPage.getHeaders().get(new Utf8(header + j));
        assertNotNull(headerSample);
        assertEquals(headers[j], headerSample.toString());
        count++;
      }
      assertEquals(count, webPage.getHeaders().size());
    }
  }

  public static void testUpdateWebPageRemoveMapEntry(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g" };
    String anchor = "anchor";

    for (String url : urls) {
      WebPage webPage = WebPage.newBuilder().build();
      webPage.setUrl(new Utf8(url));
      for (int j = 0; j < urls.length; j++) {
        webPage.getOutlinks().put(new Utf8(anchor + j), new Utf8(urls[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    // map entry removal test
    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      for (int j = 1; j < urls.length; j += 2) {
        webPage.getOutlinks().remove(new Utf8(anchor + j));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (String url : urls) {
      int count = 0;
      WebPage webPage = dataStore.get(url);
      for (int j = 1; j < urls.length; j += 2) {
        CharSequence link = webPage.getOutlinks().get(new Utf8(anchor + j));
        assertNull(link);
        //assertEquals(urls[j], link.toString());
        count++;
      }
      assertEquals(urls.length - count, webPage.getOutlinks().size());
    }
  }

  public static void testUpdateWebPageRemoveField(DataStore<String, WebPage> dataStore)
  throws Exception {
    dataStore.createSchema();

    String[] urls = {"http://a.com/a", "http://b.com/b", "http://c.com/c",
        "http://d.com/d", "http://e.com/e", "http://f.com/f", "http://g.com/g" };
    String header = "header";
    String[] headers = { "firstHeader", "secondHeader", "thirdHeader",
        "fourthHeader", "fifthHeader", "sixthHeader" };

    for (String url : urls) {
      WebPage webPage = WebPage.newBuilder().build();
      webPage.setUrl(new Utf8(url));
      webPage.setHeaders(new HashMap<CharSequence, CharSequence>());
      for (int j = 0; j < headers.length; j++) {
        webPage.getHeaders().put(new Utf8(header + j), new Utf8(headers[j]));
      }
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    // nullable map field removal test
    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      webPage.setHeaders(null);
      dataStore.put(webPage.getUrl().toString(), webPage);
    }

    dataStore.flush();

    for (String url : urls) {
      WebPage webPage = dataStore.get(url);
      assertNull(webPage.getHeaders());
    }
  }

  public static void assertWebPage(WebPage page, int i) throws Exception{
    assertNotNull(page);

    assertEquals(URLS[i], page.getUrl().toString());
    // 'content' is optional
    if (page.getContent() != null) {
      assertTrue("content error:" + new String( toByteArray(page.getContent()), Charset.defaultCharset() ) +
        " actual=" + CONTENTS[i] + " i=" + i
        , Arrays.equals( toByteArray(page.getContent() )
        , CONTENTS[i].getBytes(Charset.defaultCharset())));
    
      List<CharSequence> parsedContent = page.getParsedContent();
      assertNotNull(parsedContent);
      assertTrue(parsedContent.size() > 0);
    
      int j=0;
      String[] tokens = CONTENTS[i].split(" ");
      for(CharSequence token : parsedContent) {
        assertEquals(tokens[j++], token.toString());
      }
    } else {
      // when page.getContent() is null
      assertTrue(CONTENTS[i] == null) ;
      List<CharSequence> parsedContent = page.getParsedContent();
      assertNotNull(parsedContent);
      assertTrue(parsedContent.size() == 0);
    }

    if(LINKS[i].length > 0) {
      assertNotNull(page.getOutlinks());
      assertTrue(page.getOutlinks().size() > 0);
      for(int k=0; k<LINKS[i].length; k++) {
        assertEquals(ANCHORS[i][k],
          page.getOutlinks().get(new Utf8(URLS[LINKS[i][k]])).toString());
      }
    } else {
      assertTrue(page.getOutlinks() == null || page.getOutlinks().isEmpty());
    }
  }

  private static void testGetWebPage(DataStore<String, WebPage> store, String[] fields)
    throws Exception {
    createWebPageData(store);

    for(int i=0; i<URLS.length; i++) {
      WebPage page = store.get(URLS[i], fields);
      assertWebPage(page, i);
    }
  }

  public static void testGetWebPage(DataStore<String, WebPage> store) throws Exception {
    testGetWebPage(store, getFields(WebPage.SCHEMA$.getFields()));
  }

  public static void testGetWebPageDefaultFields(DataStore<String, WebPage> store)
  throws Exception {
    testGetWebPage(store, null);
  }

  private static void testQueryWebPageSingleKey(DataStore<String, WebPage> store
      , String[] fields) throws Exception {

    createWebPageData(store);

    for(int i=0; i<URLS.length; i++) {
      Query<String, WebPage> query = store.newQuery();
      query.setFields(fields);
      query.setKey(URLS[i]);
      Result<String, WebPage> result = query.execute();
      assertTrue(result.next());
      WebPage page = result.get();
      assertWebPage(page, i);
      assertFalse(result.next());
    }
  }

  public static void testQueryWebPageSingleKey(DataStore<String, WebPage> store)
  throws Exception {
    testQueryWebPageSingleKey(store, getFields(WebPage.SCHEMA$.getFields()));
  }

  public static void testQueryWebPageSingleKeyDefaultFields(
      DataStore<String, WebPage> store) throws Exception {
    testQueryWebPageSingleKey(store, null);
  }

  public static void testQueryWebPageKeyRange(DataStore<String, WebPage> store,
      boolean setStartKeys, boolean setEndKeys)
  throws Exception {
    createWebPageData(store);

    //create sorted set of urls
    List<String> sortedUrls = new ArrayList<>();
    Collections.addAll(sortedUrls, URLS);
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
        assertEquals(expectedLength, r);
        if(!setEndKeys)
          break;
      }
      if(!setStartKeys)
        break;
    }
  }

  public static void testQueryWebPages(DataStore<String, WebPage> store)
  throws Exception {
    testQueryWebPageKeyRange(store, false, false);
  }

  public static void testQueryWebPageStartKey(DataStore<String, WebPage> store)
  throws Exception {
    testQueryWebPageKeyRange(store, true, false);
  }

  public static void testQueryWebPageEndKey(DataStore<String, WebPage> store)
  throws Exception {
    testQueryWebPageKeyRange(store, false, true);
  }

  public static void testQueryWebPageKeyRange(DataStore<String, WebPage> store)
  throws Exception {
    testQueryWebPageKeyRange(store, true, true);
  }

  public static void testQueryWebPageEmptyResults(DataStore<String, WebPage> store)
    throws Exception {
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
    throws Exception {
    assertNumResults(query, 0);
  }

  public static<K,T extends Persistent> void assertNumResults(Query<K, T>query
      , long numResults) throws Exception {
    Result<K, T> result = query.execute();
    int actualNumResults = 0;
    while(result.next()) {
      actualNumResults++;
    }
    result.close();
    assertEquals(numResults, actualNumResults);
  }

  public static void testGetPartitions(DataStore<String, WebPage> store)
  throws Exception {
    createWebPageData(store);
    testGetPartitions(store, store.newQuery());
  }

  public static void testGetPartitions(DataStore<String, WebPage> store
      , Query<String, WebPage> query) throws Exception {
    List<PartitionQuery<String, WebPage>> partitions = store.getPartitions(query);

    assertNotNull(partitions);
    assertTrue(partitions.size() > 0);

    for(PartitionQuery<String, WebPage> partition:partitions) {
      assertNotNull(partition);
    }

    assertPartitions(store, query, partitions);
  }

  public static void assertPartitions(DataStore<String, WebPage> store,
      Query<String, WebPage> query, List<PartitionQuery<String,WebPage>> partitions)
  throws Exception {

    int count = 0, partitionsCount = 0;
    Map<String, Integer> results = new HashMap<>();
    Map<String, Integer> partitionResults = new HashMap<>();

    //execute query and count results
    Result<String, WebPage> result = store.execute(query);
    assertNotNull(result);

    while(result.next()) {
      assertNotNull(result.getKey());
      assertNotNull(result.get());
      results.put(result.getKey(), result.get().hashCode()); //keys are not reused, so this is safe
      count++;
    }
    result.close();

    assertTrue(count > 0); //assert that results is not empty
    assertEquals(count, results.size()); //assert that keys are unique

    for(PartitionQuery<String, WebPage> partition:partitions) {
      assertNotNull(partition);

      result = store.execute(partition);
      assertNotNull(result);

      while(result.next()) {
        assertNotNull(result.getKey());
        assertNotNull(result.get());
        partitionResults.put(result.getKey(), result.get().hashCode());
        partitionsCount++;
      }
      result.close();

      assertEquals(partitionsCount, partitionResults.size()); //assert that keys are unique
    }

    assertTrue(partitionsCount > 0);
    assertEquals(count, partitionsCount);

    for(Map.Entry<String, Integer> r : results.entrySet()) {
      Integer p = partitionResults.get(r.getKey());
      assertNotNull(p);
      assertEquals(r.getValue(), p);
    }
  }

  public static void testDelete(DataStore<String, WebPage> store) throws Exception {
    WebPageDataCreator.createWebPageData(store);
    //delete one by one

    int deletedSoFar = 0;
    for(String url : URLS) {
      assertTrue(store.delete(url));
      store.flush();

      //assert that it is actually deleted
      assertNull(store.get(url));

      //assert that other records are not deleted
      assertNumResults(store.newQuery(), URLS.length - ++deletedSoFar);
    }
  }

  public static void testDeleteByQuery(DataStore<String, WebPage> store)
    throws Exception {

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
    query.setFields(AvroUtils.getSchemaFieldNames(WebPage.SCHEMA$));

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
  throws Exception {

    Query<String, WebPage> query;

    //test 5 - delete all with some fields
    WebPageDataCreator.createWebPageData(store);

    query = store.newQuery();
    query.setFields("outlinks"
        , "parsedContent", "content");

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);

    store.flush();

    assertNumResults(store.newQuery(), URLS.length);

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

    assertNumResults(store.newQuery(), URLS.length);
    store.deleteByQuery(query);

    store.flush();

    assertNumResults(store.newQuery(), URLS.length);

    //assert that data is deleted
    for (int i = 0; i < URLS.length; i++) {
      WebPage page = store.get(URLS[i]);
      assertNotNull(page);
      if( URLS[i].compareTo(startKey) < 0 || URLS[i].compareTo(endKey) >= 0) {
        //not deleted
        assertWebPage(page, i);
      } else {
        //deleted
        assertNull(page.getUrl());
        assertNotNull(page.getOutlinks());
        assertNotNull(page.getParsedContent());
        assertNotNull(page.getContent());
        assertTrue(page.getOutlinks().size() > 0);
        assertTrue(page.getParsedContent().size() > 0);
      }
    }

  }
  
  public static void testPutNested(DataStore<String, WebPage> store)
          throws Exception {
    String revUrl = "foo.com:http/";
    String url = "http://foo.com/";

    store.createSchema();
    WebPage page = WebPage.newBuilder().build();
    Metadata metadata = Metadata.newBuilder().build();
    metadata.setVersion(1);
    metadata.getData().put(new Utf8("foo"), new Utf8("baz"));

    page.setMetadata(metadata);
    page.setUrl(new Utf8(url));

    store.put(revUrl, page);
    store.flush();

    page = store.get(revUrl);
    metadata = page.getMetadata();
    assertNotNull(metadata);
    assertEquals(1, metadata.getVersion().intValue()); 
    assertEquals(new Utf8("baz"), metadata.getData().get(new Utf8("foo")));
  }

  public static void testPutArray(DataStore<String, WebPage> store)
          throws Exception {
    store.createSchema();
    WebPage page = WebPage.newBuilder().build();

    String[] tokens = {"example", "content", "in", "example.com"};
    page.setParsedContent(new ArrayList<CharSequence>());
    for(String token: tokens) {
      page.getParsedContent().add(new Utf8(token));
    }

    store.put("com.example/http", page);
    store.close();

  }

  public static byte[] testPutBytes(DataStore<String, WebPage> store)
          throws Exception {

    store.createSchema();
    WebPage page = WebPage.newBuilder().build();
    page.setUrl(new Utf8("http://example.com"));
    byte[] contentBytes = "example content in example.com".getBytes(Charset.defaultCharset());
    ByteBuffer buff = ByteBuffer.wrap(contentBytes);
    page.setContent(buff);

    store.put("com.example/http", page);
    store.close();

    return contentBytes;
  }

  public static void testPutMap(DataStore<String, WebPage> store)
          throws Exception {

    store.createSchema();

    WebPage page = WebPage.newBuilder().build();

    page.setUrl(new Utf8("http://example.com"));
    page.getOutlinks().put(new Utf8("http://example2.com"), new Utf8("anchor2"));
    page.getOutlinks().put(new Utf8("http://example3.com"), new Utf8("anchor3"));
    page.getOutlinks().put(new Utf8("http://example3.com"), new Utf8("anchor4"));
    store.put("com.example/http", page);
    store.close();
  }
  
  public static void testPutMixedMapTypes(DataStore<String, WebPage> store) {
    WebPage webpage = createWebPage();
    webpage.getByteData().put(new Utf8("byteData"), ByteBuffer.wrap(ByteUtils.toBytes("hello map")));
    webpage.getStringData().put(new Utf8("stringData"), "hello map");
    store.createSchema();
    store.put(webpage.getUrl().toString(), webpage);
    store.flush();
    assertNotNull(store.get(webpage.getUrl().toString()));
  }

  private static byte[] toByteArray(ByteBuffer buffer) {
    int p = buffer.position();
    int n = buffer.limit() - p;
    byte[] bytes = new byte[n];
    for (int i = 0; i < n; i++) {
      bytes[i] = buffer.get(p++);
    }
    return bytes;
  }
  
  public static String[] getFields(List<Field> schemaFields) {
    
    List<Field> list = new ArrayList<>();
    for (Field field : schemaFields) {
      if (!Persistent.DIRTY_BYTES_FIELD_NAME.equalsIgnoreCase(field.name())) {
        list.add(field);
      }
    }
    schemaFields = list;
    
    String[] fieldNames = new String[schemaFields.size()];
    for(int i = 0; i<fieldNames.length; i++ ){
      fieldNames[i] = schemaFields.get(i).name();
    }
    
    return fieldNames;
  }
  
}
