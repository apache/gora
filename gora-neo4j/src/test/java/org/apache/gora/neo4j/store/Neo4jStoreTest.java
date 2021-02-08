/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.neo4j.store;

import java.util.Properties;
import org.apache.gora.examples.generated.EmployeeInt;
import org.apache.gora.neo4j.GoraNeo4jTestDriver;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.util.GoraException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for Neo4j Datastore.
 */
public class Neo4jStoreTest extends DataStoreTestBase {

  static {
    setTestDriver(new GoraNeo4jTestDriver());
  }

  @Test
  @Ignore
  public void testTruncateSchema() throws Exception {
  }

  @Test
  @Ignore
  public void testDeleteSchema() throws Exception {
  }

  @Test
  @Ignore
  public void testUpdate() throws Exception {
  }

  @Test
  @Ignore
  public void testGetRecursive() throws Exception {
  }

  @Test
  @Ignore
  public void testGetDoubleRecursive() throws Exception {
  }

  @Test
  @Ignore
  public void testGetWebPage() throws Exception {
  }

  @Test
  @Ignore
  public void testGetWebPageDefaultFields() throws Exception {
  }

  @Test
  @Ignore
  public void testQuery() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryStartKey() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryEndKey() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryKeyRange() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryWebPageSingleKey() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryWebPageSingleKeyDefaultFields() throws Exception {
  }

  @Test
  @Ignore
  public void testQueryWebPageQueryEmptyResults() throws Exception {
  }

  @Test
  @Ignore
  public void testDelete() throws Exception {
  }

  @Test
  @Ignore
  public void testDeleteByQuery() throws Exception {
  }

  @Test
  @Ignore
  public void testDeleteByQueryFields() throws Exception {
  }

  @Test
  @Ignore
  public void testGetPartitions() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSize() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeStartKey() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeEndKey() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeKeyRange() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeWithLimit() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeStartKeyWithLimit() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeEndKeyWithLimit() throws Exception {
  }

  @Test
  @Ignore
  public void testResultSizeKeyRangeWithLimit() throws Exception {
  }

  /**
   * XSD Validation.
   *
   * Validate bad formatted XML Mappings.
   */
  @Test(expected = GoraException.class)
  public void testXSDValidation() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("gora.xsd_validation", "true");
    properties.setProperty("gora.neo4j.mapping.file", "gora-neo4j-mapping-bad.xml");
    DataStoreTestBase.testDriver.createDataStore(String.class, EmployeeInt.class, properties);
  }

}
