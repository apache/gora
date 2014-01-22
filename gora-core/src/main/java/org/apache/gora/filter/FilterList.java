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

package org.apache.gora.filter;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.ReflectionUtils;
import org.apache.hadoop.io.Text;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilterList<K, T extends PersistentBase> implements Filter<K, T> {
  /** set operator */
  public static enum Operator {
    /** !AND */
    MUST_PASS_ALL,
    /** !OR */
    MUST_PASS_ONE
  }
  
  private Operator operator = Operator.MUST_PASS_ALL;
  private List<Filter<K, T>> filters = new ArrayList<Filter<K, T>>();
  
  public FilterList() {
  }
  
  public FilterList(final List<Filter<K, T>> rowFilters) {
    this.filters = rowFilters;
  }
  
  public FilterList(final Operator operator) {
    this.operator = operator;
  }
  
  public FilterList(final Operator operator, final List<Filter<K, T>> rowFilters) {
    this.filters = rowFilters;
    this.operator = operator;
  }

  public List<Filter<K, T>> getFilters() {
    return filters;
  }
  
  public Operator getOperator() {
    return operator;
  }
  
  public void addFilter(Filter<K, T> filter) {
    this.filters.add(filter);
  }
  
  @Override
  public void readFields(DataInput in) throws IOException {
    byte opByte = in.readByte();
    operator = Operator.values()[opByte];
    int size = in.readInt();
    if (size > 0) {
      filters = new ArrayList<Filter<K, T>>(size);
      try {
        for (int i = 0; i < size; i++) {
          @SuppressWarnings("unchecked")
          Class<? extends Filter<K, T>> cls = (Class<? extends Filter<K, T>>) Class.forName(Text.readString(in)).asSubclass(Filter.class);
          Filter<K, T> filter = ReflectionUtils.newInstance(cls);
          filter.readFields(in);
          filters.add(filter);
        }
      } catch (Exception e) {
        throw (IOException)new IOException("Failed filter init").initCause(e);
      }
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeByte(operator.ordinal());
    out.writeInt(filters.size());
    for (Filter<K, T> filter : filters) {
      Text.writeString(out, filter.getClass().getName());
      filter.write(out);
    }
  }

  @Override
  public boolean filter(K key, T persistent) {
    // TODO not yet implemented
    return false;
  }

}
