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

public class Column {

  private String name;
  private FieldType dataType;

  public Column(String name, FieldType dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FieldType getDataType() {
    return dataType;
  }

  public void setDataType(FieldType dataType) {
    this.dataType = dataType;
  }
  
  

  /**
   * For a more detailed list of data types supported by Ignite and its
   * equivalents in Java refer to
   * https://apacheignite-sql.readme.io/docs/data-types
   */
  public static enum FieldType {
    BOOLEAN,
    INT,
    TINYINT,
    SMALLINT,
    BIGINT,
    DECIMAL,
    DOUBLE,
    REAL,
    TIME,
    DATE,
    TIMESTAMP,
    VARCHAR,
    CHAR,
    UUID,
    BINARY,
    ARRAY
  }
}
