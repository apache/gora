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

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SlicePredicate;

public class SliceGet implements Callable<Pair<ColumnParent, List<ColumnOrSuperColumn>>> {

  private Cassandra.Client client;
  private String keySpace;
  private String key;
  private ColumnParent parent;
  private SlicePredicate predicate;
  private ConsistencyLevel consistencyLevel;

  public SliceGet(Cassandra.Client client, String keySpace, String key,
      ColumnParent parent, SlicePredicate predicate,
      ConsistencyLevel consistencyLevel) {
    this.client = client;
    this.keySpace = keySpace;
    this.key = key;
    this.parent = parent;
    this.predicate = predicate;
    this.consistencyLevel = consistencyLevel;
  }

  @Override
  public Pair<ColumnParent, List<ColumnOrSuperColumn>> call()
  throws Exception {
    return new Pair<ColumnParent, List<ColumnOrSuperColumn>>(parent,
        client.get_slice(keySpace, key, parent, predicate, consistencyLevel));
  }

}
