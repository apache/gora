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
package org.apache.gora.accumulo;

import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.accumulo.store.AccumuloStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author lmcgibbn
 *
 */
public class GoraAccumuloTestDriver extends GoraTestDriver {

  private static final Logger LOG = LoggerFactory.getLogger(GoraAccumuloTestDriver.class);
  private static MiniAccumuloCluster cluster = null;
  private static final String PASSWORD = "drowssap";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  public GoraAccumuloTestDriver() throws Exception {
    super(AccumuloStore.class);
  }

  @Override
  public void setUpClass() throws IOException, InterruptedException {
    log.info("Starting Accumulo MiniAccumuloCluster...");
    try {
      tmpDir.create();
      MiniAccumuloConfig miniCfg = new MiniAccumuloConfig(tmpDir.getRoot(), PASSWORD);
      miniCfg.setInstanceName("goraTest");
      miniCfg.setZooKeeperPort(56321);
      cluster = new MiniAccumuloCluster(miniCfg);
      cluster.start();
    } catch (Exception e) {
      LOG.error("Error starting Accumulo MiniAccumuloCluster: {}", e.getMessage());
      // cleanup
      tearDownClass();
    }
  }

  @Override
  public void tearDownClass() throws IOException, InterruptedException {
    log.info("Shutting down Accumulo MiniAccumuloCluster...");
    if (cluster != null) {
      cluster.stop();
    }
    tmpDir.delete();
  }
}
