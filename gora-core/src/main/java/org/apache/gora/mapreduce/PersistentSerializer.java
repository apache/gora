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
package org.apache.gora.mapreduce;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.avro.io.BinaryEncoder;
import org.apache.gora.avro.PersistentDatumWriter;
import org.apache.gora.persistency.Persistent;
import org.apache.hadoop.io.serializer.Serializer;

/**
 * Hadoop serializer using {@link PersistentDatumWriter} 
 * with {@link BinaryEncoder}. 
 */
public class PersistentSerializer implements Serializer<Persistent> {

  private PersistentDatumWriter<Persistent> datumWriter;
  private BinaryEncoder encoder;  
  
  public PersistentSerializer() {
    this.datumWriter = new PersistentDatumWriter<Persistent>();
  }
  
  @Override
  public void close() throws IOException {
    encoder.flush();
  }

  @Override
  public void open(OutputStream out) throws IOException {
    encoder = new BinaryEncoder(out);
  }

  @Override
  public void serialize(Persistent persistent) throws IOException {   
    datumWriter.setSchema(persistent.getSchema());
    datumWriter.setPersistent(persistent);
        
    datumWriter.write(persistent, encoder);
  }
}
