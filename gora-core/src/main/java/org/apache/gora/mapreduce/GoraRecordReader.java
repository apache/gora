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
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * An adapter for Result to Hadoop RecordReader.
 */
public class GoraRecordReader<K, T extends Persistent> 
extends RecordReader<K,T> {

  protected Query<K,T> query;
  protected Result<K,T> result;
  
  public GoraRecordReader(Query<K,T> query) {
    this.query = query;
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
    if(this.result == null) {
      executeQuery();
    }
    
    return result.next();
  }

  @Override
  public void close() throws IOException {
    result.close();
  }

}
