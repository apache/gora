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

package org.apache.gora.avro.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.gora.avro.query.AvroQuery;
import org.apache.gora.avro.query.AvroResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.FileBackedDataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.OperationNotSupportedException;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter DataStore for binary-compatible Avro serializations.
 * AvroDataStore supports Binary and JSON serializations.
 */
public class AvroStore<K, T extends PersistentBase>
extends FileBackedDataStoreBase<K, T> implements Configurable {

  /** The property key specifying avro encoder/decoder type to use. Can take values
   * "BINARY" or "JSON". */
  public static final String CODEC_TYPE_KEY = "codec.type";

  public static final Logger LOG = LoggerFactory.getLogger(AvroStore.class);

  /**
   * The type of the avro Encoder/Decoder.
   */
  public static enum CodecType {
    /** Avro binary encoder */
    BINARY,
    /** Avro JSON encoder */
    JSON,
  }

  private DatumReader<T> datumReader;
  private DatumWriter<T> datumWriter;
  private Encoder encoder;
  private Decoder decoder;

  private CodecType codecType = CodecType.JSON;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
          Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);

    if(properties != null && this.codecType == null) {
      String codecType = DataStoreFactory.findProperty(
              properties, this, CODEC_TYPE_KEY, "BINARY");
      this.codecType = CodecType.valueOf(codecType);
    }
  }

  public void setCodecType(CodecType codecType) {
    this.codecType = codecType;
  }

  public void setEncoder(Encoder encoder) {
    this.encoder = encoder;
  }

  public void setDecoder(Decoder decoder) {
    this.decoder = decoder;
  }

  public void setDatumReader(DatumReader<T> datumReader) {
    this.datumReader = datumReader;
  }

  public void setDatumWriter(DatumWriter<T> datumWriter) {
    this.datumWriter = datumWriter;
  }

  @Override
  public void close() {
    try{
      super.close();
      if(encoder != null) {
        encoder.flush();
      }
      encoder = null;
      decoder = null;
    }catch(IOException ex){
      LOG.error(ex.getMessage(), ex);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    throw new OperationNotSupportedException("delete is not supported for AvroStore");
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    throw new OperationNotSupportedException("delete is not supported for AvroStore");
  }

  /**
   * Executes a normal Query reading the whole data. #execute() calls this function
   * for non-PartitionQuery's.
   */
  @Override
  protected Result<K,T> executeQuery(Query<K,T> query) throws IOException {
    return new AvroResult<>(this, (AvroQuery<K,T>)query,
            getDatumReader(), getDecoder());
  }

  /**
   * Executes a PartitialQuery, reading the data between start and end.
   */
  @Override
  protected Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query)
          throws IOException {
    throw new OperationNotSupportedException("Not yet implemented");
  }

  @Override
  public void flush() throws GoraException {
    try{
      super.flush();
      if(encoder != null)
        encoder.flush();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    throw new OperationNotSupportedException();
  }

  @Override
  public AvroQuery<K,T> newQuery() {
    return new AvroQuery<>(this);
  }

  @Override
  public void put(K key, T obj) throws GoraException {
    try{
      getDatumWriter().write(obj, getEncoder());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException(e);
    }
  }

  public Encoder getEncoder() throws IOException {
    if(encoder == null) {
      encoder = createEncoder();
    }
    return encoder;
  }

  public Decoder getDecoder() throws IOException {
    if(decoder == null) {
      decoder = createDecoder();
    }
    return decoder;
  }

  public DatumReader<T> getDatumReader() {
    if(datumReader == null) {
      datumReader = createDatumReader();
    }
    return datumReader;
  }

  public DatumWriter<T> getDatumWriter() {
    if(datumWriter == null) {
      datumWriter = createDatumWriter();
    }
    return datumWriter;
  }

  protected Encoder createEncoder() throws IOException {
    switch(codecType) {
      case BINARY:
      return EncoderFactory.get().binaryEncoder(getOrCreateOutputStream(), null);
      case JSON:
      return EncoderFactory.get().jsonEncoder(schema, getOrCreateOutputStream());
    }
    return null;
  }

  protected Decoder createDecoder() throws IOException {
    switch(codecType) {
      case BINARY:
      return DecoderFactory.get().binaryDecoder(getOrCreateInputStream(), null);
      case JSON:
      return DecoderFactory.get().jsonDecoder(schema, getOrCreateInputStream());
    }
    return null;
  }

  protected DatumWriter<T> createDatumWriter() {
    return new SpecificDatumWriter<>(schema);
  }

  protected DatumReader<T> createDatumReader() {
    return new SpecificDatumReader<>(schema);
  }

  @Override
  public Configuration getConf() {
    if(conf == null) {
      conf = new Configuration();
    }
    return conf;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
  }

  @Override
  public String getSchemaName() {
    return "default";
  }
}
