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
package org.apache.gora.redis.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

/**
 * Utility class for detecting when the docker container is ready.
 */
public class RedisStartupLogWaitStrategy extends GenericContainer.AbstractWaitStrategy {

  private static final String REGEX = ".*Background AOF rewrite finished successfully.*";
  private final int times = 3;

  @Override
  protected void waitUntilReady() {
    WaitingConsumer waitingConsumer = new WaitingConsumer();
    this.container.followOutput(waitingConsumer);
    Predicate waitPredicate = (outputFrame) -> {
      String trimmedFrameText = ((OutputFrame) outputFrame).getUtf8String().replaceFirst("\n$", "");
      return trimmedFrameText.matches(REGEX);
    };

    try {
      waitingConsumer.waitUntil(waitPredicate, this.startupTimeout.getSeconds(), TimeUnit.SECONDS,
          this.times);
    } catch (TimeoutException var4) {
      throw new ContainerLaunchException(
          "Timed out waiting for log output matching Redis server startup Log  \'" + REGEX
          + "\'");
    }
  }
}
