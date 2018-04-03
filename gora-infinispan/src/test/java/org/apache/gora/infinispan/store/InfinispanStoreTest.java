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

/**
 * Testing class for all standard gora-cassandra functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora. 
 */
package org.apache.gora.infinispan.store;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.filter.FilterOp;
import org.apache.gora.filter.SingleFieldValueFilter;
import org.apache.gora.infinispan.GoraInfinispanTestDriver;
import org.apache.gora.infinispan.Utils;
import org.apache.gora.infinispan.query.InfinispanQuery;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.util.TestIOUtils;
import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.gora.infinispan.store.InfinispanClient.ISPN_CONNECTION_STRING_KEY;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link InfinispanStore}.
 * @author Pierre Sutra
 */

public class InfinispanStoreTest extends DataStoreTestBase {

  private Configuration conf;
  private InfinispanStore<String,Employee> employeeDataStore;
  @SuppressWarnings("unused")
  private InfinispanStore<String,WebPage> webPageDataStore;

  @BeforeClass
  public static void setUpClass() throws Exception {
    List<String> cacheNames = new ArrayList<>();
    cacheNames.add(Employee.class.getSimpleName());
    cacheNames.add(WebPage.class.getSimpleName());
    setTestDriver(new GoraInfinispanTestDriver(1, cacheNames));
    DataStoreTestBase.setUpClass();
  }

  @Before
  public void setUp() throws Exception {
    GoraInfinispanTestDriver driver = getTestDriver();
    conf = driver.getConfiguration();
    conf.set(ISPN_CONNECTION_STRING_KEY,getTestDriver().connectionString());
    super.setUp();
    employeeDataStore = (InfinispanStore<String, Employee>) employeeStore;
    webPageDataStore = (InfinispanStore<String, WebPage>) webPageStore;
  }

  @Test
  public void testQueryMarshability() throws Exception {
    Utils.populateEmployeeStore(employeeStore, 100);
    InfinispanQuery<String,Employee> query = new InfinispanQuery<>(employeeDataStore);
    query.setFields("field");
    query.setLimit(1);
    query.setOffset(1);
    query.build();
    TestIOUtils.testSerializeDeserialize(query);
  }

  @Test
  public void testReadWriteQuery() throws Exception {
    final int NEMPLOYEE = 100;
    Utils.populateEmployeeStore(employeeStore, NEMPLOYEE);
    InfinispanQuery<String,Employee> query;

    // Partitioning
    int retrieved = 0;
    query = new InfinispanQuery<>(employeeDataStore);
    query.build();
    for (PartitionQuery<String,Employee> q : employeeDataStore.getPartitions(query)) {
      retrieved+=((InfinispanQuery<String,Employee>) q).list().size();
    }
    assert retrieved==NEMPLOYEE;

    // Test matching everything
    query = new InfinispanQuery<>(employeeDataStore);
    SingleFieldValueFilter<String, Employee> filter = new SingleFieldValueFilter<String, Employee>();
    filter.setFieldName("name");
    filter.setFilterOp(FilterOp.EQUALS);
    List<Object> operaands = new ArrayList<>();
    operaands.add("*");
    filter.setOperands(operaands);
    query.setFilter(filter);
    query.build();
    List<Employee> result = new ArrayList<>();
    for (PartitionQuery<String,Employee> q : employeeDataStore.getPartitions(query)) {
      result.addAll(((InfinispanQuery<String,Employee>)q).list());
    }
    assertEquals(NEMPLOYEE,result.size());

    // Test matching nothing
    query = new InfinispanQuery<>(employeeDataStore);
    filter = new SingleFieldValueFilter<String, Employee>();
    filter.setFieldName("name");
    filter.setFilterOp(FilterOp.NOT_EQUALS);
    operaands.clear();
    operaands.add("*");
    filter.setOperands(operaands);
    query.setFilter(filter);
    query.build();
    assertEquals(0,query.list().size());

  }


  public GoraInfinispanTestDriver getTestDriver() {
    return (GoraInfinispanTestDriver) testDriver;
  }

  @Override
  public void testDeleteByQueryFields() throws IOException, Exception {
    // FIXME not working
  }

  @Override
  public void testDeleteByQuery() throws IOException, Exception {
    // FIXME not working
  }

  @Override
  public void testQueryEndKey() throws IOException, Exception {
    // FIXME not working
  }

  @Override
  public void testGetWithFields() throws IOException, Exception {
    // FIXME not working
  }

}
