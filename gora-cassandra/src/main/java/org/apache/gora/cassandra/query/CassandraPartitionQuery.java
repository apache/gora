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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.hadoop.io.Text;

public class CassandraPartitionQuery<K, T extends Persistent>
extends PartitionQueryImpl<K, T> {

  private String startToken;

  private String endToken;

  private String[] endPoints;

  private int splitSize;

  public CassandraPartitionQuery() {
    this.dataStore = null;
  }

  public CassandraPartitionQuery(Query<K, T> baseQuery, String startToken, String endToken, String[] endPoints,
      int splitSize) {
    super(baseQuery);
    this.startToken = startToken;
    this.endToken = endToken;
    this.endPoints = endPoints;
    this.splitSize = splitSize;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    Text.writeString(out, startToken);
    Text.writeString(out, endToken);
    out.writeInt(endPoints.length);
    for (String endPoint : endPoints) {
      Text.writeString(out, endPoint);
    }
    out.writeInt(splitSize);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    startToken = Text.readString(in);
    endToken = Text.readString(in);
    int size = in.readInt();
    endPoints = new String[size];
    for (int i = 0; i < size; i++) {
      endPoints[i] = Text.readString(in);
    }
    splitSize = in.readInt();
  }

  public String getStartToken() {
    return startToken;
  }

  public String getEndToken() {
    return endToken;
  }

  public String[] getEndPoints() {
    return endPoints;
  }

  public int getSplitSize() {
    return splitSize;
  }
}
