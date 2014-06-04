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
package org.apache.gora.mongodb.filters;

import static org.junit.Assert.assertEquals;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.filter.FilterList;
import org.apache.gora.filter.FilterOp;
import org.apache.gora.filter.MapFieldValueFilter;
import org.apache.gora.filter.SingleFieldValueFilter;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.DBObject;

public class DefaultFactoryTest {

  private FilterFactory<String, WebPage> filterFactory;
  private MongoStore<String, WebPage> store;

  @Before
  public void setUp() throws Exception {
    filterFactory = new DefaultFactory<String, WebPage>();
    filterFactory.setFilterUtil(new MongoFilterUtil<String, WebPage>(
        new Configuration()));

    // Create dummy mapping for unit testing
    store = new MongoStore<String, WebPage>();
    store.getMapping().addClassField(null, "headers", "h", "document");
    store.getMapping().addClassField(null, "url", "url", "string");
  }

  @Test
  public void testCreateFilter_singleField_notEquals() throws Exception {
    SingleFieldValueFilter<String, WebPage> filter = createUrlFilter();
    filter.setFilterOp(FilterOp.NOT_EQUALS);
    filter.setFilterIfMissing(true);

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals("{ \"url\" : { \"$ne\" : \"http://www.example.com\"}}",
        dbObject.toString());
  }

  @Test
  public void testCreateFilter_singleField_equalsOrNull() throws Exception {
    SingleFieldValueFilter<String, WebPage> filter = createUrlFilter();
    filter.setFilterOp(FilterOp.EQUALS);
    filter.setFilterIfMissing(false); // include doc with missing field

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals(
        "{ \"$or\" : [ { \"url\" : { \"$exists\" : false}} , { \"url\" : \"http://www.example.com\"}]}",
        dbObject.toString());
  }

  @Test
  public void testCreateFilter_mapField_notEquals() throws Exception {
    MapFieldValueFilter<String, WebPage> filter = createHeadersFilter();
    filter.setFilterOp(FilterOp.NOT_EQUALS);
    filter.setFilterIfMissing(true);

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals("{ \"h.C·T\" : { \"$ne\" : \"text/html\"}}",
        dbObject.toString());
  }

  @Test
  public void testCreateFilter_mapField_equalsOrNull() throws Exception {
    MapFieldValueFilter<String, WebPage> filter = createHeadersFilter();
    filter.setFilterOp(FilterOp.EQUALS);
    filter.setFilterIfMissing(false); // include doc with missing field

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals(
        "{ \"$or\" : [ { \"h.C·T\" : { \"$exists\" : false}} , { \"h.C·T\" : \"text/html\"}]}",
        dbObject.toString());
  }

  @Test
  public void testCreateFilter_list_empty() throws Exception {
    FilterList<String, WebPage> filter = new FilterList<String, WebPage>();

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals("{ }", dbObject.toString());
  }

  @Test
  public void testCreateFilter_list_2() throws Exception {
    FilterList<String, WebPage> filter = new FilterList<String, WebPage>();
    MapFieldValueFilter<String, WebPage> hFilter = createHeadersFilter();
    hFilter.setFilterIfMissing(true);
    hFilter.setFilterOp(FilterOp.EQUALS);
    filter.addFilter(hFilter);
    SingleFieldValueFilter<String, WebPage> urlFilter = createUrlFilter();
    urlFilter.setFilterIfMissing(true);
    urlFilter.setFilterOp(FilterOp.EQUALS);
    filter.addFilter(urlFilter);

    DBObject dbObject = filterFactory.createFilter(filter, store);
    assertEquals(
        "{ \"h.C·T\" : \"text/html\" , \"url\" : \"http://www.example.com\"}",
        dbObject.toString());
  }

  private MapFieldValueFilter<String, WebPage> createHeadersFilter() {
    MapFieldValueFilter<String, WebPage> filter = new MapFieldValueFilter<String, WebPage>();
    filter.setFieldName(WebPage.Field.HEADERS.toString());
    filter.setMapKey(new Utf8("C.T"));
    filter.getOperands().add("text/html");
    return filter;
  }

  private SingleFieldValueFilter<String, WebPage> createUrlFilter() {
    SingleFieldValueFilter<String, WebPage> filter = new SingleFieldValueFilter<String, WebPage>();
    filter.setFieldName(WebPage.Field.URL.toString());
    filter.getOperands().add("http://www.example.com");
    return filter;
  }
}
