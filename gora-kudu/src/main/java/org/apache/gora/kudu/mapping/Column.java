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
package org.apache.gora.kudu.mapping;

public class Column {

  private String name;
  private FieldType dataType;

  /**
   * Constructor for Column
   *
   * @param name Column's name
   * @param dataType Column's data type
   */
  public Column(String name, FieldType dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  /**
   * Returns the column's name
   *
   * @return Column's name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the column's name
   *
   * @param name Column's name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the column's data-type
   *
   * @return Column's data-type
   */
  public FieldType getDataType() {
    return dataType;
  }

  /**
   * Sets the column's data-type
   *
   * @param dataType Column's data-type
   */
  public void setDataType(FieldType dataType) {
    this.dataType = dataType;
  }

  /**
   * Kudu supported data-type enumeration. For a more detailed list of data
   * types supported by Kudu refer to
   * https://kudu.apache.org/docs/schema_design.html#column-design
   */
  public static enum DataType {
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    UNIXTIME_MICROS,
    FLOAT,
    DOUBLE,
    DECIMAL,
    STRING,
    BINARY;
  }

  public static class FieldType {

    private int precision;
    private int scale;
    private DataType type;

    public FieldType(DataType type) {
      this.type = type;
    }

    public FieldType(int precision, int scale) {
      this.type = DataType.DECIMAL;
      this.precision = precision;
      this.scale = scale;
    }

    public int getPrecision() {
      return precision;
    }

    public void setPrecision(int precision) {
      this.precision = precision;
    }

    public int getScale() {
      return scale;
    }

    public void setScale(int scale) {
      this.scale = scale;
    }

    public DataType getType() {
      return type;
    }

    public void setType(DataType type) {
      this.type = type;
    }

  }
}
