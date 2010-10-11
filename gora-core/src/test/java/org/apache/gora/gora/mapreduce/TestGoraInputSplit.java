
package org.gora.mapreduce;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.TestWritable;
import org.gora.mock.persistency.MockPersistent;
import org.gora.mock.query.MockQuery;
import org.gora.mock.store.MockDataStore;
import org.gora.query.PartitionQuery;
import org.junit.Test;

/**
 * Test case for {@link GoraInputSplit}.
 */
public class TestGoraInputSplit {

  private Configuration conf = new Configuration();
  
  private List<PartitionQuery<String, MockPersistent>> 
    getPartitions() throws IOException {
    MockDataStore store = MockDataStore.get();
    MockQuery query = store.newQuery();

    List<PartitionQuery<String, MockPersistent>> partitions = 
      store.getPartitions(query);
    return partitions;
  }
  
  @Test
  public void testGetLocations() throws IOException {
    List<PartitionQuery<String, MockPersistent>> partitions = 
      getPartitions();

    int i=0;;
    for(PartitionQuery<String, MockPersistent> partition : partitions) {
      GoraInputSplit split = new GoraInputSplit(conf, partition);
      Assert.assertEquals(split.getLocations().length, 1);
      Assert.assertEquals(split.getLocations()[0], MockDataStore.LOCATIONS[i++]);
    }
  }

  @Test
  public void testReadWrite() throws Exception {
    
    List<PartitionQuery<String, MockPersistent>> partitions = 
      getPartitions();

    for(PartitionQuery<String, MockPersistent> partition : partitions) {
      GoraInputSplit split = new GoraInputSplit(conf, partition);
      TestWritable.testWritable(split);
    }
  }
  
}
