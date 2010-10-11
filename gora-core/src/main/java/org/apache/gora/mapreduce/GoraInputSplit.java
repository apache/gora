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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.gora.query.PartitionQuery;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

/**
 * InputSplit using {@link PartitionQuery}s. 
 */
public class GoraInputSplit extends InputSplit 
  implements Writable, Configurable {

  protected PartitionQuery<?,?> query;
  private Configuration conf;
  
  public GoraInputSplit() {
  }
  
  public GoraInputSplit(Configuration conf, PartitionQuery<?,?> query) {
    setConf(conf);
    this.query = query;
  }
  
  @Override
  public Configuration getConf() {
    return conf;
  }
  
  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  @Override
  public long getLength() throws IOException, InterruptedException {
    return 0;
  }

  @Override
  public String[] getLocations() {
    return query.getLocations();
  }

  public PartitionQuery<?, ?> getQuery() {
    return query;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    try {
      query = (PartitionQuery<?, ?>) IOUtils.deserialize(conf, in, null);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    IOUtils.serialize(getConf(), out, query);
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof GoraInputSplit) {
      return this.query.equals(((GoraInputSplit)obj).query);
    }
    return false;
  }
}