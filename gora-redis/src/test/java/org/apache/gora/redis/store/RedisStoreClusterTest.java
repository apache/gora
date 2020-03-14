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
package org.apache.gora.redis.store;

import org.apache.gora.redis.GoraRedisTestDriver;
import org.apache.gora.redis.util.ServerMode;
import org.apache.gora.redis.util.StorageMode;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests extending {@link org.apache.gora.store.DataStoreTestBase} which run the
 * base JUnit test suite for Gora.
 */
public class RedisStoreClusterTest extends DataStoreTestBase {

  static {
    setTestDriver(new GoraRedisTestDriver(StorageMode.SINGLEKEY, ServerMode.CLUSTER));
  }

  // Unsupported functionality due to the limitations in Redis
  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Redis")
  @Override
  public void testTruncateSchema() throws Exception {
    super.testTruncateSchema();
  }

  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Redis")
  @Override
  public void testDeleteSchema() throws Exception {
    super.testDeleteSchema();
  }

  @Test
  @Ignore("Explicit schema creation related functionality is not supported in Redis")
  @Override
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }
}
