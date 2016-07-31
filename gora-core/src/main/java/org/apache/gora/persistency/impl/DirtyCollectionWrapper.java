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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.gora.persistency.Dirtyable;

/**
 * A {@link List} implementation that wraps another list, intercepting
 * modifications to the list structure and reporting on weather or not the list
 * has been modified, and also checking list elements for modification.
 * 
 * @param <T>
 *          The type of the list that this wrapper wraps.
 */
public class DirtyCollectionWrapper<T> implements Dirtyable,
        Collection<T> {

  /** The delegate list that the wrapper wraps */
  private final Collection<T> delegate;
  /**
   * The dirty flag, tracks if the structure of the underlying list has been
   * modified
   */
  private DirtyFlag dirtyFlag;

  DirtyCollectionWrapper(Collection<T> delegate2, DirtyFlag dirtyFlag) {
    this.delegate = delegate2;
    this.dirtyFlag = dirtyFlag;
  }

  @Override
  public boolean isDirty() {
    boolean anyDirty = false;
    for (T value : this) {
      anyDirty = anyDirty || (value instanceof Dirtyable) ? ((Dirtyable) value).isDirty():false;
    }
    return anyDirty || dirtyFlag.isDirty();
  }

  @Override
  public void clearDirty() {
    for (T value : this) {
      if (value instanceof Dirtyable)
        ((Dirtyable) value).clearDirty();
    }
    dirtyFlag.clearDirty();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return new DirtyIteratorWrapper<>(delegate.iterator(), dirtyFlag);
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <R> R[] toArray(R[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(T e) {
    boolean change = delegate.add(e);
    dirtyFlag.makeDirty(change);
    return change;
  }

  @Override
  public boolean remove(Object o) {
    boolean change = delegate.remove(o);
    dirtyFlag.makeDirty(change);
    return change;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    boolean change = delegate.addAll(c);
    dirtyFlag.makeDirty(change);
    return change;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean change = delegate.removeAll(c);
    dirtyFlag.makeDirty(change);
    return change;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    boolean change = delegate.retainAll(c);
    dirtyFlag.makeDirty(change);
    return change;
  }

  @Override
  public void clear() {
    dirtyFlag.makeDirty(size() > 0);
    delegate.clear();
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  protected Collection<T> getDelegate() {
    return delegate;
  }

  protected DirtyFlag getDirtyFlag() {
    return dirtyFlag;
  }

}
