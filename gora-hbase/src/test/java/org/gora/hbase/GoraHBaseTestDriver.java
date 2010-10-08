
package org.gora.hbase;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.gora.GoraTestDriver;
import org.gora.hbase.store.HBaseStore;

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
  
  public HBaseConfiguration getConf() {
    return hbaseUtil.getConfiguration();
  }
  
  public HBaseTestingUtility getHbaseUtil() {
    return hbaseUtil;
  }
  
}
