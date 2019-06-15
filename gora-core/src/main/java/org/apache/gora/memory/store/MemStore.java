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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.avro.Schema.Field;
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
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory based {@link DataStore} implementation for tests.
 */
public class MemStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {
  
  private static final Logger LOG = LoggerFactory.getLogger(MemStore.class); 

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

    @Override
    public int size() {
      int totalSize = map.navigableKeySet().size();
      int intLimit = (int) this.limit;
      return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
    }
  }

  // This map behaves like DB, has to be static and concurrent collection
  @SuppressWarnings("rawtypes")
  public static ConcurrentSkipListMap map = new ConcurrentSkipListMap();

  @Override
  public String getSchemaName() {
    return "MemStore";
  }

  @Override
  public boolean delete(K key) {
    return map.remove(key) != null;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    try {
      long deletedRows = 0;
      Result<K, T> result = query.execute();

      String[] fields = getFieldsToQuery(query.getFields());
      boolean isAllFields = Arrays.equals(fields, getFields());

      while (result.next()) {
        if (isAllFields) {
          if (delete(result.getKey())) {
            deletedRows++;
          }
        } else {
          ArrayList<String> excludedFields = new ArrayList<>();
          for (String field : getFields()) {
            if (!Arrays.asList(fields).contains(field)) {
              excludedFields.add(field);
            }
          }
          T newClonedObj = getPersistent(result.get(),excludedFields.toArray(new String[excludedFields.size()]));
          if (delete(result.getKey())) {
            put(result.getKey(),newClonedObj);
            deletedRows++;
          }
        }
      }
      return deletedRows;
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * An important feature of {@link MemStore#execute(Query)} is
   * that when specifying the {@link MemQuery} one should be aware 
   * that when fromKey and toKey are equal, the returned map is empty 
   * unless fromInclusive and toInclusive are both true. On the other hand
   * if either or both of fromKey and toKey are null we return no results.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    K startKey = query.getStartKey();
    K endKey = query.getEndKey();
    if(startKey == null) {
      if (!map.isEmpty()) {
        startKey = (K) map.firstKey();
      }
    }
    if(endKey == null) {
      if (!map.isEmpty()) {
        endKey = (K) map.lastKey();
      }
    }

    //check if query.fields is null
    query.setFields(getFieldsToQuery(query.getFields()));
    NavigableMap<K,T> submap = null;
    if (startKey != null && endKey != null) {
      try {
        submap =  map.subMap(startKey, true, endKey, true);
      } catch (Exception e) {
        throw new GoraException(e);
      }
    } else {
      // Empty
      submap = Collections.emptyNavigableMap() ;
    }
    
    return new MemResult<>(this, query, submap);
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

  @SuppressWarnings("unchecked")
  @Override
  public boolean exists(K key) {
    return map.containsKey(key);
  }

  /**
   * Returns a clone with exactly the requested fields shallowly copied
   */
  private static<T extends PersistentBase> T getPersistent(T obj, String[] fields) {
    List<Field> otherFields = obj.getSchema().getFields();
    String[] otherFieldStrings = new String[otherFields.size()];
    for(int i = 0; i<otherFields.size(); i++ ){
      otherFieldStrings[i] = otherFields.get(i).name();
    }
    if(Arrays.equals(fields, otherFieldStrings)) { 
      return obj;
    }
    T newObj = AvroUtils.deepClonePersistent(obj);
    newObj.clear();
    for (String field : fields) {
      Field otherField = obj.getSchema().getField(field);
      int index = otherField.pos();
      newObj.put(index, obj.get(index));
    }
    return newObj;
  }

  @Override
  public Query<K, T> newQuery() {
    return new MemQuery<>(this);
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
    List<PartitionQuery<K, T>> list = new ArrayList<>();
    PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<>(query);
    pqi.setConf(getConf());
    list.add(pqi);
    return list;
  }

  @Override
  public void close() {
  }

  /**
   * As MemStore is basically an implementation of
   * {@link java.util.concurrent.ConcurrentSkipListMap}
   * it has no concept of a schema.
   */
  @Override
  public void createSchema() { }

  @Override
  public void deleteSchema() {
    if (!map.isEmpty()) {
      map.clear();
    }
  }

  @Override
  public boolean schemaExists() {
    return true;
  }

  @Override
  public void flush() { }
}
