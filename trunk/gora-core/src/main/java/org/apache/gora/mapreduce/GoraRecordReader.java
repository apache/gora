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
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter for Result to Hadoop RecordReader.
 */
public class GoraRecordReader<K, T extends Persistent> extends RecordReader<K,T> {
  public static final Logger LOG = LoggerFactory.getLogger(GoraRecordReader.class);

  public static final String BUFFER_LIMIT_READ_NAME = "gora.buffer.read.limit";
  public static final int BUFFER_LIMIT_READ_VALUE = 10000;

  protected Query<K,T> query;
  protected Result<K,T> result;
  
  private GoraRecordCounter counter = new GoraRecordCounter();
  
  public GoraRecordReader(Query<K,T> query, TaskAttemptContext context) {
    this.query = query;

    Configuration configuration = context.getConfiguration();
    int recordsMax = configuration.getInt(BUFFER_LIMIT_READ_NAME, BUFFER_LIMIT_READ_VALUE);
    
    // Check if result set will at least contain 2 rows
    if (recordsMax <= 1) {
      LOG.info("Limit " + recordsMax + " changed to " + BUFFER_LIMIT_READ_VALUE);
      recordsMax = BUFFER_LIMIT_READ_VALUE;
    }
    
    counter.setRecordsMax(recordsMax);
    LOG.info("gora.buffer.read.limit = " + recordsMax);
    
    this.query.setLimit(recordsMax);
  }

  public void executeQuery() throws IOException {
    this.result = query.execute();
  }
  
  @Override
  public K getCurrentKey() throws IOException, InterruptedException {
    return result.getKey();
  }

  @Override
  public T getCurrentValue() throws IOException, InterruptedException {
    return result.get();
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return result.getProgress();
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context)
  throws IOException, InterruptedException { }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    if (counter.isModulo()) {
      boolean firstBatch = (this.result == null);
      if (! firstBatch) {
        this.query.setStartKey(this.result.getKey());
        if (this.query.getLimit() == counter.getRecordsMax()) {
          this.query.setLimit(counter.getRecordsMax() + 1);
        }
      }
      if (this.result != null) {
        this.result.close();
      }
      
      executeQuery();
      
      if (! firstBatch) {
        // skip first result
        this.result.next();
      }
    }
    
    counter.increment();
    return this.result.next();
  }

  @Override
  public void close() throws IOException {
    if (result != null) {
      result.close();
    }
  }

}
