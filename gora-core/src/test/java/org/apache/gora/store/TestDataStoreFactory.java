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

package org.apache.gora.store;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.apache.gora.avro.store.DataFileAvroStore;
import org.apache.gora.mock.persistency.MockPersistent;
import org.apache.gora.mock.store.MockDataStore;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Test;

public class TestDataStoreFactory {
  private Configuration conf;

  @Before
  public void setUp() {
    conf = new Configuration();
  }

  @Test
  public void testGetDataStore() throws GoraException {
    DataStore<?, ?> dataStore = DataStoreFactory.getDataStore(
        "org.apache.gora.mock.store.MockDataStore", String.class,
        MockPersistent.class, conf);
    assertNotNull(dataStore);
  }

  @Test
  public void testGetClasses() throws GoraException {
    DataStore<?, ?> dataStore = DataStoreFactory.getDataStore(
        "org.apache.gora.mock.store.MockDataStore", String.class,
        MockPersistent.class, conf);
    assertNotNull(dataStore);
    assertEquals(String.class, dataStore.getKeyClass());
    assertEquals(MockPersistent.class, dataStore.getPersistentClass());
  }

  @Test
  public void testGetDataStore2() throws GoraException {
    DataStore<?, ?> dataStore = DataStoreFactory.getDataStore(
        MockDataStore.class, String.class, MockPersistent.class, conf);
    assertNotNull(dataStore);
  }

  @Test
  public void testGetDataStore3() throws GoraException {
    DataStore<?, ?> dataStore1 = DataStoreFactory.getDataStore(
        "org.apache.gora.mock.store.MockDataStore", Object.class,
        MockPersistent.class, conf);
    DataStore<?, ?> dataStore2 = DataStoreFactory.getDataStore(
        "org.apache.gora.mock.store.MockDataStore", Object.class,
        MockPersistent.class, conf);
    DataStore<?, ?> dataStore3 = DataStoreFactory.getDataStore(
        "org.apache.gora.mock.store.MockDataStore", String.class,
        MockPersistent.class, conf);

    assertNotSame(dataStore1, dataStore2);
    assertNotSame(dataStore1, dataStore3);
  }

  @Test
  public void testReadProperties() throws GoraException {
    // indirect testing
    DataStore<?, ?> dataStore = DataStoreFactory.getDataStore(String.class,
        MockPersistent.class, conf);
    assertNotNull(dataStore);
    assertEquals(MockDataStore.class, dataStore.getClass());
  }

  @Test
  public void testFindProperty() {
    Properties properties = DataStoreFactory.createProps();

    DataStore<String, MockPersistent> store = new DataFileAvroStore<>();

    String fooValue = DataStoreFactory.findProperty(properties, store,
        "foo_property", "foo_default");
    assertEquals("foo_value", fooValue);

    String bazValue = DataStoreFactory.findProperty(properties, store,
        "baz_property", "baz_default");
    assertEquals("baz_value", bazValue);

    String barValue = DataStoreFactory.findProperty(properties, store,
        "bar_property", "bar_default");
    assertEquals("bar_value", barValue);

    String capValue = DataStoreFactory.findProperty(properties, store,
        "cap_property", "cap_default");
    assertEquals("cap_value", capValue);

    String secondCapValue = DataStoreFactory.findProperty(properties, store,
        "CAP_property", "cap_default");
    assertEquals("cap_value", secondCapValue);
  }

}
