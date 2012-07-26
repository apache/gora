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

package org.apache.gora.mapreduce;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.PersistentDeserializer;
import org.apache.gora.mapreduce.PersistentSerialization;
import org.apache.gora.mapreduce.PersistentSerializer;
import org.apache.gora.memory.store.MemStore;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.TestIOUtils;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

/** Test class for {@link PersistentSerialization}, {@link PersistentSerializer}
 *  and {@link PersistentDeserializer}
 */
public class TestPersistentSerialization {

  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeEmployee() throws Exception {

    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class, new Configuration());

    Employee employee = DataStoreTestUtil.createEmployee(store);

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @Test
  public void testSerdeEmployeeOneField() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @Test
  public void testSerdeEmployeeTwoFields() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));
    employee.setSalary(100);

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeWebPage() throws Exception {

    MemStore<String, WebPage> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, WebPage.class, new Configuration());
    WebPageDataCreator.createWebPageData(store);

    Result<String, WebPage> result = store.newQuery().execute();

    int i=0;
    while(result.next()) {
      WebPage page = result.get();
      TestIOUtils.testSerializeDeserialize(page);
      i++;
    }
    Assert.assertEquals(WebPageDataCreator.URLS.length, i);
  }

  @Test
  public void testSerdeMultipleWebPages() throws Exception {
    WebPage page1 = new WebPage();
    WebPage page2 = new WebPage();
    WebPage page3 = new WebPage();

    page1.setUrl(new Utf8("foo"));
    page2.setUrl(new Utf8("baz"));
    page3.setUrl(new Utf8("bar"));

    page1.addToParsedContent(new Utf8("coo"));

    page2.putToOutlinks(new Utf8("a"), new Utf8("b"));

    TestIOUtils.testSerializeDeserialize(page1, page2, page3);
  }

}
