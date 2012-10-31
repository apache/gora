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

package org.apache.gora.store.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.gora.avro.store.AvroStore;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.FileBackedDataStore;
import org.apache.gora.util.OperationNotSupportedException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementations for {@link FileBackedDataStore} methods.
 */
public abstract class FileBackedDataStoreBase<K, T extends PersistentBase>
  extends DataStoreBase<K, T> implements FileBackedDataStore<K, T> {

  protected long inputSize; //input size in bytes

  protected String inputPath;
  protected String outputPath;

  protected InputStream inputStream;
  protected OutputStream outputStream;
  
  public static final Logger LOG = LoggerFactory.getLogger(AvroStore.class);

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
    if(properties != null) {
      if(this.inputPath == null) {
        this.inputPath = DataStoreFactory.getInputPath(properties, this);
      }
      if(this.outputPath == null) {
        this.outputPath = DataStoreFactory.getOutputPath(properties, this);
      }
    }
  }

  @Override
public void setInputPath(String inputPath) {
    this.inputPath = inputPath;
  }

  @Override
public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }

  @Override
public String getInputPath() {
    return inputPath;
  }

  @Override
public String getOutputPath() {
    return outputPath;
  }

  @Override
public void setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
public InputStream getInputStream() {
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  /** Opens an InputStream for the input Hadoop path */
  protected InputStream createInputStream() throws IOException {
    //TODO: if input path is a directory, use smt like MultiInputStream to
    //read all the files recursively
    Path path = new Path(inputPath);
    FileSystem fs = path.getFileSystem(getConf());
    inputSize = fs.getFileStatus(path).getLen();
    return fs.open(path);
  }

  /** Opens an OutputStream for the output Hadoop path */
  protected OutputStream createOutputStream() {
    OutputStream conf = null;
    try{
      Path path = new Path(outputPath);
      FileSystem fs = path.getFileSystem(getConf());
      conf = fs.create(path);
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
    return conf;
  }

  protected InputStream getOrCreateInputStream() throws IOException {
    try{
      if(inputStream == null) {
        inputStream = createInputStream();
      }
      return inputStream;
    }catch(IOException ex){
      throw new IOException(ex);
    }
  }

  protected OutputStream getOrCreateOutputStream() throws IOException {
    if(outputStream == null) {
      outputStream = createOutputStream();
    }
    return outputStream;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query){
    List<InputSplit> splits = null;
    List<PartitionQuery<K, T>> queries = null;
    try{
      splits = GoraMapReduceUtils.getSplits(getConf(), inputPath);
      queries = new ArrayList<PartitionQuery<K,T>>(splits.size());
  
      for(InputSplit split : splits) {
        queries.add(new FileSplitPartitionQuery<K, T>(query, (FileSplit) split));
      }
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
    return queries;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    Result<K, T> results = null;
    try{
      if(query instanceof FileSplitPartitionQuery) {
        results = executePartial((FileSplitPartitionQuery<K, T>) query);
      } else {
        results = executeQuery(query);
      }
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
    return results;
  }

  /**
   * Executes a normal Query reading the whole data. #execute() calls this function
   * for non-PartitionQuery's.
   */
  protected abstract Result<K,T> executeQuery(Query<K,T> query)
    throws IOException;

  /**
   * Executes a PartitialQuery, reading the data between start and end.
   */
  protected abstract Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query)
    throws IOException;

  @Override
  public void flush() {
    try{
      if(outputStream != null)
        outputStream.flush();
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
  }

  @Override
  public void createSchema() {
  }

  @Override
  public void deleteSchema() {
    throw new OperationNotSupportedException("delete schema is not supported for " +
    		"file backed data stores");
  }

  @Override
  public boolean schemaExists() {
    return true;
  }

  @Override
  public void write(DataOutput out) {
    try{
      super.write(out);
      org.apache.gora.util.IOUtils.writeNullFieldsInfo(out, inputPath, outputPath);
      if(inputPath != null)
        Text.writeString(out, inputPath);
      if(outputPath != null)
        Text.writeString(out, outputPath);
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
  }

  @Override
  public void readFields(DataInput in) {
    try{
      super.readFields(in);
      boolean[] nullFields = org.apache.gora.util.IOUtils.readNullFieldsInfo(in);
      if(!nullFields[0])
        inputPath = Text.readString(in);
      if(!nullFields[1])
        outputPath = Text.readString(in);
    }catch(IOException ex){
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    }
  }

  @Override
  public void close() {
      IOUtils.closeStream(inputStream);
      IOUtils.closeStream(outputStream);
      inputStream = null;
      outputStream = null;
  }
}
