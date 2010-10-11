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

import java.util.BitSet;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;

/**
 * An implementation for the StateManager. This implementation assumes 
 * every Persistent object has it's own StateManager.
 */
public class StateManagerImpl implements StateManager {

  //TODO: serialize isNew in PersistentSerializer 
  protected boolean isNew;
  protected BitSet dirtyBits;
  protected BitSet readableBits;

  public StateManagerImpl() {
  }

  public void setManagedPersistent(Persistent persistent) {
    dirtyBits = new BitSet(persistent.getSchema().getFields().size());
    readableBits = new BitSet(persistent.getSchema().getFields().size());
    isNew = true;
  }

  @Override
  public boolean isNew(Persistent persistent) {
    return isNew;
  }
  
  @Override
  public void setNew(Persistent persistent) {
    this.isNew = true;
  }
  
  @Override
  public void clearNew(Persistent persistent) {
    this.isNew = false;
  }
  
  public void setDirty(Persistent persistent, int fieldIndex) {
    dirtyBits.set(fieldIndex);
    readableBits.set(fieldIndex);
  }
  
  public boolean isDirty(Persistent persistent, int fieldIndex) {
    return dirtyBits.get(fieldIndex);
  }

  public boolean isDirty(Persistent persistent) {
    return !dirtyBits.isEmpty();
  }
  
  @Override
  public void setDirty(Persistent persistent) {
    dirtyBits.set(0, dirtyBits.size());
  }
  
  @Override
  public void clearDirty(Persistent persistent, int fieldIndex) {
    dirtyBits.clear(fieldIndex);
  }
  
  public void clearDirty(Persistent persistent) {
    dirtyBits.clear();
  }
  
  public void setReadable(Persistent persistent, int fieldIndex) {
    readableBits.set(fieldIndex);
  }

  public boolean isReadable(Persistent persistent, int fieldIndex) {
    return readableBits.get(fieldIndex);
  }

  @Override
  public void clearReadable(Persistent persistent, int fieldIndex) {
    readableBits.clear(fieldIndex);
  }
  
  public void clearReadable(Persistent persistent) {
    readableBits.clear();
  }
}
