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
import org.testcontainers.containers.FixedHostPortGenericContainer;

/**
 * Helper class to execute tests in a embedded instance of Redis.
 *
 * @author Xavier Sumba
 */
public class GoraRedisTestDriver extends GoraTestDriver {

  private static final String DOCKER_CONTAINER_NAME = "grokzen/redis-cluster:latest";
  private FixedHostPortGenericContainer redisContainer = ((FixedHostPortGenericContainer) new FixedHostPortGenericContainer(DOCKER_CONTAINER_NAME)
      .waitingFor(new RedisStartupLogWaitStrategy())
      .withStartupTimeout(Duration.ofMinutes(3))
      .withEnv("STANDALONE", "true")
      .withEnv("SENTINEL", "true"))
      .withFixedExposedPort(7000, 7000)
      .withFixedExposedPort(7001, 7001)
      .withFixedExposedPort(7002, 7002)
      .withFixedExposedPort(7003, 7003)
      .withFixedExposedPort(7004, 7004)
      .withFixedExposedPort(7005, 7005)
      .withFixedExposedPort(7006, 7006)
      .withFixedExposedPort(7007, 7007)
      .withFixedExposedPort(5000, 5000)
      .withFixedExposedPort(5001, 5001)
      .withFixedExposedPort(5002, 5002);
  private StorageMode storageMode;
  private ServerMode serverMode;

  public GoraRedisTestDriver(StorageMode storageMode, ServerMode serverMode) {
    super(RedisStore.class);
    this.storageMode = storageMode;
    this.serverMode = serverMode;
  }

  @Override
  public void setUpClass() throws IOException {
    redisContainer.start();
    log.info("Setting up Redis test driver");
    conf.set("gora.datastore.redis.storage", storageMode.name());
    conf.set("gora.datastore.redis.mode", serverMode.name());
    switch (serverMode) {
      case SINGLE:
        conf.set("gora.datastore.redis.address", redisContainer.getContainerIpAddress() + ":" + 7006);
        break;
      case CLUSTER:
        conf.set("gora.datastore.redis.address",
            redisContainer.getContainerIpAddress() + ":" + 7000 + ","
            + redisContainer.getContainerIpAddress() + ":" + 7001 + ","
            + redisContainer.getContainerIpAddress() + ":" + 7002
        );
        break;
      case REPLICATED:
        conf.set("gora.datastore.redis.address",
            redisContainer.getContainerIpAddress() + ":" + 7000 + ","
            + redisContainer.getContainerIpAddress() + ":" + 7004
        );
        break;
      case SENTINEL:
        conf.set("gora.datastore.redis.masterName", "sentinel7000");
        conf.set("gora.datastore.redis.readMode", "MASTER");
        conf.set("gora.datastore.redis.address",
            redisContainer.getContainerIpAddress() + ":" + 5000 + ","
            + redisContainer.getContainerIpAddress() + ":" + 5001 + ","
            + redisContainer.getContainerIpAddress() + ":" + 5002
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
