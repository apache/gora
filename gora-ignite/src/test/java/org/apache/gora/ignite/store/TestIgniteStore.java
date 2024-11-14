/*
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
package org.apache.gora.ignite.store;

import java.util.HashMap;
import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;
import org.apache.gora.ignite.GoraIgniteTestDriver;
import org.apache.gora.store.DataStoreMetadataFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for IgniteStore.
 */
public class TestIgniteStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraIgniteTestDriver());
  }

  @Test
  public void igniteStoreMetadataAnalyzerTest() throws Exception {
    DataStoreMetadataAnalyzer createAnalyzer = DataStoreMetadataFactory.createAnalyzer(DataStoreTestBase.testDriver.getConfiguration());
    Assert.assertEquals("Ignite Store Metadata Type", "IGNITE", createAnalyzer.getType());
    Assert.assertTrue("Ignite Store Metadata Table Names", createAnalyzer.getTablesNames().equals(Lists.newArrayList("EMPLOYEE", "WEBPAGE")));
    IgniteTableMetadata tableInfo = (IgniteTableMetadata) createAnalyzer.getTableInfo("EMPLOYEE");
    Assert.assertEquals("Ignite Store Metadata Table Primary Key Column", "PKSSN", tableInfo.getPrimaryKey());
    Assert.assertEquals("Ignite Store Metadata Table Primary Key Type", "VARCHAR", tableInfo.getPrimaryKeyType());
    HashMap<String, String> hmap = new HashMap();
    hmap.put("WEBPAGE", "VARBINARY");
    hmap.put("BOSS", "VARBINARY");
    hmap.put("SALARY", "INTEGER");
    hmap.put("DATEOFBIRTH", "BIGINT");
    hmap.put("VALUE", "VARCHAR");
    hmap.put("NAME", "VARCHAR");
    hmap.put("SSN", "VARCHAR");
    Assert.assertTrue("Ignite Store Metadata Table Columns", tableInfo.getColumns().equals(hmap));
  }

}
