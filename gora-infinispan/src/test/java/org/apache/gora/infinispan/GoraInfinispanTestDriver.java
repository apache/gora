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

/**
 * @author valerio schiavoni
 *
 */

package org.apache.gora.infinispan;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.infinispan.store.InfinispanStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for third party tests using gora-infinispan backend.
 *
 * @see GoraTestDriver for test specifics. This driver is the base for all test
 *      cases that require an embedded Infinispan server. It starts (setUp) and
 *      stops (tearDown) embedded Infinispan server.
 *
 * * @author Pierre Sutra, Valerio Schiavoni
 *
 */

public class GoraInfinispanTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(GoraInfinispanTestDriver.class);

  private SimulationDriver delegate;
  private int numbderOfNodes;
  public List<String> cacheNames;

  public GoraInfinispanTestDriver(int numbderOfNodes) {
    this(numbderOfNodes, null);
  }

  public GoraInfinispanTestDriver(int numbderOfNodes, List<String> cacheNames){
    super(InfinispanStore.class);
    this.cacheNames = new ArrayList<>();
    this.numbderOfNodes = numbderOfNodes;
    if (cacheNames!=null) {
      this.cacheNames.addAll(cacheNames);
    }
  }

  public String connectionString(){
    return delegate.connectionString();
  }

  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Starting Infinispan...");
    delegate = new SimulationDriver(numbderOfNodes,cacheNames);
    try{
      delegate.create();
    }catch (Throwable e){
      throw new RuntimeException(e);
    }
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Stopping Infinispan...");
    delegate.destroy();
  }

  @Override
  public<K, T extends Persistent> DataStore<K,T>
  createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    InfinispanStore store = (InfinispanStore) super.createDataStore(keyClass, persistentClass);
    if (persistentClass.equals(Employee.class)) {
      store.setPrimaryFieldName("ssn");
      store.setPrimaryFieldPos(2);
    }else  if(persistentClass.equals(WebPage.class)) {
      store.setPrimaryFieldName("url");
      store.setPrimaryFieldPos(0);
    }
    return store;
  }


}
