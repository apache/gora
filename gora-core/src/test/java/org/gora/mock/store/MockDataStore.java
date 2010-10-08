
package org.gora.mock.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gora.mock.persistency.MockPersistent;
import org.gora.mock.query.MockQuery;
import org.gora.query.PartitionQuery;
import org.gora.query.Query;
import org.gora.query.Result;
import org.gora.query.impl.PartitionQueryImpl;
import org.gora.store.DataStoreFactory;
import org.gora.store.impl.DataStoreBase;

public class MockDataStore extends DataStoreBase<String, MockPersistent> {

  public static final int NUM_PARTITIONS = 5;
  public static final String[] LOCATIONS = {"foo1", "foo2", "foo3", "foo4", "foo1"};

  public static MockDataStore get() {
    MockDataStore dataStore = DataStoreFactory.getDataStore(MockDataStore.class
        , String.class, MockPersistent.class);
    return dataStore;
  }

  public MockDataStore() { }

  @Override
  public String getSchemaName() {
    return null;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void createSchema() throws IOException {
  }

  @Override
  public void deleteSchema() throws IOException {
  }

  @Override
  public void truncateSchema() throws IOException {
  }

  @Override
  public boolean schemaExists() throws IOException {
    return true;
  }

  @Override
  public boolean delete(String key) throws IOException {
    return false;
  }

  @Override
  public long deleteByQuery(Query<String, MockPersistent> query)
      throws IOException {
    return 0;
  }

  @Override
  public Result<String, MockPersistent> execute(
      Query<String, MockPersistent> query) throws IOException {
    return null;
  }

  @Override
  public void flush() throws IOException {
  }

  @Override
  public MockPersistent get(String key, String[] fields) throws IOException {
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
  public void put(String key, MockPersistent obj) throws IOException {
  }

  @Override
  public void setKeyClass(Class<String> keyClass) {
  }

  @Override
  public void setPersistentClass(Class<MockPersistent> persistentClass) {
  }
}
