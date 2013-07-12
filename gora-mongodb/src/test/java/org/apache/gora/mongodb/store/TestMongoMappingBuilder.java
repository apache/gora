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
/*package org.apache.gora.mongodb.store;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.gora.mongodb.beans.tests.Host;
import org.apache.gora.mongodb.beans.tests.Test1;
import org.apache.gora.mongodb.beans.tests.WebPage;
import org.apache.gora.mongodb.store.MongoMapping.DocumentFieldType;
import org.junit.Test;

public class TestMongoMappingBuilder {

  @Test
  public void testStraightMapping_Test1() throws IOException {
    MongoStore<String, Test1> store = new MongoStore<String, Test1>();
    store.setKeyClass(String.class);
    store.setPersistentClass(Test1.class);
    MongoMappingBuilder<String, Test1> builder = new MongoMappingBuilder<String, Test1>(
        store);
    builder.fromFile("/org/apache/gora/mongodb/straightmapping-test1.xml");
    MongoMapping mapping = builder.build();

    // Check collection name
    assertEquals("test1", mapping.getCollectionName());

    // Check field names
    assertEquals("mapOfBytes", mapping.getDocumentField("mapOfBytes"));
    assertEquals("mapOfStrings", mapping.getDocumentField("mapOfStrings"));
    assertEquals("mapOfInt", mapping.getDocumentField("mapOfInt"));
    assertEquals("scalarString", mapping.getDocumentField("scalarString"));
    assertEquals("scalarDate", mapping.getDocumentField("scalarDate"));
    assertEquals("scalarInt", mapping.getDocumentField("scalarInt"));

    // Check field types
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("mapOfBytes"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("mapOfStrings"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("mapOfInt"));
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("scalarString"));
    assertEquals(DocumentFieldType.DATE,
        mapping.getDocumentFieldType("scalarDate"));
    assertEquals(DocumentFieldType.INT32,
        mapping.getDocumentFieldType("scalarInt"));
  }

  @Test
  public void testRenameMapping_Test1() throws IOException {
    MongoStore<String, Test1> store = new MongoStore<String, Test1>();
    store.setKeyClass(String.class);
    store.setPersistentClass(Test1.class);
    MongoMappingBuilder<String, Test1> builder = new MongoMappingBuilder<String, Test1>(
        store);
    builder.fromFile("/org/apache/gora/mongodb/renamemapping-test1.xml");
    MongoMapping mapping = builder.build();

    // Check collection name
    assertEquals("test1", mapping.getCollectionName());
    mapping.renameCollection("test1", "newName");
    assertEquals("newName", mapping.getCollectionName());

    // Check field names
    assertEquals("maps.m1", mapping.getDocumentField("mapOfBytes"));
    assertEquals("maps.m2", mapping.getDocumentField("mapOfStrings"));
    assertEquals("maps.m3", mapping.getDocumentField("mapOfInt"));
    assertEquals("scalars.f1", mapping.getDocumentField("scalarString"));
    assertEquals("scalars.f2", mapping.getDocumentField("scalarDate"));
    assertEquals("scalars.f3", mapping.getDocumentField("scalarInt"));

    // Check field types
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("maps.m1"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("maps.m2"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("maps.m3"));
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("scalars.f1"));
    assertEquals(DocumentFieldType.DATE,
        mapping.getDocumentFieldType("scalars.f2"));
    assertEquals(DocumentFieldType.INT32,
        mapping.getDocumentFieldType("scalars.f3"));
  }

  @Test
  public void testMultiMapping_Host() throws IOException {
    MongoStore<String, Host> store = new MongoStore<String, Host>();
    store.setKeyClass(String.class);
    store.setPersistentClass(Host.class);
    MongoMappingBuilder<String, Host> builder = new MongoMappingBuilder<String, Host>(
        store);
    builder.fromFile("/org/apache/gora/mongodb/multimapping.xml");
    MongoMapping mapping = builder.build();

    // Check collection name
    assertEquals("hosts", mapping.getCollectionName());
    mapping.renameCollection("hosts", "newNameForHosts");
    assertEquals("newNameForHosts", mapping.getCollectionName());

    // Check field names
    assertEquals("metadata", mapping.getDocumentField("metadata"));
    assertEquals("links.out", mapping.getDocumentField("outlinks"));
    assertEquals("links.in", mapping.getDocumentField("inlinks"));

    // Check field types
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("metadata"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("links"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("links.out"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("links.in"));
  }

  @Test
  public void testMultiMapping_Webpage() throws IOException {
    MongoStore<String, WebPage> store = new MongoStore<String, WebPage>();
    store.setKeyClass(String.class);
    store.setPersistentClass(WebPage.class);
    MongoMappingBuilder<String, WebPage> builder = new MongoMappingBuilder<String, WebPage>(
        store);
    builder.fromFile("/org/apache/gora/mongodb/multimapping.xml");
    MongoMapping mapping = builder.build();

    // Check collection name
    assertEquals("frontier", mapping.getCollectionName());
    mapping.renameCollection("frontier", "newNameForFrontier");
    assertEquals("newNameForFrontier", mapping.getCollectionName());

    // Check field names
    assertEquals("baseUrl", mapping.getDocumentField("baseUrl"));
    assertEquals("status", mapping.getDocumentField("status"));
    assertEquals("fetchTime", mapping.getDocumentField("fetchTime"));
    assertEquals("prevFetchTime", mapping.getDocumentField("prevFetchTime"));
    assertEquals("fetchInterval", mapping.getDocumentField("fetchInterval"));
    assertEquals("retriesSinceFetch",
        mapping.getDocumentField("retriesSinceFetch"));
    assertEquals("modifiedTime", mapping.getDocumentField("modifiedTime"));
    assertEquals("protocolStatus", mapping.getDocumentField("protocolStatus"));
    assertEquals("content", mapping.getDocumentField("content"));
    assertEquals("contentType", mapping.getDocumentField("contentType"));
    assertEquals("prevSignature", mapping.getDocumentField("prevSignature"));
    assertEquals("title", mapping.getDocumentField("title"));
    assertEquals("text", mapping.getDocumentField("text"));
    assertEquals("parseStatus", mapping.getDocumentField("parseStatus"));
    assertEquals("score", mapping.getDocumentField("score"));
    assertEquals("reprUrl", mapping.getDocumentField("reprUrl"));
    assertEquals("headers", mapping.getDocumentField("headers"));
    assertEquals("outlinks", mapping.getDocumentField("outlinks"));
    assertEquals("inlinks", mapping.getDocumentField("inlinks"));
    assertEquals("markers", mapping.getDocumentField("markers"));
    assertEquals("metadata", mapping.getDocumentField("metadata"));

    // Check field types
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("baseUrl"));
    assertEquals(DocumentFieldType.INT32,
        mapping.getDocumentFieldType("status"));
    assertEquals(DocumentFieldType.INT64,
        mapping.getDocumentFieldType("fetchTime"));
    assertEquals(DocumentFieldType.INT64,
        mapping.getDocumentFieldType("prevFetchTime"));
    assertEquals(DocumentFieldType.INT32,
        mapping.getDocumentFieldType("fetchInterval"));
    assertEquals(DocumentFieldType.INT32,
        mapping.getDocumentFieldType("retriesSinceFetch"));
    assertEquals(DocumentFieldType.INT64,
        mapping.getDocumentFieldType("modifiedTime"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("protocolStatus"));
    assertEquals(DocumentFieldType.BINARY,
        mapping.getDocumentFieldType("content"));
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("contentType"));
    assertEquals(DocumentFieldType.BINARY,
        mapping.getDocumentFieldType("prevSignature"));
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("title"));
    assertEquals(DocumentFieldType.STRING, mapping.getDocumentFieldType("text"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("parseStatus"));
    assertEquals(DocumentFieldType.DOUBLE,
        mapping.getDocumentFieldType("score"));
    assertEquals(DocumentFieldType.STRING,
        mapping.getDocumentFieldType("reprUrl"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("headers"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("outlinks"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("inlinks"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("markers"));
    assertEquals(DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("metadata"));
  }
}
*/
