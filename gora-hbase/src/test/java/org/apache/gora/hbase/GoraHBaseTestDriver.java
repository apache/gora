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
import org.apache.hadoop.conf.Configuration;

//HBase imports
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;

/**
 * Helper class for third part tests using gora-hbase backend. 
 * @see GoraTestDriver
 */
public class GoraHBaseTestDriver extends GoraTestDriver {

  protected HBaseTestingUtility hbaseUtil;
  protected int numServers = 1;
  
  public GoraHBaseTestDriver() {
    super(HBaseStore.class);
    hbaseUtil = new HBaseTestingUtility();
  }

  public void setNumServers(int numServers) {
    this.numServers = numServers;
  }
  
  public int getNumServers() {
    return numServers;
  }
  
  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
    log.info("Starting HBase cluster");
    hbaseUtil.startMiniCluster(numServers);
  }

  @Override
  public void tearDownClass() throws Exception {
    super.tearDownClass();
    log.info("Stoping HBase cluster");
    hbaseUtil.shutdownMiniCluster();
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void deleteAllTables() throws Exception {
    HBaseAdmin admin = hbaseUtil.getHBaseAdmin();
    for(HTableDescriptor table:admin.listTables()) {
      admin.disableTable(table.getName());
      admin.deleteTable(table.getName());
    }
  }
  
  public Configuration  getConf() {
      return hbaseUtil.getConfiguration();
  }
  
  public HBaseTestingUtility getHbaseUtil() {
    return hbaseUtil;
  }
  
}		
