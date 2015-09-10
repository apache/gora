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

package org.apache.gora.persistency.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.avro.Schema.Field;
import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.memory.store.MemStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.hadoop.conf.Configuration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * Testcase for PersistentBase class
 */
public class TestPersistentBase {
  
  /**
   * Assert that the list of fields from the WebPage Schema
   * are as we expect. This is done by creating and accessing 
   * a WebPage object, then comparing the results against 
   * static fields of the WebPage.SCHEMA$.
   */
  @Test
  public void testGetFields() {
    WebPage page = WebPage.newBuilder().build();
    List<Field> fields = page.getSchema().getFields();
    assertEquals(WebPage.SCHEMA$.getFields(), fields);
  }
  
  /**
   * Assert that individual field values are as we would
   * expect from directly accessing WebPage.SCHEMA$ values.
   */
  @Test
  public void testGetField() {
    WebPage page = WebPage.newBuilder().build();
    for(int i=0; i<WebPage.SCHEMA$.getFields().toArray().length; i++) {
      Field field = page.getSchema().getFields().get(i);
      assertEquals(WebPage.SCHEMA$.getFields().get(i), field);
    }
  }
  
  /**
   * Assert that field positions as found within the SCHEMA array
   * are as we would expect by accessing them directly. 
   */
  @Test
  public void testGetFieldIndex() {
    WebPage page = WebPage.newBuilder().build();
    for(int i=0; i<WebPage.SCHEMA$.getFields().toArray().length; i++) {
      int index = page.getSchema().getFields().get(i).pos();
      assertEquals(i, index);
    }
  }
  
  /**
   * Assert that field positions as found within the SCHEMA array
   * are as we would expect by accessing them directly. 
   * This tests for both WebPage and Employee data beans.
   */
  @Test
  public void testFieldsWithTwoClasses() {
    WebPage page = WebPage.newBuilder().build();
    for(int i=0; i<WebPage.SCHEMA$.getFields().toArray().length; i++) {
      int index = page.getSchema().getFields().get(i).pos();
      assertEquals(i, index);
    }
    Employee employee = Employee.newBuilder().build();
    for(int i=0; i<Employee.SCHEMA$.getFields().toArray().length; i++) {
      int index = employee.getSchema().getFields().get(i).pos();
      assertEquals(i, index);
    }
  }
  
  /**
   * First we create a new WebPage object, to which we add some
   * field values. This makes the fields dirty as we have not 
   * flushed them to the datastore. We then clear the dirty
   * fields and assert that the values DO NOT exist for the 
   * field we previously made dirty.
   * We then set new values for fields, consequently making them 
   * dirty, before testing the clearing of an entirely new object
   * has all fields as null as they should be clean.
   */
  @Test
  public void testClear() {
    
    //test clear all fields
    WebPage page = WebPage.newBuilder().build();
   
    page.setUrl(new Utf8("http://foo.com"));
    page.getParsedContent().add(new Utf8("foo"));
    page.getOutlinks().put(new Utf8("foo"), new Utf8("bar"));
    page.setContent(ByteBuffer.wrap("foo baz bar".getBytes(Charset.defaultCharset())));
    
    page.clear();
    
    assertNull(page.getUrl());
    assertEquals(0, page.getParsedContent().size());
    assertEquals(0, page.getOutlinks().size());
    assertNull(page.getContent());
    
    //set fields again
    page.setUrl(new Utf8("http://bar.com"));
    page.getParsedContent().add(new Utf8("bar"));
    page.getOutlinks().put(new Utf8("bar"), new Utf8("baz"));
    page.setContent(ByteBuffer.wrap("foo baz bar barbaz".getBytes(Charset.defaultCharset())));
    
    //test clear new object
    page = WebPage.newBuilder().build();
    page.clear();
  }
  
  /**
   * Tests and asserts that an in-memory representation of the 
   * Employee object is Equal to a clone of the same object.
   * @throws IOException
   * @throws Exception
   */
  @Test
  public void testClone() throws Exception {
    //more tests for clone are in TestPersistentDatumReader
    @SuppressWarnings("unchecked")
    MemStore<String, Employee> store = DataStoreFactory.getDataStore(
        MemStore.class, String.class, Employee.class, new Configuration());

    Employee employee = DataStoreTestUtil.createEmployee();
    
    assertEquals(employee, Employee.newBuilder(employee).build());
  }
}
