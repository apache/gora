
package org.gora.persistency.impl;

import java.util.BitSet;

import org.gora.persistency.Persistent;
import org.gora.persistency.StateManager;

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
