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

import com.google.common.primitives.Ints;
import org.apache.gora.lucene.store.LuceneStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.query.impl.ResultBase;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * LuceneResult hold in memory result set once the query {@link org.apache.gora.lucene.query.LuceneQuery}
 * is executed.
 */
public class LuceneResult<K, T extends PersistentBase> 
extends ResultBase<K, T> {

  private ScoreDoc[] scoreDocs = null;
  private final LuceneStore<K, T> store;
  private String[] fields;
  private int pos = 0;
  private final SearcherManager searcherManager;
  private IndexSearcher searcher;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public LuceneResult(LuceneStore<K, T> dataStore, Query<K, T> query,
          SearcherManager sm) throws IOException {
    super(dataStore, query);

    searcherManager = sm;
    store = dataStore;
    if (query instanceof PartitionQueryImpl) {
      query = ((PartitionQueryImpl) query).getBaseQuery();
    }
    fields = query.getFields();
    if (fields != null) {
      HashSet<String> uniqFields = new HashSet<>(Arrays.asList(fields));
      String keyFld = store.getMapping().getPrimaryKey();
      uniqFields.add(keyFld); // return also primary key
      query.setFields(fields);
    }
    else {
      Collection<String> c = store.getMapping().getLuceneFields();
      String[] a = {};
      fields = c.toArray(a);
      query.setFields(fields);
    }
    // This is based on the limits of IndexSearcher.search(Query, int)
    // A custom Collector could go larger than Integer.MAX_VALUE
    // (NB: TotalHitCountCollector uses an int internally)
    if (limit < 1L)
      limit = Integer.MAX_VALUE;

    searcher = searcherManager.acquire();
    scoreDocs = searcher.search(((LuceneQuery<K, PersistentBase>) query).toLuceneQuery(), Ints.checkedCast(limit)).scoreDocs;
  }

  public ScoreDoc[] getScoreDocs() {
    return scoreDocs;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected boolean nextInner() throws IOException {
    if (scoreDocs == null || pos >= scoreDocs.length) {
      return false;
    }

    Set<String> f = null;
    if (fields != null) {
      f = new HashSet<>(fields.length);
      f.addAll(Arrays.asList(fields));
      f.add(store.getMapping().getPrimaryKey());
    }
    else {
      Collection<String> c = store.getMapping().getLuceneFields();
      String[] a = {};
      fields = c.toArray(a);
    }

    Document doc = searcher.doc(scoreDocs[pos++].doc, f);
    key = (K) doc.get(store.getMapping().getPrimaryKey());
    persistent = store.newInstance(doc, fields);
    return true;
  }

  @Override
  public void close() throws IOException {
    scoreDocs = null;
    searcherManager.release(searcher);
  }

  @Override
  public float getProgress() throws IOException {
    if (scoreDocs != null && scoreDocs.length > 0) {
      return (float)pos / (float)scoreDocs.length;
    } else {
      return 0;
    }
  }

  @Override
  public int size() {
    if (scoreDocs == null) {
      return (int) limit;
    } else {
      int totalSize = scoreDocs.length;
      int intLimit = (int) this.limit;
      return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
    }
  }

}
