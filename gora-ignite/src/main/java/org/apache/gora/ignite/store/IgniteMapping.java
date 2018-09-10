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

  /**
   * Empty constructor for the IgniteMapping class
   */
  public IgniteMapping() {
    fields = new HashMap<>();
  }

  /**
   * Returns the name of ignite table linked to the mapping.
   *
   * @return Table's name.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Sets the table name of the ignite mapping
   *
   * @param tableName Table's name
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * Returns a map with all field-column mappings
   *
   * @return Map containing mapped fields
   */
  public Map<String, Column> getFields() {
    return fields;
  }

  /**
   * Sets field-column mappings
   *
   * @param fields Map containing mapped fields
   */
  public void setFields(Map<String, Column> fields) {
    this.fields = fields;
  }

  /**
   * Returns the primary key's list of columns
   *
   * @return List with columns
   */
  public List<Column> getPrimaryKey() {
    return primaryKey;
  }

  /**
   * Sets the primary key's columns
   *
   * @param primaryKey List with columns
   */
  public void setPrimaryKey(List<Column> primaryKey) {
    this.primaryKey = primaryKey;
  }

}
