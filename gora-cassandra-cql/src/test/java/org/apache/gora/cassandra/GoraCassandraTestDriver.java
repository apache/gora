/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author lewismc
 */

package org.apache.gora.cassandra;

import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * Helper class for third party tests using gora-cassandra backend.
 *
 * @see GoraTestDriver for test specifics.
 * This driver is the base for all test cases that require an embedded Cassandra
 * server. In this case we draw on Hector's @see EmbeddedServerHelper.
 * It starts (setUp) and stops (tearDown) embedded Cassandra server.
 */
public class GoraCassandraTestDriver extends GoraTestDriver {
  private static Logger log = LoggerFactory.getLogger(GoraCassandraTestDriver.class);

  private static String baseDirectory = "target/test";

  private CassandraDaemon cassandraDaemon;

  private Thread cassandraThread;

  private Properties properties;

  public GoraCassandraTestDriver() {
    super(CassandraStore.class);
  }

  /**
   * Cleans up cassandra's temporary base directory.
   * <p>
   * In case o failure waits for 250 msecs and then tries it again, 3 times totally.
   */
  private static void cleanupDirectoriesFailover() {
    int tries = 3;
    while (tries-- > 0) {
      try {
        cleanupDirectories();
        break;
      } catch (Exception e) {
        // ignore exception
        try {
          Thread.sleep(2500);
        } catch (InterruptedException e1) {
          // ignore exception
        }
      }
    }
  }

  /**
   * Cleans up cassandra's temporary base directory.
   *
   * @throws Exception if an error occurs
   */
  private static void cleanupDirectories() throws Exception {
    File dirFile = new File(baseDirectory);
    if (dirFile.exists()) {
      FileUtils.deleteRecursive(dirFile);
    }
  }

  public void setParameters(Properties parameters) {
    this.properties = parameters;
  }

  @Override
  public <K, T extends Persistent> DataStore<K, T> createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    return DataStoreFactory.createDataStore(CassandraStore.class, keyClass, persistentClass, conf, properties, null);
  }

  /**
   * @return temporary base directory of running cassandra instance
   */
  public String getBaseDirectory() {
    return baseDirectory;
  }

  /**
   * Starts embedded Cassandra server.
   *
   * @throws Exception if an error occurs
   */
  @Override
  public void setUpClass() {
    log.info("Starting embedded Cassandra Server...");
    try {
      cleanupDirectoriesFailover();
      FileUtils.createDirectory(baseDirectory);
      System.setProperty("log4j.configuration", "log4j-server.properties");
      System.setProperty("cassandra.config", "cassandra.yaml");

      cassandraDaemon = new CassandraDaemon();
      cassandraDaemon.completeSetup();
      cassandraDaemon.applyConfig();
      cassandraDaemon.init(null);
      cassandraThread = new Thread(new Runnable() {

        public void run() {
          try {
            cassandraDaemon.start();
          } catch (Exception e) {
            log.error("Embedded casandra server run failed!", e);
          }
        }
      });

      cassandraThread.setDaemon(true);
      cassandraThread.start();
    } catch (Exception e) {
      log.error("Embedded casandra server start failed!", e);

      // cleanup
      tearDownClass();
    }
  }

  /**
   * Stops embedded Cassandra server.
   *
   * @throws Exception if an error occurs
   */
  @Override
  public void tearDownClass() {
    log.info("Shutting down Embedded Cassandra server...");
    if (cassandraThread != null) {
      cassandraDaemon.stop();
      cassandraDaemon.destroy();
    }
  }
}
