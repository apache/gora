
package org.gora.mapreduce;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.gora.examples.generated.Employee;
import org.gora.mock.persistency.MockPersistent;
import org.gora.mock.query.MockQuery;
import org.gora.mock.store.MockDataStore;
import org.gora.query.PartitionQuery;
import org.junit.Test;

public class TestGoraInputFormat {

  public List<InputSplit> getInputSplits()
    throws IOException, InterruptedException {

    Job job = new Job();
    MockDataStore store = MockDataStore.get();

    MockQuery query = store.newQuery();
    query.setFields(Employee._ALL_FIELDS);
    GoraInputFormat.setInput(job, query, false);

    GoraInputFormat<String, MockPersistent> inputFormat
      = new GoraInputFormat<String, MockPersistent>();

    inputFormat.setConf(job.getConfiguration());

    return inputFormat.getSplits(job);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void testGetSplits() throws IOException, InterruptedException {
    List<InputSplit> splits = getInputSplits();

    Assert.assertTrue(splits.size() > 0);

    InputSplit split = splits.get(0);
    PartitionQuery query = ((GoraInputSplit)split).getQuery();
    Assert.assertTrue(Arrays.equals(Employee._ALL_FIELDS, query.getFields()));
  }

}