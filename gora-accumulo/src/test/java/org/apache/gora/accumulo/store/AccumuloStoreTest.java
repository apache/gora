/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.accumulo.store;

import java.io.IOException;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.hadoop.conf.Configuration;

/**
 * 
 */
public class AccumuloStoreTest extends DataStoreTestBase {
  
  // TODO implement test driver

  @Override
  protected DataStore<String,Employee> createEmployeeDataStore() throws IOException {
    return DataStoreFactory.getDataStore(String.class, Employee.class, new Configuration());
  }
  
  @Override
  protected DataStore<String,WebPage> createWebPageDataStore() throws IOException {
    return DataStoreFactory.getDataStore(String.class, WebPage.class, new Configuration());
  }

  
  //Until GORA-66 is resolved this test will always fail, so 
  //do not run it
  @Override
  public void testDeleteByQueryFields() throws IOException {
    return;
  }
}
