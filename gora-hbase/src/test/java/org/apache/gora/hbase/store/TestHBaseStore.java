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

package org.apache.gora.hbase.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.util.Utf8;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.hbase.GoraHBaseTestDriver;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreMetadataFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * Test case for HBaseStore.
 */
public class TestHBaseStore extends DataStoreTestBase {

  private Configuration conf;
  
  static {
    setTestDriver(new GoraHBaseTestDriver());
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    conf = getTestDriver().getConf();
  }

  public GoraHBaseTestDriver getTestDriver() {
    return (GoraHBaseTestDriver) testDriver;
  }

  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    Admin admin = getTestDriver().getHbaseUtil().getAdmin();
    assertTrue("Table should exist for...", admin.tableExists(TableName.valueOf(schemaName)));
  }

  @Override
  public void assertPutArray() throws IOException {
    Connection conn = ConnectionFactory.createConnection(conf);
    TableName webPageTab = TableName.valueOf("WebPage");
    Table table = conn.getTable(webPageTab);
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    assertEquals(result.getFamilyMap(Bytes.toBytes("parsedContent")).size(), 4);
    assertTrue(Arrays.equals(result.getValue(Bytes.toBytes("parsedContent")
        ,Bytes.toBytes(0)), Bytes.toBytes("example")));
    
    assertTrue(Arrays.equals(result.getValue(Bytes.toBytes("parsedContent")
        ,Bytes.toBytes(3)), Bytes.toBytes("example.com")));
    table.close();
  }

  /**
   * Asserts that writing bytes actually works at low level in HBase.
   * Checks writing null unions too.
   */
  @Override
  public void assertPutBytes(byte[] contentBytes) throws IOException {    

    // Check first the parameter "contentBytes" if written+read right.
    Connection conn = ConnectionFactory.createConnection(conf);
    TableName webPageTab = TableName.valueOf("WebPage");
    Table table = conn.getTable(webPageTab);

    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    byte[] actualBytes = result.getValue(Bytes.toBytes("content"), null);
    assertNotNull(actualBytes);
    assertTrue(Arrays.equals(contentBytes, actualBytes));
    table.close();    

    // Since "content" is an optional field, we are forced to reopen the DataStore
    // to retrieve the union correctly
    
    // Test writing+reading a null value. FIELD in HBASE MUST become DELETED
    WebPage page = webPageStore.get("com.example/http") ;
    page.setContent(null) ;
    webPageStore.put("com.example/http", page) ;
    webPageStore.close() ;
    webPageStore = testDriver.createDataStore(String.class, WebPage.class);
    page = webPageStore.get("com.example/http") ;
    assertNull(page.getContent()) ;
    // Check directly with HBase
    table = conn.getTable(webPageTab);
    get = new Get(Bytes.toBytes("com.example/http"));
    result = table.get(get);
    actualBytes = result.getValue(Bytes.toBytes("content"), null);
    assertNull(actualBytes);
    table.close();
    
    // Test writing+reading an empty bytes field. FIELD in HBASE MUST 
    // become EMPTY (byte[0])
    page = webPageStore.get("com.example/http") ;
    page.setContent(ByteBuffer.wrap("".getBytes(Charset.defaultCharset()))) ;
    webPageStore.put("com.example/http", page) ;
    webPageStore.close() ;
    webPageStore = testDriver.createDataStore(String.class, WebPage.class);
    page = webPageStore.get("com.example/http") ;
    assertTrue(Arrays.equals("".getBytes(Charset.defaultCharset()),page.getContent().array())) ;
    // Check directly with HBase


    table = conn.getTable(TableName.valueOf("WebPage"));
    get = new Get(Bytes.toBytes("com.example/http"));
    result = table.get(get);
    actualBytes = result.getValue(Bytes.toBytes("content"), null);
    assertNotNull(actualBytes);
    assertEquals(0, actualBytes.length) ;
    table.close();
  }
  
  /**
   * Checks that when writing a top level union <code>['null','type']</code> 
   * the value is written in raw format
   * @throws Exception
   */
  @Test
  public void assertTopLevelUnions() throws Exception {
    WebPage page = webPageStore.newPersistent();
    
    // Write webpage data
    page.setUrl(new Utf8("http://example.com"));
    byte[] contentBytes = "example content in example.com".getBytes(Charset.defaultCharset());
    ByteBuffer buff = ByteBuffer.wrap(contentBytes);
    page.setContent(buff);
    webPageStore.put("com.example/http", page);
    webPageStore.flush() ;
    
    // Read directly from HBase
    Connection conn = ConnectionFactory.createConnection(conf);
    Table table = conn.getTable(TableName.valueOf("WebPage"));
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);

    byte[] bytesRead = result.getValue(Bytes.toBytes("content"), null);
    
    assertNotNull(bytesRead) ;
    assertTrue(Arrays.equals(bytesRead, contentBytes));
    table.close();
  }
  
  /**
   * Checks that when writing a top level union <code>['null','type']</code> 
   * with the option <code>RAW_ROOT_FIELDS_OPTION=true</code>
   * the column is not created, and when <code>RAW_ROOT_FIELDS_OPTION=false</code> 
   * the <code>null</code> value is serialized
   * with Avro.
   * @throws Exception
   */
  @Test
  public void assertTopLevelUnionsNull() throws Exception {
    WebPage page = webPageStore.newPersistent();
    
    // Write webpage data
    page.setUrl(new Utf8("http://example.com"));
    page.setContent(null);     // This won't change internal field status to dirty, so
    page.setDirty("content") ; // need to change it manually
    webPageStore.put("com.example/http", page);
    webPageStore.flush() ;
    
    // Read directly from HBase
    Connection conn = ConnectionFactory.createConnection(conf);
    TableName webPageTab = TableName.valueOf("WebPage");
    Table table = conn.getTable(webPageTab);
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    table.close();
    byte[] contentBytes = result.getValue(Bytes.toBytes("content"), null);

    assertNull(webPageStore.get("com.example/http", new String[]{"content"})) ;
    assertTrue(contentBytes == null || contentBytes.length == 0) ;
  }
  
  @Override
  public void assertPutMap() throws IOException {
    Connection conn = ConnectionFactory.createConnection(conf);
    TableName webPageTab = TableName.valueOf("WebPage");
    Table table = conn.getTable(webPageTab);
    Get get = new Get(Bytes.toBytes("com.example/http"));
    org.apache.hadoop.hbase.client.Result result = table.get(get);
    
    byte[] anchor2Raw = result.getValue(Bytes.toBytes("outlinks")
        , Bytes.toBytes("http://example2.com"));
    assertNotNull(anchor2Raw);
    String anchor2 = Bytes.toString(anchor2Raw);
    assertEquals("anchor2", anchor2);
    table.close();
  }

  @Test
  public void assertScannerCachingValue() {
    assertEquals(1000, ((HBaseStore<String,WebPage>)this.webPageStore).getScannerCaching()) ;
    assertEquals(1000, ((HBaseStore<String,Employee>)this.employeeStore).getScannerCaching()) ;
  }

  @Test
  public void assertMetadataAnalyzer() throws GoraException, ClassNotFoundException {
      String analyzerName = this.getTestDriver().getDataStoreClass().getCanonicalName() + "MetadataAnalyzer" ;
      DataStoreMetadataAnalyzer metadataAnalyzer = DataStoreMetadataFactory.createAnalyzer(analyzerName, this.conf) ;
      assertEquals("HBASE", metadataAnalyzer.getType()) ;

      // Expectations
      List<String> expectedTables = new ArrayList<String>() ;
      expectedTables.add("Employee");
      expectedTables.add("WebPage");
      
      List<String> expectedEmployeeFamilies = new ArrayList<String>();
      expectedEmployeeFamilies.add("info");

      List<String> expectedWebPageFamilies = new ArrayList<String>();
      expectedWebPageFamilies.add("byteData");
      expectedWebPageFamilies.add("common");
      expectedWebPageFamilies.add("content");
      expectedWebPageFamilies.add("headers");
      expectedWebPageFamilies.add("outlinks");
      expectedWebPageFamilies.add("parsedContent");
      expectedWebPageFamilies.add("stringData");
      
      Map<String, List<String>> expectedFamilies = new HashMap<>();
      expectedFamilies.put("Employee", expectedEmployeeFamilies);
      expectedFamilies.put("WebPage", expectedWebPageFamilies);
      
      // Tests
      List<String> tables = metadataAnalyzer.getTablesNames() ;
      assertEquals(expectedTables, tables) ;
      
      for (String tableName: tables) {
          Object tableInfo = metadataAnalyzer.getTableInfo(tableName);
          assertTrue("fieldsInfo expected to be class HBaseTableMetadata", tableInfo instanceof HBaseTableMetadata);
          assertEquals(expectedFamilies.get(tableName), ((HBaseTableMetadata)tableInfo).getColumnFamilies());
      }
  }
  
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void assertConfigurationException() throws GoraException {
    expectedException.expect(GoraException.class);
    expectedException.expectMessage("Gora-hbase-mapping does not include the name and keyClass in the databean.");

    Configuration exceptionalConf = HBaseConfiguration.create(conf);
    exceptionalConf.set("gora.hbase.mapping.file","gora-hbase-mapping-mismatch.xml");
    DataStoreFactory.createDataStore(HBaseStore.class, String.class, WebPage.class, exceptionalConf);
  }

  @Test
  @Ignore("HBase does not support Result#size() without limit set")
  @Override
  public void testResultSize() throws Exception {
  }

  @Test
  @Ignore("HBase does not support Result#size() without limit set")
  @Override
  public void testResultSizeStartKey() throws Exception {
  }

  @Ignore("HBase does not support Result#size() without limit set")
  @Override
  public void testResultSizeEndKey() throws Exception {
  }

  @Test
  @Ignore("HBase does not support Result#size() without limit set")
  @Override
  public void testResultSizeKeyRange() throws Exception {
  }
}
