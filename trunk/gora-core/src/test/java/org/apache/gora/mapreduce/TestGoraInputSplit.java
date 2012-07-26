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

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.gora.mapreduce.GoraInputSplit;
import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.mock.query.MockQuery;
import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.query.PartitionQuery;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.TestWritable;
import org.junit.Test;

/**
 * Test case for {@link GoraInputSplit}.
 */
public class TestGoraInputSplit {

  private Configuration conf = new Configuration();
  
  private List<PartitionQuery<String, MockPersistent>> 
    getPartitions() throws IOException {
    MockDataStore store = MockDataStore.get();
    MockQuery query = store.newQuery();

    List<PartitionQuery<String, MockPersistent>> partitions = 
      store.getPartitions(query);
    return partitions;
  }
  
  @Test
  public void testGetLocations() throws IOException {
    List<PartitionQuery<String, MockPersistent>> partitions = 
      getPartitions();

    int i=0;;
    for(PartitionQuery<String, MockPersistent> partition : partitions) {
      GoraInputSplit split = new GoraInputSplit(conf, partition);
      Assert.assertEquals(split.getLocations().length, 1);
      Assert.assertEquals(split.getLocations()[0], MockDataStore.LOCATIONS[i++]);
    }
  }

  @Test
  public void testReadWrite() throws Exception {
    
    List<PartitionQuery<String, MockPersistent>> partitions = 
      getPartitions();

    for(PartitionQuery<String, MockPersistent> partition : partitions) {
      GoraInputSplit split = new GoraInputSplit(conf, partition);
      TestWritable.testWritable(split);
    }
  }
  
}
