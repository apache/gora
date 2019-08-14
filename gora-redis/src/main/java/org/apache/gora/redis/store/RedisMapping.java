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

import java.util.Map;

/**
 * Mapping definitions for Redis.
 */
public class RedisMapping {

  private int database;
  private String prefix;
  private Map<String, String> fields;
  private Map<String, RedisType> types;

  public int getDatabase() {
    return database;
  }

  public void setDatabase(int datebase) {
    this.database = datebase;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public Map<String, String> getFields() {
    return fields;
  }

  public void setFields(Map<String, String> fields) {
    this.fields = fields;
  }

  public Map<String, RedisType> getTypes() {
    return types;
  }

  public void setTypes(Map<String, RedisType> types) {
    this.types = types;
  }

}
