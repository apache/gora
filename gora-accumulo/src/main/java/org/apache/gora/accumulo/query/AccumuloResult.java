/**
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
package org.apache.gora.accumulo.query;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.gora.accumulo.store.AccumuloStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

/**
 * 
 */
public class AccumuloResult<K,T extends PersistentBase> extends ResultBase<K,T> {
  
  private RowIterator iterator;

  public AccumuloStore<K,T> getDataStore() {
    return (AccumuloStore<K,T>) super.getDataStore();
  }

  /**
   * @param dataStore
   * @param query
   * @param scanner
   */
  public AccumuloResult(DataStore<K,T> dataStore, Query<K,T> query, Scanner scanner) {
    super(dataStore, query);
    
    // TODO set batch size based on limit, and construct iterator later
    iterator = new RowIterator(scanner.iterator());
  }
  
  @Override
  public float getProgress() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public void close() throws IOException {
    
  }
  
  @Override
  protected boolean nextInner() throws IOException {
    
    if (!iterator.hasNext())
      return false;
    
    key = null;
    
    Iterator<Entry<Key,Value>> nextRow = iterator.next();
    ByteSequence row = getDataStore().populate(nextRow, persistent);
    key = (K) ((AccumuloStore) dataStore).fromBytes(getKeyClass(), row.toArray());
    
    return true;
  }
  
}
