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
package org.apache.gora.kudu.store;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import org.apache.gora.kudu.GoraKuduTestDriver;
import org.apache.gora.store.DataStoreMetadataFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case for KuduStore.
 */
public class TestKuduStore extends DataStoreTestBase {

  static {
    setTestDriver(new GoraKuduTestDriver());
  }

  @Test
  @Ignore
  @Override
  public void testResultSize() throws Exception {
    //Kudu uses a scanner for querying. It is not possible to calculate the size of the result set without iterating it.
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeStartKey() throws Exception {
    //Kudu uses a scanner for querying. It is not possible to calculate the size of the result set without iterating it.
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeEndKey() throws Exception {
    //Kudu uses a scanner for querying. It is not possible to calculate the size of the result set without iterating it.
  }

  @Test
  @Ignore
  @Override
  public void testResultSizeKeyRange() throws Exception {
    //Kudu uses a scanner for querying. It is not possible to calculate the size of the result set without iterating it.
  }

  @Test
  public void kuduStoreMetadataAnalyzerTest() throws Exception {
    DataStoreMetadataAnalyzer createAnalyzer = DataStoreMetadataFactory.createAnalyzer(DataStoreTestBase.testDriver.getConfiguration());
    Assert.assertEquals("Kudu Store Metadata Type", "KUDU", createAnalyzer.getType());
    List<String> tablesNames = createAnalyzer.getTablesNames();
    Assert.assertTrue("Kudu Store Metadata Table Names", tablesNames.equals(Lists.newArrayList("Employee", "WebPage")));
    KuduTableMetadata tableInfo = (KuduTableMetadata) createAnalyzer.getTableInfo("Employee");
    Assert.assertEquals("Kudu Store Metadata Table Primary Key Column", "pkssn", tableInfo.getPrimaryKeyColumn());
    Assert.assertEquals("Kudu Store Metadata Table Primary Key Type", "string", tableInfo.getPrimaryKeyType());
    HashMap<String, String> hmap = new HashMap();
    hmap.put("webpage", "binary");
    hmap.put("boss", "binary");
    hmap.put("salary", "int32");
    hmap.put("dateOfBirth", "int64");
    hmap.put("value", "string");
    hmap.put("name", "string");
    hmap.put("ssn", "string");
    Assert.assertTrue("Kudu Store Metadata Table Columns", tableInfo.getColumns().equals(hmap));
  }
}
