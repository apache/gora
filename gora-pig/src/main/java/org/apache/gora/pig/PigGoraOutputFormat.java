package org.apache.gora.pig;

import java.io.IOException;

import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

public class PigGoraOutputFormat<K, T extends Persistent> extends GoraOutputFormat<K, T> {

  protected Configuration localConfiguration ;
  
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public RecordWriter<K, T> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {

    Class<? extends DataStore<K,T>> dataStoreClass = (Class<? extends DataStore<K,T>>) this.localConfiguration.getClass(DATA_STORE_CLASS, null);
    Class<K> keyClass = (Class<K>) this.localConfiguration.getClass(OUTPUT_KEY_CLASS, null);
    Class<T> rowClass = (Class<T>) this.localConfiguration.getClass(OUTPUT_VALUE_CLASS, null);
    final DataStore<K, T> store = DataStoreFactory.createDataStore(dataStoreClass, keyClass, rowClass, this.localConfiguration);
    return new GoraRecordWriter(store, context);
  }
  
  public void setConf(Configuration conf) {
    this.localConfiguration = conf ;
  }
  
  public Configuration getConf() {
    return this.localConfiguration ;
  }
  
}
