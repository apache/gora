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

package org.apache.gora.cassandra.query;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import me.prettyprint.hector.api.Serializer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.gora.cassandra.serializers.GoraSerializerTypeInferer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a unit of data: a key value pair tagged by a family name
 */
public abstract class CassandraColumn {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraColumn.class);

  public static final int SUB = 0;
  public static final int SUPER = 1;

  private String family;
  private int type;
  private Field field;
  private int unionType;
  
  public static final ThreadLocal<BinaryDecoder> decoders =
      new ThreadLocal<BinaryDecoder>();

  /*
   * Create a threadlocal map for the datum readers and writers, because
   * they are not thread safe, at least not before Avro 1.4.0 (See AVRO-650).
   * When they are thread safe, it is possible to maintain a single reader and
   * writer pair for every schema, instead of one for every thread.
   */
  
  public static final ConcurrentHashMap<String, SpecificDatumReader<?>> readerMap = 
      new ConcurrentHashMap<String, SpecificDatumReader<?>>();
  
  public void setUnionType(int pUnionType){
    this.unionType = pUnionType;
  }

  public int getUnionType(){
    return unionType;
  }

  public String getFamily() {
    return family;
  }
  public void setFamily(String family) {
    this.family = family;
  }
  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
  }
  public void setField(Field field) {
    this.field = field;
  }

  protected Field getField() {
    return this.field;
  }

  public abstract ByteBuffer getName();
  public abstract Object getValue();
  
  @SuppressWarnings({ "rawtypes" })
  protected Object fromByteBuffer(Schema schema, ByteBuffer byteBuffer) {
    Object value = null;
    Serializer<?> serializer = GoraSerializerTypeInferer.getSerializer(schema);
    if (serializer == null) {
      LOG.warn("Schema: " + schema.getName() + " is not supported. No serializer "
          + "could be found. Please report this to dev@gora.apache.org");
    } else {
      value = serializer.fromByteBuffer(byteBuffer);
      if (schema.getType().equals(Type.RECORD)){
        String schemaId = schema.getFullName();      
        
        SpecificDatumReader<?> reader = (SpecificDatumReader<?>)readerMap.get(schemaId);
        if (reader == null) {
          reader = new SpecificDatumReader(schema);// ignore dirty bits
          SpecificDatumReader localReader=null;
          if((localReader=readerMap.putIfAbsent(schemaId, reader))!=null) {
            reader = localReader;
          }
        }
        
        // initialize a decoder, possibly reusing previous one
        BinaryDecoder decoderFromCache = decoders.get();
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder((byte[])value, null);
        // put in threadlocal cache if the initial get was empty
        if (decoderFromCache==null) {
          decoders.set(decoder);
        }
        try {
          value = reader.read(null, decoder);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }        
      }
    }
    return value;
  }

}
