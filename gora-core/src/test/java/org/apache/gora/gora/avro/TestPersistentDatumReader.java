
package org.gora.avro;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.gora.examples.WebPageDataCreator;
import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.memory.store.MemStore;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.Result;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestUtil;
import org.junit.Test;

/**
 * Test case for {@link PersistentDatumReader}.
 */
public class TestPersistentDatumReader {

  private PersistentDatumReader<WebPage> webPageDatumReader 
    = new PersistentDatumReader<WebPage>();
  
  private void testClone(Persistent persistent) throws IOException {
    Persistent cloned = webPageDatumReader.clone(persistent, persistent.getSchema());
    assertClone(persistent, cloned);
  }
  
  private void assertClone(Persistent persistent, Persistent cloned) {
    Assert.assertNotNull("cloned object is null", cloned);
    Assert.assertEquals("cloned object is not equal to original object", persistent, cloned);
  }
  
  @Test
  public void testCloneEmployee() throws Exception {
    @SuppressWarnings("unchecked")
    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class);

    Employee employee = DataStoreTestUtil.createEmployee(store);
    
    testClone(employee);
  }
  
  @Test
  public void testCloneEmployeeOneField() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));

    testClone(employee);
  }

  @Test
  public void testCloneEmployeeTwoFields() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));
    employee.setSalary(100);

    testClone(employee);
  }

  @Test
  public void testCloneWebPage() throws Exception {
    @SuppressWarnings("unchecked")
    DataStore<String, WebPage> store = DataStoreFactory.createDataStore(
        MemStore.class, String.class, WebPage.class);
    WebPageDataCreator.createWebPageData(store);

    Query<String, WebPage> query = store.newQuery();
    Result<String, WebPage> result = query.execute();
    
    int tested = 0;
    while(result.next()) {
      WebPage page = result.get();
      testClone(page);
      tested++;
    }
    Assert.assertEquals(WebPageDataCreator.URLS.length, tested);
  }
}
