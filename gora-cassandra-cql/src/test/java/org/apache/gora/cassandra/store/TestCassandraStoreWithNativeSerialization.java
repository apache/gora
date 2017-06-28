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
import org.apache.gora.cassandra.example.generated.nativeSerialization.User;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * This class tests Cassandra Store functionality with Cassandra Native Serialization.
 */
public class TestCassandraStoreWithNativeSerialization {
  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static DataStore<UUID, User> userDataStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    userDataStore = testDriver.createDataStore(UUID.class, User.class);
  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    parameter.setProperty(CassandraStoreParameters.PORT, "9042");
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "native");
    parameter.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    parameter.setProperty(CassandraStoreParameters.CLUSTER_NAME,"Test Cluster");
    parameter.setProperty("gora.cassandrastore.mapping.file", "nativeSerialization/gora-cassandra-mapping.xml");
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
   * In this test case, put and get behavior of the data store are testing.
   */
  @Test
  public void testSimplePutandGet() {
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
   */
  @Test
  public void testSimplePutDeleteandGet() {
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "kasun", Date.from(Instant.now()));
    // storing data;
    userDataStore.put(id, user1);
    // get data;
    User olduser =  userDataStore.get(user1.getUserId());
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
   */
  @Test
  public void testSchemaExists() {
    userDataStore.deleteSchema();
    Assert.assertFalse(userDataStore.schemaExists());
    userDataStore.createSchema();
    Assert.assertTrue(userDataStore.schemaExists());
  }

  /**
   * In this test case, schema exists method behavior of the data store is testing.
   */
  @Test
  public void testTruncateSchema() {
    if(!userDataStore.schemaExists()) {
      userDataStore.createSchema();
    }
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "Madhawa Kasun", Date.from(Instant.now()));
    userDataStore.put(id, user1);
    User olduser =  userDataStore.get(id);
    Assert.assertEquals(olduser.getName(), user1.getName());
    Assert.assertEquals(olduser.getDateOfBirth(), user1.getDateOfBirth());
    userDataStore.truncateSchema();
    olduser =  userDataStore.get(id);
    Assert.assertNull(olduser);
  }
}
