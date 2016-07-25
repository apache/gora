package org.apache.gora.persistency.impl;

import java.util.Iterator;

/**
 * Sets the dirty flag if the iterator's remove method is called.
 */
final class DirtyIteratorWrapper<T> implements Iterator<T>, java.io.Serializable {

  private final DirtyFlag dirtyFlag;
  private Iterator<T> delegateIterator;

  DirtyIteratorWrapper(Iterator<T> delegateIterator,
      DirtyFlag dirtyFlag) {
    this.delegateIterator = delegateIterator;
    this.dirtyFlag = dirtyFlag;
  }

  @Override
  public boolean hasNext() {
    return delegateIterator.hasNext();
  }

  @Override
  public T next() {
    return delegateIterator.next();
  }

  @Override
  public void remove() {
    dirtyFlag.makeDirty(true);
    delegateIterator.remove();
  }

}