/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.store;

import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.examples.generated.Metadata;
import org.apache.gora.examples.generated.WebPage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class TestAvroSerializationWithUDT {

  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static CassandraStore<String, WebPage> webPageCassandraStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    webPageCassandraStore = (CassandraStore<String, WebPage>) testDriver.createDataStore(String.class, WebPage.class);
  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    parameter.setProperty(CassandraStoreParameters.PORT, "9042");
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERIALIZATION_TYPE, "avro");
    parameter.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    parameter.setProperty(CassandraStoreParameters.CLUSTER_NAME, "Test Cluster");
    parameter.setProperty("gora.cassandrastore.mapping.file", "avroUDT/gora-cassandra-mapping.xml");
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    testDriver.tearDownClass();
  }

  @After
  public void tearDown() throws Exception {
    testDriver.tearDown();
  }

  /**
   * This is for testGetNested() with UDT dataType with avro serialization
   */
  @Test
  public void testSimplePutAndGEt() {
    webPageCassandraStore.createSchema();
    WebPage webpage = WebPage.newBuilder().build();
    webpage.setUrl(new Utf8("url.."));
    webpage.setContent(ByteBuffer.wrap("test content".getBytes(Charset.defaultCharset())));
    webpage.setParsedContent(new ArrayList<>());
    Metadata metadata = Metadata.newBuilder().build();
    webpage.setMetadata(metadata);
    webPageCassandraStore.put("yawamu.com", webpage);
    WebPage retrievedWebPage = webPageCassandraStore.get("yawamu.com");
    Assert.assertEquals(webpage.getMetadata().getVersion(),retrievedWebPage.getMetadata().getVersion());
    for(Map.Entry entry : webpage.getMetadata().getData().entrySet()) {
      Assert.assertEquals(entry.getValue(),retrievedWebPage.getMetadata().getData().get(entry.getKey()));
    }
  }

}
