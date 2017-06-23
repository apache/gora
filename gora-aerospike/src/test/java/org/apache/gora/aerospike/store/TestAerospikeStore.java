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

package org.apache.gora.aerospike.store;

import org.apache.gora.aerospike.GoraAerospikeTestDriver;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

/**
 * Test case for AerospikeStore.
 */
public class TestAerospikeStore extends DataStoreTestBase {

  private static final String DOCKER_CONTAINER_NAME = "aerospike:3.14.0";

  @ClassRule
  public static GenericContainer aerospikeContainer = new GenericContainer(DOCKER_CONTAINER_NAME);

  static {
    setTestDriver(new GoraAerospikeTestDriver(aerospikeContainer));
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testNewInstance() throws Exception {
    // super.testNewInstance();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testCreateSchema() throws Exception {
    // super.testCreateSchema();
  }

  @Override
  public void assertSchemaExists(String schemaName) throws Exception {
    super.assertSchemaExists(schemaName);
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testAutoCreateSchema() throws Exception {
    super.testAutoCreateSchema();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void assertAutoCreateSchema() throws Exception {
    super.assertAutoCreateSchema();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testTruncateSchema() throws Exception {
    super.testTruncateSchema();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testDeleteSchema() throws Exception {
    super.testDeleteSchema();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testSchemaExists() throws Exception {
    super.testSchemaExists();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPut() throws Exception {
    super.testPut();
  }

  @Override
  public void assertPut(Employee employee) throws IOException {
    super.assertPut(employee);
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPutNested() throws Exception {
    super.testPutNested();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPutArray() throws Exception {
    super.testPutArray();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void assertPutArray() throws IOException {
    super.assertPutArray();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPutBytes() throws Exception {
    super.testPutBytes();
  }

  @Override
  public void assertPutBytes(byte[] contentBytes) throws IOException {
    super.assertPutBytes(contentBytes);
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPutMap() throws Exception {
    super.testPutMap();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void assertPutMap() throws IOException {
    super.assertPutMap();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testPutMixedMaps() throws Exception {
    super.testPutMixedMaps();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testUpdate() throws Exception {
    super.testUpdate();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testEmptyUpdate() throws Exception {
    super.testEmptyUpdate();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGet() throws Exception {
    super.testGet();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetRecursive() throws Exception {
    super.testGetRecursive();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetDoubleRecursive() throws Exception {
    super.testGetDoubleRecursive();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetNested() throws Exception {
    super.testGetNested();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGet3UnionField() throws Exception {
    super.testGet3UnionField();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetWithFields() throws Exception {
    super.testGetWithFields();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetWebPage() throws Exception {
    super.testGetWebPage();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetWebPageDefaultFields() throws Exception {
    super.testGetWebPageDefaultFields();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetNonExisting() throws Exception {
    super.testGetNonExisting();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQuery() throws Exception {
    super.testQuery();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryStartKey() throws Exception {
    super.testQueryStartKey();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryEndKey() throws Exception {
    super.testQueryEndKey();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryKeyRange() throws Exception {
    super.testQueryKeyRange();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryWebPageSingleKey() throws Exception {
    super.testQueryWebPageSingleKey();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryWebPageSingleKeyDefaultFields() throws Exception {
    super.testQueryWebPageSingleKeyDefaultFields();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testQueryWebPageQueryEmptyResults() throws Exception {
    super.testQueryWebPageQueryEmptyResults();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testDelete() throws Exception {
    super.testDelete();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testDeleteByQuery() throws Exception {
    super.testDeleteByQuery();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testDeleteByQueryFields() throws Exception {
    super.testDeleteByQueryFields();
  }

  @Test
  @Ignore("To be implemented")
  @Override
  public void testGetPartitions() throws Exception {
    super.testGetPartitions();
  }
}
