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

package org.apache.gora.arangodb.store;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

public class ArangoDBStartupWaitStrategy extends LogMessageWaitStrategy {

  private static final String regEx = ".*ArangoDB \\(version 3.6.4 \\[linux\\]\\) is ready for business. Have fun!\n";
  private int times = 1;

  public ArangoDBStartupWaitStrategy() {
    withRegEx(regEx);
    withTimes(times);
  }

}