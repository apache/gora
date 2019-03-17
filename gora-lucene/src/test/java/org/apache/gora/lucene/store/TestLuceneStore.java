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
package org.apache.gora.lucene.store;

import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.util.OperationNotSupportedException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * TestLuceneStore class executes tests for {@link org.apache.gora.lucene.store.LuceneStore}
 */
public class TestLuceneStore extends DataStoreTestBase {

  @BeforeClass
  public static void setUpClass() throws Exception {
    setTestDriver(new TestLuceneStoreDriver());
    DataStoreTestBase.setUpClass();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DataStoreTestBase.tearDownClass();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
    //this is not handled at super class level
    super.employeeStore.close();
    super.webPageStore.close();
  }

  @Test(expected = AssertionError.class)
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }

  @Test(expected = OperationNotSupportedException.class)
  public void testGetPartitions() throws Exception {
    super.testGetPartitions();
  }

  @Ignore("3 types union field is not supported by LuceneStore.")
  @Override
  public void testGet3UnionField() throws Exception {
    //3 types union field is not supported by LuceneStore.
  }

}
