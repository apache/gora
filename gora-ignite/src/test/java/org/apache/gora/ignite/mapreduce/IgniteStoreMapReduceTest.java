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
package org.apache.gora.ignite.mapreduce;

import java.io.IOException;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.ignite.GoraIgniteTestDriver;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.mapred.JobConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Executes tests for MR jobs over Ignite dataStore using a lightweight local MR runner.
 */
public class IgniteStoreMapReduceTest {

  private static final Logger LOG = LoggerFactory.getLogger(IgniteStoreMapReduceTest.class);
  private static final GoraIgniteTestDriver DRIVER = new GoraIgniteTestDriver();
  private DataStore<String, WebPage> webPageStore;
  private JobConf jobConf;

  @BeforeClass
  public static void startIgnite() throws Exception {
    DRIVER.setUpClass();
  }

  @AfterClass
  public static void stopIgnite() throws Exception {
    DRIVER.tearDownClass();
  }

  @Before
  public void setUp() throws Exception {
    DRIVER.setUp();
    jobConf = new JobConf(DRIVER.getConfiguration());
    jobConf.set("mapreduce.framework.name", "local");
    jobConf.set("mapred.job.tracker", "local");
    jobConf.set("fs.defaultFS", "file:///");
    jobConf.setInt("mapreduce.job.maps", 1);
    webPageStore = DRIVER.createDataStore(String.class, WebPage.class);
  }

  @After
  public void tearDown() throws Exception {
    if (webPageStore != null) {
      try {
        webPageStore.deleteSchema();
        webPageStore.close();
      } catch (Exception ignore) {
        LOG.warn("Failed to clean up Ignite test datastore", ignore);
      } finally {
        webPageStore = null;
      }
    }
    DRIVER.tearDown();
  }

  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, jobConf);
  }

}
