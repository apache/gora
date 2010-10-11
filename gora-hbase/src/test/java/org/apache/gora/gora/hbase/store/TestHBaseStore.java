
package org.gora.hbase.store;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.hbase.GoraHBaseTestDriver;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestBase;

/**
 * Test case for HBaseStore.
 */
public class TestHBaseStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraHBaseTestDriver());
  }
    
  @Override
  protected DataStore<String, Employee> createEmployeeDataStore()
      throws IOException {
    return DataStoreFactory.createDataStore(HBaseStore.class, String.class, 
        Employee.class);
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore()
      throws IOException {
    return DataStoreFactory.createDataStore(HBaseStore.class, String.class, 
        WebPage.class);
  }

  public GoraHBaseTestDriver getTestDriver() {
    return (GoraHBaseTestDriver) testDriver;
  }
  
  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    HBaseAdmin admin = getTestDriver().getHbaseUtil().getHBaseAdmin();
    Assert.assertTrue(admin.tableExists(schemaName));
  }

  @Override
  public void assertPutArray() throws IOException { 
    HTable table = new HTable("WebPage");
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    Assert.assertEquals(result.getFamilyMap(Bytes.toBytes("parsedContent")).size(), 4);
    Assert.assertTrue(Arrays.equals(result.getValue(Bytes.toBytes("parsedContent")
        ,Bytes.toBytes(0)), Bytes.toBytes("example")));
    
    Assert.assertTrue(Arrays.equals(result.getValue(Bytes.toBytes("parsedContent")
        ,Bytes.toBytes(3)), Bytes.toBytes("example.com")));
    table.close();
  }
  
  
  @Override
  public void assertPutBytes(byte[] contentBytes) throws IOException {    
    HTable table = new HTable("WebPage");
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    byte[] actualBytes = result.getValue(Bytes.toBytes("content"), null);
    Assert.assertNotNull(actualBytes);
    Assert.assertTrue(Arrays.equals(contentBytes, actualBytes));
    table.close();
  }
  
  @Override
  public void assertPutMap() throws IOException {
    HTable table = new HTable("WebPage");
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    byte[] anchor2Raw = result.getValue(Bytes.toBytes("outlinks")
        , Bytes.toBytes("http://example2.com"));
    Assert.assertNotNull(anchor2Raw);
    String anchor2 = Bytes.toString(anchor2Raw);
    Assert.assertEquals("anchor2", anchor2);
    table.close();
  }
  
  public static void main(String[] args) throws Exception {
    TestHBaseStore test = new TestHBaseStore();
    test.setUpClass();
    test.setUp();
    
    test.testQuery();
    
    test.tearDown();
    test.tearDownClass();
  }
}
