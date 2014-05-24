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
package org.apache.gora.mongodb.store;

import java.io.IOException;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mongodb.GoraMongodbTestDriver;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Before;
import org.junit.Ignore;

public class TestMongoStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraMongodbTestDriver());
  }

  @Deprecated
  @Override
  protected DataStore<String, Employee> createEmployeeDataStore()
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  protected DataStore<String, WebPage> createWebPageDataStore()
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  public GoraMongodbTestDriver getTestDriver() {
    return (GoraMongodbTestDriver) testDriver;
  }

  @Ignore("Skip until GORA-66 is fixed: need better semantic for end/start keys")
  @Override
  public void testDeleteByQueryFields() throws IOException {
    // Skip until GORA-66 is fixed: need better semantic for end/start keys
  }

  @Ignore("Skip until GORA-66 is fixed: need better semantic for end/start keys")
  @Override
  public void testQueryKeyRange() throws IOException, Exception {
    // Skip until GORA-66 is fixed: need better semantic for end/start keys
  }

  @Ignore("MongoStore doesn't support 3 types union field yet")
  @Override
  public void testGet3UnionField() throws IOException, Exception {
    // MongoStore doesn't support 3 types union field yet
  }

}
