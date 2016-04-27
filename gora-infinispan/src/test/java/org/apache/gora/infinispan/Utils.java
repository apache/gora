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
package org.apache.gora.infinispan;

import org.apache.gora.examples.generated.Employee;
import org.apache.gora.store.DataStore;

import java.util.Random;

/**
 * @author Pierre Sutra
 */
public class Utils {

  private static Random rand = new Random(System.currentTimeMillis());
  public static final long YEAR_IN_MS = 365L * 24L * 60L * 60L * 1000L;

  public static Employee createEmployee(int i) {
    Employee employee = Employee.newBuilder().build();
    employee.setSsn(Long.toString(i));
    employee.setName(Long.toString(rand.nextLong()));
    employee.setDateOfBirth(rand.nextLong() - 20L * YEAR_IN_MS);
    employee.setSalary(rand.nextInt());
    return employee;
  }

  public static <T extends CharSequence> void populateEmployeeStore(DataStore<T, Employee> dataStore, int n) {
    for(int i=0; i<n; i++) {
      Employee e = createEmployee(i);
      dataStore.put((T)e.getSsn(),e);
    }
    dataStore.flush();
  }

}
