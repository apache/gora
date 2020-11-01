/*
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
package org.apache.gora.aerospike.mapreduce;

import org.apache.gora.aerospike.store.AerospikeStartupLogWaitStrategy;
import org.apache.gora.aerospike.store.AerospikeStore;
import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.MapReduceTestUtils;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * Aerospike Mapreduce test case for the word count
 */
public class TestAerospikeStoreWordCount {

  private static final String DOCKER_CONTAINER_NAME = "aerospike/aerospike-server:4.3.1.4";

  @ClassRule
  public static GenericContainer aerospikeContainer = new GenericContainer(DOCKER_CONTAINER_NAME)
          .withExposedPorts(3000).waitingFor(new AerospikeStartupLogWaitStrategy())
          .withStartupTimeout(Duration.ofSeconds(240));

  private AerospikeStore<String, WebPage> webPageStore;

  private AerospikeStore<String, TokenDatum> tokenStore;

  private Configuration conf = new Configuration();

  @Before
  public void setUp() throws Exception {

    conf.set("gora.aerospikestore.server.ip", aerospikeContainer.getContainerIpAddress());
    conf.set("gora.aerospikestore.server.port", aerospikeContainer.getMappedPort(3000).toString());

    webPageStore = DataStoreFactory
            .createDataStore(AerospikeStore.class, String.class, WebPage.class, conf);
    tokenStore = DataStoreFactory
            .createDataStore(AerospikeStore.class, String.class, TokenDatum.class, conf);
  }

  @After
  public void tearDown() throws Exception {
    webPageStore.close();
    tokenStore.close();
  }

  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testWordCount(conf, webPageStore, tokenStore);
  }
}
