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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configurable; 
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * Base class for Query implementations.
 */
public abstract class QueryBase<K, T extends PersistentBase>  
implements Query<K,T>, Writable, Configurable {
	
  protected DataStoreBase<K,T> dataStore;

  protected String queryString;
  protected String[] fields;

  protected K startKey;
  protected K endKey;

  protected long startTime = -1;
  protected long endTime = -1;

  protected String filter;

  protected long limit = -1;

  protected boolean isCompiled = false;

  private Configuration conf;

  public QueryBase(DataStore<K,T> dataStore) {
    this.dataStore = (DataStoreBase<K, T>)dataStore;
  }

  @Override
  public Result<K,T> execute() throws Exception, IOException {
    //compile();
    return dataStore.execute(this);
  }

//  @Override
//  public void compile() {
//    if(!isCompiled) {
//      isCompiled = true;
//    }
//  }

  @Override
  public void setDataStore(DataStore<K, T> dataStore) {
    this.dataStore = (DataStoreBase<K, T>)dataStore;
  }

  @Override
  public DataStore<K, T> getDataStore() {
    return dataStore;
  }

//  @Override
//  public void setQueryString(String queryString) {
//    this.queryString = queryString;
//  }
//
//  @Override
//  public String getQueryString() {
//    return queryString;
//  }

  @Override
  public void setFields(String... fields) {
    this.fields = fields;
  }

  @Override
public String[] getFields() {
    return fields;
  }

  @Override
  public void setKey(K key) {
    setKeyRange(key, key);
  }

  @Override
  public void setStartKey(K startKey) {
    this.startKey = startKey;
  }

  @Override
  public void setEndKey(K endKey) {
    this.endKey = endKey;
  }

  @Override
  public void setKeyRange(K startKey, K endKey) {
    this.startKey = startKey;
    this.endKey = endKey;
  }

  @Override
  public K getKey() {
    if(startKey == endKey) {
      return startKey; //address comparison
    }
    return null;
  }

  @Override
  public K getStartKey() {
    return startKey;
  }

  @Override
  public K getEndKey() {
    return endKey;
  }

  @Override
  public void setTimestamp(long timestamp) {
    setTimeRange(timestamp, timestamp);
  }

  @Override
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  @Override
  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  @Override
  public void setTimeRange(long startTime, long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public long getTimestamp() {
    return startTime == endTime ? startTime : -1;
  }

  @Override
  public long getStartTime() {
    return startTime;
  }

  @Override
  public long getEndTime() {
    return endTime;
  }

//  @Override
//  public void setFilter(String filter) {
//    this.filter = filter;
//  }
//
//  @Override
//  public String getFilter() {
//    return filter;
//  }

  @Override
  public void setLimit(long limit) {
    this.limit = limit;
  }

  @Override
  public long getLimit() {
    return limit;
  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readFields(DataInput in) throws IOException {
    String dataStoreClass = Text.readString(in);
    try {
      dataStore = (DataStoreBase<K, T>) ReflectionUtils.newInstance(ClassLoadingUtils.loadClass(dataStoreClass), conf);
      dataStore.readFields(in);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }

    boolean[] nullFields = IOUtils.readNullFieldsInfo(in);

    if(!nullFields[0])
      queryString = Text.readString(in);
    if(!nullFields[1])
      fields = IOUtils.readStringArray(in);
    if(!nullFields[2])
      startKey = IOUtils.deserialize(null, in, null, dataStore.getKeyClass());
    if(!nullFields[3])
      endKey = IOUtils.deserialize(null, in, null, dataStore.getKeyClass());
    if(!nullFields[4])
      filter = Text.readString(in);

    startTime = WritableUtils.readVLong(in);
    endTime = WritableUtils.readVLong(in);
    limit = WritableUtils.readVLong(in);
  }

  //@Override
  public void write(DataOutput out) throws IOException {
    //write datastore
    Text.writeString(out, dataStore.getClass().getCanonicalName());
    dataStore.write(out);

    IOUtils.writeNullFieldsInfo(out, queryString, (fields)
        , startKey, endKey, filter);

    if(queryString != null)
      Text.writeString(out, queryString);
    if(fields != null)
      IOUtils.writeStringArray(out, fields);
    if(startKey != null)
      IOUtils.serialize(getConf(), out, startKey, dataStore.getKeyClass());
    if(endKey != null)
      IOUtils.serialize(getConf(), out, endKey, dataStore.getKeyClass());
    if(filter != null)
      Text.writeString(out, filter);

    WritableUtils.writeVLong(out, getStartTime());
    WritableUtils.writeVLong(out, getEndTime());
    WritableUtils.writeVLong(out, getLimit());
  }

  @SuppressWarnings({ "rawtypes" })
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof QueryBase) {
      QueryBase that = (QueryBase) obj;
      EqualsBuilder builder = new EqualsBuilder();
      builder.append(dataStore, that.dataStore);
      builder.append(queryString, that.queryString);
      builder.append(fields, that.fields);
      builder.append(startKey, that.startKey);
      builder.append(endKey, that.endKey);
      builder.append(filter, that.filter);
      builder.append(limit, that.limit);
      return builder.isEquals();
    }
    return false;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(dataStore);
    builder.append(queryString);
    builder.append(fields);
    builder.append(startKey);
    builder.append(endKey);
    builder.append(filter);
    builder.append(limit);
    return builder.toHashCode();
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("dataStore", dataStore);
    builder.append("fields", fields);
    builder.append("startKey", startKey);
    builder.append("endKey", endKey);
    builder.append("filter", filter);
    builder.append("limit", limit);

    return builder.toString();
  }
}
