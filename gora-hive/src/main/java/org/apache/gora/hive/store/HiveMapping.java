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

package org.apache.gora.hive.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping definitions for Hive Store
 */
public class HiveMapping {

  public static final String DEFAULT_KEY_NAME = "primary_key";

  /**
   * Name of the schema table to be used
   */
  private String tableName;

  /**
   * List of field names in the schema
   */
  private List<String> fields = new ArrayList<>();

  /**
   * Map of column names mapping to field names
   */
  private Map<String, String> columnFieldMap = new HashMap<>();

  /**
   * Getter for table name of the schema
   * @return table name as a String
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Setter for the table name of the schema
   * @param tableName table name as a string
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * Getter for the list of field names in the schema
   * @return field names as a list of strings.
   */
  public List<String> getFields() {
    return fields;
  }

  /**
   * Setter for the list of field names in the schema
   * @param fields field names as a list of strings.
   */
  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  /**
   * Getter for columnFieldMap
   * @return map of column names to field names
   */
  public Map<String, String> getColumnFieldMap() {
    return columnFieldMap;
  }

  /**
   * Setter for columnFieldMap
   * @param columnFieldMap map of column names to field names map
   */
  public void setColumnFieldMap(Map<String, String> columnFieldMap) {
    this.columnFieldMap = columnFieldMap;
  }
}
