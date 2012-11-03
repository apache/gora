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
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.AvroUtils;
import org.apache.hadoop.io.serializer.Deserializer;

/**
* Hadoop deserializer using {@link PersistentDatumReader}
* with {@link BinaryDecoder}.
*/
public class PersistentDeserializer
   implements Deserializer<PersistentBase> {

  private BinaryDecoder decoder;
  private Class<? extends PersistentBase> persistentClass;
  private boolean reuseObjects;
  private PersistentDatumReader<PersistentBase> datumReader;

  public PersistentDeserializer(Class<? extends PersistentBase> c, boolean reuseObjects) {
    this.persistentClass = c;
    this.reuseObjects = reuseObjects;
    try {
      Schema schema = AvroUtils.getSchema(persistentClass);
      datumReader = new PersistentDatumReader<PersistentBase>(schema, true);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  //@Override
  public void open(InputStream in) throws IOException {
    /* It is very important to use a direct buffer, since Hadoop
     * supplies an input stream that is only valid until the end of one
     * record serialization. Each time deserialize() is called, the IS
     * is advanced to point to the right location, so we should not
     * buffer the whole input stream at once.
     */
    decoder = new DecoderFactory().configureDirectDecoder(true)
      .createBinaryDecoder(in, decoder);
  }

  //@Override
  public void close() throws IOException { }

  @Override
  public PersistentBase deserialize(PersistentBase persistent) throws IOException {
    return datumReader.read(reuseObjects ? persistent : null, decoder);
  }
}
