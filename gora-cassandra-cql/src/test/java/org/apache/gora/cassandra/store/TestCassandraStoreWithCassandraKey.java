/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.store;

import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraKey;
import org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class tests Cassandra Store functionality with CassandraKey.
 */
public class TestCassandraStoreWithCassandraKey {
  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static CassandraStore<CassandraKey, CassandraRecord> cassandraRecordDataStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    cassandraRecordDataStore = (CassandraStore<CassandraKey, CassandraRecord>) testDriver.createDataStore(CassandraKey.class, CassandraRecord.class);
  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    parameter.setProperty(CassandraStoreParameters.PORT, "9042");
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "avro");
    parameter.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    parameter.setProperty(CassandraStoreParameters.CLUSTER_NAME, "Test Cluster");
    parameter.setProperty("gora.cassandrastore.mapping.file", "compositeKey/gora-cassandra-mapping.xml");
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    testDriver.tearDownClass();
  }

  @After
  public void tearDown() throws Exception {
    testDriver.tearDown();
  }

  /**
   * In this test case, schema exists method behavior of the data store is testing.
   */
  @Test
  public void testSchemaRelatedBehaviour() {
    cassandraRecordDataStore.createSchema();
    Assert.assertTrue(cassandraRecordDataStore.schemaExists());
    cassandraRecordDataStore.deleteSchema();
    Assert.assertFalse(cassandraRecordDataStore.schemaExists());
    cassandraRecordDataStore.createSchema();
    Assert.assertTrue(cassandraRecordDataStore.schemaExists());
  }

  /**
   * In this test case, get, put and delete methods behaviour of the data store is testing.
   */
  @Test
  public void testSimplePutGet() {
    cassandraRecordDataStore.createSchema();
    CassandraRecord record = new CassandraRecord();
    record.setDataLong(719411002L);
    record.setDataString(new Utf8("M.K.H. Gunasekara"));
    record.setDataInt(144);
    record.setDataBytes(ByteBuffer.wrap("No 144, Gunasekara Mawatha, Mattumgala, Ragama".getBytes(Charset.defaultCharset())));
    record.setDataDouble(3.14159d);
    CassandraKey key = new CassandraKey();
    key.setTimestamp(2027L);
    key.setUrl("www.apache.org");
    cassandraRecordDataStore.put(key, record);
    CassandraRecord retrievedRecord = cassandraRecordDataStore.get(key);
    Assert.assertEquals(record.getDataInt(), retrievedRecord.getDataInt());
    Assert.assertEquals(record.getDataString(), retrievedRecord.getDataString());
    Assert.assertEquals(record.getDataLong(), retrievedRecord.getDataLong());
    Assert.assertEquals(record.getDataBytes(), retrievedRecord.getDataBytes());
    Assert.assertEquals(record.getDataDouble(), retrievedRecord.getDataDouble());
    cassandraRecordDataStore.delete(key);
    Assert.assertNull(cassandraRecordDataStore.get(key));
  }

  /**
   * In this test case, execute and deleteByQuery methods behaviour of the data store is testing.
   *
   * @throws Exception
   */
  @Test
  public void testExecuteQuery() throws Exception {
    Query<CassandraKey, CassandraRecord> query = cassandraRecordDataStore.newQuery();
    cassandraRecordDataStore.truncateSchema();
    CassandraKey key = new CassandraKey();
    key.setTimestamp(2027L);
    key.setUrl("www.apache.org");
    query.setKey(key);
    Result<CassandraKey, CassandraRecord> result = query.execute();
    Assert.assertFalse(result.next());
    CassandraRecord record = new CassandraRecord();
    record.setDataLong(719411002L);
    record.setDataString(new Utf8("M.K.H. Gunasekara"));
    record.setDataInt(144);
    record.setDataBytes(ByteBuffer.wrap("No 144, Gunasekara Mawatha, Mattumgala, Ragama".getBytes(Charset.defaultCharset())));
    record.setDataDouble(3.14159d);
    // test simple put and query with setKey
    cassandraRecordDataStore.put(key, record);
    CassandraRecord retrievedRecord = cassandraRecordDataStore.get(key);
    Assert.assertEquals(record.getDataInt(), retrievedRecord.getDataInt());
    Assert.assertEquals(record.getDataString(), retrievedRecord.getDataString());
    Assert.assertEquals(record.getDataLong(), retrievedRecord.getDataLong());
    Assert.assertEquals(record.getDataBytes(), retrievedRecord.getDataBytes());
    Assert.assertEquals(record.getDataDouble(), retrievedRecord.getDataDouble());
    result = query.execute();
    Assert.assertTrue(result.next());
    // verify data
    retrievedRecord = result.get();
    Assert.assertEquals(record.getDataInt(), retrievedRecord.getDataInt());
    Assert.assertEquals(record.getDataString(), retrievedRecord.getDataString());
    Assert.assertEquals(record.getDataLong(), retrievedRecord.getDataLong());
    Assert.assertEquals(record.getDataBytes(), retrievedRecord.getDataBytes());
    Assert.assertEquals(record.getDataDouble(), retrievedRecord.getDataDouble());
    // test delete by query
    cassandraRecordDataStore.deleteByQuery(query);
    result = query.execute();
    Assert.assertFalse(result.next());
    // test empty query
    Query<CassandraKey, CassandraRecord> emptyQuery = cassandraRecordDataStore.newQuery();
    result = emptyQuery.execute();
    Assert.assertFalse(result.next());
    cassandraRecordDataStore.put(key, record);
    result = query.execute();
    Assert.assertTrue(result.next());
  }

  @Test
  public void testExecuteQueryWithRange() throws Exception {
    // test Range with Query
    cassandraRecordDataStore.truncateSchema();
    //insert data
    CassandraRecord record1 = new CassandraRecord();
    CassandraRecord record2 = new CassandraRecord();
    CassandraRecord record3 = new CassandraRecord();
    CassandraRecord record4 = new CassandraRecord();
    record1.setDataLong(719411002L);
    record1.setDataString(new Utf8("Madawa"));
    record1.setDataInt(100);
    record2.setDataLong(712778588L);
    record2.setDataString(new Utf8("Kasun"));
    record2.setDataInt(101);
    record3.setDataLong(716069539L);
    record3.setDataString(new Utf8("Charith"));
    record3.setDataInt(102);
    record4.setDataLong(112956051L);
    record4.setDataString(new Utf8("Bhanuka"));
    record4.setDataInt(103);
    CassandraKey key1 = new CassandraKey();
    key1.setTimestamp(200L);
    key1.setUrl("www.apache.org");
    CassandraKey key2 = new CassandraKey();
    key2.setTimestamp(205L);
    key2.setUrl("www.apache.org");
    CassandraKey key3 = new CassandraKey();
    key3.setTimestamp(210L);
    key3.setUrl("www.apache.org");
    CassandraKey key4 = new CassandraKey();
    key4.setTimestamp(215L);
    key4.setUrl("www.apache.org");
    cassandraRecordDataStore.put(key1, record1);
    cassandraRecordDataStore.put(key2, record2);
    cassandraRecordDataStore.put(key3, record3);
    cassandraRecordDataStore.put(key4, record4);
    Query<CassandraKey, CassandraRecord> rangeQuery = cassandraRecordDataStore.newQuery();
    rangeQuery.setStartKey(key2);
    rangeQuery.setEndKey(key2);
    Result<CassandraKey, CassandraRecord> result = rangeQuery.execute();
    int i = 0;
    while (result.next()) {
      i++;
    }
    Assert.assertEquals(1, i);

    rangeQuery.setStartKey(key2);
    rangeQuery.setEndKey(key3);
    result = rangeQuery.execute();
    i = 0;
    while (result.next()) {
      i++;
    }
    Assert.assertEquals(2, i);
  }

  @Test
  public void testUpdateByQuery() {
    cassandraRecordDataStore.truncateSchema();
    //insert data
    CassandraRecord record1 = new CassandraRecord();
    CassandraRecord record2 = new CassandraRecord();
    CassandraRecord record3 = new CassandraRecord();
    CassandraRecord record4 = new CassandraRecord();
    record1.setDataLong(719411002L);
    record1.setDataString(new Utf8("Madawa"));
    record1.setDataInt(100);
    record2.setDataLong(712778588L);
    record2.setDataString(new Utf8("Kasun"));
    record2.setDataInt(101);
    record3.setDataLong(716069539L);
    record3.setDataString(new Utf8("Charith"));
    record3.setDataInt(102);
    record4.setDataLong(112956051L);
    record4.setDataString(new Utf8("Bhanuka"));
    record4.setDataInt(103);
    CassandraKey key1 = new CassandraKey();
    key1.setTimestamp(200L);
    key1.setUrl("www.apache.org");
    CassandraKey key2 = new CassandraKey();
    key2.setTimestamp(205L);
    key2.setUrl("www.apache.org");
    CassandraKey key3 = new CassandraKey();
    key3.setTimestamp(210L);
    key3.setUrl("www.apache.org");
    CassandraKey key4 = new CassandraKey();
    key4.setTimestamp(215L);
    key4.setUrl("www.apache.org");
    cassandraRecordDataStore.put(key1, record1);
    cassandraRecordDataStore.put(key2, record2);
    cassandraRecordDataStore.put(key3, record3);
    cassandraRecordDataStore.put(key4, record4);
    CassandraQuery<CassandraKey, CassandraRecord> query = new CassandraQuery<>(cassandraRecordDataStore);
    query.setKey(key1);
    query.addUpdateField("dataString", new Utf8("test123"));
    cassandraRecordDataStore.updateByQuery(query);
    CassandraRecord result = cassandraRecordDataStore.get(key1);
    Assert.assertEquals(new Utf8("test123"), result.getDataString());
  }


  @Test
  public void testDataTypes() {
    cassandraRecordDataStore.truncateSchema();
    CassandraRecord record = new CassandraRecord();
    record.setDataLong(719411002L);
    record.setDataString(new Utf8("M.K.H. Gunasekara"));
    record.setDataInt(144);
    record.setDataBytes(ByteBuffer.wrap("No 144, Gunasekara Mawatha, Mattumgala, Ragama".getBytes(Charset.defaultCharset())));
    record.setDataDouble(3.14159d);
    ArrayList<Double> doubles = new ArrayList<>();
    doubles.add(2.1D);
    doubles.add(3.14D);
    record.setArrayDouble(doubles);
    ArrayList<Integer> integers = new ArrayList<>();
    integers.add(2);
    integers.add(3);
    record.setArrayInt(integers);
    ArrayList<Long> longs = new ArrayList<>();
    longs.add(2L);
    longs.add(3L);
    record.setArrayLong(longs);
    ArrayList<CharSequence> strings = new ArrayList<>();
    strings.add(new Utf8("Hello World"));
    strings.add(new Utf8("Srilanka"));
    record.setArrayString(strings);
    HashMap<CharSequence, Double > map = new HashMap<>();
    map.put(new Utf8("Life"), 7.3D);
    record.setMapDouble(map);
    CassandraKey key = new CassandraKey();
    key.setTimestamp(2027L);
    key.setUrl("www.apache.org");
    cassandraRecordDataStore.put(key, record);
    CassandraRecord retrievedRecord = cassandraRecordDataStore.get(key);
    Assert.assertEquals(record.getDataInt(), retrievedRecord.getDataInt());
    Assert.assertEquals(record.getDataString(), retrievedRecord.getDataString());
    Assert.assertEquals(record.getDataLong(), retrievedRecord.getDataLong());
    Assert.assertEquals(record.getDataBytes(), retrievedRecord.getDataBytes());
    Assert.assertEquals(record.getDataDouble(), retrievedRecord.getDataDouble());
    int i =0;
    for(Double obj : retrievedRecord.getArrayDouble()) {
      Assert.assertEquals(doubles.get(i), obj);
      i++;
    }
    i = 0;
    for(Integer obj : retrievedRecord.getArrayInt()) {
      Assert.assertEquals(integers.get(i), obj);
      i++;
    }
    i = 0;
    for(Long obj : retrievedRecord.getArrayLong()) {
      Assert.assertEquals(longs.get(i), obj);
      i++;
    }
    i = 0;
    for(CharSequence obj : retrievedRecord.getArrayString()) {
      Assert.assertEquals(strings.get(i), obj);
      i++;
    }

    for(Map.Entry entry : map.entrySet()) {
      Assert.assertEquals(entry.getValue(), retrievedRecord.getMapDouble().get(entry.getKey()));
    }
  }
}
