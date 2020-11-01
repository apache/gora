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
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.AvroUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.apache.gora.examples.WebPageDataCreator.URLS;

/**
 * Test case for AerospikeStore.
 */
public class TestAerospikeStore extends DataStoreTestBase {

  private static final String DOCKER_CONTAINER_NAME = "aerospike/aerospike-server:4.3.1.4";

  @ClassRule
  public static GenericContainer aerospikeContainer = new GenericContainer(DOCKER_CONTAINER_NAME)
          .withExposedPorts(3000).waitingFor(new AerospikeStartupLogWaitStrategy())
          .withStartupTimeout(Duration.ofSeconds(240));

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
  @Override
  public void testDeleteByQuery() throws Exception {

    // Can not use the super method as they query key ranges are not supported
    Query<String, WebPage> query;
    //test 1 - delete all
    WebPageDataCreator.createWebPageData(webPageStore);

    query = webPageStore.newQuery();

    DataStoreTestUtil.assertNumResults(webPageStore.newQuery(), URLS.length);
    webPageStore.deleteByQuery(query);
    webPageStore.flush();
    DataStoreTestUtil.assertEmptyResults(webPageStore.newQuery());

    //test 2 - delete all
    WebPageDataCreator.createWebPageData(webPageStore);

    query = webPageStore.newQuery();
    query.setFields(AvroUtils.getSchemaFieldNames(WebPage.SCHEMA$));

    DataStoreTestUtil.assertNumResults(webPageStore.newQuery(), URLS.length);
    webPageStore.deleteByQuery(query);
    webPageStore.flush();
    DataStoreTestUtil.assertEmptyResults(webPageStore.newQuery());

    webPageStore.truncateSchema();
  }

  // Unsupported functionality due to the limitations in Aerospike java client

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
  @Ignore("Query key ranges based on primary key is not supported via the java client")
  @Override
  public void testQueryStartKey() throws Exception {
    super.testQueryStartKey();
  }

  @Test
  @Ignore("Query key ranges based on primary key is not supported via the java client")
  @Override
  public void testQueryEndKey() throws Exception {
    super.testQueryEndKey();
  }

  @Test
  @Ignore("Query key ranges based on primary key is not supported via the java client")
  @Override
  public void testQueryKeyRange() throws Exception {
    super.testQueryKeyRange();
  }

  @Test
  @Ignore("Not supported as query key ranges are not supported")
  @Override
  public void testDeleteByQueryFields() throws Exception {
    super.testDeleteByQueryFields();
  }
}
