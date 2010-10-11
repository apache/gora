
package org.gora.avro.mapreduce;

import static org.gora.avro.store.TestAvroStore.WEBPAGE_OUTPUT;

import java.io.IOException;

import org.gora.avro.store.DataFileAvroStore;
import org.gora.examples.generated.WebPage;
import org.gora.mapreduce.DataStoreMapReduceTestBase;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;

/**
 * Mapreduce tests for {@link DataFileAvroStore}.
 */
public class TestDataFileAvroStoreMapReduce extends DataStoreMapReduceTestBase {

  public TestDataFileAvroStoreMapReduce() throws IOException {
    super();
  }

  @Override
  protected DataStore<String, WebPage> createWebPageDataStore() 
    throws IOException {
    DataFileAvroStore<String,WebPage> webPageStore = new DataFileAvroStore<String, WebPage>();
    webPageStore.initialize(String.class, WebPage.class, DataStoreFactory.properties);
    webPageStore.setOutputPath(WEBPAGE_OUTPUT);
    webPageStore.setInputPath(WEBPAGE_OUTPUT);
    
    return webPageStore;
  }

}
