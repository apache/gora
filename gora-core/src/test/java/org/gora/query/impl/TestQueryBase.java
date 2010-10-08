

package org.gora.query.impl;

import junit.framework.Assert;

import org.gora.mock.query.MockQuery;
import org.gora.mock.store.MockDataStore;
import org.gora.util.TestIOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link QueryBase}.
 */
public class TestQueryBase {

  private MockDataStore dataStore = MockDataStore.get();
  private MockQuery query;
  
  private static final String[] FIELDS = {"foo", "baz", "bar"};
  private static final String START_KEY = "1_start";
  private static final String END_KEY = "2_end";
  
  @Before
  public void setUp() {
    query = dataStore.newQuery(); //MockQuery extends QueryBase
  }
  
  @Test
  public void testReadWrite() throws Exception {
    query.setFields(FIELDS);
    query.setKeyRange(START_KEY, END_KEY);
    TestIOUtils.testSerializeDeserialize(query);
    
    Assert.assertNotNull(query.getDataStore());
  }
  
  @Test
  public void testReadWrite2() throws Exception {
    query.setLimit(1000);
    query.setTimeRange(0, System.currentTimeMillis());
    TestIOUtils.testSerializeDeserialize(query);
  }

}
