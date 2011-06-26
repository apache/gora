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
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.hbase.HBaseClusterTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests related to {@link HBaseStore} using mapreduce.
 */
public class TestHBaseStoreMapReduce extends HBaseClusterTestCase{

  private HBaseStore<String, WebPage> webPageStore;
  private HBaseStore<String, TokenDatum> tokenStore;
  
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    webPageStore = DataStoreFactory.getDataStore(
        HBaseStore.class, String.class, WebPage.class);
    tokenStore = DataStoreFactory.getDataStore(HBaseStore.class, 
        String.class, TokenDatum.class);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    webPageStore.close();
  }
  
  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, conf);
  }
  
  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testWordCount(conf, webPageStore, tokenStore);
  }
  
  public static void main(String[] args) throws Exception {
   TestHBaseStoreMapReduce test =  new TestHBaseStoreMapReduce();
   test.setUp();
   test.testCountQuery();
  }
}
