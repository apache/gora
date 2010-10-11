
package org.gora.mapreduce;

import junit.framework.Assert;

import org.apache.avro.util.Utf8;
import org.gora.examples.WebPageDataCreator;
import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.memory.store.MemStore;
import org.gora.query.Result;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestUtil;
import org.gora.util.TestIOUtils;
import org.junit.Test;

/** Test class for {@link PersistentSerialization}, {@link PersistentSerializer}
 *  and {@link PersistentDeserializer}
 */
public class TestPersistentSerialization {

  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeEmployee() throws Exception {

    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class);

    Employee employee = DataStoreTestUtil.createEmployee(store);

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @Test
  public void testSerdeEmployeeOneField() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @Test
  public void testSerdeEmployeeTwoFields() throws Exception {
    Employee employee = new Employee();
    employee.setSsn(new Utf8("11111"));
    employee.setSalary(100);

    TestIOUtils.testSerializeDeserialize(employee);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSerdeWebPage() throws Exception {

    MemStore<String, WebPage> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, WebPage.class);
    WebPageDataCreator.createWebPageData(store);

    Result<String, WebPage> result = store.newQuery().execute();

    int i=0;
    while(result.next()) {
      WebPage page = result.get();
      TestIOUtils.testSerializeDeserialize(page);
      i++;
    }
    Assert.assertEquals(WebPageDataCreator.URLS.length, i);
  }

  @Test
  public void testSerdeMultipleWebPages() throws Exception {
    WebPage page1 = new WebPage();
    WebPage page2 = new WebPage();
    WebPage page3 = new WebPage();

    page1.setUrl(new Utf8("foo"));
    page2.setUrl(new Utf8("baz"));
    page3.setUrl(new Utf8("bar"));

    page1.addToParsedContent(new Utf8("coo"));

    page2.putToOutlinks(new Utf8("a"), new Utf8("b"));

    TestIOUtils.testSerializeDeserialize(page1, page2, page3);
  }

}
