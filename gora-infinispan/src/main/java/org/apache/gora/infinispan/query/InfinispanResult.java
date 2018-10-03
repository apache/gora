/*
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
package org.apache.gora.infinispan.query;

import org.apache.gora.infinispan.store.InfinispanStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/*
 * @author Pierre Sutra, Valerio Schiavoni
 */
public class InfinispanResult<K, T extends PersistentBase> extends ResultBase<K, T>  {

  public static final Logger LOG = LoggerFactory.getLogger(InfinispanResult.class);

  private List<T> list;
  private int current;
  private int primaryFieldPos;

  public InfinispanResult(DataStore<K, T> dataStore, InfinispanQuery<K, T> query) {
    super(dataStore, query);
    list = query.list();
    current = 0;
    primaryFieldPos = ((InfinispanStore<K,T>)dataStore).getPrimaryFieldPos();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    LOG.debug("getProgress()");
    if (list.size()==0) return 1;
    float progress = ((float)current/(float)list.size());
    LOG.trace("progress: "+progress);
    return progress;
  }

  @Override
  protected boolean nextInner() throws IOException {
    LOG.debug("nextInner()");
    if(current==list.size()) {
      LOG.trace("end");
      return false;
    }
    persistent = list.get(current);
    key = (K) list.get(current).get(primaryFieldPos);
    current++;
    LOG.trace("current: "+persistent);
    return true;
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  protected void clear() {
    LOG.debug("clear()");
    // do nothing
  }

}
