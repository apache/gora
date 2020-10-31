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
import java.time.Duration;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.redis.store.RedisStore;
import org.apache.gora.redis.util.RedisStartupLogWaitStrategy;
import org.apache.gora.redis.util.ServerMode;
import org.apache.gora.redis.util.StorageMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.print.Doc;

/**
 * Helper class to execute tests in a embedded instance of Redis.
 *
 */
public class GoraRedisTestDriver extends GoraTestDriver {

  private static final String DOCKER_IMAGE = "grokzen/redis-cluster:latest";
  private final GenericContainer redisContainer;

  private final StorageMode storageMode;
  private final ServerMode serverMode;

  public GoraRedisTestDriver(StorageMode storageMode, ServerMode serverMode) {
    super(RedisStore.class);
    this.storageMode = storageMode;
    this.serverMode = serverMode;
    GenericContainer container = new GenericContainer(DockerImageName.parse(DOCKER_IMAGE))
        .waitingFor(new RedisStartupLogWaitStrategy())
        .withStartupTimeout(Duration.ofMinutes(3))
        .withEnv("STANDALONE", "true")
        .withEnv("SENTINEL", "true");
    redisContainer = container;

  }

  @Override
  public void setUpClass() throws IOException {
    redisContainer.start();
    log.info("Setting up Redis test driver");
    conf.set("gora.datastore.redis.storage", storageMode.name());
    conf.set("gora.datastore.redis.mode", serverMode.name());
    String bridgeIpAddress = redisContainer.getContainerInfo()
        .getNetworkSettings()
        .getNetworks()
        .values()
        .iterator()
        .next()
        .getIpAddress();
    switch (serverMode) {
      case SINGLE:
        conf.set("gora.datastore.redis.address", bridgeIpAddress + ":" + 7006);
        break;
      case CLUSTER:
        conf.set("gora.datastore.redis.address",
            bridgeIpAddress + ":" + 7000 + ","
            + bridgeIpAddress + ":" + 7001 + ","
            + bridgeIpAddress + ":" + 7002
        );
        break;
      case REPLICATED:
        conf.set("gora.datastore.redis.address",
            bridgeIpAddress + ":" + 7000 + ","
            + bridgeIpAddress + ":" + 7004
        );
        break;
      case SENTINEL:
        conf.set("gora.datastore.redis.masterName", "sentinel7000");
        conf.set("gora.datastore.redis.readMode", "MASTER");
        conf.set("gora.datastore.redis.address",
            bridgeIpAddress + ":" + 5000 + ","
            + bridgeIpAddress + ":" + 5001 + ","
            + bridgeIpAddress + ":" + 5000
        );
        break;
      default:
        throw new AssertionError(serverMode.name());
    }
  }

  @Override
  public void tearDownClass() throws Exception {
    redisContainer.stop();
    log.info("Tearing down Redis test driver");
  }
}
