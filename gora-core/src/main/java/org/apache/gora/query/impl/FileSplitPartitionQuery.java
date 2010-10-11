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

package org.apache.gora.query.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * Keeps a {@link FileSplit} to represent the partition boundaries.
 * FileSplitPartitionQuery is best used with existing {@link InputFormat}s.
 */
public class FileSplitPartitionQuery<K, T extends Persistent>
  extends PartitionQueryImpl<K,T> {

  private FileSplit split;

  public FileSplitPartitionQuery() {
    super();
  }

  public FileSplitPartitionQuery(Query<K, T> baseQuery, FileSplit split)
    throws IOException {
    super(baseQuery, split.getLocations());
    this.split = split;
  }

  public FileSplit getSplit() {
    return split;
  }

  public long getLength() {
    return split.getLength();
  }

  public long getStart() {
    return split.getStart();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    split.write(out);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    if(split == null)
      split = new FileSplit(null, 0, 0, null); //change to new FileSplit() once hadoop-core.jar is updated
    split.readFields(in);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof FileSplitPartitionQuery) {
      return super.equals(obj) &&
      this.split.equals(((FileSplitPartitionQuery)obj).split);
    }
    return false;
  }

}
