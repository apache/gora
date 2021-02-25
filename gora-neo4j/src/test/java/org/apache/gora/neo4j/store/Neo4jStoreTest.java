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

import avro.shaded.com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Properties;
import org.apache.gora.examples.generated.EmployeeInt;
import org.apache.gora.neo4j.GoraNeo4jTestDriver;
import org.apache.gora.store.DataStoreMetadataFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for Neo4j Datastore.
 */
public class Neo4jStoreTest extends DataStoreTestBase {

  static {
    setTestDriver(new GoraNeo4jTestDriver());
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

  @Test
  public void neo4jStoreMetadataAnalyzerTest() throws Exception {
    DataStoreMetadataAnalyzer createAnalyzer = DataStoreMetadataFactory.createAnalyzer(DataStoreTestBase.testDriver.getConfiguration());
    Assert.assertEquals("Neo4j Store Metadata Type", "NEO4J", createAnalyzer.getType());
    Assert.assertTrue("Neo4j Store Metadata Table Names", createAnalyzer.getTablesNames().equals(Lists.newArrayList("Employee", "Webpage")));
    Neo4jTableMetadata tableInfo = (Neo4jTableMetadata) createAnalyzer.getTableInfo("Employee");
    Assert.assertTrue("Neo4j Node Key", tableInfo.getNodeKey().equals("pkssn"));
    Assert.assertTrue("Neo4j Properties", tableInfo.getProperties().equals(Lists.newArrayList("ssn")));
  }
}
