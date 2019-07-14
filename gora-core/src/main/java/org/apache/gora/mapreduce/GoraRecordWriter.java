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

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hadoop record writer that flushes the Gora datastore regularly.
 *
 */
public class GoraRecordWriter<K, T> extends RecordWriter<K, T> {
  public static final Logger LOG = LoggerFactory.getLogger(GoraRecordWriter.class);
  
  private static final String BUFFER_LIMIT_WRITE_NAME = "gora.buffer.write.limit";
  private static final int BUFFER_LIMIT_WRITE_VALUE = 10000;

  private DataStore<K, Persistent> store;
  private GoraRecordCounter counter = new GoraRecordCounter();

  public GoraRecordWriter(DataStore<K, Persistent> store, TaskAttemptContext context) {
    this.store = store;
    
    Configuration configuration = context.getConfiguration();
    int recordsMax = configuration.getInt(BUFFER_LIMIT_WRITE_NAME, BUFFER_LIMIT_WRITE_VALUE);
    counter.setRecordsMax(recordsMax);
    LOG.info("gora.buffer.write.limit = " + recordsMax);
  }

  @Override
  public void close(TaskAttemptContext context) throws IOException,
      InterruptedException {
    try{
      store.close();
    }catch(Exception e){
      LOG.warn("Exception at GoraRecordWriter.class while closing datastore: {}", e.getMessage());
      LOG.warn("Trace: {}", e.getStackTrace());
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(K key, T value) throws IOException, InterruptedException {
    try{
      store.put(key, (Persistent) value);
      counter.increment();
      if (counter.isModulo()) {
        LOG.info("Flushing the datastore after " + counter.getRecordsNumber() + " records");
        store.flush();
      }
    }catch(Exception e){
      LOG.warn("Exception at GoraRecordWriter.class while writing to datastore: {}", e.getMessage());
      LOG.warn("Trace: {}", e.getStackTrace());
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Method that deletes an entity from a store by key.
   * Needed for gora-pig.
   * 
   * @param key
   * @throws IOException
   */
  public void delete(K key) throws IOException {
    try{
      store.delete(key) ;
      counter.increment();
      if (counter.isModulo()) {
        LOG.info("Flushing the datastore after " + counter.getRecordsNumber() + " records");
        store.flush();
      }
    }catch(Exception e){
      LOG.error("Exception at GoraRecordWriter#delete() while deleting to datastore." + e.getMessage());
      throw new IOException(e) ;
    }
  }

}
