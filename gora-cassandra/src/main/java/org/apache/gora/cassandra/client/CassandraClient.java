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
package org.apache.gora.cassandra.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.TokenRange;

public interface CassandraClient {

  public void setKeySpace(String keySpace);

  public void setConsistencyLevel(ConsistencyLevel level);

  public Row get(String key, Select select) throws IOException;

  public RowIterable getRange(String startKey, String endKey, int rowCount,
      Select select)
  throws IOException;

  public RowIterable getTokenRange(String startToken, String endToken,
      int rowCount, Select select)
  throws IOException;

  public void mutate(String key, Mutate mutation) throws IOException;

  public Map<String, Map<String, String>> describeKeySpace()
  throws IOException;

  public List<TokenRange> describeRing() throws IOException;

  public List<String> describeSplits(String startToken, String endToken, int size)
  throws IOException;

  public void close();
}
