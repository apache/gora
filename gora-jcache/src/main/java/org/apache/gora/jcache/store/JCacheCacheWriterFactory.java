/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.jcache.store;

import org.apache.gora.persistency.impl.PersistentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.configuration.Factory;
import java.lang.reflect.Constructor;

public class JCacheCacheWriterFactory<K, T extends PersistentBase> implements Factory<JCacheCacheWriter<K,T>> {

  public static final long serialVersionUID = 201205101621L;
  private static final Logger LOG = LoggerFactory.getLogger(JCacheCacheWriterFactory.class);
  private Class<K> keyClass;
  private Class<T> persistentClass;

  public JCacheCacheWriterFactory(Class<K> keyClass,
                                  Class<T> persistentClass) {
    this.keyClass = keyClass;
    this.persistentClass = persistentClass;
  }

  public JCacheCacheWriter<K,T> create() {
    try {
      return (JCacheCacheWriter<K,T>) new JCacheCacheWriter(keyClass, persistentClass);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to create an instance of " + JCacheCacheWriter.class, ex);
    }
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other != null && this.getClass() == other.getClass()) {
      return true;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return this.hashCode();
  }

}