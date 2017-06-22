/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * This Class represents the Cassandra Column.
 */
public class Field {

  private String fieldName;

  private String columnName;

  private String type;

  public Field() {
    properties = new HashMap<>(2);
  }

  private Map<String, String> properties;

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getProperty(String key) {
    return this.properties.get(key);
  }
}
