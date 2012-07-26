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

import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.mapred.HadoopTestCase;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Before;
import org.junit.Test;

// Slf4j logging imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for Mapreduce based tests. This is just a convenience
 * class, which actually only uses {@link MapReduceTestUtils} methods to
 * run the tests.
 */
@SuppressWarnings("deprecation")
public abstract class DataStoreMapReduceTestBase extends HadoopTestCase {
  public static final Logger LOG = LoggerFactory.getLogger(DataStoreMapReduceTestBase.class);

  private DataStore<String, WebPage> webPageStore;
  private JobConf job;

  public DataStoreMapReduceTestBase(int mrMode, int fsMode, int taskTrackers,
      int dataNodes) throws IOException {
    super(mrMode, fsMode, taskTrackers, dataNodes);
  }

  public DataStoreMapReduceTestBase() throws IOException {
    this(HadoopTestCase.CLUSTER_MR, HadoopTestCase.DFS_FS, 2, 2);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    LOG.info("Setting up Hadoop Test Case...");
    try {
      super.setUp();
      webPageStore = createWebPageDataStore();
      job = createJobConf();
    } catch (Exception e) {
      LOG.error("Hadoop Test Case set up failed", e);
      // cleanup
      tearDown();
    }
  } 

  @Override
  public void tearDown() throws Exception {
    LOG.info("Tearing down Hadoop Test Case...");
    super.tearDown();
    webPageStore.close();
  }

  protected abstract DataStore<String, WebPage> createWebPageDataStore()
    throws IOException;

  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, job);
  }

 // TODO The correct implementation for this test need to be created
 // and implemented. For a WIP and more details see GORA-104 
 // @Test
 // public void testWordCount() throws Exception {
 //   MapReduceTestUtils.testWordCount(job, tokenDatumStore, webPageStore);
 // }
}
