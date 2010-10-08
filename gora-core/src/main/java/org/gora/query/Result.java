
package org.gora.query;

import java.io.Closeable;
import java.io.IOException;

import org.gora.persistency.Persistent;
import org.gora.store.DataStore;

/**
 * A result to a {@link Query}. Objects in the result set can be 
 * iterated by calling {@link #next()}, {@link #get()} 
 * and {@link #getKey()}. 
 */
public interface Result<K,T extends Persistent> extends Closeable {

  /**
   * Returns the DataStore, that this Result is associated with.
   * @return the DataStore of the Result
   */
  public abstract DataStore<K,T> getDataStore();
  
  /**
   * Returns the Query object for this Result.
   * @return the Query object for this Result.
   */
  public abstract Query<K, T> getQuery();
  
  /**
   * Advances to the next element and returns false if at end.
   * @return true if end is not reached yet
   */
  public abstract boolean next() throws IOException;
  
  /**
   * Returns the current key.
   * @return current key
   */
  public abstract K getKey();
  
  /**
   * Returns the current object.
   * @return current object
   */
  public abstract T get();
  
  /**
   * Returns the class of the keys
   * @return class of the keys
   */
  public abstract Class<K> getKeyClass();
    
  /**
   * Returns the class of the persistent objects
   * @return class of the persistent objects
   */
  public abstract Class<T> getPersistentClass();
  
  /**
   * Returns the number of times next() is called with return value true.
   * @return the number of results so far
   */
  public long getOffset();
  
  /**
   * Returns how far along the result has iterated, a value between 0 and 1.
   */
  public float getProgress() throws IOException;
  
  @Override
  public abstract void close() throws IOException;
  
}
