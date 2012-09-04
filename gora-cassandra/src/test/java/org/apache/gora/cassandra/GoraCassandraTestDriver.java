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

/**
 * @author lewismc
 *
 */

package org.apache.gora.cassandra;

import java.io.IOException;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.cassandra.store.CassandraStore;

import org.apache.hadoop.conf.Configuration;

import java.io.File;

import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.thrift.CassandraDaemon;

// Logging imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for third party tests using gora-cassandra backend. 
 * @see GoraTestDriver for test specifics.
 * This driver is the base for all test cases that require an embedded Cassandra
 * server. In this case we draw on Hector's @see EmbeddedServerHelper.
 * It starts (setUp) and stops (tearDown) embedded Cassandra server.
 * 
 * @author lewismc
 */

public class GoraCassandraTestDriver extends GoraTestDriver {
  private static Logger log = LoggerFactory.getLogger(GoraCassandraTestDriver.class);
  
  private String baseDirectory = "target/test";

  private CassandraDaemon cassandraDaemon;

  private Thread cassandraThread;

  /**
   * @return temporary base directory of running cassandra instance
   */
  public String getBaseDirectory() {
    return baseDirectory;
  }

  public GoraCassandraTestDriver() {
    super(CassandraStore.class);
  }
	
  /**
   * starts embedded Cassandra server.
   *
   * @throws Exception
   * 	if an error occurs
   */
  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Starting embedded Cassandra Server...");
    try {
      cleanupDirectoriesFailover();
      FileUtils.createDirectory(baseDirectory);
      System.setProperty("log4j.configuration", "file:target/test-classes/log4j-server.properties");
      System.setProperty("cassandra.config", "file:target/test-classes/cassandra.yaml");
      
      cassandraDaemon = new CassandraDaemon();
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
   * @throws Exception
   * 	if an error occurs
   */
  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Shutting down Embedded Cassandra server...");
    if (cassandraThread != null) {
      cassandraDaemon.stop();
      cassandraDaemon.destroy();
      cassandraThread.interrupt();
      cassandraThread = null;
    }
    cleanupDirectoriesFailover();
  }  

  /**
   * Cleans up cassandra's temporary base directory.
   *
   * In case o failure waits for 250 msecs and then tries it again, 3 times totally.
   */
  public void cleanupDirectoriesFailover() {
    int tries = 3;
    while (tries-- > 0) {
      try {
	cleanupDirectories();
	break;
      } catch (Exception e) {
	// ignore exception
	try {
	  Thread.sleep(250);
	} catch (InterruptedException e1) {
	  // ignore exception
	}
      }
    }
  }

  /**
   * Cleans up cassandra's temporary base directory.
   *
   * @throws Exception
   * 	if an error occurs
   */
  public void cleanupDirectories() throws Exception {
    File dirFile = new File(baseDirectory);
    if (dirFile.exists()) {
      FileUtils.deleteRecursive(dirFile);
    }
  }
}
