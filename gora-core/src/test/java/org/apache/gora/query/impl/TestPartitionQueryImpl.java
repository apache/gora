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

import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.mock.query.MockQuery;
import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.hadoop.io.TestWritable;
import org.junit.Test;

/**
 * Test case for {@link PartitionQueryImpl}
 */
public class TestPartitionQueryImpl {

  private MockDataStore dataStore = MockDataStore.get();
  
  @Test
  public void testReadWrite() throws Exception {
    
    MockQuery baseQuery = dataStore.newQuery();
    baseQuery.setStartKey("start");
    baseQuery.setLimit(42);
    
    PartitionQueryImpl<String, MockPersistent> 
      query = new PartitionQueryImpl<String, MockPersistent>(baseQuery);
    
    TestWritable.testWritable(query);
  }
  
}
