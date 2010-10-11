
package org.gora.memory.store;

import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestBase;

/**
 * Test case for {@link MemStore}.
 */
public class TestMemStore extends DataStoreTestBase {

  @SuppressWarnings("unchecked")
  @Override
  protected DataStore<String, Employee> createEmployeeDataStore() {
    return DataStoreFactory.getDataStore(MemStore.class, String.class, Employee.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() {
    return DataStoreFactory.getDataStore(MemStore.class, String.class, WebPage.class);
  }
}
