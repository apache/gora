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
package org.apache.gora.solr.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.solr.store.SolrMapping;
import org.apache.gora.solr.store.SolrStore;
import org.apache.gora.store.DataStore;

/**
 * Solr specific implementation of the {@link Query} interface.
 */
public class SolrQuery<K, T extends PersistentBase> extends QueryBase<K, T> {
  SolrStore<K, T> store;

  /**
   * Constructor for the query
   */
  public SolrQuery() {
    super(null);
    store = null;
  }

  /**
   * Constructor for the query
   *
   * @param dataStore Data store used
   *
   */
  public SolrQuery(DataStore<K, T> dataStore) {
    super(dataStore);
    store = (SolrStore<K, T>)dataStore;
  }

  /**
   * Create a solr query
   *
   * @return the solr query string
   */
  public String toSolrQuery() {
    SolrMapping mapping = store.getMapping();
    String fld = mapping.getPrimaryKey();
    String q;
    if (getKey() != null) {
      q = fld + ":" + SolrStore.escapeQueryKey(getKey().toString());
    } else {
      q = fld + ":[";
      if (getStartKey() != null) {
        q += SolrStore.escapeQueryKey(getStartKey().toString());
      } else {
        q += "*";
      }
      q += " TO ";
      if (getEndKey() != null) {
        q += SolrStore.escapeQueryKey(getEndKey().toString());
      } else {
        q += "*";
      }
      q += "]";
    }
    return q;
  }
}
