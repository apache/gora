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

package org.apache.gora.cassandra.query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * List data structure to keep the order coming from the Cassandra selects.
 */
public class CassandraResultSet<K> extends ArrayList<CassandraRow<K>> {

  /**
   * 
   */
  private static final long serialVersionUID = -7620939600192859652L;

  /**
   * Maps keys to indices in the list.
   */
  private HashMap<K, Integer> indexMap = new HashMap<K, Integer>();

  public CassandraRow<K> getRow(K key) {
    Integer integer = this.indexMap.get(key);
    if (integer == null) {
      return null;
    }
    
    return this.get(integer);
  }

  public void putRow(K key, CassandraRow<K> cassandraRow) {
    this.add(cassandraRow);
    this.indexMap.put(key, this.size()-1);
  } 
  

}
