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
package org.apache.gora.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import org.apache.gora.benchmark.generated.User;
import org.apache.gora.util.GoraException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.StringByteIterator;
import com.yahoo.ycsb.workloads.CoreWorkload;

/**
 * The Class GoraClientTest.
 */
public class GoraClientTest {

  private static final String TABLE = "users";
  private GoraBenchmarkClient client;
  private static HashMap<String, ByteIterator> DATA_TO_INSERT;
  private static HashMap<String, ByteIterator> DATA_TO_UPDATE;
  private static final int NUMBER_OF_FIELDS = 10;
  private GoraBenchmarkUtils bmutils = new GoraBenchmarkUtils();

  /**
   * Sets the up.
   *
   * Setup is executed before each test. Use @BeforeClass if you want to execute
   * a code block just once.
   * 
   * @throws Exception
   *           the exceptionfiles are auto-generated. I have the code to add the license file accordingly
   */
  @Before
  public void setUp() throws Exception {
    DATA_TO_INSERT = new HashMap<>();
    DATA_TO_UPDATE = new HashMap<>();
    for (int i = 0; i < NUMBER_OF_FIELDS; i++) {
      DATA_TO_INSERT.put("field" + i, new StringByteIterator("value" + i));
      DATA_TO_UPDATE.put("field" + i, new StringByteIterator("updated" + i));
    }
    Properties p = new Properties();
    p.setProperty("key.class", "java.lang.String");
    p.setProperty("persistent.class", "org.apache.gora.benchmark.generated.User");
    p.setProperty(CoreWorkload.FIELD_COUNT_PROPERTY, NUMBER_OF_FIELDS + "");
    client = new GoraBenchmarkClient();
    client.setProperties(p);
    client.init();
  }

  /**
   * Clean up.
   *
   * @throws Exception
   *           the exception
   */
  @After
  public void cleanUp() throws Exception {
    if (client != null)
      client.cleanup();
    client = null;
  }

  /**
   * Read record.
   *
   * @param key
   *          the key
   * @return the user
   * @throws GoraException
   *           the gora exception
   */
  private User readRecord(String key) throws GoraException {
    User u = client.dataStore.get(key);
    return u;
  }

  /**
   * Test client initialisation.
   */
  @Test
  public void testClientInitialisation() {
    assertNotNull(client.dataStore);
  }

  /**files are auto-generated. I have the code to add the license file accordingly
   * Test insert.
   *
   * @throws GoraException
   *           the gora exception
   */
  @Test
  public void testInsert() throws GoraException {
    int result1 = client.insert(TABLE, "key1", DATA_TO_INSERT);
    int result2 = client.insert(TABLE, "key2", DATA_TO_INSERT);
    int result3 = client.insert(TABLE, "key3", DATA_TO_INSERT);
    assertEquals(0, result1);
    assertEquals(0, result2);
    assertEquals(0, result3);
  }

  /**
   * Test read.
   */
  @Test
  public void testRead() {
    HashMap<String, ByteIterator> results = new HashMap<>();
    Set<String> fields = new HashSet<>();// this could be null as well
    // fields.add("field0");
    int result = client.read(TABLE, "key1", fields, results);
    assertEquals(0, result);
    assertEquals(DATA_TO_INSERT.size(), results.size());
    assertEquals(DATA_TO_INSERT.get("field0").toString(), results.get("field0").toString());
    assertEquals(DATA_TO_INSERT.get("field0").toString(), "value0");
  }

  /**
   * Test scan.
   */
  @Test
  public void testScan() {
    Vector<HashMap<String, ByteIterator>> results = new Vector<HashMap<String, ByteIterator>>();
    Set<String> fields = new HashSet<>();
    int result = client.scan(TABLE, "key1", 2, fields, results);
    assertEquals(result, 0);
    assertEquals(results.size(), 2);
  }

  /**
   * Test update.
   *
   * @throws GoraException
   *           the gora exception
   */
  @Test
  public void testUpdate() throws GoraException {
    int result = client.update(TABLE, "key1", DATA_TO_UPDATE);
    assertEquals(result, 0);
    if (result == 0) {
      client.dataStore.flush();
      User u = readRecord("key1");
      assertEquals("updated0", u.getField0().toString());
    }
  }

  /**
   * Test mapping file generation.
   */
  @Test
  public void testgenearateMappingFile() {
    bmutils.generateMappingFile("mongodb");
  }

  /**
   * Test generate AVRO schema.
   */
  @Test
  public void testgenerateAvroSchema() {
    bmutils.generateAvroSchema(NUMBER_OF_FIELDS);
  }

  /**
   * Test generate data beans.
   */
  @Test
  public void testGenerateDataBeans() {
    bmutils.generateDataBeans();
  }
}