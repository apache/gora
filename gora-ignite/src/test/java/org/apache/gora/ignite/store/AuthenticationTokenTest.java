/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.ignite.store;

import java.util.List;
import java.util.Properties;
import org.apache.ignite.minicluster.MiniIgniteCluster;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests authentication token is serialized correctly.
 */
public class AuthenticationTokenTest {
  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationTokenTest.class);

  private static final String GORA_DATASTORE =
      DataStoreFactory.GORA + "." + DataStoreFactory.DATASTORE + ".";
  private static final String PASSWORD = "password";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MiniIgniteCluster cluster;
  private DataStore<String, Employee> employeeStore;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    cluster = new MiniIgniteCluster(temporaryFolder.getRoot(), PASSWORD);
    cluster.start();

    Properties properties = DataStoreFactory.createProps();
    properties.setProperty(
        GORA_DATASTORE + IgniteStore.MOCK_PROPERTY,
        "false");
    properties.setProperty(
        GORA_DATASTORE + IgniteStore.INSTANCE_NAME_PROPERTY,
        cluster.getInstanceName());
    properties.setProperty(
        GORA_DATASTORE + IgniteStore.ZOOKEEPERS_NAME_PROPERTY,
        cluster.getZooKeepers());
    properties.setProperty(
        GORA_DATASTORE + IgniteStore.PASSWORD_PROPERTY,
        PASSWORD);

    employeeStore = DataStoreFactory.createDataStore(
        IgniteStore.class,
        String.class,
        Employee.class,
        new Configuration(),
        properties);
  }

  @After
  public void tearDown() throws Exception {
    cluster.stop();
  }

  @Test
  public void testAuthenticationTokenIsSerializedCorrectly() throws Exception {
    List<PartitionQuery<String, Employee>> partitions =
        employeeStore.getPartitions(employeeStore.newQuery());
    LOG.debug("partitions {}", partitions);
  }
}
