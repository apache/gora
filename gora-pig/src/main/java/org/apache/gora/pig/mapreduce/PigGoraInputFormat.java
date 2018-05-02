package org.apache.gora.pig.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.gora.mapreduce.GoraInputFormat;
import org.apache.gora.mapreduce.GoraInputSplit;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.pig.StorageConfiguration;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is needed to override the default behavior of GoraInputFormat that saves the QueryBase in Configuration in a serialized format,
 * shared by all the Pig Job, that will show problems once deserialized for each split. This class overrides that behavior, since we don't need to
 * serialize the query and deserialize it later.
 * 
 * - Serialization:
 * 
 * In Pig, the GoraInputSplits are serialized and send to the backend.
 * Once in the backend, the GoraInputSplits are deserialized and from the DataInput we get the PartitionQuery. The local configuration is applied by Pig.
 * The PartitionQuery deserializes the QueryBase (and conf applied).
 * The QueryBase instanciates a new datastore and configures it with the serialized attributes, including the key, value and Properties (were
 * we have the important information).
 * 
 * All data needed data is sent serialized with the Writable interface, and we must not use the Configurable interface (hadoop configuration) to
 * pass not-shared configuration.
 * 
 * @param <K> - Key
 * @param <T> - Persistent value
 */
public class PigGoraInputFormat<K, T extends PersistentBase> extends GoraInputFormat<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(PigGoraInputFormat.class);

  protected Configuration conf ;
  
  /**
   * The GoraStorage configuration setted at constructor (converted from json to bean)
   */
  protected StorageConfiguration storageConfiguration ;
  
  protected DataStore<K,T> dataStore ;
  
  /**
   * The visibility of the attribute query in GoraInputFormat is private
   */
  protected Query<K,T> query ;
  
  @Override
  public List<InputSplit> getSplits(JobContext context) throws IOException, InterruptedException {

    List<PartitionQuery<K,T>> partitionsQueries = this.dataStore.getPartitions(query);
    List<InputSplit> splits = new ArrayList<>(partitionsQueries.size());
   
    for(PartitionQuery<K,T> queryForSplit : partitionsQueries) {
      splits.add(new GoraInputSplit(context.getConfiguration(), queryForSplit));
    }

    return splits;
  }
  
  public StorageConfiguration getStorageConfiguration() {
    return storageConfiguration;
  }

  public void setStorageConfiguration(StorageConfiguration storageConfiguration) {
    this.storageConfiguration = storageConfiguration;
  }

  /**
   * Override the setQuery because we don't want the serialization into Configuration made by super
   * @param query
   * @throws IOException
   */
  public void setQuery(Query<K, T> query) throws IOException {
    this.query = query ;
  }

  /**
   * Override the getQuery because we don't want the deserialization from Configuration made by super
   */
  public Query<K, T> getQuery(Configuration conf) throws IOException {
    return this.query ;
  }

  /**
   * Override to disable super implementation
   */
  public void setConf(Configuration conf) {
    this.conf = conf ;
  }
  
  /**
   * Override to disable super implementation
   */
  public Configuration getConf() {
    return this.conf ;
  }

  /**
   * Override to disable super implementation
   */
  public DataStore<?, ? extends PersistentBase> getDataStore() {
    return dataStore;
  }

  /**
   * Override to disable super implementation
   */
  public void setDataStore(DataStore<K,T> dataStore) {
    this.dataStore = dataStore;
  }
  
}
