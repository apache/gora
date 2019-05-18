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

import java.io.IOException;
import org.apache.gora.redis.GoraRedisTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests extending {@link org.apache.gora.store.DataStoreTestBase}
 * which run the base JUnit test suite for Gora.
 */
public class RedisStoreTest extends DataStoreTestBase {

  static {
    try {
    setTestDriver(new GoraRedisTestDriver());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  public GoraRedisTestDriver getTestDriver() {
    return (GoraRedisTestDriver) testDriver;
  }

  //Until GORA-66 is resolved this test will always fail, so
  //do not run it
  @Ignore("skipped until GORA-66 is resolved")
  @Override
  public void testDeleteByQueryFields() throws IOException {
  }
  
  @Test
  @Ignore("Redis does not support Result#size() without limit set")
  @Override
  public void testResultSize() throws Exception {
  }

  @Test
  @Ignore("Redis does not support Result#size() without limit set")
  @Override
  public void testResultSizeStartKey() throws Exception {
  }

  @Ignore("Redis does not support Result#size() without limit set")
  @Override
  public void testResultSizeEndKey() throws Exception {
  }

  @Test
  @Ignore("Redis does not support Result#size() without limit set")
  @Override
  public void testResultSizeKeyRange() throws Exception {
  }
}
