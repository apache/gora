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
package org.apache.gora.mongodb.store;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mongodb.GoraMongodbTestDriver;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.hadoop.conf.Configuration;
import org.junit.Before;

import java.io.IOException;

public class TestMongoStore extends DataStoreTestBase {
//public class TestMongoStore {

  private Configuration conf;
  
  static {
    setTestDriver(new GoraMongodbTestDriver());
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected DataStore<String, Employee> createEmployeeDataStore() throws IOException {
    return DataStoreFactory.getDataStore(MongoStore.class, String.class, Employee.class, conf);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() throws IOException {
    return DataStoreFactory.getDataStore(MongoStore.class, String.class, WebPage.class, conf);
  }
  
  public GoraMongodbTestDriver getTestDriver() {
    return (GoraMongodbTestDriver) testDriver;
  }

  @Override
  public void testDeleteByQueryFields() throws IOException {
      // Skip until GORA-66 is fixed: need better semantic for end/start keys
  }

  @Override
  public void testQueryKeyRange() throws IOException, Exception {
      // Skip until GORA-66 is fixed: need better semantic for end/start keys
  }
  
  public static void main (String[] args) throws Exception {
    TestMongoStore test = new TestMongoStore();
    TestMongoStore.setUpClass();
    test.setUp();
    
    test.tearDown();
    TestMongoStore.tearDownClass();
  }
  //============================================================================
  // //We need to skip the following tests for a while until we fix some
  // issues..
  /*@Override
  public void testQueryStartKey() throws IOException {
    log.info("test method: TestQueryStartKey SKIPPED.");
  }
  
  @Override
  public void testQueryEndKey() throws IOException {
    log.info("test method: TestQueryEndKey SKIPPED.");
  }
  
  @Override
  public void testDeleteByQueryFields() throws IOException {
    log.info("test method: TestDeleteByQueryFields SKIPPED.");
  }
  
  @Override
  public void testNewInstance() throws IOException, Exception {
    log.info("test method: TestNewInstance SKIPPED.");
  }
  
  @Override
  public void testAutoCreateSchema() throws Exception {
    log.info("test method: TestAutoCreateSchema SKIPPED.");
  }
  
  @Override
  public void testTruncateSchema() throws Exception {
    log.info("test method: TestTruncateSchema SKIPPED.");
  }
  
  @Override
  public void testPutNested() throws IOException, Exception {
    log.info("test method: TestPutNested SKIPPED.");
  }
  
  @Override
  public void testPutArray() throws IOException, Exception {
    log.info("test method: TestPutArray SKIPPED.");
  }
  
  @Override
  public void testPutBytes() throws IOException, Exception {
    log.info("test method: TestPutBytes SKIPPED.");
  }
  
  @Override
  public void testPutMap() throws IOException, Exception {
    log.info("test method: TestPutMap SKIPPED.");
  }
  
  @Override
  public void testEmptyUpdate() throws IOException, Exception {
    log.info("test method: TestEmptyUpdate SKIPPED.");
  }
  
  @Override
  public void testDeleteSchema() throws IOException, Exception {
    log.info("test method: TestDeleteSchema SKIPPED.");
  }
  
  @Override
  public void testGetWithFields() throws IOException, Exception {
    log.info("test method: TestGetWithFields SKIPPED.");
  }*/
  //============================================================================

  /*protected MongoStore<String, Test1> storeTest1S;

  protected MongoStore<String, Test1> storeTest1R;

  protected Test1 t1Simple;

  protected MongoStore<String, Host> storeHost;

  protected Host h1Simple;

  protected MongoStore<String, WebPage> storeWebPage;

  protected WebPage w1Simple;

  protected byte[] w1SimpleContent;
  */

  /**
   * Shortcut to create a Utf8 string
   */
  /*protected static Utf8 u8(String s) {
    if (s == null)
      return new Utf8();
    else
      return new Utf8(s);
  }*/

  /**
   * Set up the instances that will be used for the tests.
   * 
   * @throws IOException
   */
  /*@Before
  public void setupTest1() throws IOException {
    Properties config = new Properties();
    config.setProperty(MongoStore.PROP_MONGO_SERVERS, "localhost:27017 ");
    config.setProperty(MongoStore.PROP_MONGO_DB, "integrationtests");
    config.setProperty(MongoStore.PROP_OVERRIDING, Boolean.TRUE.toString());

    // Store for Test1 instances: straight mapping
    config.setProperty(MongoStore.PROP_MAPPING_FILE,
        "/org/apache/gora/mongodb/straightmapping-test1.xml");
    storeTest1S = new MongoStore<String, Test1>();
    storeTest1S.initialize(String.class, Test1.class, config);
    storeTest1S.createSchema();

    // Store for Test1 instances: rename mapping
    config.setProperty(MongoStore.PROP_MAPPING_FILE,
        "/org/apache/gora/mongodb/renamemapping-test1.xml");
    storeTest1R = new MongoStore<String, Test1>();
    storeTest1R.initialize(String.class, Test1.class, config);
    storeTest1R.createSchema();

    // Test1 instance
    t1Simple = storeTest1S.newPersistent();
    t1Simple.setScalarDate(new Utf8("2013-01-10T08:55:00Z"));
    t1Simple.setScalarInt(42);
    t1Simple.setScalarString(new Utf8("not a string"));
    t1Simple.putToMapOfBytes(new Utf8("k1"),
        ByteBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 }));
    t1Simple.putToMapOfBytes(new Utf8("k2"),
        ByteBuffer.wrap(new byte[] { 0x03, 0x04, 0x05 }));
    t1Simple.putToMapOfInt(new Utf8("k1"), 10);
    t1Simple.putToMapOfInt(new Utf8("k2"), 11);
    t1Simple.putToMapOfInt(new Utf8("k3"), 12);
    t1Simple.putToMapOfInt(new Utf8("k4"), 13);
    t1Simple.putToMapOfStrings(new Utf8("k1"), new Utf8("just a string"));
    t1Simple.putToMapOfStrings(new Utf8("k2"), new Utf8("just another string"));
    t1Simple.putToMapOfStrings(new Utf8("k3"),
        new Utf8("...and another string"));
    t1Simple.addToListOfInt(0);
    t1Simple.addToListOfInt(1);
    t1Simple.addToListOfInt(2);
    t1Simple.addToListOfInt(3);
  }*/

  /**
   * Set up the instances that will be used for the tests.
   * 
   * @throws IOException
   */
  /*@Before
  public void setupHost() throws IOException {
    Properties config = new Properties();
    config.setProperty(MongoStore.PROP_MONGO_SERVERS, "localhost");
    config.setProperty(MongoStore.PROP_MONGO_DB, "integrationtests");
    config.setProperty(MongoStore.PROP_OVERRIDING, Boolean.TRUE.toString());

    // Store for Host instances
    config.setProperty(MongoStore.PROP_MAPPING_FILE,
        "/org/apache/gora/mongodb/multimapping.xml");
    storeHost = new MongoStore<String, Host>();
    storeHost.initialize(String.class, Host.class, config);
    storeHost.createSchema();

    // Host1 instance
    h1Simple = storeHost.newPersistent();
    h1Simple.putToMetadata(new Utf8("metadata-key1"),
        ByteBuffer.wrap(new byte[] { 0x0A, 0x0B, 0x0C }));
    h1Simple.putToMetadata(new Utf8("metadata-key2"),
        ByteBuffer.wrap(new byte[] { 0x0D, 0x0E, 0x0F }));
    h1Simple.putToInlinks(new Utf8("https://www.facebook.com/home.php"),
        new Utf8("i1"));
    h1Simple.putToInlinks(new Utf8("http://9gag.com/hot/id/6290565"), new Utf8(
        "i2"));
    h1Simple.putToOutlinks(new Utf8("http://9gag.com/gag/6298282"), new Utf8(
        "o1"));
  }*/

  /**
   * Set up the instances that will be used for the tests.
   * 
   * @throws IOException
   */
  /*@Before
  public void setupWebPage() throws IOException {
    Properties config = new Properties();
    config.setProperty(MongoStore.PROP_MONGO_SERVERS, "localhost");
    config.setProperty(MongoStore.PROP_MONGO_DB, "integrationtests");
    config.setProperty(MongoStore.PROP_OVERRIDING, Boolean.TRUE.toString());

    // Store for Host instances
    config.setProperty(MongoStore.PROP_MAPPING_FILE,
        "/org/apache/gora/mongodb/multimapping.xml");
    storeWebPage = new MongoStore<String, WebPage>();
    storeWebPage.initialize(String.class, WebPage.class, config);
    storeWebPage.createSchema();

    // Load web page content
    InputStream is = getClass().getResourceAsStream(
        "/org/apache/gora/mongodb/store/9gag.com_gag_6301703.html");
    w1SimpleContent = new byte[10000];
    is.read(w1SimpleContent, 0, 10000);
    is.close();

    // WebPage instance
    w1Simple = storeWebPage.newPersistent();
    w1Simple.setBaseUrl(u8("http://9gag.com/gag/6301703"));
    w1Simple.setContentType(u8("text/html"));
    w1Simple.setContent(ByteBuffer.wrap(w1SimpleContent));
    w1Simple.setFetchInterval(10000);
    long today = Calendar.getInstance().getTime().getTime();
    w1Simple.setFetchTime(today);
    w1Simple.setModifiedTime(today - 10000);
    w1Simple.setPrevFetchTime(today - 10000);
    w1Simple.setReprUrl(u8("http://9gag.com/gag/6301703"));
    w1Simple.setRetriesSinceFetch(0);
    w1Simple.setScore(0.123456f);
    w1Simple.setStatus(1);
    w1Simple.setText(u8(new String(w1SimpleContent, Charset.forName("utf-8"))));
    w1Simple.setTitle(u8("9GAG - So I heard you like chemistry puns"));
    w1Simple.putToHeaders(u8("Cache-Control"),
        u8("no-store, no-cache, must-revalidate, post-check=0, pre-check=0"));
    w1Simple.putToHeaders(u8("Connection"), u8("keep-alive"));
    w1Simple.putToHeaders(u8("Content-Encoding"), u8("gzip"));
    w1Simple.putToHeaders(u8("Content-Type"), u8("text/html"));
    w1Simple.putToHeaders(u8("Date"), u8("Thu, 17 Jan 2013 20:49:57 GMT"));
    w1Simple.putToHeaders(u8("Expires"), u8("Thu, 19 Nov 1981 08:52:00 GMT"));
    w1Simple.putToHeaders(u8("Pragma"), u8("no-cache"));
    w1Simple.putToHeaders(u8("Server"), u8("nginx"));
    w1Simple.putToHeaders(u8("transfer-encoding"), u8("chunked"));
    w1Simple.putToHeaders(u8("Vary"), u8("Accept-Encoding"));
    w1Simple.putToHeaders(u8("_ip"), u8("91.198.174.226"));
    w1Simple.putToMarkers(u8("_gnmrk"), u8("1343206321-307289411"));
    w1Simple.putToMarkers(u8("_injmrk_"), u8("y"));
    w1Simple.putToMarkers(u8("_test"), u8("whatevervalue"));
    w1Simple.putToMetadata(u8("_csh_"),
        ByteBuffer.wrap(new byte[] { 0x13, 0x16 }));
    w1Simple.putToInlinks(u8("http://fr.wiktionary.org/wiki/Angicourtois"),
        u8("Angicourtois"));
    w1Simple.putToInlinks(u8("http://fr.wiktionary.org/wiki/Angicourtoise"),
        u8("Angicourtoise"));
    w1Simple.putToInlinks(u8("http://fr.wiktionary.org/wiki/Angicourtoises"),
        u8("1.3 Références"));
    w1Simple
        .putToOutlinks(
            u8("http://bits.wikimedia.org/fr.wiktionary.org/load.php?debug=false&lang=fr&modules=ext.gadget.TOC%7Cext.wikihiero%7Cmediawiki.legacy.commonPrint%2Cshared%7Cskins.vector&only=styles&skin=vector&*"),
            u8(null));
    w1Simple
        .putToOutlinks(
            u8("http://bits.wikimedia.org/fr.wiktionary.org/load.php?debug=false&lang=fr&modules=site&only=scripts&skin=vector&*"),
            u8(null));
    w1Simple.putToOutlinks(
        u8("http://creativecommons.org/licenses/by-sa/3.0/deed.fr"),
        u8("licence Creative Commons attribution partage à l'identique"));
    w1Simple.putToOutlinks(u8("http://donate.wikimedia.org/wiki"),
        u8("Faire un don"));

    ParseStatus pas = new ParseStatus(new StateManagerImpl());
    pas.setMajorCode(10);
    pas.setMinorCode(11);
    pas.addToArgs(new Utf8("arg1"));
    pas.addToArgs(new Utf8("arg2"));
    w1Simple.setParseStatus(pas);

    ProtocolStatus pos = new ProtocolStatus(new StateManagerImpl());
    pos.setCode(200);
    pos.setLastModified(10000L);
    pos.addToArgs(new Utf8("arg1"));
    pos.addToArgs(new Utf8("arg2"));
    w1Simple.setProtocolStatus(pos);
  }*/

  /**
   * Clear the instances that are used for the tests.
   */
  /*@After
  public void unsetupTest1() {
    // Close store for Test1 instances
    storeTest1S.deleteSchema();
    storeTest1S.close();

    // Reset instances
    t1Simple.clear();
    t1Simple = null;
  }*/

  /**
   * Clear the instances that are used for the tests.
   */
  /*@After
  public void unsetupHost() {
    // Close store for Test1 instances
    storeHost.deleteSchema();
    storeHost.close();

    // Reset instances
    h1Simple.clear();
    h1Simple = null;
  }*/

  /**
   * Clear the instances that are used for the tests.
   */
  /*@After
  public void unsetupWebPage() {
    // Close store for Test1 instances
    storeWebPage.deleteSchema();
    storeWebPage.close();

    // Reset instances
    w1Simple.clear();
    w1Simple = null;
  }*/

  /*@Test
  public void simpleInsert_Test1_straight() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Retrieve the data
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings", "listOfInt",
        "mapOfInt", "scalarString", "scalarDate", "scalarInt" };
    Test1 tfetched = storeTest1S.get("kStraight", fields);

    // Check states, even within maps
    assertFalse(tfetched.isNew());
    assertFalse(tfetched.isDirty());
    assertFalse(tfetched.isDirty("mapOfBytes"));
    assertTrue(tfetched.getMapOfBytes() instanceof StatefulMap<?, ?>);
    for (State e : ((StatefulMap<?, ?>) tfetched.getMapOfBytes()).states()
        .values()) {
      assertEquals(State.CLEAN, e);
    }
    assertFalse(tfetched.isDirty("mapOfInt"));
    assertTrue(tfetched.getMapOfInt() instanceof StatefulMap<?, ?>);
    for (State e : ((StatefulMap<?, ?>) tfetched.getMapOfInt()).states()
        .values()) {
      assertEquals(State.CLEAN, e);
    }
    assertFalse(tfetched.isDirty("mapOfStrings"));
    assertTrue(tfetched.getMapOfStrings() instanceof StatefulMap<?, ?>);
    for (State e : ((StatefulMap<?, ?>) tfetched.getMapOfStrings()).states()
        .values()) {
      assertEquals(State.CLEAN, e);
    }
    assertFalse(tfetched.isDirty("listOfInt"));
    assertTrue(tfetched.getListOfInt() instanceof GenericArray<?>);

    // Compare
    assertEquals(t1Simple.getScalarDate(), tfetched.getScalarDate());
    assertEquals(t1Simple.getScalarInt(), tfetched.getScalarInt());
    assertEquals(t1Simple.getScalarString(), tfetched.getScalarString());

    assertEquals(t1Simple.getListOfInt().size(), tfetched.getListOfInt().size());
    Iterator<Integer> itT1 = t1Simple.getListOfInt().iterator();
    Iterator<Integer> itT2 = tfetched.getListOfInt().iterator();
    for (; itT1.hasNext();) {
      assertEquals(itT1.next(), itT2.next());
    }

    assertEquals(t1Simple.getMapOfBytes().size(), tfetched.getMapOfBytes()
        .size());
    for (Utf8 k : t1Simple.getMapOfBytes().keySet())
      assertEquals(t1Simple.getMapOfBytes().get(k),
          tfetched.getFromMapOfBytes(k));

    assertTrue(tfetched.getMapOfInt() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfInt().size(), tfetched.getMapOfInt().size());
    for (Utf8 k : t1Simple.getMapOfInt().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched.getFromMapOfInt(k));

    assertTrue(tfetched.getMapOfStrings() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfStrings().size(), tfetched.getMapOfStrings()
        .size());
    for (Utf8 k : t1Simple.getMapOfStrings().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched.getFromMapOfInt(k));
  }*/

  /*@Test
  public void simplePut_Test1_new() throws IOException {
    // Check object state
    assertTrue(t1Simple.isNew());
    assertTrue(t1Simple.isDirty());
    for (Field f : t1Simple.getSchema().getFields()) {
      assertTrue(t1Simple.isReadable(f.pos()));
      assertTrue(t1Simple.isDirty(f.pos()));
    }

    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Now the object should not be new or dirty anymore
    assertFalse(t1Simple.isNew());
    assertFalse(t1Simple.isDirty());
    for (Field f : t1Simple.getSchema().getFields()) {
      assertTrue(t1Simple.isReadable(f.pos()));
      assertFalse(t1Simple.isDirty(f.pos()));
    }
  }

  @Test
  public void simpleGet_Test1_partial() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Retrieve the data partially
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings" };
    Test1 tfetched = storeTest1S.get("kStraight", fields);

    // The object should not be new or dirty
    assertFalse(tfetched.isNew());
    assertFalse(tfetched.isDirty());

    // Only the loaded fields should be readable
    assertTrue(tfetched.isReadable("mapOfBytes"));
    assertFalse(tfetched.isDirty("mapOfBytes"));
    assertTrue(tfetched.getMapOfBytes() instanceof StatefulMap<?, ?>);
    for (State e : ((StatefulMap<?, ?>) tfetched.getMapOfBytes()).states()
        .values()) {
      assertEquals(State.CLEAN, e);
    }

    assertTrue(tfetched.isReadable("mapOfStrings"));
    assertFalse(tfetched.isDirty("mapOfStrings"));
    assertTrue(tfetched.getMapOfStrings() instanceof StatefulMap<?, ?>);
    for (State e : ((StatefulMap<?, ?>) tfetched.getMapOfStrings()).states()
        .values()) {
      assertEquals(State.CLEAN, e);
    }

    assertFalse(tfetched.isReadable("mapOfInt"));

    assertFalse(tfetched.isReadable("scalarString"));
    assertFalse(tfetched.isReadable("scalarDate"));
    assertFalse(tfetched.isReadable("scalarInt"));
  }

  @Test
  public void simpleGet_Test1_complete() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Retrieve the whole data
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings", "listOfInt",
        "mapOfInt", "scalarString", "scalarDate", "scalarInt" };
    Test1 tfetched = storeTest1S.get("kStraight", fields);

    // The object should not be new or dirty
    assertFalse(tfetched.isNew());
    assertFalse(tfetched.isDirty());

    // Only the loaded fields should be readable
    assertTrue(tfetched.isReadable("mapOfBytes"));
    assertTrue(tfetched.getMapOfBytes() instanceof StatefulMap<?, ?>);
    assertFalse(tfetched.isDirty("mapOfBytes"));

    assertTrue(tfetched.isReadable("mapOfStrings"));
    assertTrue(tfetched.getMapOfStrings() instanceof StatefulMap<?, ?>);
    assertFalse(tfetched.isDirty("mapOfStrings"));

    assertTrue(tfetched.isReadable("mapOfInt"));
    assertTrue(tfetched.getMapOfInt() instanceof StatefulMap<?, ?>);
    assertFalse(tfetched.isDirty("mapOfInt"));

    assertTrue(tfetched.isReadable("listOfInt"));
    assertTrue(tfetched.getListOfInt() instanceof GenericArray<?>);
    assertFalse(tfetched.isDirty("listOfInt"));

    assertTrue(tfetched.isReadable("scalarString"));
    assertFalse(tfetched.isDirty("scalarString"));
    assertTrue(tfetched.isReadable("scalarDate"));
    assertFalse(tfetched.isDirty("scalarDate"));
    assertTrue(tfetched.isReadable("scalarInt"));
    assertFalse(tfetched.isDirty("scalarInt"));
  }

  @Test
  public void simpleUpdate_Test1_complete() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Retrieve the whole data
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings", "listOfInt",
        "mapOfInt", "scalarString", "scalarDate", "scalarInt" };
    Test1 tfetched1 = storeTest1S.get("kStraight", fields);

    // The object should not be new or dirty
    assertFalse(tfetched1.isNew());
    assertFalse(tfetched1.isDirty());

    // Make it dirty by changing one value
    assertFalse(tfetched1.isDirty());
    assertFalse(tfetched1.isDirty("scalarInt"));
    assertEquals(t1Simple.getScalarInt(), tfetched1.getScalarInt());
    tfetched1.setScalarInt(43);

    // Check it is dirty only on this value
    assertTrue(tfetched1.isDirty());
    assertTrue(tfetched1.isDirty("scalarInt"));
    assertFalse(tfetched1.isDirty("scalarDate"));
    assertFalse(tfetched1.isDirty("scalarString"));
    assertFalse(tfetched1.isDirty("mapOfInt"));
    assertFalse(tfetched1.isDirty("mapOfStrings"));
    assertFalse(tfetched1.isDirty("mapOfBytes"));

    // Insert in the store so that the original is updated, and retrieve it
    storeTest1S.put("kStraight", tfetched1);
    Test1 tfetched2 = storeTest1S.get("kStraight", fields);

    // The object should not be new nor dirty
    assertFalse(tfetched2.isNew());
    assertFalse(tfetched2.isDirty());

    // Check the unchanged values are still the same that the original
    assertTrue(tfetched2.getMapOfBytes() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfBytes().size(), tfetched2.getMapOfBytes()
            .size());
    for (Utf8 k : t1Simple.getMapOfBytes().keySet())
      assertEquals(t1Simple.getMapOfBytes().get(k),
          tfetched2.getFromMapOfBytes(k));

    assertTrue(tfetched2.getMapOfInt() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfInt().size(), tfetched2.getMapOfInt().size());
    for (Utf8 k : t1Simple.getMapOfInt().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched2.getFromMapOfInt(k));

    assertTrue(tfetched2.getMapOfStrings() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfStrings().size(), tfetched2.getMapOfStrings()
            .size());
    for (Utf8 k : t1Simple.getMapOfStrings().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched2.getFromMapOfInt(k));

    assertTrue(tfetched2.getListOfInt() instanceof GenericArray<?>);
    assertEquals(t1Simple.getListOfInt().size(), tfetched2.getListOfInt()
            .size());
    Iterator<Integer> itT1 = t1Simple.getListOfInt().iterator();
    Iterator<Integer> itT2 = tfetched2.getListOfInt().iterator();
    for (; itT1.hasNext();) {
      assertEquals(itT1.next(), itT2.next());
    }

    assertEquals(t1Simple.getScalarDate(), tfetched2.getScalarDate());
    assertEquals(t1Simple.getScalarString(), tfetched2.getScalarString());

    // Only the changed field should have changed
    assertEquals(tfetched1.getScalarInt(), tfetched2.getScalarInt());
  }

  @Test
  public void simpleUpdate_Test1_partial() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Retrieve the whole data
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings", "scalarInt" };
    Test1 tfetched1 = storeTest1S.get("kStraight", fields);

    // The object should not be new or dirty
    assertFalse(tfetched1.isNew());
    assertFalse(tfetched1.isDirty());

    // Make it dirty by changing one value
    assertFalse(tfetched1.isDirty());
    assertFalse(tfetched1.isDirty("scalarInt"));
    assertEquals(t1Simple.getScalarInt(), tfetched1.getScalarInt());
    tfetched1.setScalarInt(43);

    // Check it is dirty only on this value
    assertTrue(tfetched1.isDirty());
    assertTrue(tfetched1.isDirty("scalarInt"));
    assertFalse(tfetched1.isReadable("scalarDate"));
    assertFalse(tfetched1.isReadable("scalarString"));
    assertFalse(tfetched1.isReadable("listOfInt"));
    assertFalse(tfetched1.isReadable("mapOfInt"));
    assertFalse(tfetched1.isDirty("mapOfStrings"));
    assertFalse(tfetched1.isDirty("mapOfBytes"));

    // Insert in the store so that the original is updated, and
    // retrieve it completely
    storeTest1S.put("kStraight", tfetched1);
    fields = new String[] { "mapOfBytes", "mapOfStrings", "mapOfInt",
        "listOfInt", "scalarString", "scalarDate", "scalarInt" };
    Test1 tfetched2 = storeTest1S.get("kStraight", fields);

    // The object should not be new nor dirty and all fields readable
    assertFalse(tfetched2.isNew());
    assertFalse(tfetched2.isDirty());
    assertTrue(tfetched2.isReadable("mapOfBytes"));
    assertTrue(tfetched2.isReadable("mapOfStrings"));
    assertTrue(tfetched2.isReadable("mapOfInt"));
    assertTrue(tfetched2.isReadable("listOfInt"));
    assertTrue(tfetched2.isReadable("scalarInt"));
    assertTrue(tfetched2.isReadable("scalarDate"));
    assertTrue(tfetched2.isReadable("scalarString"));

    // Check the unchanged values are still the same that the original
    assertTrue(tfetched2.getMapOfBytes() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfBytes().size(), tfetched2.getMapOfBytes()
        .size());
    for (Utf8 k : t1Simple.getMapOfBytes().keySet())
      assertEquals(t1Simple.getMapOfBytes().get(k),
          tfetched2.getFromMapOfBytes(k));

    assertTrue(tfetched2.getMapOfInt() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfInt().size(), tfetched2.getMapOfInt().size());
    for (Utf8 k : t1Simple.getMapOfInt().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched2.getFromMapOfInt(k));

    assertTrue(tfetched2.getMapOfStrings() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfStrings().size(), tfetched2.getMapOfStrings()
        .size());
    for (Utf8 k : t1Simple.getMapOfStrings().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched2.getFromMapOfInt(k));

    assertTrue(tfetched2.getListOfInt() instanceof GenericArray<?>);
    assertEquals(t1Simple.getListOfInt().size(), tfetched2.getListOfInt()
        .size());
    Iterator<Integer> itT1 = t1Simple.getListOfInt().iterator();
    Iterator<Integer> itT2 = tfetched2.getListOfInt().iterator();
    for (; itT1.hasNext();) {
      assertEquals(itT1.next(), itT2.next());
    }

    assertEquals(t1Simple.getScalarDate(), tfetched2.getScalarDate());
    assertEquals(t1Simple.getScalarString(), tfetched2.getScalarString());

    // Only the changed field should have changed
    assertEquals(tfetched1.getScalarInt(), tfetched2.getScalarInt());
  }

  @Test
  public void simpleInsert_Test1_rename() throws IOException {
    // Insert the instance in the store
    storeTest1R.put("kRename", t1Simple);

    // Retrieve the data
    String[] fields = new String[] { "mapOfBytes", "mapOfStrings", "listOfInt",
        "mapOfInt", "scalarString", "scalarDate", "scalarInt" };
    Test1 tfetched = storeTest1R.get("kRename", fields);

    // Compare
    assertEquals(t1Simple.getScalarDate(), tfetched.getScalarDate());
    assertEquals(t1Simple.getScalarInt(), tfetched.getScalarInt());
    assertEquals(t1Simple.getScalarString(), tfetched.getScalarString());

    assertTrue(tfetched.getMapOfBytes() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfBytes().size(), tfetched.getMapOfBytes()
        .size());
    for (Utf8 k : t1Simple.getMapOfBytes().keySet())
      assertEquals(t1Simple.getMapOfBytes().get(k),
          tfetched.getFromMapOfBytes(k));

    assertTrue(tfetched.getMapOfInt() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfInt().size(), tfetched.getMapOfInt().size());
    for (Utf8 k : t1Simple.getMapOfInt().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched.getFromMapOfInt(k));

    assertTrue(tfetched.getMapOfStrings() instanceof StatefulMap<?, ?>);
    assertEquals(t1Simple.getMapOfStrings().size(), tfetched.getMapOfStrings()
        .size());
    for (Utf8 k : t1Simple.getMapOfStrings().keySet())
      assertEquals(t1Simple.getMapOfInt().get(k), tfetched.getFromMapOfInt(k));

    assertTrue(tfetched.getListOfInt() instanceof GenericArray<?>);
    assertEquals(t1Simple.getListOfInt().size(), tfetched.getListOfInt().size());
    Iterator<Integer> itT1 = t1Simple.getListOfInt().iterator();
    Iterator<Integer> itT2 = tfetched.getListOfInt().iterator();
    for (; itT1.hasNext();) {
      assertEquals(itT1.next(), itT2.next());
    }
  }

  @Test
  public void simpleInsert_Host() throws IOException {
    // Insert the instance in the store
    storeHost.put("kHost1", h1Simple);

    // Retrieve the data
    String[] fields = new String[] { "metadata", "inlinks", "outlinks" };
    Host hfetched = storeHost.get("kHost1", fields);

    // Compare

    assertTrue(hfetched.getMetadata() instanceof StatefulMap<?, ?>);
    assertEquals(h1Simple.getMetadata().size(), hfetched.getMetadata().size());
    for (Utf8 k : h1Simple.getMetadata().keySet())
      assertEquals(h1Simple.getMetadata().get(k), hfetched.getMetadata().get(k));

    assertTrue(hfetched.getInlinks() instanceof StatefulMap<?, ?>);
    assertEquals(h1Simple.getInlinks().size(), hfetched.getInlinks().size());
    for (Utf8 k : h1Simple.getInlinks().keySet())
      assertEquals(h1Simple.getInlinks().get(k), hfetched.getInlinks().get(k));

    assertTrue(hfetched.getOutlinks() instanceof StatefulMap<?, ?>);
    assertEquals(h1Simple.getOutlinks().size(), hfetched.getOutlinks().size());
    for (Utf8 k : h1Simple.getOutlinks().keySet())
      assertEquals(h1Simple.getOutlinks().get(k), hfetched.getOutlinks().get(k));
  }

  @Test
  public void simpleInsert_WebPage() throws IOException {
    // Insert the instance in the store
    storeWebPage.put("com.9gag:http/gag/6301703", w1Simple);

    // Retrieve the data
    WebPage wfetched = storeWebPage.get("com.9gag:http/gag/6301703", null);

    // Compare
    assertEquals(w1Simple.getBaseUrl(), wfetched.getBaseUrl());
    assertEquals(w1Simple.getContentType(), wfetched.getContentType());
    assertArrayEquals(w1Simple.getContent().array(), wfetched.getContent()
        .array());
    assertEquals(w1Simple.getFetchInterval(), wfetched.getFetchInterval());
    assertEquals(w1Simple.getFetchTime(), wfetched.getFetchTime());
    assertEquals(w1Simple.getModifiedTime(), wfetched.getModifiedTime());
    assertEquals(w1Simple.getPrevFetchTime(), wfetched.getPrevFetchTime());
    assertEquals(w1Simple.getReprUrl(), wfetched.getReprUrl());
    assertEquals(w1Simple.getRetriesSinceFetch(),
            wfetched.getRetriesSinceFetch());
    assertEquals(w1Simple.getScore(), wfetched.getScore(), 0.00001);
    assertEquals(w1Simple.getStatus(), wfetched.getStatus());
    assertEquals(w1Simple.getText(), wfetched.getText());
    assertEquals(w1Simple.getTitle(), wfetched.getTitle());

    assertEquals(w1Simple.getHeaders().size(), wfetched.getHeaders().size());
    for (Entry<Utf8, Utf8> e : wfetched.getHeaders().entrySet()) {
      assertEquals(w1Simple.getHeaders().get(e.getKey()), e.getValue());
    }

    assertEquals(w1Simple.getMarkers().size(), wfetched.getMarkers().size());
    for (Entry<Utf8, Utf8> e : wfetched.getMarkers().entrySet()) {
      assertEquals(w1Simple.getMarkers().get(e.getKey()), e.getValue());
    }

    assertEquals(w1Simple.getMetadata().size(), wfetched.getMetadata().size());
    for (Entry<Utf8, ByteBuffer> e : wfetched.getMetadata().entrySet()) {
      assertArrayEquals(w1Simple.getMetadata().get(e.getKey()).array(), e
          .getValue().array());
    }

    assertEquals(w1Simple.getInlinks().size(), wfetched.getInlinks().size());
    for (Entry<Utf8, Utf8> e : wfetched.getInlinks().entrySet()) {
      assertEquals(w1Simple.getInlinks().get(e.getKey()), e.getValue());
    }

    assertEquals(w1Simple.getOutlinks().size(), wfetched.getOutlinks().size());
    for (Entry<Utf8, Utf8> e : wfetched.getOutlinks().entrySet()) {
      assertEquals(w1Simple.getOutlinks().get(e.getKey()), e.getValue());
    }
  }*/

  /**
   * This test corresponds to the context used by Nutch generate job.
   * 
   * @throws IOException
   */
  /*@Test
  public void simpleUpdate_WebPage_partial() throws IOException {
    // Insert the instance in the store
    w1Simple.getMarkers().remove("_gnmrk_");
    storeWebPage.put("info.fabienpoulard.wwww:http/somepage", w1Simple);

    // Retrieve the partial data
    String[] fields = new String[] { "prevFetchTime", "score", "status",
        "markers" };
    WebPage wFetched1 = storeWebPage.get(
        "info.fabienpoulard.wwww:http/somepage", fields);

    // The object should not be new or dirty and only selected fields
    // should be readable
    assertFalse(wFetched1.isNew());
    assertFalse(wFetched1.isDirty());
    for (Field f : wFetched1.getSchema().getFields()) {
      if ("prevFetchTime".equals(f.name()) || "score".equals(f.name())
          || "status".equals(f.name()) || "markers".equals(f.name()))
        assertTrue(wFetched1.isReadable(f.pos()));
      else
        assertFalse(wFetched1.isReadable(f.pos()));
    }

    // Add a marker to page
    assertFalse(wFetched1.isDirty());
    assertFalse(wFetched1.isDirty("markers"));
    wFetched1.putToMarkers(new Utf8("_gnmrk_"),
        new Utf8("1358868583-795703838"));
    assertTrue(wFetched1.isDirty());
    assertTrue(wFetched1.isDirty("markers"));

    // Update in the store and retrieve
    storeWebPage.put("info.fabienpoulard.wwww:http/somepage", wFetched1);
    WebPage wFetched2 = storeWebPage.get(
        "info.fabienpoulard.wwww:http/somepage", fields);

    // The object should not be new or dirty and only selected fields
    // should be readable
    assertFalse(wFetched2.isNew());
    assertFalse(wFetched2.isDirty());
    for (Field f : wFetched2.getSchema().getFields()) {
      if ("prevFetchTime".equals(f.name()) || "score".equals(f.name())
          || "status".equals(f.name()) || "markers".equals(f.name()))
        assertTrue(wFetched2.isReadable(f.pos()));
      else
        assertFalse(wFetched2.isReadable(f.pos()));
    }

    // Check the presence of the marker
    assertEquals(wFetched1.getMarkers().size(), wFetched2.getMarkers().size());
    assertTrue(wFetched2.getMarkers().containsKey(new Utf8("_gnmrk_")));
    assertEquals(new Utf8("1358868583-795703838"),
        wFetched2.getMarkers().get(new Utf8("_gnmrk_")));
  }

  @Test
  public void simpleDelete() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Instance is persisted into store
    assertNotNull(storeTest1S.get("kStraight"));

    // Remove instance from store
    storeTest1S.delete("kStraight");

    // Instance is removed from store
    assertNull(storeTest1S.get("kStraight"));
  }

  @Test
  public void simpleDeleteWithQuery_startEndKey() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);

    // Instance is persisted into store
    assertNotNull(storeTest1S.get("kStraight"));

    // Empty query should note remove item
    final Query<String, Test1> invalidQuery = storeTest1S.newQuery();
    invalidQuery.setStartKey("invalidKey");
    invalidQuery.setEndKey("invalidKey");
    long deletedCount = storeTest1S.deleteByQuery(invalidQuery);
    assertEquals(0, deletedCount);
    assertNotNull(storeTest1S.get("kStraight"));

    // Instance is removed from store
    final Query<String, Test1> realQuery = storeTest1S.newQuery();
    realQuery.setStartKey("kStraight");
    realQuery.setEndKey("kStraight");
    deletedCount = storeTest1S.deleteByQuery(realQuery);
    assertEquals(1, deletedCount);
    assertNull(storeTest1S.get("kStraight"));
  }

  @Test
  public void simpleExecuteWithAllFields() throws IOException {
    // Insert the instance in the store
    storeTest1S.put("kStraight", t1Simple);
    assertNotNull(storeTest1S.get("kStraight"));

    // Search (without criteria) with all fields
    final Query<String, Test1> emptyQuery = storeTest1S.newQuery();
    emptyQuery.setFields(Test1._ALL_FIELDS);
    Result<String, Test1> results = storeTest1S.execute(emptyQuery);

    // There is *one* result
    assertTrue(results.next());
    final Test1 object = results.get();
    assertNotNull(object);
    assertNotNull(object.getScalarDate());

    // No other item after first one
    assertFalse(results.next());
  }*/
}
