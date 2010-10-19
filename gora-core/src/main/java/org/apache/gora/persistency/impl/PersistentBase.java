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
package org.apache.gora.persistency.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.specific.SpecificRecord;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.persistency.ListGenericArray;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;

/**
 * Base classs implementing common functionality for Persistent
 * classes.
 */
public abstract class PersistentBase implements Persistent {

  protected static Map<Class<?>, Map<String, Integer>> FIELD_MAP =
    new HashMap<Class<?>, Map<String,Integer>>();

  protected static Map<Class<?>, String[]> FIELDS =
    new HashMap<Class<?>, String[]>();

  protected static final PersistentDatumReader<Persistent> datumReader =
    new PersistentDatumReader<Persistent>();
    
  private StateManager stateManager;

  protected PersistentBase() {
    this(new StateManagerImpl());
  }

  protected PersistentBase(StateManager stateManager) {
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
  public StateManager getStateManager() {
    return stateManager;
  }

  @Override
  public String[] getFields() {
    return FIELDS.get(getClass());
  }

  @Override
  public String getField(int index) {
    return FIELDS.get(getClass())[index];
  }

  @Override
  public int getFieldIndex(String field) {
    return FIELD_MAP.get(getClass()).get(field);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void clear() {
    List<Field> fields = getSchema().getFields();

    for(int i=0; i<getFields().length; i++) {
      switch(fields.get(i).schema().getType()) {
        case MAP: if(get(i) != null) ((Map)get(i)).clear(); break;
        case ARRAY:
          if(get(i) != null) {
            if(get(i) instanceof ListGenericArray) {
              ((ListGenericArray)get(i)).clear();
            } else {
              put(i, new ListGenericArray(fields.get(i).schema()));
            }
          }
          break;
        case RECORD :
          Persistent field = ((Persistent)get(i));
          if(field != null) field.clear();
          break;
        case BOOLEAN: put(i, false); break;
        case INT    : put(i, 0); break;
        case DOUBLE : put(i, 0d); break;
        case FLOAT  : put(i, 0f); break;
        case LONG   : put(i, 0l); break;
        case NULL   : break;
        default     : put(i, null); break;
      }
    }
    clearDirty();
    clearReadable();
  }

  @Override
  public boolean isNew() {
    return getStateManager().isNew(this);
  }

  @Override
  public void setNew() {
    getStateManager().setNew(this);
  }

  @Override
  public void clearNew() {
    getStateManager().clearNew(this);
  }

  @Override
  public boolean isDirty() {
    return getStateManager().isDirty(this);
  }

  @Override
  public boolean isDirty(int fieldIndex) {
    return getStateManager().isDirty(this, fieldIndex);
  }

  @Override
  public boolean isDirty(String field) {
    return isDirty(getFieldIndex(field));
  }

  @Override
  public void setDirty() {
    getStateManager().setDirty(this);
  }

  @Override
  public void setDirty(int fieldIndex) {
    getStateManager().setDirty(this, fieldIndex);
  }

  @Override
  public void setDirty(String field) {
    setDirty(getFieldIndex(field));
  }

  @Override
  public void clearDirty(int fieldIndex) {
    getStateManager().clearDirty(this, fieldIndex);
  }

  @Override
  public void clearDirty(String field) {
    clearDirty(getFieldIndex(field));
  }

  @Override
  public void clearDirty() {
    getStateManager().clearDirty(this);
  }

  @Override
  public boolean isReadable(int fieldIndex) {
    return getStateManager().isReadable(this, fieldIndex);
  }

  @Override
  public boolean isReadable(String field) {
    return isReadable(getFieldIndex(field));
  }

  @Override
  public void setReadable(int fieldIndex) {
    getStateManager().setReadable(this, fieldIndex);
  }

  @Override
  public void setReadable(String field) {
    setReadable(getFieldIndex(field));
  }

  @Override
  public void clearReadable() {
    getStateManager().clearReadable(this);
  }

  @Override
  public void clearReadable(int fieldIndex) {
    getStateManager().clearReadable(this, fieldIndex);
  }

  @Override
  public void clearReadable(String field) {
    clearReadable(getFieldIndex(field));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SpecificRecord)) return false;

    SpecificRecord r2 = (SpecificRecord)o;
    if (!this.getSchema().equals(r2.getSchema())) return false;

    return this.hashCode() == r2.hashCode();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    List<Field> fields = this.getSchema().getFields();
    int end = fields.size();
    for (int i = 0; i < end; i++) {
      result = prime * result + getFieldHashCode(i, fields.get(i));
    }
    return result;
  }

  private int getFieldHashCode(int i, Field field) {
    Object o = get(i);
    if(o == null)
      return 0;

    if(field.schema().getType() == Type.BYTES) {
      return getByteBufferHashCode((ByteBuffer)o);
    }

    return o.hashCode();
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
  public Persistent clone() {
    return datumReader.clone(this, getSchema());
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(super.toString());
    builder.append(" {\n");
    List<Field> fields = getSchema().getFields();
    for(int i=0; i<fields.size(); i++) {
      builder.append("  \"").append(fields.get(i).name()).append("\":\"");
      builder.append(get(i)).append("\"\n");
    }
    builder.append("}");
    return builder.toString();
  }
  
  protected boolean isFieldEqual(int index, Object value) {
    Object old = get(index);
    if (old == null && value == null)
      return true;
    if (old == null || value == null)
      return false;
    return value.equals(old);
  }
}
