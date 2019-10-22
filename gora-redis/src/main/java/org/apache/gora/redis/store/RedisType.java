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

/**
 * Supported data types for Redis.
 *
 * Refer to: https://redis.io/topics/data-types
 */
public enum RedisType {
  /**
   * Strings are the most basic kind of Redis value. Redis Strings are binary
   * safe, this means that a Redis string can contain any kind of data.
   */
  STRING,
  /**
   * Redis Lists are simply lists of strings, sorted by insertion order.
   */
  LIST,
  /**
   * Redis Hashes are maps between string fields and string values, so they are
   * the perfect data type to represent objects.
   */
  HASH
}
