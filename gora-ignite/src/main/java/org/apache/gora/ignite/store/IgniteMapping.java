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
package org.apache.gora.ignite.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping definitions for Ignite.
 */
public class IgniteMapping {

  private String tableName;
  private Map<String, Column> fields;
  private List<Column> primaryKey;

  public IgniteMapping() {
    fields = new HashMap<>();
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public Map<String, Column> getFields() {
    return fields;
  }

  public void setFields(Map<String, Column> fields) {
    this.fields = fields;
  }

  public List<Column> getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(List<Column> primaryKey) {
    this.primaryKey = primaryKey;
  }

}
