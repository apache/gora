
package org.gora.hbase.mapreduce;

import org.apache.hadoop.hbase.HBaseClusterTestCase;
import org.gora.examples.generated.TokenDatum;
import org.gora.examples.generated.WebPage;
import org.gora.hbase.store.HBaseStore;
import org.gora.mapreduce.MapReduceTestUtils;
import org.gora.store.DataStoreFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests related to {@link HBaseStore} using mapreduce.
 */
public class TestHBaseStoreMapReduce extends HBaseClusterTestCase{

  private HBaseStore<String, WebPage> webPageStore;
  private HBaseStore<String, TokenDatum> tokenStore;
  
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    webPageStore = DataStoreFactory.getDataStore(
        HBaseStore.class, String.class, WebPage.class);
    tokenStore = DataStoreFactory.getDataStore(HBaseStore.class, 
        String.class, TokenDatum.class);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    webPageStore.close();
  }
  
  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, conf);
  }
  
  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testWordCount(conf, webPageStore, tokenStore);
  }
  
  public static void main(String[] args) throws Exception {
   TestHBaseStoreMapReduce test =  new TestHBaseStoreMapReduce();
   test.setUp();
   test.testCountQuery();
  }
}
