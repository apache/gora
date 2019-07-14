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

/**
 * BeanFactory's enable contruction of keys and Persistent objects.
 *
 * @param <K> a key
 * @param <T> a {@link Persistent} object
 */
public interface BeanFactory<K, T> {

  /**
   * Constructs a new instance of the key class
   *
   * @return a new instance of the key class
   * @throws Exception if unabe to create the key class
   */
  K newKey() throws Exception;

  /**
   * Constructs a new instance of the Persistent class
   *
   * @return a new instance of the Persistent class
   */
  T newPersistent();

  /**
   * Returns an instance of the key object to be
   * used to access static fields of the object. Returned object MUST
   * be treated as read-only. No fields other than the static fields
   * of the object should be assumed to be readable.
   *
   * @return a cached instance of the key object
   */
  K getCachedKey();

  /**
   * Returns an instance of the {@link Persistent} object to be
   * used to access static fields of the object. Returned object MUST
   * be treated as read-only. No fields other than the static fields
   * of the object should be assumed to be readable.
   *
   * @return a cached instance of the Persistent object
   */
  T getCachedPersistent();

  /**
   * Returns the key class
   *
   * @return the key class
   */
  Class<K> getKeyClass();

  /**
   * Returns the persistent class
   *
   * @return the persistent class
   */
  Class<T> getPersistentClass();

}