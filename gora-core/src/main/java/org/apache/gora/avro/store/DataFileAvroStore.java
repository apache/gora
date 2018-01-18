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

import java.io.IOException;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.mapred.FsInput;
import org.apache.gora.avro.query.DataFileAvroResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.OperationNotSupportedException;
import org.apache.hadoop.fs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataFileAvroStore is file based store which uses Avro's 
 * DataFile{Writer,Reader}'s as a backend. This datastore supports 
 * mapreduce.
 */
public class DataFileAvroStore<K, T extends PersistentBase> extends AvroStore<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(AvroStore.class);
  
  public DataFileAvroStore() {
  }
  
  private DataFileWriter<T> writer;
  
  @Override
  public T get(K key, String[] fields) throws GoraException {
    throw new OperationNotSupportedException(
        "Avro DataFile's does not support indexed retrieval");
  }

  @Override
  public void put(K key, T obj) throws GoraException {
    try{
      getWriter().append(obj);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException(e);
    }
  }

  private DataFileWriter<T> getWriter() throws IOException {
    if(writer == null) {
      writer = new DataFileWriter<>(getDatumWriter());
      writer.create(schema, getOrCreateOutputStream());
    }
    return writer;
  }
  
  @Override
  protected Result<K, T> executeQuery(Query<K, T> query) throws IOException {
      return new DataFileAvroResult<>(this, query
          , createReader(createFsInput()));
  }
 
  @Override
  protected Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query) throws IOException {
      FsInput fsInput = createFsInput();
      DataFileReader<T> reader = createReader(fsInput);
      return new DataFileAvroResult<>(this, query, reader, fsInput
          , query.getStart(), query.getLength());
  }
  
  private DataFileReader<T> createReader(FsInput fsInput) throws IOException {
    return new DataFileReader<>(fsInput, getDatumReader());
  }
  
  private FsInput createFsInput() throws IOException {
    Path path = new Path(getInputPath());
    return new FsInput(path, getConf());
  }
  
  @Override
  public void flush() throws GoraException {
    try{
      super.flush();
      if(writer != null) {
        writer.flush();
      }
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new GoraException(e);
    }
  }
  
  @Override
  public void close() {
    try{
      if(writer != null)  
        writer.close(); //hadoop 0.20.2 HDFS streams do not allow 
                        //to close twice, so close the writer first 
      writer = null;
      super.close();
    } catch(IOException ex){
      LOG.error(ex.getMessage(), ex);
    }
  }
}
