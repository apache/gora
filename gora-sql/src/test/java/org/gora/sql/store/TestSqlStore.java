
package org.gora.sql.store;

import java.io.IOException;

import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.sql.GoraSqlTestDriver;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestBase;

/**
 * Test case for {@link SqlStore}
 */
public class TestSqlStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraSqlTestDriver());
  }

  public TestSqlStore() {
  }

  @Override
  protected DataStore<String, Employee> createEmployeeDataStore() throws IOException {
    SqlStore<String, Employee> store = new SqlStore<String, Employee>();
    store.initialize(String.class, Employee.class, DataStoreFactory.properties);
    return store;
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() throws IOException {
    SqlStore<String, WebPage> store = new SqlStore<String, WebPage>();
    store.initialize(String.class, WebPage.class, DataStoreFactory.properties);
    return store;
  }

  //@Override
  public void testDeleteByQueryFields() {
    //TODO: implement delete fields in SqlStore
  }

  //@Override
  public void testDeleteByQuery() throws IOException {
    //HSQLDB somehow hangs for this test. we need to solve the issue or switch to
    //another embedded db.
  }

  public static void main(String[] args) throws Exception {
    TestSqlStore test = new TestSqlStore();
    TestSqlStore.setUpClass();
    test.setUp();
    test.testDeleteByQuery();
    test.tearDown();
    TestSqlStore.tearDownClass();
  }
}
