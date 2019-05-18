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
package org.apache.gora.redis;

import java.io.IOException;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.redis.store.RedisStore;
import org.apache.redis.minicluster.MiniRedisCluster;
import org.apache.redis.minicluster.MiniRedisConfig;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lmcgibbn
 *
 */
public class GoraRedisTestDriver extends GoraTestDriver {

  private static final Logger LOG = LoggerFactory.getLogger(GoraRedisTestDriver.class);
  private static MiniRedisCluster cluster = null;
  private static final String PASSWORD = "drowssap";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  public GoraRedisTestDriver() throws Exception {
    super(RedisStore.class);
  }

  @Override
  public void setUpClass() throws IOException, InterruptedException {
    log.info("Starting Redis MiniRedisCluster...");
    try {
      tmpDir.create();
      MiniRedisConfig miniCfg = new MiniRedisConfig(tmpDir.getRoot(), PASSWORD);
      miniCfg.setInstanceName("goraTest");
      miniCfg.setZooKeeperPort(56321);
      cluster = new MiniRedisCluster(miniCfg);
      cluster.start();
    } catch (Exception e) {
      LOG.error("Error starting Redis MiniRedisCluster: {}", e.getMessage());
      // cleanup
      tearDownClass();
    }
  }

  @Override
  public void tearDownClass() throws IOException, InterruptedException {
    log.info("Shutting down Redis MiniRedisCluster...");
    if (cluster != null) {
      cluster.stop();
    }
    tmpDir.delete();
  }
}
