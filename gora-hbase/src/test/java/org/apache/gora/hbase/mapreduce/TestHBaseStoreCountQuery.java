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

package org.apache.gora.hbase.mapreduce;

import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.hbase.store.HBaseStore;
import org.apache.gora.hbase.util.HBaseClusterSingleton;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.store.DataStoreFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests related to {@link HBaseStore} using mapreduce.
 */
public class TestHBaseStoreCountQuery {
  private static final HBaseClusterSingleton cluster = HBaseClusterSingleton.build(1);

  private HBaseStore<String, WebPage> webPageStore;

  @Before
  public void setUp() throws Exception {
    cluster.deleteAllTables();
    webPageStore = DataStoreFactory.getDataStore(
        HBaseStore.class, String.class, WebPage.class, cluster.getConf());
  }

  @After
  public void tearDown() throws Exception {
    webPageStore.close();
  }
  
  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, cluster.getConf());
  }

  public static void main(String[] args) throws Exception {
   TestHBaseStoreCountQuery test =  new TestHBaseStoreCountQuery();
   test.setUp();
   test.testCountQuery();
  }
}
