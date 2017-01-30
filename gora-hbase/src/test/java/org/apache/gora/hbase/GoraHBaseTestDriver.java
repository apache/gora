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

package org.apache.gora.hbase;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.hbase.store.HBaseStore;
import org.apache.gora.hbase.util.HBaseClusterSingleton;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;

/**
 * Helper class for third part tests using gora-hbase backend. 
 * @see GoraTestDriver
 */
public class GoraHBaseTestDriver extends GoraTestDriver {

  /**
   * Cluster object used for testing.
   */
  private static  HBaseClusterSingleton cluster ;//= HBaseClusterSingleton.build(1);

  /**
   * Default Constructor.
   */
  public GoraHBaseTestDriver() {
    super(HBaseStore.class);
  }
  
  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    conf = getConf();
    log.info("Setting up HBase Test Driver");
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Teardown HBase test driver");
  }

  @Override
  public void setUp() throws Exception {
    cluster.truncateAllTables();
    // super.setUp() deletes all tables, but must only truncate in the right way -HBaseClusterSingleton-
    //super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    // Do nothing. setUp() must ensure the right data.
  }

  /**
   * Deletes all tables from the MiniCluster
   * @throws Exception in case some table is not able to be deleted.
   */
  public void deleteAllTables() throws Exception {
    cluster.deleteAllTables();
  }

  /**
   * Gets the configuration from the MiniCluster.
   * @return Configuration from MiniCluster.
   */
  public Configuration getConf() {
    return cluster.getHbaseTestingUtil().getConfiguration();
  }

  /**
   * Gets HBaseTestingUtility from the MiniCluster object.
   * @return HBaseTestingUtility object
   */
  public HBaseTestingUtility getHbaseUtil() {
    return cluster.getHbaseTestingUtil();
  }
}
