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

package org.apache.gora.jcache.query;

import java.io.IOException;

import org.apache.gora.jcache.store.JCacheStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

public class JCacheResult<K,T extends PersistentBase> extends ResultBase<K,T> {
  
  public JCacheStore<K,T> getDataStore() {
    return (JCacheStore<K,T>) super.getDataStore();
  }

  public JCacheResult(DataStore<K,T> dataStore, Query<K,T> query) {
    super(dataStore, query);
  }
  
  @Override
  public float getProgress() throws IOException {
    return 0;
  }
  
  @Override
  public void close() throws IOException {
    
  }
  
  @Override
  protected boolean nextInner() throws IOException {
    return true;
  }
  
}
