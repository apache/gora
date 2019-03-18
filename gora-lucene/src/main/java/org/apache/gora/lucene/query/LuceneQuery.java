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
package org.apache.gora.lucene.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.lucene.store.LuceneMapping;
import org.apache.gora.lucene.store.LuceneStore;
import org.apache.gora.store.DataStore;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

/**
 * LuceneQuery hold in memory Gora representation for  {@link org.apache.lucene.search.Query}
 */
public class LuceneQuery<K, T extends PersistentBase> extends QueryBase<K, T> {
  private final LuceneStore<K, T> store;

  public LuceneQuery() {
    super(null);
    store = null;
  }

  public LuceneQuery(DataStore<K, T> dataStore) {
    super(dataStore);
    store = (LuceneStore<K, T>) dataStore;
  }

  public Query toLuceneQuery() {
    LuceneMapping mapping = store.getMapping();
    String pk = mapping.getPrimaryKey();
    Query q;
    if (getKey() != null) {
      q = new TermQuery(new Term(pk, getKey().toString()));
    } else {
      //TODO: Change this to a NumericRangeQuery when necessary (it's faster)
      String lower = null;
      String upper = null;
      if (getStartKey() != null) {
        //Do we need to escape the term?
        lower = getStartKey().toString();
      }
      if (getEndKey() != null) {
        upper = getEndKey().toString();
      }
      if (upper == null && lower == null) {
        q = new MatchAllDocsQuery();
      } else {
        q = TermRangeQuery.newStringRange(pk, lower, upper, true, true);
      }
    }
    return q;
  }
}
