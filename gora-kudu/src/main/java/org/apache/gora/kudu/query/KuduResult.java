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
package org.apache.gora.kudu.query;

import java.io.IOException;
import org.apache.gora.kudu.store.KuduStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.RowResult;
import org.apache.kudu.client.RowResultIterator;

/**
 * KuduResult specific implementation of the
 * {@link org.apache.gora.query.Result} interface.
 */
public class KuduResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private final KuduScanner result;
  private RowResultIterator resultIt;

  public KuduResult(DataStore<K, T> dataStore, Query<K, T> query, KuduScanner result) {
    super(dataStore, query);
    this.result = result;
  }

  @Override
  protected boolean nextInner() throws IOException {
    boolean more = false;
    if (this.resultIt == null || !this.resultIt.hasNext()) {
      while (this.result.hasMoreRows()) {
        RowResultIterator nextRows = this.result.nextRows();
        if (nextRows.hasNext()) {
          resultIt = nextRows;
          more = true;
        }
      }
    } else {
      more = true;
    }
    if (more) {
      RowResult next = resultIt.next();
      key = ((KuduStore<K, T>) getDataStore()).extractKey(next);
      persistent = ((KuduStore<K, T>) getDataStore()).newInstance(next, getQuery().getFields());
    }
    return more;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    if (this.limit != -1) {
      return (float) this.offset / (float) this.limit;
    } else {
      return 0;
    }
  }

  @Override
  public int size() {
    return (int) this.limit;
  }

  @Override
  public void close() throws IOException {
    if (result != null) {
      result.close();
    }
  }

}
