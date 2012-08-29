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
/**
 * @author Renato Marroquin
 */

package org.apache.gora.persistency.ws.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;

/**
 * Base classs implementing common functionality for Web services 
 * backed persistent classes.
 */
public abstract class PersistentWSBase implements Persistent  {

  /**
   * Maps keys to their own classes
   */
  protected static Map<Class<?>, Map<String, Integer>> FIELD_MAP =
    new HashMap<Class<?>, Map<String,Integer>>();

  /**
   * Maps fields to their own classes
   */
  protected static Map<Class<?>, String[]> FIELDS =
    new HashMap<Class<?>, String[]>();
    
  /**
   * Object used to manage the state of fields
   */
  private StateManager stateManager;

  /**
   * Constructor
   */
  protected PersistentWSBase() {
    this(new StateManagerWSImpl());
  }

  /**
   * Constructor using a stateManager object
   * @param stateManager
   */
  protected PersistentWSBase(StateManager stateManager) {
    this.stateManager = stateManager;
    stateManager.setManagedPersistent(this);
  }

  /** Subclasses should call this function for all the persistable fields
   * in the class to register them.
   * @param clazz the Persistent class
   * @param fields the name of the fields of the class
   */
  protected static void registerFields(Class<?> clazz, String... fields) {
    FIELDS.put(clazz, fields);
    int fieldsLength = fields == null ? 0 :fields.length;
    HashMap<String, Integer> map = new HashMap<String, Integer>(fieldsLength);

    for(int i=0; i < fieldsLength; i++) {
      map.put(fields[i], i);
    }
    FIELD_MAP.put(clazz, map);
  }

  @Override
  /**
   * Gets the state manager
   */
  public StateManager getStateManager() {
    return stateManager;
  }

  @Override
  /**
   * Gets fields using a specific class
   */
  public String[] getFields() {
    return FIELDS.get(getClass());
  }

  @Override
  /**
   * Gets a specific field from the fields map
   */
  public String getField(int index) {
    return FIELDS.get(getClass())[index];
  }

  @Override
  /**
   * Gets a field index based on the field name
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

  @Override
  /**
   * Determines if a class is new or not
   */
  public boolean isNew() {
    return getStateManager().isNew(this);
  }

  @Override
  /**
   * Sets this element as a new one inside the stateManager object
   */
  public void setNew() {
    getStateManager().setNew(this);
  }

  @Override
  /**
   * Clears a new object from the stateManager
   */
  public void clearNew() {
    getStateManager().clearNew(this);
  }

  @Override
  /**
   * Determines if an object has been modified or not
   */
  public boolean isDirty() {
    return getStateManager().isDirty(this);
  }

  @Override
  /**
   * Determines if an object has been modified or not
   * based on its field index
   */
  public boolean isDirty(int fieldIndex) {
    return getStateManager().isDirty(this, fieldIndex);
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
   * Sets this class as dirty
   */
  public void setDirty() {
    getStateManager().setDirty(this);
  }

  @Override
  /**
   * Sets a specific field as dirty using its index
   */
  public void setDirty(int fieldIndex) {
    getStateManager().setDirty(this, fieldIndex);
  }

  @Override
  /**
   * Sets a specific field as dirty using its name
   */
  public void setDirty(String field) {
    setDirty(getFieldIndex(field));
  }

  @Override
  /**
   * Clears dirty fields using its index
   */
  public void clearDirty(int fieldIndex) {
    getStateManager().clearDirty(this, fieldIndex);
  }

  @Override
  /**
   * Clears dirty fields using its name
   */
  public void clearDirty(String field) {
    clearDirty(getFieldIndex(field));
  }

  @Override
  /**
   * Clears dirty fields from the state manager
   */
  public void clearDirty() {
    getStateManager().clearDirty(this);
  }

  @Override
  /**
   * Checks if a field is readable using its index
   */
  public boolean isReadable(int fieldIndex) {
    return getStateManager().isReadable(this, fieldIndex);
  }

  @Override
  /**
   * Checks if a field is readable using its name
   */
  public boolean isReadable(String field) {
    return isReadable(getFieldIndex(field));
  }

  @Override
  /**
   * Sets a field as readable using its index
   */
  public void setReadable(int fieldIndex) {
    getStateManager().setReadable(this, fieldIndex);
  }

  @Override
  /**
   * Sets a field as readable using its name
   */
  public void setReadable(String field) {
    setReadable(getFieldIndex(field));
  }

  @Override
  /**
   * Clears this readable object from the state manager
   */
  public void clearReadable() {
    getStateManager().clearReadable(this);
  }

  @Override
  /**
   * Clears a readable object based on its field index
   * using a stateManager object
   */
  public void clearReadable(int fieldIndex) {
    getStateManager().clearReadable(this, fieldIndex);
  }

  @Override
  /**
   * Clears a readable object based on its field name
   * using a stateManager object
   */
  public void clearReadable(String field) {
    clearReadable(getFieldIndex(field));
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
  // TODO
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
   * @param index
   * @param value
   * @return
   */
  protected boolean isFieldEqual(int index, Object value) {
    // TODO
	  return true;
  }
}
