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
package org.apache.gora.filter;

import org.apache.gora.persistency.Persistent;
import org.apache.hadoop.io.Writable;

/**
 * Defines filtering (possibly including modification) of rows. By default
 * all filtering is done client side. (In generic Gora classes). Datastore
 * implementations can decide if they install remote filters, when possible.
 */
public interface Filter<K, T extends Persistent> extends Writable{

  /**
   * Filter the key and persistent. Modification is possible.
   *
   * @param key the key to use in the filter
   * @param persistent the {@link Persistent} object to filter on
   * @return <code>true</code> if the row is filtered out (excluded),
   * <code>false</code> otherwise.
   */
  public boolean filter(K key, T persistent);
}
