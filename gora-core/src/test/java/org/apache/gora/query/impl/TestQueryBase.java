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


package org.apache.gora.query.impl;

import junit.framework.Assert;

import org.apache.gora.mock.query.MockQuery;
import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.util.TestIOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link QueryBase}.
 */
public class TestQueryBase {

  private MockDataStore dataStore = MockDataStore.get();
  private MockQuery query;
  
  private static final String[] FIELDS = {"foo", "baz", "bar"};
  private static final String START_KEY = "1_start";
  private static final String END_KEY = "2_end";
  
  @Before
  public void setUp() {
    query = dataStore.newQuery(); //MockQuery extends QueryBase
  }
  
  @Test
  public void testReadWrite() throws Exception {
    query.setFields(FIELDS);
    query.setKeyRange(START_KEY, END_KEY);
    TestIOUtils.testSerializeDeserialize(query);
    
    Assert.assertNotNull(query.getDataStore());
  }
  
  @Test
  public void testReadWrite2() throws Exception {
    query.setLimit(1000);
    query.setTimeRange(0, System.currentTimeMillis());
    TestIOUtils.testSerializeDeserialize(query);
  }

}
