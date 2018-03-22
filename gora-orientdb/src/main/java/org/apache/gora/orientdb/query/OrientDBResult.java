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

import java.io.IOException;
import java.util.Iterator;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OConcurrentResultSet;
import org.apache.gora.orientdb.store.OrientDBStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OrientDB specific implementation of the {@link org.apache.gora.query.Result} interface.
 *
 */
public class OrientDBResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  /**
   * Reference to the OrientDB Results set.
   */
  private OConcurrentResultSet<ODocument> resultSet;
  private int size;
  private static final Logger log = LoggerFactory.getLogger(OrientDBResult.class);
  private Iterator<ODocument> resultSetIterator;

  public OrientDBResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  public OrientDBResult(DataStore<K, T> dataStore,
                        Query<K, T> query,
                        OConcurrentResultSet<ODocument> resultSet) {
    super(dataStore, query);
    this.resultSet = resultSet;
    this.size = resultSet.size();
    this.resultSetIterator = resultSet.iterator();
  }

  public OrientDBStore<K, T> getDataStore() {
    return (OrientDBStore<K, T>) super.getDataStore();
  }

  @Override
  public float getProgress() throws IOException {
    if (resultSet == null) {
      return 0;
    } else if (size == 0) {
      return 1;
    } else {
      return offset / (float) size;
    }
  }

  @Override
  public void close() throws IOException {
    resultSet.clear();
  }

  @Override
  protected boolean nextInner() throws IOException {
    ODatabaseDocumentTx loadTx = ((OrientDBStore<K, T>) getDataStore())
            .getConnectionPool().acquire();
    loadTx.activateOnCurrentThread();
    try {

      if (!resultSetIterator.hasNext()) {
        return false;
      }

      ODocument obj = resultSetIterator.next();
      key = (K) obj.field("_id");
      persistent = ((OrientDBStore<K, T>) getDataStore())
              .convertOrientDocToAvroBean(obj, getQuery().getFields());
      return persistent != null;
    } finally {
      loadTx.close();
    }
  }

    @Override
    public int size() {
        return size;
    }

}
