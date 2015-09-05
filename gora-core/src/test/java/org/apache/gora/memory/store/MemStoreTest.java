package org.apache.gora.memory.store;

import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.store.DataStore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

public class MemStoreTest {

  @Test
  public void testGetMissingValue() {
    DataStore<String, WebPage> store = new MemStore<>();
    WebPage nullWebPage = store.get("missing", new String[0]);
    assertNull(nullWebPage);
    store.close();
  }

  @Test
  public void testPutGet() throws Exception {
    String key = "org.apache.gora:http:/";
    DataStore<String, WebPage> store = new MemStore<>();
    assumeTrue(store.get(key, new String[0]) == null);
    store.put(key, WebPage.newBuilder().build());
    assertNotNull(store.get(key, new String[0]));
    store.close();
  }

}
