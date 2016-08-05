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
import java.util.Map;
import java.util.Set;

import org.apache.gora.persistency.Dirtyable;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class DirtyMapWrapper<K, V> implements Map<K, V>, Dirtyable {

  public static class DirtyEntryWrapper<K, V> implements Entry<K, V>, Dirtyable {
    private final Entry<K, V> entryDelegate;
    private DirtyFlag dirtyFlag;

    public DirtyEntryWrapper(Entry<K, V> delegate, DirtyFlag dirtyFlag) {
      this.entryDelegate = delegate;
      this.dirtyFlag = dirtyFlag;
    }

    @Override
    public K getKey() {
      return entryDelegate.getKey();
    }

    @Override
    public V getValue() {
      return entryDelegate.getValue();
    }

    @Override
    public V setValue(V value) {
      dirtyFlag.makeDirty(valueChanged(value, entryDelegate.getValue()));
      return entryDelegate.setValue(value);
    }

    @Override
    public boolean equals(Object o) {
      return entryDelegate.equals(o);
    }

    @Override
    public int hashCode() {
      return entryDelegate.hashCode();
    }

    @Override
    public boolean isDirty() {
      return dirtyFlag.isDirty() || (entryDelegate instanceof Dirtyable) ? ((Dirtyable) entryDelegate
          .getValue()).isDirty() : false;
    }

    @Override
    public void clearDirty() {
      dirtyFlag.clearDirty();
    }

  }

  private final Map<K, V> delegate;

  private final DirtyFlag dirtyFlag;

  public DirtyMapWrapper(Map<K, V> delegate) {
    this(delegate, new DirtyFlag());
  }

  DirtyMapWrapper(Map<K, V> delegate, DirtyFlag dirtyFlag) {
    this.dirtyFlag = dirtyFlag;
    this.delegate = delegate;
  }

  @Override
  public boolean isDirty() {
    boolean anyDirty = false;
    for (V v : this.values()) {
      anyDirty = anyDirty || (v instanceof Dirtyable) ? ((Dirtyable) v)
          .isDirty() : false;
    }
    return anyDirty || dirtyFlag.isDirty();
  }

  @Override
  public void clearDirty() {
    for (V v : this.values()) {
      if (v instanceof Dirtyable)
        ((Dirtyable) v).clearDirty();
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
  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return delegate.get(key);
  }

  @Override
  public V put(K key, V value) {
    checkPutWillMakeDirty(key, value);
    return delegate.put(key, value);
  }

  private void checkPutWillMakeDirty(K key, V value) {
    if (containsKey(key)) {
      dirtyFlag.makeDirty(valueChanged(value, get(key)));
    } else {
      dirtyFlag.makeDirty(true);
    }
  }

  private static <V> boolean valueChanged(V value, V oldValue) {
    return (value == null && oldValue != null)
        || (value != null && !value.equals(oldValue));
  }

  @Override
  public V remove(Object key) {
    dirtyFlag.makeDirty(containsKey(key));
    return delegate.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      checkPutWillMakeDirty(entry.getKey(), entry.getValue());
    }
    delegate.putAll(m);
  }

  @Override
  public void clear() {
    if (delegate.size() != 0) {
      dirtyFlag.makeDirty(true);
    }
    delegate.clear();
  }

  @Override
  public Set<K> keySet() {
    return delegate.keySet();
  }

  @Override
  public Collection<V> values() {
    return new DirtyCollectionWrapper<>(delegate.values(), dirtyFlag);
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    Collection<DirtyEntryWrapper<K, V>> dirtyEntrySet = Collections2.transform(
        delegate.entrySet(),
        new Function<Entry<K, V>, DirtyEntryWrapper<K, V>>() {
          @Override
          public DirtyEntryWrapper<K, V> apply(java.util.Map.Entry<K, V> input) {
            return new DirtyEntryWrapper<>(input, dirtyFlag);
          }
        });
    return new DirtySetWrapper(dirtyEntrySet, dirtyFlag);
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

}
