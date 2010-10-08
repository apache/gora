
package org.gora.persistency;

/**
 * BeanFactory's enable contruction of keys and Persistent objects. 
 */
public interface BeanFactory<K, T extends Persistent> {

  /**
   * Constructs a new instance of the key class
   * @return a new instance of the key class
   */
  public abstract K newKey() throws Exception;

  /**
   * Constructs a new instance of the Persistent class
   * @return a new instance of the Peristent class
   */
  public abstract T newPersistent();

  /**
   * Returns an instance of the key object to be 
   * used to access static fields of the object. Returned object MUST  
   * be treated as read-only. No fields other than the static fields 
   * of the object should be assumed to be readable. 
   * @return a cached instance of the key object
   */
  public abstract K getCachedKey();
  
  /**
   * Returns an instance of the {@link Persistent} object to be 
   * used to access static fields of the object. Returned object MUST  
   * be treated as read-only. No fields other than the static fields 
   * of the object should be assumed to be readable. 
   * @return a cached instance of the Persistent object
   */
  public abstract T getCachedPersistent();

  /**
   * Returns the key class
   * @return the key class
   */
  public abstract Class<K> getKeyClass();

  /**
   * Returns the persistent class
   * @return the persistent class
   */
  public abstract Class<T> getPersistentClass();

}