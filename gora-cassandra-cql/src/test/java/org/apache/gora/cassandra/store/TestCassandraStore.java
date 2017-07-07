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

/**
 * Testing class for all standard gora-cassandra functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora. 
 */
package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;
import java.util.Properties;

/**
 * Test for CassandraStore.
 */
public class TestCassandraStore extends DataStoreTestBase{
  private static Properties properties;

  static {
    GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
    setProperties();
    testDriver.setParameters(properties);
    setTestDriver(testDriver);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }


  private static void setProperties() {
    properties = new Properties();
    properties.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS, "localhost");
    properties.setProperty(CassandraStoreParameters.PORT, "9042");
    properties.setProperty(CassandraStoreParameters.ENABLE_JMX_REPORTING, "false");
    properties.setProperty(CassandraStoreParameters.PROTOCOL_VERSION, "3");
    properties.setProperty(CassandraStoreParameters.CLUSTER_NAME,"Test Cluster");
    properties.setProperty("gora.cassandrastore.mapping.file", "avro/gora-cassandra-mapping.xml");
  }
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQuery() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryStartKey() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryEndKey() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryKeyRange() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryWebPageSingleKey() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryWebPageSingleKeyDefaultFields() throws IOException {}
  @Ignore("GORA-299 o.a.g.cassandra.CassandraStore#newQuery() should not use query.setFields(getFieldsToQuery(null));")
  @Override
  public void testQueryWebPageQueryEmptyResults() throws IOException {}
  @Ignore("GORA-154 delete() and deleteByQuery() methods are not implemented at CassandraStore, and always returns false or 0")
  @Override
  public void testDelete() throws IOException {}
  @Ignore("GORA-154 delete() and deleteByQuery() methods are not implemented at CassandraStore, and always returns false or 0")
  @Override
  public void testDeleteByQuery() throws IOException {}
  @Ignore("GORA-154 delete() and deleteByQuery() methods are not implemented at CassandraStore, and always returns false or 0")
  @Override
  public void testDeleteByQueryFields() throws IOException {}
  @Ignore("GORA-298 Implement CassandraStore#getPartitions")
  @Override
  public void testGetPartitions() throws IOException {}
}
