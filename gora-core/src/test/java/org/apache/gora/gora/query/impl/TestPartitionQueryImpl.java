
package org.gora.query.impl;

import org.apache.hadoop.io.TestWritable;
import org.gora.mock.persistency.MockPersistent;
import org.gora.mock.query.MockQuery;
import org.gora.mock.store.MockDataStore;
import org.junit.Test;

/**
 * Test case for {@link PartitionQueryImpl}
 */
public class TestPartitionQueryImpl {

  private MockDataStore dataStore = MockDataStore.get();
  
  @Test
  public void testReadWrite() throws Exception {
    
    MockQuery baseQuery = dataStore.newQuery();
    baseQuery.setStartKey("start");
    baseQuery.setLimit(42);
    
    PartitionQueryImpl<String, MockPersistent> 
      query = new PartitionQueryImpl<String, MockPersistent>(baseQuery);
    
    TestWritable.testWritable(query);
  }
  
}
