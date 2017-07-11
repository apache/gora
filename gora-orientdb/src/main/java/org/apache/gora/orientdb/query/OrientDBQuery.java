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

package org.apache.gora.orientdb.query;

import com.github.raymanrt.orientqb.query.Parameter;
import com.github.raymanrt.orientqb.query.Projection;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.apache.gora.orientdb.store.OrientDBMapping;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;
import com.github.raymanrt.orientqb.query.Query;

import java.util.HashMap;
import java.util.Map;

import static com.github.raymanrt.orientqb.query.Projection.projection;


/**
 * OrientDB specific implementation of the {@link org.apache.gora.query.Query} interface.
 */
public class OrientDBQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  private OSQLSynchQuery<ODocument> dbQuery;
  private Map<String, Object> params;

  public OrientDBQuery() {
    super(null);
  }

  public OrientDBQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  /**
   * Return populated {@link OSQLSynchQuery} Orient DB query.
   *
   * @return a {@link OSQLSynchQuery} query executable over Orient DB.
   */
  public OSQLSynchQuery<ODocument> getOrientDBQuery() {
    return dbQuery;
  }

  /**
   * Dynamic parameters for {@link OSQLSynchQuery} Orient DB query.
   *
   * @return a param map related to {@link OSQLSynchQuery} Orient DB query.
   */
  public Map<String, Object> getParams() {
    return params;
  }

  /**
   * Convert Gora query to Orient DB specific query which underline API understands.
   * And maintain it s state encapsulated to Gora implementation of the {@link org.apache.gora.query.Query}.
   *
   * @return a {@link OSQLSynchQuery} query executable over Orient DB.
   */
  public OSQLSynchQuery<ODocument> populateOrientDBQuery(final OrientDBMapping orientDBMapping,
                                                         final String[] fields,
                                                         final String[] schemaFields) {
    params = new HashMap<String, Object>();
    Query selectQuery = new Query();
    selectQuery.from(orientDBMapping.getDocumentClass());
    if ((this.getStartKey() != null) && (this.getEndKey() != null)
            && this.getStartKey().equals(this.getEndKey())) {
      selectQuery.where(projection("_id").eq(Parameter.parameter("key")));
      params.put("key", this.getStartKey());
    } else if (this.getStartKey() != null || this.getEndKey() != null) {
      if (this.getStartKey() != null) {
        selectQuery.where(projection("_id").ge(Parameter.parameter("key_lower")));
        params.put("key_lower", this.getStartKey());
      }
      if (this.getEndKey() != null) {
        selectQuery.where(projection("_id").le(Parameter.parameter("key_upper")));
        params.put("key_upper", this.getEndKey());
      }
    }

    if (fields.length == schemaFields.length) {
      selectQuery.select(Projection.ALL);
    } else {
      for (String k : fields) {
        String dbFieldName = orientDBMapping.getDocumentField(k);
        if (dbFieldName != null && dbFieldName.length() > 0) {
          selectQuery.select(dbFieldName);
        }
      }
      selectQuery.select("_id");
    }
    dbQuery = new OSQLSynchQuery<ODocument>(selectQuery.toString());
    return dbQuery;
  }

}
