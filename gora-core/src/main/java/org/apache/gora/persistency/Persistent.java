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
package org.apache.gora.persistency;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
* Objects that are persisted by Gora implements this interface.
*/
public interface Persistent extends Dirtyable, Cloneable {

  public static String DIRTY_BYTES_FIELD_NAME = "__g__dirty";

  /**
* Clears the inner state of the object without any modification to the actual
* data on the data store. This method should be called before re-using the
* object to hold the data for another result.
*/
  void clear();

  /**
* Returns whether the field has been modified.
*
* @param fieldIndex
* the offset of the field in the object
* @return whether the field has been modified.
*/
  boolean isDirty(int fieldIndex);

  /**
* Returns whether the field has been modified.
*
* @param field
* the name of the field
* @return whether the field has been modified.
*/
  boolean isDirty(String field);

  /**
* Sets all the fields of the object as dirty.
*/
  void setDirty();

  /**
* Sets the field as dirty.
*
* @param fieldIndex
* the offset of the field in the object
*/
  @JsonIgnore
  void setDirty(int fieldIndex);

  /**
* Sets the field as dirty.
*
* @param field
* the name of the field
*/
  @JsonIgnore
  void setDirty(String field);

  /**
* Clears the field as dirty.
*
* @param fieldIndex
* the offset of the field in the object
*/
  void clearDirty(int fieldIndex);

  /**
* Clears the field as dirty.
*
* @param field
* the name of the field
*/
  void clearDirty(String field);

  /**
* Get an object which can be used to mark this field as deleted (rather than
* state unknown, which is indicated by null).
*
* @return a tombstone.
*/
  public abstract Tombstone getTombstone();

  /**
* Get a list of fields from this persistent object's schema that are not
* managed by Gora.
*
* @return the unmanaged fields
*/
  public List<Field> getUnmanagedFields();

  /**
   * Constructs a new instance of the object by using appropriate builder. This
   * method is intended to be used by Gora framework.
   * 
   * @return a new instance of the object
   */
  Persistent newInstance();
  
  /**
   * Returns the avro's data schema
   * @return the parsed schema definition
   */
  public Schema getSchema();
  
  /**
   * Returns a deep copy of a Persistent instance. Each generated Persistent's subclass implements the clone with a builder.
   * @return A deep copy of a Persistent instance.
   * @throws CloneNotSupportedException which indicates that the clone method in 
   * class Object has been called to clone an object, but that the object's 
   * class does not implement the Cloneable interface.
   */
  public Persistent clone() throws CloneNotSupportedException;
  
}