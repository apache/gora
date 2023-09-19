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

package org.apache.gora.arangodb.store;

import org.apache.gora.arangodb.ArangoDBTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * Executes all the dataStore specific integration tests for ArangoDB dataStore.
 */
public class ArangoDBGoraDataStoreTest extends DataStoreTestBase {

  private static final Logger log = LoggerFactory.getLogger(ArangoDBGoraDataStoreTest.class);

  private static final String DOCKER_CONTAINER_NAME = "arangodb/arangodb:3.6.4";

  @ClassRule
  public static GenericContainer arangodbContainer = new GenericContainer(DOCKER_CONTAINER_NAME)
          .withExposedPorts(8529)
          .withEnv("ARANGO_ROOT_PASSWORD", "root")
          .waitingFor(new ArangoDBStartupWaitStrategy())
          .withStartupTimeout(Duration.ofSeconds(240));

  @BeforeClass
  public static void setUpClass() throws Exception {
    setTestDriver(new ArangoDBTestDriver(arangodbContainer));
    DataStoreTestBase.setUpClass();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DataStoreTestBase.tearDownClass();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  @Test
  public void testGet() throws Exception {
    log.info("test method: testGet");
    DataStoreTestUtil.testGetEmployee(this.employeeStore);
  }

  @Ignore("3 types union field is not supported by OrientDBStore.")
  @Override
  public void testGet3UnionField() {
    //3 types union field is not supported by OrientDBStore.
  }

  @Ignore("delete by query fields is not supported as remove attribute not supported by ArangoDB.")
  @Override
  public void testDeleteByQueryFields() {
  }

  @Ignore("updates of map fields are not supported as ArangoDB overrides but keep existing entries.")
  @Override
  public void testUpdate() {
  }

}
