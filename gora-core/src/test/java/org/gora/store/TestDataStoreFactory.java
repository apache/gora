
package org.gora.store;

import java.util.Properties;

import junit.framework.Assert;

import org.gora.avro.store.DataFileAvroStore;
import org.gora.mock.persistency.MockPersistent;
import org.gora.mock.store.MockDataStore;
import org.junit.Before;
import org.junit.Test;

public class TestDataStoreFactory {
  
  @Before
  public void setUp() {
  }

  @Test
  public void testGetDataStore() throws ClassNotFoundException {
    DataStore<?,?> dataStore = DataStoreFactory.getDataStore("org.gora.mock.store.MockDataStore"
        , String.class, MockPersistent.class);
    Assert.assertNotNull(dataStore);
  }
  
  @Test
  public void testGetClasses() throws ClassNotFoundException {
    DataStore<?,?> dataStore = DataStoreFactory.getDataStore("org.gora.mock.store.MockDataStore"
        , String.class, MockPersistent.class);
    Assert.assertNotNull(dataStore);
    Assert.assertEquals(String.class, dataStore.getKeyClass());
    Assert.assertEquals(MockPersistent.class, dataStore.getPersistentClass());
  }
  
  @Test
  public void testGetDataStore2() throws ClassNotFoundException {
    DataStore<?,?> dataStore = DataStoreFactory.getDataStore(MockDataStore.class
        , String.class, MockPersistent.class);
    Assert.assertNotNull(dataStore);
  }
  
  @Test
  public void testGetDataStore3() throws ClassNotFoundException {
    DataStore<?,?> dataStore1 = DataStoreFactory.getDataStore("org.gora.mock.store.MockDataStore"
        , Object.class, MockPersistent.class);
    DataStore<?,?> dataStore2 = DataStoreFactory.getDataStore("org.gora.mock.store.MockDataStore"
        , Object.class, MockPersistent.class);
    DataStore<?,?> dataStore3 = DataStoreFactory.getDataStore("org.gora.mock.store.MockDataStore"
        , String.class, MockPersistent.class);
    
    Assert.assertTrue(dataStore1 == dataStore2);
    Assert.assertNotSame(dataStore1, dataStore3);
  }
  
  @Test
  public void testReadProperties() {
    //indirect testing
    DataStore<?,?> dataStore = DataStoreFactory.getDataStore(String.class, MockPersistent.class);
    Assert.assertNotNull(dataStore);
    Assert.assertEquals(MockDataStore.class, dataStore.getClass());
  }
  
  @Test
  public void testFindProperty() {
    Properties properties = DataStoreFactory.properties;
    
    DataStore<String, MockPersistent> store = new DataFileAvroStore<String,MockPersistent>();
    
    String fooValue = DataStoreFactory.findProperty(properties, store
        , "foo_property", "foo_default");
    Assert.assertEquals("foo_value", fooValue);
    
    String bazValue = DataStoreFactory.findProperty(properties, store
        , "baz_property", "baz_default");
    Assert.assertEquals("baz_value", bazValue);
    
    String barValue = DataStoreFactory.findProperty(properties, store
        , "bar_property", "bar_default");
    Assert.assertEquals("bar_value", barValue);
  }
  
}
