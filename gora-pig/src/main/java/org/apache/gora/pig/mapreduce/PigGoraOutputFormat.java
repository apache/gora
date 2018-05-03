package org.apache.gora.pig.mapreduce;

import java.io.IOException;

import org.apache.gora.mapreduce.GoraRecordWriter;
import org.apache.gora.mapreduce.NullOutputCommitter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.pig.StorageConfiguration;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PigGoraOutputFormat<K, T extends Persistent> extends OutputFormat<K, T> implements Configurable {
  
  public static final Logger LOG = LoggerFactory.getLogger(PigGoraOutputFormat.class);

  protected Configuration conf ;
  
  /**
   * The GoraStorage configuration setted at constructor (converted from json to bean)
   */
  protected StorageConfiguration storageConfiguration ;
  
  protected DataStore<K,T> dataStore ;
  
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public RecordWriter<K, T> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {

    if ( this.dataStore == null ) {
      this.dataStore = DataStoreFactory.getDataStore(
          this.storageConfiguration.getKeyClass(),
          this.storageConfiguration.getPersistentClass(),
          this.storageConfiguration.getGoraPropertiesAsProperties(),
          this.conf
          ) ;      
    }
    
    return new GoraRecordWriter(this.dataStore, context);
  }


  @Override
  public void checkOutputSpecs(JobContext context) throws IOException, InterruptedException {
    // Nothing
  }

  @Override
  public OutputCommitter getOutputCommitter(TaskAttemptContext context) throws IOException, InterruptedException {
    return new NullOutputCommitter();
  }
  
  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  public StorageConfiguration getStorageConfiguration() {
    return storageConfiguration;
  }

  public void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    this.storageConfiguration = storageConfiguration;
  }

  public DataStore<K, T> getDataStore() {
    return dataStore;
  }

  public void setDataStore(DataStore<K, T> dataStore) {
    this.dataStore = dataStore;
  }
  
}
