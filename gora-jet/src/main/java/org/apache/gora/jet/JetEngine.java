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
package org.apache.gora.jet;

import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;

/**
 * Core class which handles Gora - Jet Engine integration.
 */
public class JetEngine<KeyIn, ValueIn extends PersistentBase, KeyOut, ValueOut extends PersistentBase> {
  static DataStore dataOutStore;
  static DataStore dataInStore;
  static Query query;

  public BatchSource<JetInputOutputFormat<KeyIn, ValueIn>> createDataSource(DataStore<KeyIn, ValueIn> dataOutStore) {
    return createDataSource(dataOutStore, dataOutStore.newQuery());
  }

  public BatchSource<JetInputOutputFormat<KeyIn, ValueIn>> createDataSource(DataStore<KeyIn, ValueIn> dataOutStore,
                                                                            Query<KeyIn, ValueIn> query) {
    JetEngine.dataInStore = dataOutStore;
    JetEngine.query = query;
    return Sources.batchFromProcessor("gora-jet-source", new JetSource<KeyIn, ValueIn>());
  }

  public Sink<JetInputOutputFormat<KeyOut, ValueOut>> createDataSink(DataStore<KeyOut, ValueOut> dataOutStore) {
    JetEngine.dataOutStore = dataOutStore;
    return Sinks.fromProcessor("gora-jet-sink", new JetSink<KeyOut, ValueOut>());
  }
}
