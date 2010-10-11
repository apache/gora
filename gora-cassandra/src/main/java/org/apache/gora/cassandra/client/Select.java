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

import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.gora.util.ByteUtils;

public class Select {

  private static final SlicePredicate ALL_PREDICATE;

  static {
    ALL_PREDICATE = new SlicePredicate();
    ALL_PREDICATE.setSlice_range(new SliceRange(new byte[0], new byte[0],
                                                false, Integer.MAX_VALUE));
  }

  private Map<ColumnParent, SlicePredicate> predicateMap;

  public Select() {
    predicateMap = new HashMap<ColumnParent, SlicePredicate>();
  }

  private SlicePredicate getOrCreate(ColumnParent columnParent) {
    SlicePredicate predicate = predicateMap.get(columnParent);
    if (predicate == null) {
      predicate = new SlicePredicate();
      predicateMap.put(columnParent, predicate);
    }
    return predicate;
  }

  public Select addColumnName(String superColumnFamily, String superColumn,
      String columnName) {
    ColumnParent parent = new ColumnParent(superColumnFamily);
    parent.setSuper_column(ByteUtils.toBytes(superColumn));
    SlicePredicate predicate = getOrCreate(parent);
    if (predicate.getSlice_range() != null) {
      // TODO: Make this another exception
      throw new RuntimeException("Can't add columns if slice_range is not null");
    }
    predicate.addToColumn_names(ByteUtils.toBytes(columnName));
    return this;
  }

  public Select addColumnName(String columnFamily, String columnName) {
    SlicePredicate predicate = getOrCreate(new ColumnParent(columnFamily));
    if (predicate.getSlice_range() != null) {
      // TODO: Make this another exception
      throw new RuntimeException("Can't add columns if slice_range is not null");
    }
    predicate.addToColumn_names(ByteUtils.toBytes(columnName));
    return this;
  }

  public Select addSuperColumnAll(String superColumnFamily) {
    return addColumnAll(superColumnFamily);
  }

  public Select addAllColumnsForSuperColumn(String superColumnFamily, String superColumnName) {
    ColumnParent parent = new ColumnParent(superColumnFamily);
    parent.setSuper_column(ByteUtils.toBytes(superColumnName));
    predicateMap.put(parent, ALL_PREDICATE);
    return this;
  }

  public Select addColumnAll(String columnFamily) {
    predicateMap.put(new ColumnParent(columnFamily), ALL_PREDICATE);
    return this;
  }

  /*package*/ Map<ColumnParent, SlicePredicate> getPredicateMap() {
    return predicateMap;
  }
}
