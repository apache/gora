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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.gora.util.ByteUtils;

public class Mutate {
  private Map<String, List<Mutation>> mutationMap;

  public Mutate() {
    mutationMap = new HashMap<String, List<Mutation>>();
  }

  private void addToMutationMap(String columnFamily, Mutation mutation) {
    List<Mutation> mutationList = mutationMap.get(columnFamily);
    if (mutationList == null) {
      mutationList = new ArrayList<Mutation>();
      mutationMap.put(columnFamily, mutationList);
    }
    mutationList.add(mutation);
  }

  public Mutate put(String columnFamily, String columnName,
      byte[] value) {
    return put(columnFamily, columnName, value, System.currentTimeMillis());
  }

  public Mutate put(String columnFamily, String columnName,
      byte[] value, long timestamp) {

    Mutation mutation = new Mutation();
    ColumnOrSuperColumn csc = new ColumnOrSuperColumn();
    csc.column = new Column(ByteUtils.toBytes(columnName), value, timestamp);
    mutation.setColumn_or_supercolumn(csc);
    addToMutationMap(columnFamily, mutation);

    return this;
  }

  public Mutate put(String superColumnFamily, String superColumnName,
      String columnName, byte[] value) {
    return put(superColumnFamily, superColumnName, columnName, value,
        System.currentTimeMillis());
  }

  public Mutate put(String superColumnFamily, String superColumnName,
      String columnName, byte[] value, long timestamp) {

    Mutation mutation = new Mutation();
    ColumnOrSuperColumn csc = new ColumnOrSuperColumn();
    csc.super_column = new SuperColumn();
    csc.super_column.name = ByteUtils.toBytes(superColumnName);
    // TODO: This will probably be slow. Try to group all columns
    // under a supercolumn within a single mutation object
    csc.super_column.addToColumns(
        new Column(ByteUtils.toBytes(columnName), value, timestamp));
    mutation.setColumn_or_supercolumn(csc);
    addToMutationMap(superColumnFamily, mutation);

    return this;
  }

  public Mutate deleteAll(String columnFamily) {
    Deletion deletion = new Deletion();
    deletion.setTimestamp(Long.MAX_VALUE); //TODO: check this
    deletion.predicate = new SlicePredicate();
    SliceRange sliceRange =
      new SliceRange(new byte[0], new byte[0], false, Integer.MAX_VALUE);
    deletion.predicate.slice_range = sliceRange;
    Mutation mutation = new Mutation();
    mutation.deletion = deletion;
    addToMutationMap(columnFamily, mutation);
    return this;
  }


  public Mutate delete(String columnFamily, String columnName) {
    Deletion deletion = new Deletion().setTimestamp(Long.MAX_VALUE);
    // TODO: This will also probably be slow. Try to group
    // deletes together
    deletion.predicate = new SlicePredicate();
    deletion.predicate.addToColumn_names(ByteUtils.toBytes(columnName));

    Mutation mutation = new Mutation();
    mutation.deletion = deletion;
    addToMutationMap(columnFamily, mutation);
    return this;
  }

  public Mutate delete(String superColumnFamily, String superColumnName,
      String columnName) {
    Deletion deletion = new Deletion().setTimestamp(Long.MAX_VALUE);
    // TODO: This will also probably be slow. Try to group
    // deletes together
    deletion.predicate = new SlicePredicate();
    deletion.super_column = ByteUtils.toBytes(superColumnName);
    deletion.predicate.addToColumn_names(ByteUtils.toBytes(columnName));

    Mutation mutation = new Mutation();
    mutation.deletion = deletion;
    addToMutationMap(superColumnFamily, mutation);
    return this;
  }

  /*package*/ Map<String, List<Mutation>> getMutationMap() {
    return mutationMap;
  }
  
  public boolean isEmpty() {
    return mutationMap.isEmpty();
  }
}
