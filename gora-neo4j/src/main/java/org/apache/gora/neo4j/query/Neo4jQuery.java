/*
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
package org.apache.gora.neo4j.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;

/**
 * Neo4j specific implementation of the {@link Query} interface.
 */
public class Neo4jQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  /**
   * Constructor for the query
   *
   * @param dataStore Data store used
   *
   */
  public Neo4jQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  /**
   * Constructor for the query
   */
  public Neo4jQuery() {
    super(null);
  }
}
