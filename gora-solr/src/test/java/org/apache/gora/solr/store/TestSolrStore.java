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
package org.apache.gora.solr.store;

import java.io.IOException;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.solr.GoraSolrTestDriver;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Ignore;

public class TestSolrStore extends DataStoreTestBase {
  
  static {
    setTestDriver(new GoraSolrTestDriver());
  }

  @Override
  protected DataStore<String, Employee> createEmployeeDataStore()
      throws IOException {
    SolrStore<String, Employee> store = new SolrStore<String, Employee>();
    store.initialize(String.class, Employee.class, DataStoreFactory.createProps());
    return store;
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore()
      throws IOException {
    SolrStore<String, WebPage> store = new SolrStore<String, WebPage>();
    store.initialize(String.class, WebPage.class, DataStoreFactory.createProps());
    return store;
  }


  @Ignore("GORA-310 and GORA-311 issues are not fixed at SolrStore")
  @Override
  public void testDeleteByQueryFields() throws IOException {}
}
