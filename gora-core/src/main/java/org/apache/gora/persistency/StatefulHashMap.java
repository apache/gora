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

  public StatefulHashMap() {
    this(null);
  }

  public StatefulHashMap(Map<K, V> m) {
    super();
    if (m == null) {
      return;
    }
    super.putAll(m);
  }
  
  @Override
  public V put(K key, V value) {
    keyStates.put(key, State.DIRTY);
    return super.put(key, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public V remove(Object key) {
    if (keyStates.containsKey(key)) {
      keyStates.put((K) key, State.DELETED);
    }
    return super.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void clear() {
    for (Entry<K, V> e : entrySet()) {
      keyStates.put(e.getKey(), State.DELETED);
    }
    super.clear();
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
}
