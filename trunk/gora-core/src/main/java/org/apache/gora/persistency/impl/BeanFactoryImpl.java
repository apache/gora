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

import java.lang.reflect.Constructor;

import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.util.ReflectionUtils;

/**
 * A default implementation of the {@link BeanFactory} interface. Constructs 
 * the keys using by reflection, {@link Persistent} objects by calling 
 * {@link Persistent#newInstance(org.apache.gora.persistency.StateManager)}. 
 */
public class BeanFactoryImpl<K, T extends Persistent> implements BeanFactory<K, T> {

  private Class<K> keyClass;
  private Class<T> persistentClass;
  
  private Constructor<K> keyConstructor;
  
  private K key;
  private T persistent;
  
  private boolean isKeyPersistent = false;
  
  public BeanFactoryImpl(Class<K> keyClass, Class<T> persistentClass) {
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
  public K newKey() throws Exception {
    if(isKeyPersistent)
      return (K)((Persistent)key).newInstance(new StateManagerImpl());
    else if(keyConstructor == null) {
      throw new RuntimeException("Key class does not have a no-arg constructor");
    }
    else
      return keyConstructor.newInstance(ReflectionUtils.EMPTY_OBJECT_ARRAY);
  }
 
  @SuppressWarnings("unchecked")
  @Override
  public T newPersistent() {
    return (T) persistent.newInstance(new StateManagerImpl());
  }
  
  @Override
  public K getCachedKey() {
    return key;
  }
  
  @Override
  public T getCachedPersistent() {
    return persistent;
  }
  
  @Override
  public Class<K> getKeyClass() {
    return keyClass;
  }
  
  @Override
  public Class<T> getPersistentClass() {
    return persistentClass;
  }
  
  public boolean isKeyPersistent() {
    return isKeyPersistent;
  }
}
