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

package org.apache.gora.avro;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.memory.store.MemStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

/**
 * Test case for {@link PersistentDatumReader}.
 */
public class TestPersistentDatumReader {

  private PersistentDatumReader<WebPage> webPageDatumReader 
    = new PersistentDatumReader<WebPage>();
  private Configuration conf = new Configuration();
  
  private void testClone(PersistentBase persistent) throws IOException {
    PersistentBase cloned = ((PersistentBase)webPageDatumReader.clone(persistent, persistent.getSchema()));
    assertClone(persistent, cloned);
  }
  
  private void assertClone(PersistentBase persistent, PersistentBase cloned) {
    Assert.assertNotNull("cloned object is null", cloned);
    Assert.assertEquals("cloned object is not equal to original object", persistent, cloned);
  }
  
  @Test
  public void testCloneEmployee() throws Exception {
    @SuppressWarnings("unchecked")
    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class, conf);

    Employee employee = DataStoreTestUtil.createEmployee(store);
    
    testClone(employee);
  }
  
  @Test
  public void testCloneEmployeeOneField() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));

    testClone(employee);
  }

  @Test
  public void testCloneEmployeeTwoFields() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));
    employee.setSalary(100);

    testClone(employee);
  }

  @Test
  public void testCloneWebPage() throws Exception {
    @SuppressWarnings("unchecked")
    DataStore<String, WebPage> store = DataStoreFactory.createDataStore(
        MemStore.class, String.class, WebPage.class, conf);
    WebPageDataCreator.createWebPageData(store);

    Query<String, WebPage> query = store.newQuery();
    Result<String, WebPage> result = query.execute();
    
    int tested = 0;
    while(result.next()) {
      WebPage page = result.get();
      testClone(page);
      tested++;
    }
    Assert.assertEquals(WebPageDataCreator.URLS.length, tested);
  }
}
