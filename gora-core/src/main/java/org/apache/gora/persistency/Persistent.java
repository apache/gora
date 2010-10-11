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
package org.apache.gora.persistency;

import org.apache.avro.specific.SpecificRecord;

/**
 * Objects that are persisted by Gora implements this interface.
 */
public interface Persistent extends SpecificRecord, Cloneable {

  /**
   * Returns the StateManager which manages the persistent 
   * state of the object.
   * @return the StateManager of the object
   */
  public StateManager getStateManager();

  /**
   * Constructs a new instance of the object with the given StateManager.
   * This method is intended to be used by Gora framework.
   * @param stateManager the StateManager to manage the persistent state 
   * of the object
   * @return a new instance of the object
   */
  public Persistent newInstance(StateManager stateManager);

  /**
   * Returns sorted field names of the object
   * @return the field names of the object as a String[]
   */
  public String[] getFields();
  
  /**
   * Returns the field name with the given index
   * @param index the index of the field  
   * @return the name of the field
   */
  public String getField(int index);
  
  /**
   * Returns the index of the field with the given name
   * @param field the name of the field
   * @return the index of the field
   */
  public int getFieldIndex(String field);
  
  /**
   * Clears the inner state of the object without any modification
   * to the actual data on the data store. This method should be called 
   * before re-using the object to hold the data for another result.  
   */
  public void clear();
  
  /**
   * Returns whether the object is newly constructed.
   * @return true if the object is newly constructed, false if
   * retrieved from a datastore. 
   */
  public boolean isNew();
  
  /**
   * Sets the state of the object as new for persistency
   */
  public void setNew();
  
  /**
   * Clears the new state 
   */
  public void clearNew();
  
  /**
   * Returns whether any of the fields of the object has been modified 
   * after construction or loading. 
   * @return whether any of the fields of the object has changed
   */
  public boolean isDirty();
  
  /**
   * Returns whether the field has been modified.
   * @param fieldIndex the offset of the field in the object
   * @return whether the field has been modified.
   */
  public boolean isDirty(int fieldIndex);

  /**
   * Returns whether the field has been modified.
   * @param field the name of the field
   * @return whether the field has been modified.
   */
  public boolean isDirty(String field);
  
  /**
   * Sets all the fields of the object as dirty.
   */
  public void setDirty();
  
  /**
   * Sets the field as dirty.
   * @param fieldIndex the offset of the field in the object
   */
  public void setDirty(int fieldIndex);
 
  /**
   * Sets the field as dirty.
   * @param field the name of the field
   */
  public void setDirty(String field);
  
  /**
   * Clears the field as dirty.
   * @param fieldIndex the offset of the field in the object
   */
  public void clearDirty(int fieldIndex);
  
  /**
   * Clears the field as dirty.
   * @param field the name of the field
   */
  public void clearDirty(String field);
  
  /**
   * Clears the dirty state.
   */
  public void clearDirty();
  
  /**
   * Returns whether the field has been loaded from the datastore. 
   * @param fieldIndex the offset of the field in the object
   * @return whether the field has been loaded 
   */
  public boolean isReadable(int fieldIndex);

  /**
   * Returns whether the field has been loaded from the datastore. 
   * @param field the name of the field
   * @return whether the field has been loaded 
   */
  public boolean isReadable(String field);
  
  /**
   * Sets the field as readable.
   * @param fieldIndex the offset of the field in the object
   */
  public void setReadable(int fieldIndex);

  /**
   * Sets the field as readable.
   * @param field the name of the field
   */
  public void setReadable(String field);

  /**
   * Clears the field as readable.
   * @param fieldIndex the offset of the field in the object
   */
  public void clearReadable(int fieldIndex);
  
  /**
   * Sets the field as readable.
   * @param field the name of the field
   */
  public void clearReadable(String field);
  
  /**
   * Clears the readable state.
   */
  public void clearReadable();
  
  public Persistent clone();
}
