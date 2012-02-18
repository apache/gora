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

package org.apache.gora.hbase.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.TokenDatum;
import org.junit.Assert;
import org.junit.Test;

public class TestHBaseByteInterface {

  private static final Random RANDOM = new Random();

  @Test
  public void testEncodingDecoding() throws Exception {
    for (int i=0; i < 1000; i++) {
    
      //employer
      Utf8 name = new Utf8("john");
      long dateOfBirth = System.currentTimeMillis();
      int salary = 1337;
      Utf8 ssn = new Utf8(String.valueOf(RANDOM.nextLong()));
      
      Employee e = new Employee();
      e.setName(name);
      e.setDateOfBirth(dateOfBirth);
      e.setSalary(salary);
      e.setSsn(ssn);
      
      byte[] employerBytes = HBaseByteInterface.toBytes(e, Employee._SCHEMA);
      Employee e2 = (Employee) HBaseByteInterface.fromBytes(Employee._SCHEMA, 
          employerBytes);
      
      Assert.assertEquals(name, e2.getName());
      Assert.assertEquals(dateOfBirth, e2.getDateOfBirth());
      Assert.assertEquals(salary, e2.getSalary());
      Assert.assertEquals(ssn, e2.getSsn());
      
      
      //metadata
      Utf8 key = new Utf8("theKey");
      Utf8 value = new Utf8("theValue " + RANDOM.nextLong());
      
      Metadata m = new Metadata();
      m.putToData(key, value);
      
      byte[] datumBytes = HBaseByteInterface.toBytes(m, Metadata._SCHEMA);
      Metadata m2 = (Metadata) HBaseByteInterface.fromBytes(Metadata._SCHEMA, 
          datumBytes);
      
      Assert.assertEquals(value, m2.getFromData(key));
    }
  }
  
  @Test
  public void testEncodingDecodingMultithreaded() throws Exception {
    // create a fixed thread pool
    int numThreads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(numThreads);

    // define a list of tasks
    Collection<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new Callable<Integer>() {
        @Override
        public Integer call() {
          try {
            // run a sequence
            testEncodingDecoding();
            // everything ok, return 0
            return 0;
          } catch (Exception e) {
            e.printStackTrace();
            // this will fail the test
            return 1;
          }
        }
      });
    }
    // submit them at once
    List<Future<Integer>> results = pool.invokeAll(tasks);

    // check results
    for (Future<Integer> result : results) {
      Assert.assertEquals(0, (int) result.get());
    }
  }

}