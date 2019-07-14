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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.memory.store.MemStore;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.TestIOUtils;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link PersistentSerialization}, {@link PersistentSerializer}
 * and {@link PersistentDeserializer}
 */
public class TestPersistentSerialization {

  /**
   * Creates an Employee object in-memory setting several fields to dirty. 
   * Asserts that it can be serialized and 
   * deserialzed without loosing data. We do this by asserting
   * what we get 'before' and 'after' (de)serialization processes.
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeEmployee() throws Exception {

    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
            MemStore.class, String.class, Employee.class, new Configuration());

    Employee employee = DataStoreTestUtil.createEmployee();

    TestIOUtils.testSerializeDeserialize(employee);
  }

  /**
   * Creates an Employee object but only sets one field as dirty.
   * We then do (de)serialization and check 'before' and 'after'
   * states.
   * @throws Exception
   */
  @Test
  public void testSerdeEmployeeOneField() throws Exception {
    Employee employee = Employee.newBuilder().build();
    employee.setSsn(new Utf8("11111"));

    TestIOUtils.testSerializeDeserialize(employee);
  }

  /**
   * Creates an Employee object setting only two fields as dirty.
   * We then do (de)serialization and check 'before' and 'after'
   * states.
   * @throws Exception
   */
  @Test
  public void testSerdeEmployeeTwoFields() throws Exception {
    Employee employee = Employee.newBuilder().build();
    employee.setSsn(new Utf8("11111"));
    employee.setSalary(100);

    TestIOUtils.testSerializeDeserialize(employee);
  }

  /**
   * Creates an WebPage object in-memory setting several fields to dirty.
   * Run a query over the persistent data.
   * Asserts that the results can be serialized and
   * deserialzed without loosing data. We do this by asserting
   * what we get 'before' and 'after' (de)serialization processes.
   * Also simple assertion for equal number of URL's in WebPage
   * and results.
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeWebPage() throws Exception {

    MemStore<String, WebPage> store = DataStoreFactory.getDataStore(
            MemStore.class, String.class, WebPage.class, new Configuration());
    WebPageDataCreator.createWebPageData(store);

    Result<String, WebPage> result = store.newQuery().execute();

    int i = 0;
    while (result.next()) {
      WebPage page = result.get();
      TestIOUtils.testSerializeDeserialize(page);
      i++;
    }
    assertEquals(WebPageDataCreator.URLS.length, i);
  }

  /**
   * Creates multiple WebPage objects setting several fields to dirty.
   * Asserts that the data can be serialized and
   * deserialzed without loosing data. We do this by asserting
   * what we get 'before' and 'after' (de)serialization processes.
   * @throws Exception
   */
  @Test
  public void testSerdeMultipleWebPages() throws Exception {
    WebPage page1 = WebPage.newBuilder().build();
    WebPage page2 = WebPage.newBuilder().build();
    WebPage page3 = WebPage.newBuilder().build();

    page1.setUrl(new Utf8("foo"));
    page2.setUrl(new Utf8("baz"));
    page3.setUrl(new Utf8("bar"));
    page1.setParsedContent(new ArrayList<CharSequence>());
    page1.getParsedContent().add(new Utf8("coo"));
    page2.setOutlinks(new HashMap<CharSequence, CharSequence>());
    page2.getOutlinks().put(new Utf8("a"), new Utf8("b"));

    TestIOUtils.testSerializeDeserialize(page1, page2, page3);
  }

}

