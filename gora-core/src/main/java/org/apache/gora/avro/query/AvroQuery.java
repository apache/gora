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

package org.apache.gora.avro.query;

import org.apache.gora.avro.store.AvroStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.impl.QueryBase;

/**
 * A simple Query implementation for Avro. Due to the data model, 
 * most of the operations for Query, like setting start,end keys is not 
 * supported. Setting query limit is supported.
 */
public class AvroQuery<K, T extends Persistent> extends QueryBase<K,T> {

  public AvroQuery() {
    super(null);
  }
  
  public AvroQuery(AvroStore<K,T> dataStore) {
    super(dataStore);
  }
  
}
