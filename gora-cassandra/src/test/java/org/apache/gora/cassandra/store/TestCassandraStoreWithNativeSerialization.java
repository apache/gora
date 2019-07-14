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

import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.cassandra.example.generated.nativeSerialization.ComplexTypes;
import org.apache.gora.cassandra.example.generated.nativeSerialization.User;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * This class tests Cassandra Store functionality with Cassandra Native Serialization.
 */
public class TestCassandraStoreWithNativeSerialization {
  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static CassandraStore<UUID, User> userDataStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    userDataStore = (CassandraStore<UUID, User>) testDriver.createDataStore(UUID.class, User.class);
  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    parameter.setProperty(CassandraStoreParameters.PORT, "9042");
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "native");
    parameter.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    parameter.setProperty(CassandraStoreParameters.CLUSTER_NAME, "Test Cluster");
    parameter.setProperty("gora.cassandrastore.mapping.file", "nativeSerialization/gora-cassandra-mapping.xml");
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
   * In this test case, put and get behavior of the data store are testing.
   * @throws GoraException 
   */
  @Test
  public void testSimplePutAndGet() throws GoraException {
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "madhawa", Date.from(Instant.now()));
    // storing data;
    userDataStore.put(id, user1);
    // get data;
    User olduser = userDataStore.get(id);
    Assert.assertEquals(olduser.getName(), user1.getName());
    Assert.assertEquals(olduser.getDateOfBirth(), user1.getDateOfBirth());
  }

  /**
   * In this test case, put and delete behavior of the data store are testing.
   * @throws GoraException 
   */
  @Test
  public void testSimplePutDeleteAndGet() throws GoraException {
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "kasun", Date.from(Instant.now()));
    // storing data;
    userDataStore.put(id, user1);
    // get data;
    User olduser = userDataStore.get(user1.getUserId());
    Assert.assertEquals(olduser.getName(), user1.getName());
    Assert.assertEquals(olduser.getDateOfBirth(), user1.getDateOfBirth());
    // delete data;
    userDataStore.delete(user1.getUserId());
    // get data
    User deletedUser = userDataStore.get(id);
    Assert.assertNull(deletedUser);
  }

  /**
   * In this test case, schema exists method behavior of the data store is testing.
   * @throws GoraException 
   */
  @Test()
  public void testSchemaExists() throws GoraException {
    userDataStore.deleteSchema();
    Assert.assertFalse(userDataStore.schemaExists());
    userDataStore.createSchema();
    Assert.assertTrue(userDataStore.schemaExists());
  }

  /**
   * In this test case, schema exists method behavior of the data store is testing.
   * @throws GoraException 
   */
  @Test
  public void testTruncateSchema() throws GoraException {
    if (!userDataStore.schemaExists()) {
      userDataStore.createSchema();
    }
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "Madhawa Kasun", Date.from(Instant.now()));
    userDataStore.put(id, user1);
    User olduser = userDataStore.get(id);
    Assert.assertEquals(olduser.getName(), user1.getName());
    Assert.assertEquals(olduser.getDateOfBirth(), user1.getDateOfBirth());
    userDataStore.truncateSchema();
    olduser = userDataStore.get(id);
    Assert.assertNull(olduser);
  }

  /**
   * In this test case, get with fields method behavior of the data store is testing.
   * @throws GoraException 
   */
  @Test
  public void testGetWithFields() throws GoraException {
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "Madhawa Kasun Gunasekara", Date.from(Instant.now()));
    userDataStore.put(id, user1);
    // get data;
    User olduser = userDataStore.get(user1.getUserId());
    Assert.assertEquals(olduser.getName(), user1.getName());
    Assert.assertEquals(olduser.getDateOfBirth(), user1.getDateOfBirth());
    User olduserWithFields = userDataStore.get(id, new String[]{"name"});
    Assert.assertNull(olduserWithFields.getDateOfBirth());
  }

  /**
   * In this test case, get with fields method behavior of the data store is testing.
   */
  @Test
  public void testExecute() throws Exception {
    userDataStore.truncateSchema();
    Map<UUID, User> users = new HashMap<>();
    UUID id1 = UUID.randomUUID();
    User user1 = new User(id1, "user1", Date.from(Instant.now()));
    users.put(id1, user1);
    userDataStore.put(id1, user1);
    UUID id2 = UUID.randomUUID();
    User user2 = new User(id2, "user2", Date.from(Instant.now()));
    users.put(id2, user2);
    userDataStore.put(id2, user2);
    UUID id3 = UUID.randomUUID();
    User user3 = new User(id3, "user3", Date.from(Instant.now()));
    users.put(id3, user3);
    userDataStore.put(id3, user3);
    Query<UUID, User> query1 = userDataStore.newQuery();
    Result<UUID, User> result1 = userDataStore.execute(query1);
    int i = 0;
    Assert.assertEquals(result1.getProgress(), 0.0, 0.0);
    while (result1.next()) {
      // check objects values
      Assert.assertEquals(result1.get().getName(), users.get(result1.getKey()).getName());
      Assert.assertEquals(result1.get().getDateOfBirth(), users.get(result1.getKey()).getDateOfBirth());
      Assert.assertEquals(result1.get().getUserId(), users.get(result1.getKey()).getUserId());
      i++;
    }
    Assert.assertEquals(result1.getProgress(), 1.0, 0.0);
    Assert.assertEquals(3, i);

    // Check limit query
    Query<UUID, User> query2 = userDataStore.newQuery();
    query2.setLimit(2);
    Result<UUID, User> result2 = userDataStore.execute(query2);
    i = 0;
    while (result2.next()) {
      Assert.assertEquals(result2.get().getName(), users.get(result2.getKey()).getName());
      Assert.assertEquals(result2.get().getDateOfBirth(), users.get(result2.getKey()).getDateOfBirth());
      Assert.assertEquals(result2.get().getUserId(), users.get(result2.getKey()).getUserId());
      i++;
    }
    Assert.assertEquals(2, i);

    // check key element
    Query<UUID, User> query3 = userDataStore.newQuery();
    query3.setKey(id1);

    Result<UUID, User> result3 = userDataStore.execute(query3);
    i = 0;
    while (result3.next()) {
      Assert.assertEquals(result3.get().getName(), users.get(result3.getKey()).getName());
      Assert.assertEquals(result3.get().getDateOfBirth(), users.get(result3.getKey()).getDateOfBirth());
      Assert.assertEquals(result3.get().getUserId(), users.get(result3.getKey()).getUserId());
      i++;
    }
    Assert.assertEquals(1, i);
  }

  /**
   * In this test case, delete by query method behavior of the data store is testing.
   */
  @Test
  public void testDeleteByQuery() throws Exception {
    userDataStore.truncateSchema();
    UUID id1 = UUID.randomUUID();
    User user1 = new User(id1, "user1", Date.from(Instant.now()));
    userDataStore.put(id1, user1);
    UUID id2 = UUID.randomUUID();
    User user2 = new User(id2, "user2", Date.from(Instant.now()));
    userDataStore.put(id2, user2);
    Query<UUID, User> query1 = userDataStore.newQuery();
    query1.setKey(id1);
    userDataStore.deleteByQuery(query1);
    User user = userDataStore.get(id1);
    Assert.assertNull(user);

    //test deleteByFields
    Query<UUID, User> query2 = userDataStore.newQuery();
    query2.setKey(id2);
    query2.setFields("name");
    userDataStore.deleteByQuery(query2);
    User partialDeletedUser = userDataStore.get(id2);
    Assert.assertNull(partialDeletedUser.getName());
    Assert.assertEquals(partialDeletedUser.getDateOfBirth(), user2.getDateOfBirth());
  }

  /**
   * In this test case, update by quert method behavior of the data store is testing.
   * @throws GoraException 
   */
  @Test
  public void testUpdateByQuery() throws GoraException {
    userDataStore.truncateSchema();
    UUID id1 = UUID.randomUUID();
    User user1 = new User(id1, "user1", Date.from(Instant.now()));
    userDataStore.put(id1, user1);
    UUID id2 = UUID.randomUUID();
    User user2 = new User(id2, "user2", Date.from(Instant.now()));
    userDataStore.put(id2, user2);
    Query<UUID, User> query1 = userDataStore.newQuery();
    if (query1 instanceof CassandraQuery) {
      ((CassandraQuery) query1).addUpdateField("name", "madhawa");
    }
    query1.setKey(id1);
    if (userDataStore instanceof CassandraStore) {
      userDataStore.updateByQuery(query1);
    }
    User user = userDataStore.get(id1);
    Assert.assertEquals(user.getName(), "madhawa");
  }

  @Test
  public void testComplexTypes() throws GoraException {
    DataStore<String, ComplexTypes> documentDataStore = testDriver.createDataStore(String.class, ComplexTypes.class);
    ComplexTypes document = new ComplexTypes("document1");
    document.setIntArrayDataType(new int[]{1, 2, 3});
    document.setStringArrayDataType(new String[]{"madhawa", "kasun", "gunasekara", "pannipitiya", "srilanka"});
    document.setListDataType(new ArrayList<>(Arrays.asList("gora", "nutch", "tika", "opennlp", "olingo")));
    document.setSetDataType(new HashSet<>(Arrays.asList("important", "keeper")));
    HashMap<String, String> map = new HashMap<>();
    map.put("LK", "Colombo");
    document.setMapDataType(map);
    documentDataStore.put("document1", document);
    ComplexTypes retrievedDocuemnt = documentDataStore.get("document1");
    // verify list data
    for (int i = 0; i < document.getListDataType().size(); i++) {
      Assert.assertEquals(document.getListDataType().get(i), retrievedDocuemnt.getListDataType().get(i));
    }
    // verify set data
    for (int i = 0; i < document.getSetDataType().size(); i++) {
      Assert.assertTrue(Arrays.equals(document.getSetDataType().toArray(), retrievedDocuemnt.getSetDataType().toArray()));
    }
    // verify array data
    for (int i = 0; i < document.getIntArrayDataType().length; i++) {
      Assert.assertTrue(Arrays.equals(document.getIntArrayDataType(), retrievedDocuemnt.getIntArrayDataType()));
    }
    for (int i = 0; i < document.getStringArrayDataType().length; i++) {
      Assert.assertTrue(Arrays.equals(document.getStringArrayDataType(), retrievedDocuemnt.getStringArrayDataType()));
    }
    // verify map data
    Assert.assertEquals(map.get("LK"), retrievedDocuemnt.getMapDataType().get("LK"));
  }
}
