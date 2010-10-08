
package org.gora.mapreduce;

import java.io.IOException;

import org.apache.hadoop.mapred.HadoopTestCase;
import org.apache.hadoop.mapred.JobConf;
import org.gora.examples.generated.WebPage;
import org.gora.store.DataStore;
import org.junit.Before;
import org.junit.Test;


/**
 * Base class for Mapreduce based tests. This is just a convenience
 * class, which actually only uses {@link MapReduceTestUtils} methods to
 * run the tests.
 */
@SuppressWarnings("deprecation")
public abstract class DataStoreMapReduceTestBase extends HadoopTestCase {

  private DataStore<String, WebPage> webPageStore;
  private JobConf job;

  public DataStoreMapReduceTestBase(int mrMode, int fsMode, int taskTrackers,
      int dataNodes) throws IOException {
    super(mrMode, fsMode, taskTrackers, dataNodes);
  }

  public DataStoreMapReduceTestBase() throws IOException {
    this(HadoopTestCase.CLUSTER_MR, HadoopTestCase.DFS_FS, 2, 2);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    webPageStore = createWebPageDataStore();
    job = createJobConf();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    webPageStore.close();
  }

  protected abstract DataStore<String, WebPage> createWebPageDataStore()
    throws IOException;

  @Test
  public void testCountQuery() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, job);
  }

  @Test
  public void testWordCount() throws Exception {
    MapReduceTestUtils.testCountQuery(webPageStore, job);
  }
}
