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

package org.apache.gora.sql.store;

import java.io.IOException;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.sql.GoraSqlTestDriver;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;

/**
 * Test case for {@link SqlStore}
 */
public class TestSqlStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraSqlTestDriver());
  }

  public TestSqlStore() {
  }

  @Override
  protected DataStore<String, Employee> createEmployeeDataStore() throws IOException {
    SqlStore<String, Employee> store = new SqlStore<String, Employee>();
    store.initialize(String.class, Employee.class, DataStoreFactory.properties);
    return store;
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() throws IOException {
    SqlStore<String, WebPage> store = new SqlStore<String, WebPage>();
    store.initialize(String.class, WebPage.class, DataStoreFactory.properties);
    return store;
  }

  //@Override
  public void testDeleteByQueryFields() {
    //TODO: implement delete fields in SqlStore
  }

  //@Override
  public void testDeleteByQuery() throws IOException {
    //HSQLDB somehow hangs for this test. we need to solve the issue or switch to
    //another embedded db.
  }

  public static void main(String[] args) throws Exception {
    TestSqlStore test = new TestSqlStore();
    TestSqlStore.setUpClass();
    test.setUp();
    test.testDeleteByQuery();
    test.tearDown();
    TestSqlStore.tearDownClass();
  }
}
