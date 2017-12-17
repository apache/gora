/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.gora.persistency.impl;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.gora.persistency.Dirtyable;
import org.apache.gora.persistency.Persistent;

/**
* Base classs implementing common functionality for Persistent classes.
*/
public abstract class PersistentBase extends SpecificRecordBase implements
    Persistent, java.io.Externalizable {

  /** Bytes used to represent weather or not a field is dirty. */
  private java.nio.ByteBuffer __g__dirty;

  public PersistentBase() {
    __g__dirty = java.nio.ByteBuffer.wrap(new byte[getFieldsCount()]);
  }

  public abstract int getFieldsCount();

  public static class PersistentData extends SpecificData {
    private static final PersistentData INSTANCE = new PersistentData();

    public static PersistentData get() {
      return INSTANCE;
    }

    public boolean equals(SpecificRecord obj1, SpecificRecord that) {
      if (that == obj1)
        return true; // identical object
      if (!(that instanceof SpecificRecord))
        return false; // not a record
      if (obj1.getClass() != that.getClass())
        return false; // not same schema
      return PersistentData.get().compare(obj1, that, obj1.getSchema(), true) == 0;
    }

  }

  @Override
  public void clearDirty() {
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    for (int i = 0; i < dirtyBytes.limit(); i++) {
      dirtyBytes.put(i, (byte) 0);
    }
    for (Field field : getSchema().getFields()) {
      clearDirynessIfFieldIsDirtyable(field.pos());
    }
  }

  private void clearDirynessIfFieldIsDirtyable(int fieldIndex) {
    Object value = get(fieldIndex);
    if (value instanceof Dirtyable) {
      ((Dirtyable) value).clearDirty();
    }
  }

  @Override
  public void clearDirty(int fieldIndex) {
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    int byteOffset = fieldIndex / 8;
    int bitOffset = fieldIndex % 8;
    byte currentByte = dirtyBytes.get(byteOffset);
    currentByte = (byte) ((~(1 << bitOffset)) & currentByte);
    dirtyBytes.put(byteOffset, currentByte);
    clearDirynessIfFieldIsDirtyable(fieldIndex);
  }

  @Override
  public void clearDirty(String field) {
    clearDirty(getSchema().getField(field).pos());
  }

  @Override
  public boolean isDirty() {
    List<Field> fields = getSchema().getFields();
    boolean isSubRecordDirty = false;
    for (Field field : fields) {
      isSubRecordDirty = isSubRecordDirty || checkIfMutableFieldAndDirty(field);
    }
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    boolean dirty = false;
    for (int i = 0; i < dirtyBytes.limit(); i++) {
      dirty = dirty || dirtyBytes.get(i) != 0;
    }
    return isSubRecordDirty || dirty;
  }

  private boolean checkIfMutableFieldAndDirty(Field field) {
    switch (field.schema().getType()) {
    case RECORD:
    case MAP:
    case ARRAY:
      Object value = get(field.pos());
      return !(value instanceof Dirtyable) || value==null ? false : ((Dirtyable) value).isDirty();
    case UNION:
      value = get(field.pos());
      return !(value instanceof Dirtyable) || value==null ? false : ((Dirtyable) value).isDirty();
    default:
      break;
    }
    return false;
  }

  @Override
  public boolean isDirty(int fieldIndex) {
    Field field = getSchema().getFields().get(fieldIndex);
    boolean isSubRecordDirty = checkIfMutableFieldAndDirty(field);
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    int byteOffset = fieldIndex / 8;
    int bitOffset = fieldIndex % 8;
    byte currentByte = dirtyBytes.get(byteOffset);
    return isSubRecordDirty || 0 != ((1 << bitOffset) & currentByte);
  }

  @Override
  public boolean isDirty(String fieldName) {
    Field field = getSchema().getField(fieldName);
    if(field == null){
      throw new IndexOutOfBoundsException
      ("Field "+ fieldName + " does not exist in this schema.");
    }
    return isDirty(field.pos());
  }

  @Override
  public void setDirty() {
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    for (int i = 0; i < dirtyBytes.limit(); i++) {
      dirtyBytes.put(i, (byte) -128);
    }
  }

  @Override
  public void setDirty(int fieldIndex) {
    ByteBuffer dirtyBytes = getDirtyBytes();
    assert (dirtyBytes.position() == 0);
    int byteOffset = fieldIndex / 8;
    int bitOffset = fieldIndex % 8;
    byte currentByte = dirtyBytes.get(byteOffset);
    currentByte = (byte) ((1 << bitOffset) | currentByte);
    dirtyBytes.put(byteOffset, currentByte);
  }

  @Override
  public void setDirty(String field) {
    setDirty(getSchema().getField(field).pos());
  }

  /**
   * Exposing dirty bytes over public method. Purpose is to preserve dirty bytes content
   * while transporting AVRO data beans over TCP wire in serialized form.
   * Since {@link org.apache.gora.persistency.impl.PersistentBase} implements {@link java.io.Externalizable},
   * this method can be used to retrieve the dirty bytes as {@link java.nio.ByteBuffer} and and get the content
   * as bytes[] and write byte stream to the TCP wire.
   * See {@link java.io.Externalizable#writeExternal(ObjectOutput)} abstract method implementation
   * on velocity template record.vm.
   * <p>
   * Note {@link java.nio.ByteBuffer} is not itself not in serializable form.
   *
   * @return __g__dirty dirty bytes
   */
  public ByteBuffer getDirtyBytes() {
    return __g__dirty;
  }

  /**
   * Setter method for assign dirty bytes when deserializing AVRO bean from dirty bytes
   * preserved in serialized bytes form.
   * Since {@link org.apache.gora.persistency.impl.PersistentBase} implements {@link java.io.Externalizable}
   * and when actual deserialization happens for {@link org.apache.gora.persistency.impl.PersistentBase}
   * new instance, acquire byte stream from TCP wire, extracting specific byte[] from byte stream
   * and create {@link java.nio.ByteBuffer} instance and set using this public method.
   * See {@link java.io.Externalizable#readExternal(ObjectInput)} abstract method implementation
   * on velocity template record.vm.
   * <p>
   * Note {@link java.io.Externalizable} extending means it is mandatory to have default public constructor.
   *
   * @param __g__dirty dirty bytes
   */
  public void setDirtyBytes(ByteBuffer __g__dirty) {
    this.__g__dirty = __g__dirty;
  }

  @Override
  public void clear() {
    Collection<Field> unmanagedFields = getUnmanagedFields();
    for (Field field : getSchema().getFields()) {
      if (!unmanagedFields.contains(field))
        continue;
      put(field.pos(), PersistentData.get().deepCopy(field.schema(), PersistentData.get().getDefaultValue(field)));
    }
    clearDirty();
  }

  @Override
  public boolean equals(Object that) {
    if (that == this) {
      return true;
    } else if (that instanceof Persistent) {
      return PersistentData.get().equals(this, (SpecificRecord) that);
    } else {
      return false;
    }
  }
  
  public List<Field> getUnmanagedFields(){
    List<Field> fields = getSchema().getFields();
    //return fields.subList(1, fields.size());
    return fields;
  }
  
}
