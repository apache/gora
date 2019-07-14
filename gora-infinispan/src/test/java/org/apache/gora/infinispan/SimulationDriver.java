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
package org.apache.gora.infinispan;

import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TestResourceTracker;
import org.infinispan.test.fwk.TransportFlags;
import org.infinispan.transaction.TransactionMode;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.infinispan.test.TestingUtil.blockUntilCacheStatusAchieved;

/**
 * @author Pierre Sutra
 */
public class SimulationDriver extends MultipleCacheManagersTest {

  private int numberOfNodes = 0 ;
  @SuppressWarnings("unchecked")
  private List<String> cacheNames = EMPTY_LIST;
  private List<HotRodServer> servers = new ArrayList<>();
  private String connectionString ="";

  public SimulationDriver(int numberOfNodes, List<String> cacheNames){
    this.numberOfNodes = numberOfNodes;
    this.cacheNames = cacheNames;
    TestResourceTracker.setThreadTestName("test");
  }

  public int getNumberOfNodes(){
    return numberOfNodes;
  }

  public List<String> getCacheNames(){
    return this.cacheNames;
  }

  public void create() throws Throwable {
    createCacheManagers();
  }

  @Override
  public void createCacheManagers() throws Throwable {
    ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
    create(numberOfNodes, builder);
  }

  @Override
  public void destroy(){
    // Correct order is to stop servers first
    try {
      for (HotRodServer server : servers)
        HotRodClientTestingUtil.killServers(server);
    } finally {
      // And then the caches and cache managers
      super.destroy();
    }
  }

  public String connectionString(){
    return connectionString;
  }

  // Helpers

  protected void create(int nnodes, ConfigurationBuilder defaultBuilder) {

    // Start Hot Rod servers at each site.
    for (int j = 0; j < nnodes; j++) {
      GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      Transport transport = gbuilder.transport().getTransport();
      gbuilder.transport().transport(transport);
      gbuilder.transport().clusterName("test");
      startHotRodServer(gbuilder, defaultBuilder, j + 1);
    }

    // Create appropriate caches at each node.
    ConfigurationBuilder builder = hotRodCacheConfiguration(getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
    builder.indexing()
    .enable()
    .index(Index.LOCAL)
    .addProperty("default.directory_provider", "ram")
    .addProperty("hibernate.search.default.exclusive_index_use","true")
    .addProperty("hibernate.search.default.indexmanager","near-real-time")
    .addProperty("hibernate.search.default.indexwriter.ram_buffer_size","128")
    .addProperty("lucene_version", "LUCENE_CURRENT");
    builder.clustering().hash().numOwners(1);
    builder.jmxStatistics().enable();
    builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
    Configuration configuration = builder.build();
    for (int j = 0; j < nnodes; j++) {
      for (String name : cacheNames) {
        manager(j).defineConfiguration(name,configuration);
        manager(j).getCache(name, true);
      }
    }
    // Verify that default caches are started.
    for (int j = 0; j < nnodes; j++) {
      assert manager(j).getCache() != null;
    }

    // Verify that the default caches is running.
    for (int j = 0; j < nnodes; j++) {
      blockUntilCacheStatusAchieved(
          manager(j).getCache(), ComponentStatus.RUNNING, 10000);
    }

    for (int j = 0; j < nnodes; j++) {
      if (j!=0) connectionString+=";";
      connectionString += server(j).getHost() + ":" + server(j).getPort();
    }
  }

  protected HotRodServer server(int i) {
    return servers.get(i);
  }

  protected List<HotRodServer> servers(){
    return  servers;
  }

  protected void startHotRodServer(GlobalConfigurationBuilder gbuilder, ConfigurationBuilder builder, int nodeIndex) {
    TransportFlags transportFlags = new TransportFlags();
    EmbeddedCacheManager cm = addClusterEnabledCacheManager(gbuilder, builder, transportFlags);
    HotRodServer server = HotRodClientTestingUtil.startHotRodServer(cm);
    servers.add(server);
  }
}
