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

package org.apache.gora.hbase.query;

import java.io.IOException;

import org.apache.gora.hbase.store.HBaseStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;

/**
 * An {@link HBaseResult} based on the result of a HBase {@link Get} query.
 */
public class HBaseGetResult<K, T extends PersistentBase> extends HBaseResult<K,T> {

  private Result result;
  
  public HBaseGetResult(HBaseStore<K, T> dataStore, Query<K, T> query
      , Result result) {
    super(dataStore, query);
    this.result = result;
  }

  @Override
  public float getProgress() throws IOException {
    return key == null ? 0f : 1f;
  }

  @Override
  public boolean nextInner() throws IOException {
    if(result == null || result.getRow() == null 
        || result.getRow().length == 0) {
      return false;
    }
    if(key == null) {
      readNext(result);
      return key != null;
    }
    
    return false;
  }

  @Override
  public void close() throws IOException {
  }
}
