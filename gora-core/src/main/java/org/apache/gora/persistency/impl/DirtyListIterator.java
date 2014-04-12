package org.apache.gora.persistency.impl;

import java.util.ListIterator;

/**
 * Sets the dirty flag if the list iterator's modification methods (remove,
 * set, add) are called.
 */
final class DirtyListIterator<T> implements ListIterator<T> {

  private final ListIterator<T> iteratorDelegate;
  private final DirtyFlag dirtyFlag;

  DirtyListIterator(ListIterator<T> delegate, DirtyFlag dirtyFlag) {
    this.iteratorDelegate = delegate;
    this.dirtyFlag = dirtyFlag;
  }

  @Override
  public boolean hasNext() {
    return iteratorDelegate.hasNext();
  }

  @Override
  public T next() {
    return iteratorDelegate.next();
  }

  @Override
  public boolean hasPrevious() {
    return iteratorDelegate.hasPrevious();
  }

  @Override
  public T previous() {
    return iteratorDelegate.previous();
  }

  @Override
  public int nextIndex() {
    return iteratorDelegate.nextIndex();
  }

  @Override
  public int previousIndex() {
    return iteratorDelegate.previousIndex();
  }

  @Override
  public void remove() {
    dirtyFlag.makeDirty(true);
    iteratorDelegate.remove();
  }

  @Override
  public void set(T e) {
    dirtyFlag.makeDirty(true);
    iteratorDelegate.set(e);
  }

  @Override
  public void add(T e) {
    dirtyFlag.makeDirty(true);
    iteratorDelegate.add(e);
  }

}