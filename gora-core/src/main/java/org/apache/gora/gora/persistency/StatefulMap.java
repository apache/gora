
package org.gora.persistency;

import java.util.Map;

/**
 * StatefulMap extends the Map interface to keep track of the 
 * persistency states of individual elements in the Map.  
 */
public interface StatefulMap<K, V> extends Map<K, V> {

  public abstract State getState(K key);
  
  public abstract void putState(K key, State state);

  public abstract Map<K, State> states();

  public abstract void clearStates();
  
}