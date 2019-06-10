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

import com.github.fppt.jedismock.RedisServer;
import java.io.IOException;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.redis.store.RedisStore;

/**
 * Helper class to execute tests in a embedded instance of Redis.
 *
 * @author Xavier Sumba
 */
public class GoraRedisTestDriver extends GoraTestDriver {

  private static RedisServer server = null;

  public GoraRedisTestDriver() {
    super(RedisStore.class);
  }

  @Override
  public void setUpClass() throws IOException {
    server = RedisServer.newRedisServer(6387);
    server.start();
  }

  @Override
  public void tearDownClass() throws Exception {
    log.info("Shutting down Redis Mock...");
    server.stop();
    server = null;
  }
}