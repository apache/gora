
package org.gora.avro.store;

import static org.gora.examples.WebPageDataCreator.URLS;
import static org.gora.examples.WebPageDataCreator.URL_INDEXES;
import static org.gora.examples.WebPageDataCreator.createWebPageData;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.gora.avro.store.AvroStore.CodecType;
import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.query.Query;
import org.gora.query.Result;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link AvroStore}.
 */
public class TestAvroStore {

  public static final String EMPLOYEE_OUTPUT =
    System.getProperty("test.build.data") + "/testavrostore/employee.data";
  public static final String WEBPAGE_OUTPUT =
    System.getProperty("test.build.data") + "/testavrostore/webpage.data";

  protected AvroStore<String,Employee> employeeStore;
  protected AvroStore<String,WebPage> webPageStore;
  protected Configuration conf = new Configuration();

  @Before
  public void setUp() throws Exception {
    employeeStore = createEmployeeDataStore();
    employeeStore.initialize(String.class, Employee.class, DataStoreFactory.properties);
    employeeStore.setOutputPath(EMPLOYEE_OUTPUT);
    employeeStore.setInputPath(EMPLOYEE_OUTPUT);

    webPageStore = new AvroStore<String, WebPage>();
    webPageStore.initialize(String.class, WebPage.class, DataStoreFactory.properties);
    webPageStore.setOutputPath(WEBPAGE_OUTPUT);
    webPageStore.setInputPath(WEBPAGE_OUTPUT);
  }

  @SuppressWarnings("unchecked")
  protected AvroStore<String, Employee> createEmployeeDataStore() {
    return (AvroStore<String, Employee>) DataStoreFactory.getDataStore(
        AvroStore.class, String.class, Employee.class);
  }

  protected AvroStore<String, WebPage> createWebPageDataStore() {
    return new AvroStore<String, WebPage>();
  }

  @After
  public void tearDown() throws Exception {
    deletePath(employeeStore.getOutputPath());
    deletePath(webPageStore.getOutputPath());

    employeeStore.close();
    webPageStore.close();
  }

  private void deletePath(String output) throws IOException {
    if(output != null) {
      Path path = new Path(output);
      path.getFileSystem(conf).delete(path, true);
    }
  }

  @Test
  public void testNewInstance() throws IOException {
    DataStoreTestUtil.testNewPersistent(employeeStore);
  }

  @Test
  public void testCreateSchema() throws IOException {
    DataStoreTestUtil.testCreateEmployeeSchema(employeeStore);
  }

  @Test
  public void testAutoCreateSchema() throws IOException {
    DataStoreTestUtil.testAutoCreateSchema(employeeStore);
  }

  @Test
  public void testPut() throws IOException {
    DataStoreTestUtil.testPutEmployee(employeeStore);
  }

  @Test
  public void testQuery() throws IOException {
    createWebPageData(webPageStore);
    webPageStore.close();

    webPageStore.setInputPath(webPageStore.getOutputPath());
    testQueryWebPages(webPageStore);
  }

  @Test
  public void testQueryBinaryEncoder() throws IOException {
    webPageStore.setCodecType(CodecType.BINARY);
    webPageStore.setInputPath(webPageStore.getOutputPath());

    createWebPageData(webPageStore);
    webPageStore.close();
    testQueryWebPages(webPageStore);
  }

  //AvroStore should be closed so that Hadoop file is completely flushed,
  //so below test is copied and modified to close the store after pushing data
  public static void testQueryWebPages(DataStore<String, WebPage> store)
  throws IOException {

    Query<String, WebPage> query = store.newQuery();
    Result<String, WebPage> result = query.execute();

    int i=0;
    while(result.next()) {
      WebPage page = result.get();
      DataStoreTestUtil.assertWebPage(page, URL_INDEXES.get(page.getUrl().toString()));
      i++;
    }
    Assert.assertEquals(i, URLS.length);
  }

}
