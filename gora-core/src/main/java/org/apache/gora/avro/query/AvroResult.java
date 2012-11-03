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

import java.io.EOFException;
import java.io.IOException;

import org.apache.avro.AvroTypeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.gora.avro.store.AvroStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.ResultBase;

/**
 * Adapter to convert DatumReader to Result.
 */
public class AvroResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private DatumReader<T> reader;
  private Decoder decoder;
  
  public AvroResult(AvroStore<K,T> dataStore, AvroQuery<K,T> query
      , DatumReader<T> reader, Decoder decoder) {
    super(dataStore, query);
    this.reader = reader;
    this.decoder = decoder;
  }

  public void close() throws IOException {
  }

  @Override
  public float getProgress() throws IOException {
    //TODO: FIXME
    return 0;
  }

  @Override
  public boolean nextInner() throws IOException {
    try {
      persistent = reader.read(persistent, decoder);
      
    } catch (AvroTypeException ex) {
      //TODO: it seems that avro does not respect end-of file and return null
      //gracefully. Report the issue.
      return false;
    } catch (EOFException ex) {
      return false;
    }
    
    return persistent != null;
  }  
}
