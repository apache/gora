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
package org.apache.gora.filter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.filter.FilterList.Operator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases which test and validate functionality
 * of {@link org.apache.gora.filter.FilterList}
 * 
 */
public class TestFilterList {

  private MapFieldValueFilter<String, WebPage> filter1 = null;

  private SingleFieldValueFilter<String, WebPage> filter2 = null;

  private FilterList<String, WebPage> filterList = null;
  
  private List<Filter<String, WebPage>> fList;

  /**
   * Setup involves creating a FilterList comprising two filters
   * which are applied with MUST_PASS_ALL (e.g. AND) set operator.
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    filter1 = new MapFieldValueFilter<>();
    filter2 = new SingleFieldValueFilter<>();
    fList = new ArrayList<>();
    fList.add(filter1);
    fList.add(filter2);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    filter1 = null;
    filter2 = null;
    filterList = null;
    fList = null;
  }

  /**
   * Test method for {@link org.apache.gora.filter.FilterList#getFilters()}.
   */
  @Test
  public void testGetFilters() {
    filterList = new FilterList<>(Operator.MUST_PASS_ALL, fList);
    List<Filter<String, WebPage>> copyList = filterList.getFilters();
    assertEquals(2, filterList.getFilters().size());
    assertEquals(2, copyList.size());
  }

  /**
   * Test method for {@link org.apache.gora.filter.FilterList#getOperator()}.
   */
  @Test
  public void testGetOperator() {
    filterList = new FilterList<>(Operator.MUST_PASS_ALL, fList);
    assertEquals(Operator.MUST_PASS_ALL, filterList.getOperator());
  }

  /**
   * Test method for {@link org.apache.gora.filter.FilterList#addFilter(org.apache.gora.filter.Filter)}.
   */
  @Test
  public void testAddFilter() {
    filterList = new FilterList<>(Operator.MUST_PASS_ALL, fList);
    int a = 3;
    for (int i = 0; i < 10; i++) {
      filterList.addFilter(new SingleFieldValueFilter<String, WebPage>());
      assertEquals(a++, filterList.getFilters().size());
    }
  }

  /**
   * Test method for verifying 
   * {@link org.apache.gora.filter.FilterList.Operator#MUST_PASS_ALL}
   * functionality.
   */
  @Test
  public void testOperatorMustPassAll() {
    filter1.setFieldName(WebPage.Field.OUTLINKS.toString());
    filter1.setMapKey(new Utf8("example"));
    filter1.setFilterOp(FilterOp.EQUALS);
    filter1.setFilterIfMissing(true);
    filter1.getOperands().add(new Utf8("http://example.org"));
    
    filter2.setFieldName(WebPage.Field.OUTLINKS.toString());
    filter2.setFilterOp(FilterOp.EQUALS);
    filter2.setFilterIfMissing(true);
    filter2.getOperands().add(new Utf8("http://example.org"));
    
    WebPage page = WebPage.newBuilder().build();
    page.getOutlinks().put(new Utf8("example"), new Utf8("http://example.org"));
    
    filterList = new FilterList<>(Operator.MUST_PASS_ALL, fList);
    assertTrue(filterList.filter("irrelevant", page));
  }
  
  /**
   * Test method for verifying 
   * {@link org.apache.gora.filter.FilterList.Operator#MUST_PASS_ONE}
   * functionality.
   */
  @Test
  public void testOperatorMustPassOne() {
    filter1.setFieldName(WebPage.Field.OUTLINKS.toString());
    filter1.setMapKey(new Utf8("example"));
    filter1.setFilterOp(FilterOp.EQUALS);
    filter1.setFilterIfMissing(true);
    filter1.getOperands().add(new Utf8("http://example.org"));
    
    filter2.setFieldName(WebPage.Field.OUTLINKS.toString());
    filter2.setFilterOp(FilterOp.EQUALS);
    filter2.setFilterIfMissing(true);
    filter2.getOperands().add(new Utf8("http://example2.org"));
    
    WebPage page = WebPage.newBuilder().build();
    page.getOutlinks().put(new Utf8("example"), new Utf8("http://example.org"));
    
    filterList = new FilterList<>(Operator.MUST_PASS_ONE, fList);
    assertTrue(filterList.filter("irrelevant", page));

  }

}
