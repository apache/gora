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

package org.apache.gora.arangodb.query;

import org.apache.gora.arangodb.store.ArangoDBMapping;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;

import java.util.HashMap;
import java.util.Map;

/**
 * ArangoDB specific implementation of the {@link org.apache.gora.query.Query} interface.
 */
public class ArangoDBQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  private String arangoDBQuery;
  private Map<String, Object> params;

  public ArangoDBQuery() {
    super(null);
  }

  public ArangoDBQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  public String populateArangoDBQuery(final ArangoDBMapping arangoDBMapping,
                                      final boolean isSelect) {
    params = new HashMap<String, Object>();
    StringBuffer dbQuery = new StringBuffer();
    if ((this.getStartKey() != null) && (this.getEndKey() != null)
            && this.getStartKey().equals(this.getEndKey())) {
      dbQuery.append("FOR t IN @collection FILTER t._key == @key "
              .replace("@collection", arangoDBMapping.getDocumentClass()));
      params.put("key", encodeArangoDBKey(this.getStartKey()));
    } else if (this.getStartKey() != null && this.getEndKey() != null) {
      dbQuery.append("FOR t IN @collection FILTER t._key >= @lower && t._key <= @upper "
              .replace("@collection", arangoDBMapping.getDocumentClass()));
      params.put("lower", encodeArangoDBKey(this.getStartKey()));
      params.put("upper", encodeArangoDBKey(this.getEndKey()));
    } else if (this.getStartKey() != null) {
      dbQuery.append("FOR t IN @collection FILTER t._key >= @lower "
              .replace("@collection", arangoDBMapping.getDocumentClass()));
      params.put("lower", encodeArangoDBKey((this.getStartKey())));
    } else if (this.getEndKey() != null) {
      dbQuery.append("FOR t IN @collection FILTER t._key <= @upper "
              .replace("@collection", arangoDBMapping.getDocumentClass()));
      params.put("upper", encodeArangoDBKey(this.getEndKey()));
    } else {
      dbQuery.append("FOR t IN @collection "
              .replace("@collection", arangoDBMapping.getDocumentClass()));
    }

    if (this.getLimit() != -1) {
      dbQuery.append("Limit @count ");
      params.put("count", this.getLimit());
    }

    if (isSelect) {
      dbQuery.append("RETURN t");
    } else {
      dbQuery.append("REMOVE t IN @collection".replace("@collection", arangoDBMapping.getDocumentClass()));
    }

    arangoDBQuery = dbQuery.toString();
    return arangoDBQuery;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public String getArangoDBQuery() {
    return arangoDBQuery;
  }

  public K encodeArangoDBKey(final K key) {
    if (key == null) {
      return null;
    } else if (!(key instanceof String)) {
      return key;
    }
    return (K) key.toString().replace("/", "%2F")
            .replace("&", "%26");
  }

}
