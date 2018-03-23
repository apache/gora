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
import org.junit.Before;
import org.junit.Test;

public class TestLuceneStore extends DataStoreTestBase {

  static {
    setTestDriver(new TestLuceneStoreDriver());
  }

  
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test(expected=AssertionError.class)
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }
  
  @Test(expected=OperationNotSupportedException.class)
  public void testGetPartitions() throws Exception {
    super.testGetPartitions();
  }


  public void testUpdate() throws Exception {
  }
  public void testGetRecursive() throws Exception {
  }
  public void testGetDoubleRecursive() throws Exception {
  }
  public void testGetNested() throws Exception {
  }
  public void testGet3UnionField() throws Exception {
  }
  public void testDeleteByQueryFields() throws Exception {
  }
  
}
