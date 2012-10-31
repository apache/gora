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

package org.apache.gora.mock.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.mock.query.MockQuery;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;

public class MockDataStore extends DataStoreBase<String, MockPersistent> {

  public static final int NUM_PARTITIONS = 5;
  public static final String[] LOCATIONS = {"foo1", "foo2", "foo3", "foo4", "foo1"};

  public static MockDataStore get() {
    MockDataStore dataStore;
    try {
      dataStore = DataStoreFactory.getDataStore(MockDataStore.class
          , String.class, MockPersistent.class, new Configuration());
      return dataStore;
    } catch (GoraException ex) {
      throw new RuntimeException(ex);
    }
  }

  public MockDataStore() { }

  @Override
  public String getSchemaName() {
    return null;
  }

  @Override
  public void close() {
  }

  @Override
  public void createSchema() {
  }

  @Override
  public void deleteSchema() {
  }

  @Override
  public void truncateSchema() {
  }

  @Override
  public boolean schemaExists() {
    return true;
  }

  @Override
  public boolean delete(String key) {
    return false;
  }

  @Override
  public long deleteByQuery(Query<String, MockPersistent> query) {
    return 0;
  }

  @Override
  public Result<String, MockPersistent> execute(Query<String, MockPersistent> query) {
    return null;
  }

  @Override
  public void flush() {
  }

  @Override
  public MockPersistent get(String key, String[] fields) {
    return null;
  }

  @Override
  public Class<String> getKeyClass() {
    return String.class;
  }

  @Override
  public List<PartitionQuery<String, MockPersistent>> getPartitions(
      Query<String, MockPersistent> query) throws IOException {

    ArrayList<PartitionQuery<String, MockPersistent>> list =
      new ArrayList<PartitionQuery<String,MockPersistent>>();

    for(int i=0; i<NUM_PARTITIONS; i++) {
      list.add(new PartitionQueryImpl<String, MockPersistent>(query, LOCATIONS[i]));
    }

    return list;
  }

  @Override
  public Class<MockPersistent> getPersistentClass() {
    return MockPersistent.class;
  }

  @Override
  public MockQuery newQuery() {
    return new MockQuery(this);
  }

  @Override
  public void put(String key, MockPersistent obj) {
  }

  @Override
  public void setKeyClass(Class<String> keyClass) {
  }

  @Override
  public void setPersistentClass(Class<MockPersistent> persistentClass) {
  }
}
