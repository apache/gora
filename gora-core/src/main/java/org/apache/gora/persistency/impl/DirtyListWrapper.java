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
import java.util.List;
import java.util.ListIterator;

import org.apache.gora.persistency.Dirtyable;

/**
 * A {@link List} implementation that wraps another list, intercepting
 * modifications to the list structure and reporting on weather or not the list
 * has been modified, and also checking list elements for modification.
 * 
 * @param <T>
 *          The type of the list that this wrapper wraps.
 */
public class DirtyListWrapper<T> extends DirtyCollectionWrapper<T> implements
    Dirtyable, List<T> {

  /**
   * Create a DirtyListWrapper that wraps a getDelegate().
   * 
   * @param delegate
   *          The getDelegate().to wrap.
   */
  public DirtyListWrapper(List<T> delegate) {
    this(delegate, new DirtyFlag());
  }

  DirtyListWrapper(List<T> delegate, DirtyFlag dirtyFlag) {
    super(delegate, dirtyFlag);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    boolean change = getDelegate().addAll(index, c);
    getDirtyFlag().makeDirty(change);
    return change;
  }

  @Override
  public T get(int index) {
    return getDelegate().get(index);
  }

  @Override
  public T set(int index, T element) {
    getDirtyFlag().makeDirty(true);
    return getDelegate().set(index, element);
  }

  @Override
  public void add(int index, T element) {
    getDirtyFlag().makeDirty(true);
    getDelegate().add(index, element);
  }

  @Override
  public T remove(int index) {
    getDirtyFlag().makeDirty(true);
    return getDelegate().remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return getDelegate().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return getDelegate().lastIndexOf(o);
  }

  @Override
  public ListIterator<T> listIterator() {
    return new DirtyListIterator<>(getDelegate().listIterator(),
        getDirtyFlag());
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return new DirtyListIterator<>(getDelegate().listIterator(index),
        getDirtyFlag());
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return new DirtyListWrapper<>(getDelegate().subList(fromIndex, toIndex),
        getDirtyFlag());
  }

  @Override
  protected List<T> getDelegate() {
    return (List<T>) super.getDelegate();
  }

}
