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
package org.apache.gora.elasticsearch.mapping;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Field definition for the Elasticsearch index.
 */
public class Field {

  private String name;
  private FieldType dataType;

  /**
   * Constructor for Field.
   *
   * @param name     Field's name
   * @param dataType Field's data type
   */
  public Field(String name, FieldType dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  /**
   * Returns the field's name.
   *
   * @return Field's name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the field's name.
   *
   * @param name Field's name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the field's data-type.
   *
   * @return Field's data-type
   */
  public FieldType getDataType() {
    return dataType;
  }

  /**
   * Sets the field's data-type.
   *
   * @param dataType Field's data-type
   */
  public void setDataType(FieldType dataType) {
    this.dataType = dataType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Field field = (Field) o;
    return Objects.equals(name, field.name) && Objects.equals(dataType, field.dataType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, dataType);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Field.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("dataType=" + dataType)
            .toString();
  }

  /**
   * Elasticsearch supported data-type enumeration. For a more detailed list of data
   * types supported by Elasticsearch refer to
   * https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
   */
  public enum DataType {
    BINARY,
    BOOLEAN,
    KEYWORD,
    CONSTANT_KEYWORD,
    WILDCARD,
    LONG,
    INTEGER,
    SHORT,
    BYTE,
    DOUBLE,
    FLOAT,
    HALF_FLOAT,
    SCALED_FLOAT,
    OBJECT,
    FLATTENED,
    NESTED,
    TEXT,
    COMPLETION,
    SEARCH_AS_YOU_TYPE,
    TOKEN_COUNT
  }

  public static class FieldType {

    private DataType type;

    // Parameter for scaled_float type.
    private int scalingFactor;

    /**
     * Constructor for FieldType.
     *
     * @param type Elasticsearch data type
     */
    public FieldType(DataType type) {
      this.type = type;
    }

    /**
     * Constructor for FieldType Implicitly uses scaled_float Elasticsearch data type
     * with scaling factor parameter.
     *
     * @param scalingFactor scaled_float field's scaling factor
     */
    public FieldType(int scalingFactor) {
      this.type = DataType.SCALED_FLOAT;
      this.scalingFactor = scalingFactor;
    }

    /**
     * @return Elasticsearch data type
     */
    public DataType getType() {
      return type;
    }

    /**
     * @param type Elasticsearch data type
     */
    public void setType(DataType type) {
      this.type = type;
    }

    /**
     * Returns the scaling factor of scaled_float type.
     *
     * @return scaled_float field's scaling factor
     */
    public int getScalingFactor() {
      return scalingFactor;
    }

    /**
     * Sets the scaling factor of scaled_float type.
     *
     * @param scalingFactor scaled_float field's scaling factor
     */
    public void setScalingFactor(int scalingFactor) {
      this.scalingFactor = scalingFactor;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FieldType fieldType = (FieldType) o;
      return scalingFactor == fieldType.scalingFactor && type == fieldType.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, scalingFactor);
    }
  }
}
