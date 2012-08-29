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

package org.apache.gora.persistency.ws.impl;

import java.lang.reflect.Constructor;

import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.util.ReflectionUtils;

/**
 * A default implementation of the {@link BeanFactory} interface. Constructs 
 * the keys using by reflection, {@link Persistent} objects by calling 
 * {@link Persistent#newInstance(org.apache.gora.persistency.StateManager)}. 
 */
public class BeanFactoryWSImpl<K, T extends Persistent> implements BeanFactory<K, T> {

  /**
   * Class of the key to be used
   */
  private Class<K> keyClass;
  
  /**
   * Class of the persistent objects to be stored
   */
  private Class<T> persistentClass;
  
  /**
   * Constructor of the key
   */
  private Constructor<K> keyConstructor;
  
  /**
   * Object's key
   */
  private K key;
  
  /**
   * Persistent object of class T
   */
  private T persistent;
  
  /**
   * Flag to be used to determine if a key is persistent or not
   */
  private boolean isKeyPersistent = false;
  
  /**
   * Constructor
   * @param keyClass
   * @param persistentClass
   */
  public BeanFactoryWSImpl(Class<K> keyClass, Class<T> persistentClass) {
    this.keyClass = keyClass;
    this.persistentClass = persistentClass;
    
    try {
      if(ReflectionUtils.hasConstructor(keyClass)) {
        this.keyConstructor = ReflectionUtils.getConstructor(keyClass);
        this.key = keyConstructor.newInstance(ReflectionUtils.EMPTY_OBJECT_ARRAY);
      }
      this.persistent = ReflectionUtils.newInstance(persistentClass);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    
    isKeyPersistent = Persistent.class.isAssignableFrom(keyClass);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  /**
   * Creates a new key
   */
  public K newKey() throws Exception {
    // TODO this method should be checked to see how object states will be managed
    if(isKeyPersistent)
      return (K)((Persistent)key).newInstance(new StateManagerWSImpl());
    else if(keyConstructor == null) {
      throw new RuntimeException("Key class does not have a no-arg constructor");
    }
    else
      return keyConstructor.newInstance(ReflectionUtils.EMPTY_OBJECT_ARRAY);
  }
 
  @SuppressWarnings("unchecked")
  @Override
  /**
   * Creates a new persistent object
   */
  public T newPersistent() {
    return (T) persistent.newInstance(new StateManagerWSImpl());
  }
  
  @Override
  /**
   * Gets a cached key
   */
  public K getCachedKey() {
    return key;
  }
  
  @Override
  /**
   * Gets a cached persistent object
   */
  public T getCachedPersistent() {
    return persistent;
  }
  
  @Override
  /**
   * Gets the key class
   */
  public Class<K> getKeyClass() {
    return keyClass;
  }
  
  @Override
  /**
   * Gets the persistent object class
   */
  public Class<T> getPersistentClass() {
    return persistentClass;
  }
  
  /**
   * Returns if a key is an object of a persistent class
   * @return True if it is or false if it is not
   */
  public boolean isKeyPersistent() {
    return isKeyPersistent;
  }
}
