
package org.gora.persistency.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.util.Utf8;
import org.gora.examples.generated.Employee;
import org.gora.examples.generated.WebPage;
import org.gora.memory.store.MemStore;
import org.gora.store.DataStoreFactory;
import org.gora.store.DataStoreTestUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testcase for PersistentBase class
 */
public class TestPersistentBase {
  
  @Test
  public void testGetFields() {
    WebPage page = new WebPage();
    String[] fields = page.getFields();
    Assert.assertArrayEquals(WebPage._ALL_FIELDS, fields);
  }
  
  @Test
  public void testGetField() {
    WebPage page = new WebPage();
    for(int i=0; i<WebPage._ALL_FIELDS.length; i++) {
      String field = page.getField(i);
      Assert.assertEquals(WebPage._ALL_FIELDS[i], field);
    }
  }
  
  @Test
  public void testGetFieldIndex() {
    WebPage page = new WebPage();
    for(int i=0; i<WebPage._ALL_FIELDS.length; i++) {
      int index = page.getFieldIndex(WebPage._ALL_FIELDS[i]);
      Assert.assertEquals(i, index);
    }
  }
  
  @Test
  public void testFieldsWithTwoClasses() {
    WebPage page = new WebPage();
    for(int i=0; i<WebPage._ALL_FIELDS.length; i++) {
      int index = page.getFieldIndex(WebPage._ALL_FIELDS[i]);
      Assert.assertEquals(i, index);
    }
    Employee employee = new Employee();
    for(int i=0; i<Employee._ALL_FIELDS.length; i++) {
      int index = employee.getFieldIndex(Employee._ALL_FIELDS[i]);
      Assert.assertEquals(i, index);
    }
  }
  
  @Test
  public void testClear() {
    
    //test clear all fields
    WebPage page = new WebPage();
    page.setUrl(new Utf8("http://foo.com"));
    page.addToParsedContent(new Utf8("foo"));
    page.putToOutlinks(new Utf8("foo"), new Utf8("bar"));
    page.setContent(ByteBuffer.wrap("foo baz bar".getBytes()));
    
    page.clear();
    
    Assert.assertNull(page.getUrl());
    Assert.assertEquals(0, page.getParsedContent().size());
    Assert.assertEquals(0, page.getOutlinks().size());
    Assert.assertNull(page.getContent());
    
    //set fields again
    page.setUrl(new Utf8("http://bar.com"));
    page.addToParsedContent(new Utf8("bar"));
    page.putToOutlinks(new Utf8("bar"), new Utf8("baz"));
    page.setContent(ByteBuffer.wrap("foo baz bar barbaz".getBytes()));
    
    //test clear new object
    page = new WebPage();
    page.clear();
    
    //test primitive fields
    Employee employee = new Employee();
    employee.clear();
  }
  
  @Test
  public void testClone() throws IOException {
    //more tests for clone are in TestPersistentDatumReader
    @SuppressWarnings("unchecked")
    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class);

    Employee employee = DataStoreTestUtil.createEmployee(store);
    
    Assert.assertEquals(employee, employee.clone());
  }
}
