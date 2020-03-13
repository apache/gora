/**
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

package org.apache.gora.persistency.ws.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.gora.persistency.Persistent;

/**
 * Base class implementing common functionality for Web services
 * backed persistent classes.
 */
public abstract class PersistentWSBase implements Persistent {

  /**
   * Maps keys to their own classes
   */
  protected static Map<Class<?>, Map<String, Integer>> FIELD_MAP =
          new HashMap<>();

  /**
   * Maps fields to their own classes
   */
  protected static Map<Class<?>, String[]> FIELDS =
          new HashMap<>();


  /** Subclasses should call this function for all the persistable fields
   * in the class to register them.
   * @param clazz the Persistent class
   * @param fields the name of the fields of the class
   */
  protected static void registerFields(Class<?> clazz, String... fields) {
    FIELDS.put(clazz, fields);
    int fieldsLength = fields == null ? 0 :fields.length;
    HashMap<String, Integer> map = new HashMap<>(fieldsLength);

    for(int i=0; i < fieldsLength; i++) {
      map.put(fields[i], i);
    }
    FIELD_MAP.put(clazz, map);
  }

  /**
   * Gets fields using a specific class
   * @return a {@link java.util.Map} of fields to their own classes
   */
  public String[] getFields() {
    return FIELDS.get(getClass());
  }

  /**
   * Gets a specific field from the fields map
   * @return a specific {@link java.lang.String} field
   */
  public String getField(int index) {
    return FIELDS.get(getClass())[index];
  }

  /**
   * Gets a field index based on the field name
   * @return a specific {@link java.lang.String} field
   */
  public int getFieldIndex(String field) {
    return FIELD_MAP.get(getClass()).get(field);
  }

  @Override
  /**
   * Clears maps of fields
   */
  public void clear() {
    // TODO study the specific cases for other datatypes
    clearDirty();
    clearReadable();
  }

  private void clearReadable() {

  }

  @Override
  /**
   * Determines if an object has been modified or not
   */
  public boolean isDirty() {
    return true;
  }

  @Override
  /**
   * Determines if an object has been modified or not
   * based on its field index
   */
  public boolean isDirty(int fieldIndex) {
    return true;
  }

  @Override
  /**
   * Determines if an object has been modified or not
   * based on its field name
   */
  public boolean isDirty(String field) {
    return isDirty(getFieldIndex(field));
  }

  @Override
  /**
   * Determines if an object is equal to this class
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    // TODO we should check if the object has schema or not
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    return result;
  }

  /** ByteBuffer.hashCode() takes into account the position of the
   * buffer, but we do not want that*/
  private int getByteBufferHashCode(ByteBuffer buf) {
    int h = 1;
    int p = buf.arrayOffset();
    for (int j = buf.limit() - 1; j >= p; j--)
      h = 31 * h + buf.get(j);
    return h;
  }

  @Override
  /**
   * Clones a persistent object
   */
  public Persistent clone() {
    return null;
  }

  @Override
  /**
   * Converts an object to string
   */
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(super.toString());
    builder.append(" {\n");
    // TODO get fields
    builder.append("}");
    return builder.toString();
  }

  /**
   * Checks if a field is equal between two objects
   * @param index index position of the field to compare
   * @param value object to compare against
   * @return true if this field if equal otherwise false
   */
  protected boolean isFieldEqual(int index, Object value) {
    //TODO not implemented
    return true;
  }
}
