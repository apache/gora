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

import java.util.BitSet;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;

/**
 * An implementation for the StateManager. This implementation assumes 
 * every Persistent object has it's own StateManager.
 */
public class StateManagerWSImpl implements StateManager {

  //TODO: serialize isNew in PersistentSerializer 
  protected boolean isNew;
  protected BitSet dirtyBits;
  protected BitSet readableBits;

  /**
   * Constructor
   */
  public StateManagerWSImpl() {
  }

  /**
   * Sets dirtyBits and readableBits sizes
   */
  public void setManagedPersistent(Persistent persistent) {
   // dirtyBits = new BitSet(persistent.getSchema().getFields().size());
   // readableBits = new BitSet(persistent.getSchema().getFields().size());
    isNew = true;
  }

  @Override
  /**
   * Checks if an object is new or not
   */
  public boolean isNew(Persistent persistent) {
    return isNew;
  }
  
  @Override
  /**
   * Sets an object as new
   */
  public void setNew(Persistent persistent) {
    this.isNew = true;
  }
  
  @Override
  /**
   * Clear the new object by setting it as not new
   */
  public void clearNew(Persistent persistent) {
    this.isNew = false;
  }
  
  /**
   * Sets an object as dirty using its index
   */
  public void setDirty(Persistent persistent, int fieldIndex) {
    dirtyBits.set(fieldIndex);
    readableBits.set(fieldIndex);
  }
  
  /**
   * Determines if an object is dirty or not based on its index
   */
  public boolean isDirty(Persistent persistent, int fieldIndex) {
    return dirtyBits.get(fieldIndex);
  }

  /**
   * Determines if an object is dirty
   */
  public boolean isDirty(Persistent persistent) {
    return !dirtyBits.isEmpty();
  }
  
  @Override
  /**
   * Sets an object as dirty
   */
  public void setDirty(Persistent persistent) {
    dirtyBits.set(0, dirtyBits.size());
  }
  
  @Override
  /**
   * Marks a persistent object as not dirty using its index
   */
  public void clearDirty(Persistent persistent, int fieldIndex) {
    dirtyBits.clear(fieldIndex);
  }
  
  /**
   * Marks all objects as not dirty
   */
  public void clearDirty(Persistent persistent) {
    dirtyBits.clear();
  }
  
  /**
   * Sets a persistent object as readable using its index
   */
  public void setReadable(Persistent persistent, int fieldIndex) {
    readableBits.set(fieldIndex);
  }

  /**
   * Determines if an object is readable using its index
   */
  public boolean isReadable(Persistent persistent, int fieldIndex) {
    return readableBits.get(fieldIndex);
  }

  @Override
  /**
   * Marks an object as non-readable using its index
   */
  public void clearReadable(Persistent persistent, int fieldIndex) {
    readableBits.clear(fieldIndex);
  }
  
  /**
   * Marks all objects as non-readable
   */
  public void clearReadable(Persistent persistent) {
    readableBits.clear();
  }
}
