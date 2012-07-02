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
import org.apache.gora.avro.mapreduce.FsInput;
import org.apache.gora.avro.query.DataFileAvroResult;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.util.OperationNotSupportedException;
import org.apache.hadoop.fs.Path;

/**
 * DataFileAvroStore is file based store which uses Avro's 
 * DataFile{Writer,Reader}'s as a backend. This datastore supports 
 * mapreduce.
 */
public class DataFileAvroStore<K, T extends PersistentBase> extends AvroStore<K, T> {

  public DataFileAvroStore() {
  }
  
  private DataFileWriter<T> writer;
  
  @Override
  public T get(K key, String[] fields) throws java.io.IOException {
    throw new OperationNotSupportedException(
        "Avro DataFile's does not support indexed retrieval");
  };
  
  @Override
  public void put(K key, T obj) throws java.io.IOException {
    getWriter().append(obj);
  };
  
  private DataFileWriter<T> getWriter() throws IOException {
    if(writer == null) {
      writer = new DataFileWriter<T>(getDatumWriter());
      writer.create(schema, getOrCreateOutputStream());
    }
    return writer;
  }
  
  @Override
  protected Result<K, T> executeQuery(Query<K, T> query) throws IOException {
    return new DataFileAvroResult<K, T>(this, query
        , createReader(createFsInput()));
  }
 
  @Override
  protected Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query) 
    throws IOException {
    FsInput fsInput = createFsInput();
    DataFileReader<T> reader = createReader(fsInput);
    return new DataFileAvroResult<K, T>(this, query, reader, fsInput
        , query.getStart(), query.getLength());
  }
  
  private DataFileReader<T> createReader(FsInput fsInput) throws IOException {
    return new DataFileReader<T>(fsInput, getDatumReader());
  }
  
  private FsInput createFsInput() throws IOException {
    Path path = new Path(getInputPath());
    return new FsInput(path, getConf());
  }
  
  @Override
  public void flush() throws IOException {
    super.flush();
    if(writer != null) {
      writer.flush();
    }
  }
  
  @Override
  public void close() throws IOException {
    if(writer != null)  
      writer.close(); //hadoop 0.20.2 HDFS streams do not allow 
                      //to close twice, so close the writer first 
    writer = null;
    super.close();
  }
}
