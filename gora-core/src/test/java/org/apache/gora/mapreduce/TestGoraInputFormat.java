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
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema.Field;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.mock.query.MockQuery;
import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.query.PartitionQuery;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestGoraInputFormat {

  public List<InputSplit> getInputSplits()
    throws IOException, InterruptedException {

    Job job = Job.getInstance(new Configuration());
    MockDataStore store = MockDataStore.get();

    MockQuery query = store.newQuery();
    
    query.setFields(getEmployeeFieldNames());
    GoraInputFormat.setInput(job, query, false);

    GoraInputFormat<String, MockPersistent> inputFormat
      = new GoraInputFormat<>();

    inputFormat.setConf(job.getConfiguration());

    return inputFormat.getSplits(job);
  }

  /**
   * First, asserts that the attempt to obtain splits results in 
   * greater than 0 splits which can be used for computation.
   * We then check that the partition query (obtained by using the 
   * splits) has the same fields as we would expect by directly 
   * accessing the fields of an Employee object.
   * @throws IOException
   * @throws InterruptedException
   */
  @Test
  @SuppressWarnings("rawtypes")
  public void testGetSplits() throws IOException, InterruptedException {
    List<InputSplit> splits = getInputSplits();

    assertTrue(splits.size() > 0);

    InputSplit split = splits.get(0);
    PartitionQuery query = ((GoraInputSplit)split).getQuery();
    assertTrue(Arrays.equals(getEmployeeFieldNames(), query.getFields()));
  }
  
  private static String[] getEmployeeFieldNames(){
    List<Field> fields = Employee.SCHEMA$.getFields();
    String[] fieldNames = new String[fields.size()];
    for(int i = 0; i< fieldNames.length; i++){
      fieldNames[i] = fields.get(i).name();
    }
    return fieldNames;
  }

}