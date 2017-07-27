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
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * This class tests Cassandra Store functionality with CassandraKey.
 */
public class TestCassandraStoreWithCassandraKey {
  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static DataStore<CassandraKey, CassandraRecord> cassandraRecordDataStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    cassandraRecordDataStore = testDriver.createDataStore(CassandraKey.class, CassandraRecord.class);
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

  @After
  public void tearDown() throws Exception {
    testDriver.tearDown();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    testDriver.tearDownClass();
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
   * @throws Exception
   */
  @Test
  public void testExecuteQuery() throws Exception {
    Query query = cassandraRecordDataStore.newQuery();
    cassandraRecordDataStore.truncateSchema();
    CassandraKey key = new CassandraKey();
    key.setTimestamp(2027L);
    key.setUrl("www.apache.org");
    query.setKey(key);
    Result result = query.execute();
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
    retrievedRecord = (CassandraRecord) result.get();
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
    Query emptyQuery = cassandraRecordDataStore.newQuery();
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
    cassandraRecordDataStore.put(key1,record1);
    cassandraRecordDataStore.put(key2,record2);
    cassandraRecordDataStore.put(key3,record3);
    cassandraRecordDataStore.put(key4,record4);
    Query rangeQuery = cassandraRecordDataStore.newQuery();
    rangeQuery.setStartKey(key2);
    rangeQuery.setEndKey(key2);
    Result result = rangeQuery.execute();
    int i = 0;
    while (result.next()) {
      i++;
    }
    Assert.assertEquals(1,i);

    rangeQuery.setStartKey(key2);
    rangeQuery.setEndKey(key3);
    result = rangeQuery.execute();
    i = 0;
    while (result.next()) {
      i++;
    }
    Assert.assertEquals(2,i);
  }

}
