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

import org.apache.gora.lucene.store.LuceneMapping;
import org.apache.gora.lucene.store.LuceneStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

/**
 * LuceneQuery hold in memory Gora representation for
 * {@link org.apache.lucene.search.Query}
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
      if (getEndKey() == null && getStartKey() == null) {
        return new MatchAllDocsQuery();
      } else {
        q = inferType(pk, getStartKey(), getEndKey());
      }
    }
    return q;
  }

  private <K> Query inferType(String pk, K lower, K upper) {
    if (((lower != null && lower.getClass() == Integer.class)
            || (upper != null && upper.getClass() == Integer.class))) {
      int ilower = lower == null ? Integer.MIN_VALUE : (Integer) lower;
      int iupper = upper == null ? Integer.MAX_VALUE : (Integer) upper;
      return IntPoint.newRangeQuery(pk, ilower, iupper);
    } else if (((lower != null && lower.getClass() == Long.class)
            || (upper != null && upper.getClass() == Long.class))) {
      long llower = lower == null ? Long.MIN_VALUE : (Long) lower;
      long lupper = upper == null ? Long.MAX_VALUE : (Long) upper;
      return LongPoint.newRangeQuery(pk, llower, lupper);
    } else if (((lower != null && lower.getClass() == Float.class)
            || (upper != null && upper.getClass() == Float.class))) {
      float flower = lower == null ? Float.MIN_VALUE : (Float) lower;
      float fupper = upper == null ? Float.MAX_VALUE : (Float) upper;
      return FloatPoint.newRangeQuery(pk, flower, fupper);
    } else if (((lower != null && lower.getClass() == Double.class)
            || (upper != null && upper.getClass() == Double.class))) {
      double dlower = lower == null ? Double.MIN_VALUE : (Double) lower;
      double dupper = upper == null ? Double.MAX_VALUE : (Double) upper;
      return DoublePoint.newRangeQuery(pk, dlower, dupper);
    } else {
      // Infer string type by default if it cannot detect a numeric datatype.
      String slower = lower == null ? null : lower.toString();
      String supper = upper == null ? null : upper.toString();
      return TermRangeQuery.newStringRange(pk, slower, supper, true, true);
    }
  }
}
