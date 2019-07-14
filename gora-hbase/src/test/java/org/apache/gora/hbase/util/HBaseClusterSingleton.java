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
package org.apache.gora.hbase.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that will creates a single instance of HBase MiniCluster.
 * Copied from HBase support for Apache James:
 * http://svn.apache.org/repos/asf/james/mailbox/trunk/hbase/src/test/java/org/apache/james/mailbox/hbase/
 */
public final class HBaseClusterSingleton {

  private static final Logger LOG = LoggerFactory.getLogger(HBaseClusterSingleton.class);
  private static final HBaseTestingUtility htu = new HBaseTestingUtility();
  private static HBaseClusterSingleton cluster = null;
  private MiniHBaseCluster hbaseCluster;
  private Configuration conf;

  /**
   * Builds a MiniCluster instance.
   * @return the {@link HBaseClusterSingleton} instance
   * @throws RuntimeException
   */
  public static synchronized HBaseClusterSingleton build(int numServers) throws RuntimeException {
    LOG.info("Retrieving cluster instance.");
    if (cluster == null) {
      cluster = new HBaseClusterSingleton(numServers);
    }
    return cluster;
  }

  private HBaseClusterSingleton(int numServers) throws RuntimeException {

    // Workaround for HBASE-5711, we need to set config value dfs.datanode.data.dir.perm
    // equal to the permissions of the temp dirs on the filesystem. These temp dirs were
    // probably created using this process' umask. So we guess the temp dir permissions as
    // 0777 & ~umask, and use that to set the config value.
    try {
      Process process = Runtime.getRuntime().exec("/bin/sh -c umask");
      BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
      int rc = process.waitFor();
      if(rc == 0) {
        String umask = br.readLine();

        int umaskBits = Integer.parseInt(umask, 8);
        int permBits = 0777 & ~umaskBits;
        String perms = Integer.toString(permBits, 8);

        LOG.info("Setting dfs.datanode.data.dir.perm to {}",  perms);
        htu.getConfiguration().set("dfs.datanode.data.dir.perm", perms);
      } else {
        LOG.warn("Failed running umask command in a shell, nonzero return value");
      }
    } catch (Exception e) {
      // ignore errors, we might not be running on POSIX, or "sh" might not be on the path
      LOG.warn("Couldn't get umask {}", e);
    }

    htu.getConfiguration().setBoolean("dfs.support.append", true);
    htu.getConfiguration().setInt("zookeeper.session.timeout", 20000);
    //htu.getConfiguration().set("hbase.zookeeper.quorum", "localhost");
    //htu.getConfiguration().setInt("hbase.zookeeper.property.clientPort", 2181);
    
    try {
      LOG.info("Start HBase mini cluster.");
      hbaseCluster = htu.startMiniCluster(numServers);
      LOG.info("After cluster start-up.");
      hbaseCluster.waitForActiveAndReadyMaster();
      LOG.info("After active and ready.");
      conf = hbaseCluster.getConfiguration();
      /*
      LOG.info("Start mini mapreduce cluster.");
      htu.startMiniMapReduceCluster(numServers);
      LOG.info("After mini mapreduce cluster start-up.");
      */
    } catch (Exception ex) {
      throw new RuntimeException("Minicluster not starting.");
    } finally {
      // add a shutdown hook for shutting down the minicluster.
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            if (hbaseCluster != null) {
              hbaseCluster.shutdown();
            }
            //htu.shutdownMiniMapReduceCluster();
          } catch (IOException e) {
            throw new RuntimeException("Exception shutting down cluster.");
          }
        }
      }));
    }
  }

  /**
   * Return a configuration for the runnning MiniCluster.
   * @return
   */
  public Configuration getConf() {
    return conf;
  }

  /**
   * Creates a table with the specified column families.
   * @param tableName the table name
   * @param columnFamilies the colum families
   * @throws IOException
   */
  public void ensureTable(String tableName, String... columnFamilies) throws IOException {
    byte[][] cfs = new byte[columnFamilies.length][];
    for (int i = 0; i < columnFamilies.length; i++) {
      cfs[i] = Bytes.toBytes(columnFamilies[i]);
    }
    ensureTable(Bytes.toBytes(tableName), cfs);
  }

  /**
   * Creates a table with the specified column families.
   * @param tableName the table name
   * @param cfs the column families
   * @throws IOException
   */
  public void ensureTable(byte[] tableName, byte[][] cfs) throws IOException {
    Admin admin = htu.getAdmin();
    if (!admin.tableExists(TableName.valueOf(tableName))) {
      Table hTable = htu.createTable(TableName.valueOf(tableName), cfs);
      hTable.close();
    }
  }

  /**
   * Truncates all tables
   * @throws Exception
   */
  public void truncateAllTables() throws Exception {
    Admin admin = htu.getAdmin();
    for(TableDescriptor table:admin.listTableDescriptors()) {
      Table hTable = htu.deleteTableData(table.getTableName());
      hTable.close();
    }
  }
  
  
  /**
   * Delete all tables
   * @throws Exception
   */
  public void deleteAllTables() throws Exception {
    Admin admin = htu.getAdmin();
    for(TableDescriptor table:admin.listTableDescriptors()) {
      admin.disableTable(table.getTableName());
      admin.deleteTable(table.getTableName());
    }
  }

  public void shutdownMiniCluster() throws IOException {
    hbaseCluster.shutdown();
  }

  /**
   * Returns the HBaseTestingUtility instance
   * @return static instance of HBaseTestingUtility
   */
  public HBaseTestingUtility getHbaseTestingUtil() {
    return htu;
  }
}
