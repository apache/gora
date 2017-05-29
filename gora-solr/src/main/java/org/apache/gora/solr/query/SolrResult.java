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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.solr.store.SolrStore;
import org.apache.gora.store.DataStore;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;

/**
 * SolrResult specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class SolrResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  SolrDocumentList list = null;
  SolrStore<K, T> store;
  String[] fields;
  int pos = 0;

  /**
   * Constructor for the result set
   *
   * @param dataStore Data store used
   * @param query     Query used
   * @param server    A client that talks directly to a Solr server
   * @param resultsSize  The number of rows to be returned
   */
  public SolrResult(DataStore<K, T> dataStore, Query<K, T> query,
      SolrClient server, int resultsSize) throws IOException {
    super(dataStore, query);
    store = (SolrStore<K, T>)dataStore;
    ModifiableSolrParams params = new ModifiableSolrParams();
    if (query instanceof PartitionQueryImpl) {
      query = ((PartitionQueryImpl<K, T>)query).getBaseQuery();
    }
    String q = ((SolrQuery<K, T>)query).toSolrQuery();
    params.set(CommonParams.Q, q);
    fields = query.getFields();
    if (fields == null) {
      params.set(CommonParams.FL, "*");
    } else {
      HashSet<String> uniqFields = new HashSet<>(Arrays.asList(fields));
      String keyFld = ((SolrStore<K, T>)dataStore).getMapping().getPrimaryKey();
      uniqFields.add(keyFld); // return also primary key
      StringBuilder sb = new StringBuilder();
      for (String f : uniqFields) {
        if (sb.length() > 0) sb.append(',');
        sb.append(f);
      }
      params.set(CommonParams.FL, sb.toString());
    }
    params.set(CommonParams.ROWS, resultsSize);
    try {
      QueryResponse rsp = server.query(params);
      list = rsp.getResults();
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  /**
   * Gets the next item
   */
  @SuppressWarnings("unchecked")
  @Override
  protected boolean nextInner() throws IOException {
    if (list == null || pos >= list.size()) {
      return false;
    }
    SolrDocument doc = list.get(pos++);
    key = (K) doc.get(store.getMapping().getPrimaryKey());
    persistent = store.newInstance(doc, fields);
    return true;
  }
  
  @Override
  public void close() throws IOException {
    if (list != null) list.clear();
  }

  /**
   * Gets the items reading progress
   */
  @Override
  public float getProgress() throws IOException {
    if (list != null && list.size() > 0) {
      return (float)pos / (float)list.size();
    } else {
      return 0;
    }
  }
}
