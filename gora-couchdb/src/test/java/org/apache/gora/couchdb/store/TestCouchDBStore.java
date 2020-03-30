/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.couchdb.store;

import org.apache.avro.util.Utf8;
import org.apache.gora.couchdb.GoraCouchDBTestDriver;
import org.apache.gora.couchdb.query.CouchDBResult;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.util.GoraException;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Tests extending {@link DataStoreTestBase}
 * which run the base JUnit test suite for Gora.
 */
public class TestCouchDBStore extends DataStoreTestBase {

  private static final String DOCKER_CONTAINER_NAME = "couchdb:1.6";
  /**
   * JUnit integration testing with Docker and Testcontainers
   */
  @ClassRule
  public static GenericContainer CouchDB_CONTAINER = new GenericContainer(DOCKER_CONTAINER_NAME);

  static {
    try {
      setTestDriver(new GoraCouchDBTestDriver(CouchDB_CONTAINER));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testPutAndGet() throws GoraException {
    WebPage page = webPageStore.newPersistent();

    // Write webpage data
    page.setUrl(new Utf8("http://example.com"));
    byte[] contentBytes = "example content in example.com".getBytes(Charset.defaultCharset());
    ByteBuffer buff = ByteBuffer.wrap(contentBytes);
    page.setContent(buff);
    webPageStore.put("com.example/http", page);
    webPageStore.flush();

    WebPage storedPage = webPageStore.get("com.example/http");

    assertNotNull(storedPage);
    assertEquals(page.getUrl(), storedPage.getUrl());
  }

  @Test
  public void testCreateAndDeleteSchema() throws IOException {
    WebPage page = webPageStore.newPersistent();

    // Write webpage data
    page.setUrl(new Utf8("http://example.com"));
    webPageStore.put("com.example/http", page);
    webPageStore.flush();

    assertEquals("WebPage isn't created.", page.getUrl(), webPageStore.get("com.example/http").getUrl());

    webPageStore.deleteSchema();

    assertNull(webPageStore.get("com.example/http"));
  }

  @Test
  public void testGetSchemaName() throws IOException {
    assertEquals("WebPage", webPageStore.getSchemaName());
    assertEquals("Employee", employeeStore.getSchemaName());
  }

  @Test
  public void testExecute() throws IOException {
    WebPageDataCreator.createWebPageData(webPageStore);

    final Query<String, WebPage> query = webPageStore.newQuery();

    int limit = 5;
    query.setLimit(limit);
    CouchDBResult<String, WebPage> result = (CouchDBResult<String, WebPage>) webPageStore.execute(query);
    assertEquals(limit, result.getResultData().size());

    limit = 10;
    query.setLimit(limit);
    result = (CouchDBResult<String, WebPage>) webPageStore.execute(query);
    assertEquals(limit, result.getResultData().size());

  }

  /**
   * By design, you cannot update a CouchDB document blindly, you can only attempt to update a specific revision of a document. FIXME
   */
  @Test
  @Ignore
  public void testUpdate() throws Exception {
    //By design, you cannot update a CouchDB document blindly, you can only attempt to update a specific revision of a document. FIXME
  }

  @Ignore("CouchDBStore doesn't support 3 types union field yet")
  @Override
  public void testGet3UnionField() throws Exception {
    // CouchDBStore doesn't support 3 types union field yet
  }

  @Ignore("Skip until GORA-66 is fixed: need better semantic for end/start keys")
  @Override
  public void testDeleteByQueryFields() throws IOException {
    // Skip until GORA-66 is fixed: need better semantic for end/start keys
  }

}