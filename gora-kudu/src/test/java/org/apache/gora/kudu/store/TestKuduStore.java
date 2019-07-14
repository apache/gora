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
package org.apache.gora.kudu.store;

import org.apache.gora.kudu.GoraKuduTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case for KuduStore.
 */
public class TestKuduStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraKuduTestDriver());
  }

  @Test
  @Ignore
  @Override
  public void testUpdate() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testEmptyUpdate() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetRecursive() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetDoubleRecursive() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetNested() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGet3UnionField() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetWithFields() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetWebPage() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetWebPageDefaultFields() throws Exception {
  }

 

  @Test
  @Ignore
  @Override
  public void testQueryWebPageSingleKey() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testQueryWebPageSingleKeyDefaultFields() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testQueryWebPageQueryEmptyResults() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testDeleteByQuery() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testDeleteByQueryFields() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testGetPartitions() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testResultSize() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeStartKey() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeEndKey() throws Exception {
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeKeyRange() throws Exception {
  }


}
