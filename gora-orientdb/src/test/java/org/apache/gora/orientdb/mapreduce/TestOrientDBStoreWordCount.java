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
package org.apache.gora.orientdb.mapreduce;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.orientdb.GoraOrientDBTestDriver;
import org.apache.gora.orientdb.store.OrientDBStore;
import org.apache.gora.store.DataStoreFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests related to {@link org.apache.gora.orientdb.store.OrientDBStore} using
 * mapreduce.
 */
public class TestOrientDBStoreWordCount  {

  private OrientDBStore<String, WebPage> webPageStore;
  private OrientDBStore<String, TokenDatum> tokenStore;

  protected static GoraTestDriver testDriver = new GoraOrientDBTestDriver();

  @Before
  public void setUp() throws Exception {
    testDriver.setUpClass();
    webPageStore = DataStoreFactory.getDataStore(OrientDBStore.class,
        String.class, WebPage.class, testDriver.getConfiguration());
    tokenStore = DataStoreFactory.getDataStore(OrientDBStore.class, String.class,
        TokenDatum.class, testDriver.getConfiguration());
  }

  @After
  public void tearDown() throws Exception {
    webPageStore.close();
    tokenStore.close();
    testDriver.tearDownClass();
  }

  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testWordCount(testDriver.getConfiguration(),
            webPageStore, tokenStore);
  }

  @Test
  public void testFlinkWordCountFlink() throws Exception {
    MapReduceTestUtils.testFlinkWordCount(testDriver.getConfiguration(), webPageStore, tokenStore);
  }

}
