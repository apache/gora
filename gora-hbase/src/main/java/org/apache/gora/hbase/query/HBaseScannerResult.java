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
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;

/**
 * Result of a query based on an HBase scanner.
 */
public class HBaseScannerResult<K, T extends PersistentBase> 
  extends HBaseResult<K, T> {

  private final ResultScanner scanner;
  
  public HBaseScannerResult(HBaseStore<K,T> dataStore, Query<K, T> query, 
      ResultScanner scanner) {
    super(dataStore, query);
    this.scanner = scanner;
  }

  // do not clear object in scanner result
  @Override
  protected void clear() { }
  
  @Override
  public boolean nextInner() throws IOException {
    
    Result result = scanner.next();
    if (result == null) {
      return false;
    }
    
    readNext(result);
    
    return true;
  }

  @Override
  public void close() throws IOException {
    scanner.close();
  }
  
  @Override
  public float getProgress() throws IOException {
    //TODO: if limit is set, we know how far we have gone 
    return 0;
  }
  
}
