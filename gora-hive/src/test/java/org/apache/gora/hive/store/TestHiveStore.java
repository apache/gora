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

package org.apache.gora.hive.store;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.hive.GoraHiveTestDriver;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 * HiveStore Tests extending {@link DataStoreTestBase} which run the base JUnit test suite for
 * Gora.
 */
public class TestHiveStore extends DataStoreTestBase {

  static {
    try {
      setTestDriver(new GoraHiveTestDriver());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    assertTrue(employeeStore.schemaExists());
  }

  @Override
  public void assertPut(Employee employee) throws GoraException {
    employeeStore.put(employee.getSsn().toString(), employee);
  }

  @Override
  public void testGetWithFields() throws Exception {
    //Overrides DataStoreTestBase.testGetWithFields to avoid recursive field "boss"
    Employee employee = DataStoreTestUtil.createEmployee();
    WebPage webpage = DataStoreTestUtil.createWebPage();
    employee.setWebpage(webpage);
    String ssn = employee.getSsn().toString();
    employeeStore.put(ssn, employee);
    employeeStore.flush();

    String[] fields = ((HiveStore<String, Employee>) employeeStore).getFields();
    for (Set<String> subset : StringUtils.powerset(fields)) {
      if (subset.isEmpty()) {
        continue;
      }
      Employee after = employeeStore.get(ssn, subset.toArray(new String[subset.size()]));
      Employee expected = Employee.newBuilder().build();
      for (String field : subset) {
        int index = expected.getSchema().getField(field).pos();
        expected.put(index, employee.get(index));
      }

      DataStoreTestUtil.assertEqualEmployeeObjects(expected, after);
    }
  }

  @Override
  public void testGet() throws Exception {
    //Overrides DataStoreTestBase.testGet to avoid recursive field "boss"
    log.info("test method: testGet");
    employeeStore.createSchema();
    Employee employee = DataStoreTestUtil.createEmployee();
    String ssn = employee.getSsn().toString();
    employeeStore.put(ssn, employee);
    employeeStore.flush();
    Employee after = employeeStore.get(ssn, null);
    DataStoreTestUtil.assertEqualEmployeeObjects(employee, after);
  }

  @Ignore("This test is taking too much time (> 20 min)")
  @Override
  public void testBenchmarkExists() throws Exception {
    log.info("test method: testBenchmarkExists");
    DataStoreTestUtil.testBenchmarkGetExists(employeeStore);
  }

  @Override
  public void testGetNested() throws Exception {
    //Overrides DataStoreTestBase.testGetNested to avoid recursive field "boss"
    Employee employee = DataStoreTestUtil.createEmployee();

    WebPage webpage = new BeanFactoryImpl<>(String.class, WebPage.class).newPersistent();
    webpage.setUrl(new Utf8("url.."));
    webpage.setContent(ByteBuffer.wrap("test content".getBytes(Charset.defaultCharset())));
    webpage.setParsedContent(new ArrayList<>());

    Metadata metadata = new BeanFactoryImpl<>(String.class, Metadata.class).newPersistent();
    webpage.setMetadata(metadata);
    employee.setWebpage(webpage);
    String ssn = employee.getSsn().toString();

    employeeStore.put(ssn, employee);
    employeeStore.flush();
    Employee after = employeeStore.get(ssn, null);
    DataStoreTestUtil.assertEqualEmployeeObjects(employee, after);
    DataStoreTestUtil.assertEqualWebPageObjects(webpage, after.getWebpage());
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testExists() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDelete() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDeleteByQuery() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testDeleteByQueryFields() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive test server doesn't support deleting and updating entries")
  @Override
  public void testUpdate() throws Exception {
    //Hive test server doesn't support deleting and updating entries
  }

  @Ignore("Hive datastore doesn't support recursive records")
  @Override
  public void testGetRecursive() throws Exception {
    //Hive datastore doesn't support recursive records
  }

  @Ignore("Hive datastore doesn't support recursive records")
  @Override
  public void testGetDoubleRecursive() throws Exception {
    //Hive datastore doesn't support recursive records
  }

  @Ignore("As recursive records are not supported, employee.boss field cannot be processed.")
  @Override
  public void testGet3UnionField() throws Exception {
    //As recursive records are not supported, employee.boss field cannot be processed.
  }
}
