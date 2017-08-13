/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.cassandra.example.generated.nativeSerialization.Customer;
import org.apache.gora.cassandra.example.generated.nativeSerialization.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

/**
 * This class contains the tests cases to test the behaviour of Native Serialization with UDT dataType.
 */
public class TestNativeSerializationWithUDT {

  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static CassandraStore<String, Document> documentCassandraStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    documentCassandraStore = (CassandraStore<String, Document>) testDriver.createDataStore(String.class, Document.class);
  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    parameter.setProperty(CassandraStoreParameters.PORT, "9042");
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "native");
    parameter.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    parameter.setProperty(CassandraStoreParameters.CLUSTER_NAME, "Test Cluster");
    parameter.setProperty("gora.cassandrastore.mapping.file", "nativeUDT/gora-cassandra-mapping.xml");
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
   * This is for testGetNested() with UDT dataType with native serialization.
   */
  @Test
  public void testSimplePutAndGEt() {
    documentCassandraStore.createSchema();
    Document document = new Document();
    document.setDefaultId("yawamu.com");
    Customer customer = new Customer();
    customer.setId("144");
    customer.setName("Madhawa");
    document.setCustomer(customer);
    documentCassandraStore.put("yawamu.com", document);
    Document retrievedDocument = documentCassandraStore.get("yawamu.com");
    Assert.assertEquals(customer.getId(), retrievedDocument.getCustomer().getId());
    Assert.assertEquals(customer.getName(), retrievedDocument.getCustomer().getName());
  }

}
