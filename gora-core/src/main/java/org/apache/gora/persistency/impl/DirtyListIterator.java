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