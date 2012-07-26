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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class StatefulHashMap<K, V> extends HashMap<K, V> 
  implements StatefulMap<K, V> {
  
  /* This is probably a terrible design but I do not yet have a better
   * idea of managing write/delete info on a per-key basis
   */
  private Map<K, State> keyStates = new HashMap<K, State>();

  /**
   * Create an empty instance.
   */
  public StatefulHashMap() {
    this(null);
  }

  /**
   * Create an instance with initial entries. These entries are added stateless;
   * in other words the statemap will be clear after the construction.
   * 
   * @param m The map with initial entries.
   */
  public StatefulHashMap(Map<K, V> m) {
    super();
    if (m == null) {
      return;
    }
    for (java.util.Map.Entry<K, V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
    clearStates();
  }
  
  @Override
  public V put(K key, V value) {
    keyStates.remove(key);
    V old = super.put(key, value);
    //if old value is different or null, set state to dirty
    if (!value.equals(old)) {
      keyStates.put(key, State.DIRTY);
    }
    return old;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V remove(Object key) {
    keyStates.put((K) key, State.DELETED);
    return null;
    // We do not remove the actual entry from the map.
    // When we keep the entries, we can compare previous state to make Datastore
    // puts more efficient. (In the case of new puts that are in fact unchanged)
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void clear() {
    // The problem with clear() is that we cannot delete entries that were not
    // initially set on the input.  This means that for a clear() to fully
    // reflect on a datastore you have to input the full map from the store.
    // This is acceptable for now. Another way around this is to implement
    // some sort of "clear marker" that indicates a map should be fully cleared,
    // with respect to any possible new entries.
    for (Entry<K, V> e : entrySet()) {
      keyStates.put(e.getKey(), State.DELETED);
    }
    // Do not actually clear the map, i.e. with super.clear()
    // When we keep the entries, we can compare previous state to make Datastore
    // puts more efficient. (In the case of new puts that are in fact unchanged)
  }

  public State getState(K key) {
    return keyStates.get(key);
  };
  
  /* (non-Javadoc)
   * @see org.apache.gora.persistency.StatefulMap#resetStates()
   */
  public void clearStates() {
    keyStates.clear();
  }

  /* (non-Javadoc)
   * @see org.apache.gora.persistency.StatefulMap#putState(K, org.apache.gora.persistency.State)
   */
  public void putState(K key, State state) {
    keyStates.put(key, state);
  }

  /* (non-Javadoc)
   * @see org.apache.gora.persistency.StatefulMap#states()
   */
  public Map<K, State> states() {
    return keyStates;
  }

  /* (non-Javadoc)
   * @see org.apache.gora.persistency.StatefulMap#reuse()
   */
  public void reuse() {
    super.clear();
    clearStates();
  }
}
