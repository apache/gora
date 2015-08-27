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

package org.apache.gora.memory.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;


import org.apache.avro.Schema.Field;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;

/**
 * Memory based {@link DataStore} implementation for tests.
 */
public class MemStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static class MemQuery<K, T extends PersistentBase> extends QueryBase<K, T> {
    public MemQuery() {
      super(null);
    }
    public MemQuery(DataStore<K, T> dataStore) {
      super(dataStore);
    }
  }

  public static class MemResult<K, T extends PersistentBase> extends ResultBase<K, T> {
    private NavigableMap<K, T> map;
    private Iterator<K> iterator;
    public MemResult(DataStore<K, T> dataStore, Query<K, T> query
        , NavigableMap<K, T> map) {
      super(dataStore, query);
      this.map = map;
      iterator = map.navigableKeySet().iterator();
    }
    //@Override
    public void close() { }
    
    @Override
    public float getProgress() throws IOException {
      return 0;
    }

    @Override
    protected void clear() {  } //do not clear the object in the store

    @Override
    public boolean nextInner() throws IOException {
      if(!iterator.hasNext()) {
        return false;
      }

      key = iterator.next();
      persistent = map.get(key);

      return true;
    }
  }

  // This map behaves like DB, has to be static and concurrent collection
  @SuppressWarnings("rawtypes")
  public static ConcurrentSkipListMap map = new ConcurrentSkipListMap();

  @Override
  public String getSchemaName() {
    return "default";
  }

  @Override
  public boolean delete(K key) {
    return map.remove(key) != null;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
  try{
    long deletedRows = 0;
      Result<K,T> result = query.execute();

      while(result.next()) {
        if(delete(result.getKey()))
          deletedRows++;
      }
      return deletedRows;
    } catch (Exception e) {
      return 0;
    }
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public Result<K, T> execute(Query<K, T> query) {
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    if(startKey == null) {
      startKey = (K) map.firstKey();
    }
    if(endKey == null) {
      endKey = (K) map.lastKey();
    }

    //check if query.fields is null
    query.setFields(getFieldsToQuery(query.getFields()));

    ConcurrentNavigableMap<K,T> submap = map.subMap(startKey, true, endKey, true);

    return new MemResult<K,T>(this, query, submap);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public T get(K key, String[] fields) {
    T obj = (T) map.get(key);
    if (obj == null) {
      return null;
    }
    return getPersistent(obj, getFieldsToQuery(fields));
  }

  /**
   * Returns a clone with exactly the requested fields shallowly copied
   */
  private static<T extends Persistent> T getPersistent(T obj, String[] fields) {
    List<Field> otherFields = obj.getSchema().getFields();
    String[] otherFieldStrings = new String[otherFields.size()];
    for(int i = 0; i<otherFields.size(); i++ ){
      otherFieldStrings[i] = otherFields.get(i).name();
    }
    if(Arrays.equals(fields, otherFieldStrings)) { 
      return obj;
    }
    T newObj = (T) AvroUtils.deepClonePersistent(obj); 
      for(int i = 0; i<otherFields.size(); i++) {
      int index = otherFields.get(i).pos(); 
      newObj.put(index, obj.get(index));
    }
    return newObj;
  }

  @Override
  public Query<K, T> newQuery() {
    return new MemQuery<K, T>(this);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void put(K key, T obj) {
    map.put(key, obj);
  }

  @Override
  /**
   * Returns a single partition containing the original query
   */
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query){
    List<PartitionQuery<K, T>> list = new ArrayList<PartitionQuery<K,T>>();
    PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<K, T>(query);
    pqi.setConf(getConf());
    list.add(pqi);
    return list;
  }

  @Override
  public void close() {
  }

  @Override
  public void createSchema() { }

  @Override
  public void deleteSchema() {
    map.clear();
  }

  @Override
  public boolean schemaExists() {
    return true;
  }

  @Override
  public void flush() {
    map.clear();
  }
}
