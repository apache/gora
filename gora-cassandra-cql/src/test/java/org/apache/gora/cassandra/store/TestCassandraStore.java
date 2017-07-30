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
 * <p>
 * Testing class for all standard gora-cassandra functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora.
 * <p>
 * Testing class for all standard gora-cassandra functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora.
 */

/**
 * Testing class for all standard gora-cassandra functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora. 
 */
package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

/**
 * Test for CassandraStore.
 */
public class TestCassandraStore extends DataStoreTestBase {
  private static Properties properties;

  static {
    GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
    setProperties();
    testDriver.setParameters(properties);
    setTestDriver(testDriver);
  }

  private static void setProperties() {
    properties = new Properties();
    properties.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    properties.setProperty(CassandraStoreParameters.PORT, "9042");
    properties.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "avro");
    properties.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    properties.setProperty(CassandraStoreParameters.CLUSTER_NAME, "Test Cluster");
    properties.setProperty("gora.cassandrastore.mapping.file", "avro/gora-cassandra-mapping.xml");
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Ignore("GORA-298 Implement CassandraStore#getPartitions")
  @Override
  public void testGetPartitions() throws IOException {
  }

  @Test
  public void testQuery() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testQuery");
    DataStoreTestUtil.testQueryWebPages(webPageStore);
  }

  @Test
  public void testQueryStartKey() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testQueryStartKey");
    DataStoreTestUtil.testQueryWebPageStartKey(webPageStore);
  }

  @Test
  public void testQueryEndKey() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testQueryEndKey");
    DataStoreTestUtil.testQueryWebPageEndKey(webPageStore);
  }

  @Test
  public void testQueryKeyRange() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testQueryKetRange");
    DataStoreTestUtil.testQueryWebPageKeyRange(webPageStore);
  }

  @Test
  public void testDelete() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testDelete");
    DataStoreTestUtil.testDelete(webPageStore);
  }

  //TODO need to fix the test
  @Ignore
  @Test
  public void testDeleteByQuery() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testDeleteByQuery");
    DataStoreTestUtil.testDeleteByQuery(webPageStore);
  }

  //TODO need to fix the test
  @Ignore
  @Test
  public void testDeleteByQueryFields() throws Exception {
    webPageStore.truncateSchema();
    log.info("test method: testQueryByQueryFields");
    DataStoreTestUtil.testDeleteByQueryFields(webPageStore);
  }

  @Ignore
  public void testGet3UnionField() {
  }
}
