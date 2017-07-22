/*
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

package org.apache.gora.aerospike.store;

import org.apache.gora.aerospike.GoraAerospikeTestDriver;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.apache.gora.examples.WebPageDataCreator.URLS;

/**
 * Test case for AerospikeStore.
 */
public class TestAerospikeStore extends DataStoreTestBase {

  private static final String DOCKER_CONTAINER_NAME = "aerospike/aerospike-server:latest";

  @ClassRule
  public static GenericContainer aerospikeContainer = new GenericContainer(DOCKER_CONTAINER_NAME);

  static {
    setTestDriver(new GoraAerospikeTestDriver(aerospikeContainer));
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Aerospike")
  @Override
  public void testTruncateSchema() throws Exception {
    super.testTruncateSchema();
  }

  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Aerospike")
  @Override
  public void testDeleteSchema() throws Exception {
    super.testDeleteSchema();
  }

  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Aerospike")
  @Override
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }

  @Test
  @Override
  public void testQuery() throws Exception {
    // Clearing the test data in the database
    Query<String, WebPage> query;
    WebPageDataCreator.createWebPageData(webPageStore);
    query = webPageStore.newQuery();
    webPageStore.deleteByQuery(query);

    super.testQuery();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testQueryStartKey() throws Exception {
    super.testQueryStartKey();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testQueryEndKey() throws Exception {
    super.testQueryEndKey();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testQueryKeyRange() throws Exception {
    super.testQueryKeyRange();
  }

  @Test
  @Override
  public void testDelete() throws Exception {
    // Clearing the test data in the database
    Query<String, WebPage> query;
    WebPageDataCreator.createWebPageData(webPageStore);
    query = webPageStore.newQuery();
    webPageStore.deleteByQuery(query);

    super.testDelete();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testDeleteByQuery() throws Exception {
    super.testDeleteByQuery();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testDeleteByQueryFields() throws Exception {
    super.testDeleteByQueryFields();
  }

  @Test
  @Ignore("Functionality is to be implemented in the next iteration")
  @Override
  public void testGetPartitions() throws Exception {
    super.testGetPartitions();
  }
}
