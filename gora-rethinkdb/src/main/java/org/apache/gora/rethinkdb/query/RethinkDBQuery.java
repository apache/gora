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

package org.apache.gora.rethinkdb.query;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.ReqlExpr;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.rethinkdb.store.RethinkDBMapping;
import org.apache.gora.rethinkdb.store.RethinkDBStoreParameters;
import org.apache.gora.store.DataStore;

/**
 * RethinkDB specific implementation of the {@link org.apache.gora.query.Query} interface.
 */
public class RethinkDBQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  public static final RethinkDB r = RethinkDB.r;
  private ReqlExpr dbQuery;

  public RethinkDBQuery() {
    super(null);
  }

  public RethinkDBQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  public Object populateRethinkDBQuery(final RethinkDBMapping rethinkDBMapping,
                                       final RethinkDBStoreParameters rethinkDBStoreParameters,
                                       final String[] fields,
                                       final String[] schemaFields) {
    if ((this.getStartKey() != null) && (this.getEndKey() != null)
            && this.getStartKey().equals(this.getEndKey())) {
      dbQuery = r.db(rethinkDBStoreParameters.getDatabaseName())
              .table(rethinkDBMapping.getDocumentClass())
              .filter(row -> row.g("id").eq(this.getStartKey()));
    } else if (this.getStartKey() != null || this.getEndKey() != null) {
      if (this.getStartKey() != null
              && this.getEndKey() == null) {
        dbQuery = r.db(rethinkDBStoreParameters.getDatabaseName())
                .table(rethinkDBMapping.getDocumentClass())
                .filter(row -> row.g("id").ge(this.getStartKey()));
      } else if (this.getEndKey() != null
              && this.getStartKey() == null) {
        dbQuery = r.db(rethinkDBStoreParameters.getDatabaseName())
                .table(rethinkDBMapping.getDocumentClass())
                .filter(row -> row.g("id").le(this.getEndKey()));
      } else {
        dbQuery = r.db(rethinkDBStoreParameters.getDatabaseName())
                .table(rethinkDBMapping.getDocumentClass())
                .filter(row -> row.g("id").ge(this.getStartKey()).
                        and(row.g("id").le(this.getEndKey())));
      }
    } else {
      dbQuery = r.db(rethinkDBStoreParameters.getDatabaseName())
              .table(rethinkDBMapping.getDocumentClass());
    }

    if (fields.length == schemaFields.length) {
      // all
    } else {
      String[] projection = new String[fields.length + 1];
      int counter = 0;
      for (String k : fields) {
        String dbFieldName = rethinkDBMapping.getDocumentField(k);
        if (dbFieldName != null && dbFieldName.length() > 0) {
          projection[counter] = dbFieldName;
          counter++;
        }
      }
      projection[counter] = "id";
      dbQuery = dbQuery.pluck(projection);
    }

    return dbQuery;
  }

  public ReqlExpr getRethinkDBDbQuery() {
    return dbQuery;
  }

}
