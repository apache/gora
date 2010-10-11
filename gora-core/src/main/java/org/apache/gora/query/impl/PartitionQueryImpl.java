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
import java.util.Arrays;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.IOUtils;

/**
 * Implementation for {@link PartitionQuery}.
 */
public class PartitionQueryImpl<K, T extends Persistent>
  extends QueryBase<K, T> implements PartitionQuery<K, T> {

  protected Query<K, T> baseQuery;
  protected String[] locations;

  public PartitionQueryImpl() {
    super(null);
  }

  public PartitionQueryImpl(Query<K, T> baseQuery, String... locations) {
    this(baseQuery, null, null, locations);
  }

  public PartitionQueryImpl(Query<K, T> baseQuery, K startKey, K endKey,
      String... locations) {
    super(baseQuery.getDataStore());
    this.baseQuery = baseQuery;
    this.locations = locations;
    setStartKey(startKey);
    setEndKey(endKey);
    this.dataStore = baseQuery.getDataStore();
  }

  @Override
public String[] getLocations() {
    return locations;
  }

  public Query<K, T> getBaseQuery() {
    return baseQuery;
  }

  /* Override everything except start-key/end-key */

  @Override
  public String[] getFields() {
    return baseQuery.getFields();
  }

  @Override
  public DataStore<K, T> getDataStore() {
    return baseQuery.getDataStore();
  }

  @Override
  public long getTimestamp() {
    return baseQuery.getTimestamp();
  }

  @Override
  public long getStartTime() {
    return baseQuery.getStartTime();
  }

  @Override
  public long getEndTime() {
    return baseQuery.getEndTime();
  }

  @Override
  public long getLimit() {
    return baseQuery.getLimit();
  }

  @Override
  public void setFields(String... fields) {
    baseQuery.setFields(fields);
  }

  @Override
  public void setTimestamp(long timestamp) {
    baseQuery.setTimestamp(timestamp);
  }

  @Override
  public void setStartTime(long startTime) {
    baseQuery.setStartTime(startTime);
  }

  @Override
  public void setEndTime(long endTime) {
    baseQuery.setEndTime(endTime);
  }

  @Override
  public void setTimeRange(long startTime, long endTime) {
    baseQuery.setTimeRange(startTime, endTime);
  }

  @Override
  public void setLimit(long limit) {
    baseQuery.setLimit(limit);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    super.write(out);
    IOUtils.serialize(null, out, baseQuery);
    IOUtils.writeStringArray(out, locations);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    super.readFields(in);
    try {
      baseQuery = IOUtils.deserialize(null, in, null);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
    locations = IOUtils.readStringArray(in);
    //we should override the data store as basequery's data store
    //also we may not call super.readFields so that temporary this.dataStore
    //is not created at all
    this.dataStore = baseQuery.getDataStore();
  }

  @Override
  @SuppressWarnings({ "rawtypes" })
  public boolean equals(Object obj) {
    if(obj instanceof PartitionQueryImpl) {
      PartitionQueryImpl that = (PartitionQueryImpl) obj;
      return this.baseQuery.equals(that.baseQuery)
        && Arrays.equals(locations, that.locations);
    }
    return false;
  }
}
