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

import static org.apache.gora.hbase.util.HBaseByteInterface.fromBytes;

import java.io.IOException;

import org.apache.gora.hbase.store.HBaseStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.hadoop.hbase.client.Result;

/**
 * Base class for {@link Result} implementations for HBase.  
 */
public abstract class HBaseResult<K, T extends Persistent> 
  extends ResultBase<K, T> {

  public HBaseResult(HBaseStore<K,T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }
  
  @Override
  public HBaseStore<K, T> getDataStore() {
    return (HBaseStore<K, T>) super.getDataStore();
  }
  
  protected void readNext(Result result) throws IOException {
    key = fromBytes(getKeyClass(), result.getRow());
    persistent = getDataStore().newInstance(result, query.getFields());
  }
  
}
