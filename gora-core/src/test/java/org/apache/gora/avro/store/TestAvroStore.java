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

package org.apache.gora.avro.store;

import static org.apache.gora.examples.WebPageDataCreator.URLS;
import static org.apache.gora.examples.WebPageDataCreator.URL_INDEXES;
import static org.apache.gora.examples.WebPageDataCreator.createWebPageData;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.gora.avro.store.AvroStore.CodecType;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link AvroStore}.
 */
public class TestAvroStore {

  public static final String EMPLOYEE_OUTPUT =
    System.getProperty("test.build.data") + "/testavrostore/employee.data";
  public static final String WEBPAGE_OUTPUT =
    System.getProperty("test.build.data") + "/testavrostore/webpage.data";

  protected AvroStore<String,Employee> employeeStore;
  protected AvroStore<String,WebPage> webPageStore;
  protected Configuration conf = new Configuration();

  @Before
  public void setUp() throws Exception {
    employeeStore = createEmployeeDataStore();
    employeeStore.initialize(String.class, Employee.class, DataStoreFactory.createProps());
    employeeStore.setOutputPath(EMPLOYEE_OUTPUT);
    employeeStore.setInputPath(EMPLOYEE_OUTPUT);

    webPageStore = new AvroStore<String, WebPage>();
    webPageStore.initialize(String.class, WebPage.class, DataStoreFactory.createProps());
    webPageStore.setOutputPath(WEBPAGE_OUTPUT);
    webPageStore.setInputPath(WEBPAGE_OUTPUT);
  }

  @SuppressWarnings("unchecked")
  protected AvroStore<String, Employee> createEmployeeDataStore() throws GoraException {
    return DataStoreFactory.getDataStore(
        AvroStore.class, String.class, Employee.class, conf);
  }

  protected AvroStore<String, WebPage> createWebPageDataStore() {
    return new AvroStore<String, WebPage>();
  }

  @After
  public void tearDown() throws Exception {
    deletePath(employeeStore.getOutputPath());
    deletePath(webPageStore.getOutputPath());

    employeeStore.close();
    webPageStore.close();
  }

  private void deletePath(String output) throws IOException {
    if(output != null) {
      Path path = new Path(output);
      path.getFileSystem(conf).delete(path, true);
    }
  }

  @Test
  public void testNewInstance() throws IOException, Exception {
    DataStoreTestUtil.testNewPersistent(employeeStore);
  }

  @Test
  public void testCreateSchema() throws IOException, Exception {
    DataStoreTestUtil.testCreateEmployeeSchema(employeeStore);
  }

  @Test
  public void testAutoCreateSchema() throws IOException, Exception {
    DataStoreTestUtil.testAutoCreateSchema(employeeStore);
  }

  @Test
  public void testPut() throws IOException, Exception {
    DataStoreTestUtil.testPutEmployee(employeeStore);
  }

  @Test
  public void testQuery() throws IOException, Exception {
    createWebPageData(webPageStore);
    webPageStore.close();

    webPageStore.setInputPath(webPageStore.getOutputPath());
    testQueryWebPages(webPageStore);
  }

  @Test
  public void testQueryBinaryEncoder() throws IOException, Exception {
    webPageStore.setCodecType(CodecType.BINARY);
    webPageStore.setInputPath(webPageStore.getOutputPath());

    createWebPageData(webPageStore);
    webPageStore.close();
    testQueryWebPages(webPageStore);
  }

  //AvroStore should be closed so that Hadoop file is completely flushed,
  //so below test is copied and modified to close the store after pushing data
  public static void testQueryWebPages(DataStore<String, WebPage> store)
  throws IOException, Exception {

    Query<String, WebPage> query = store.newQuery();
    Result<String, WebPage> result = query.execute();

    int i=0;
    while(result.next()) {
      WebPage page = result.get();
      DataStoreTestUtil.assertWebPage(page, URL_INDEXES.get(page.getUrl().toString()));
      i++;
    }
    Assert.assertEquals(i, URLS.length);
  }

}
