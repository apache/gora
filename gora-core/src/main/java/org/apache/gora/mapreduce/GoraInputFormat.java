/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.FileBackedDataStore;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * {@link InputFormat} to fetch the input from Gora data stores. The
 * query to fetch the items from the datastore should be prepared and
 * set via {@link #setQuery(Job, Query)}, before submitting the job.
 *
 * <p> The {@link InputSplit}s are prepared from the {@link PartitionQuery}s
 * obtained by calling {@link DataStore#getPartitions(Query)}.
 * <p>
 * Hadoop jobs can be either configured through static 
 * <code>setInput()</code> methods, or from {@link GoraMapper}.
 * 
 * @see GoraMapper
 */
public class GoraInputFormat<K, T extends Persistent>
  extends InputFormat<K, T> implements Configurable {

  public static final String QUERY_KEY   = "gora.inputformat.query";

  private DataStore<K, T> dataStore;

  private Configuration conf;

  private Query<K, T> query;

  @SuppressWarnings({ "rawtypes" })
  private void setInputPath(PartitionQuery<K,T> partitionQuery
      , TaskAttemptContext context) throws IOException {
    //if the data store is file based
    if(partitionQuery instanceof FileSplitPartitionQuery) {
      FileSplit split = ((FileSplitPartitionQuery<K,T>)partitionQuery).getSplit();
      //set the input path to FileSplit's path.
      ((FileBackedDataStore)partitionQuery.getDataStore()).setInputPath(
          split.getPath().toString());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public RecordReader<K, T> createRecordReader(InputSplit split,
      TaskAttemptContext context) throws IOException, InterruptedException {
    PartitionQuery<K,T> partitionQuery = (PartitionQuery<K, T>)
      ((GoraInputSplit)split).getQuery();

    setInputPath(partitionQuery, context);
    return new GoraRecordReader<K, T>(partitionQuery, context);
  }

  @Override
  public List<InputSplit> getSplits(JobContext context) throws IOException,
      InterruptedException {

    List<PartitionQuery<K, T>> queries = dataStore.getPartitions(query);
    List<InputSplit> splits = new ArrayList<InputSplit>(queries.size());

    for(PartitionQuery<K,T> query : queries) {
      splits.add(new GoraInputSplit(context.getConfiguration(), query));
    }

    return splits;
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
    try {
      this.query = getQuery(conf);
      this.dataStore = query.getDataStore();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static<K, T extends Persistent> void setQuery(Job job
      , Query<K, T> query) throws IOException {
    IOUtils.storeToConf(query, job.getConfiguration(), QUERY_KEY);
  }

  public Query<K, T> getQuery(Configuration conf) throws IOException {
    return IOUtils.loadFromConf(conf, QUERY_KEY);
  }

  /**
   * Sets the input parameters for the job
   * @param job the job to set the properties for
   * @param query the query to get the inputs from
   * @param reuseObjects whether to reuse objects in serialization
   * @throws IOException
   */
  public static <K1, V1 extends Persistent> void setInput(Job job
      , Query<K1,V1> query, boolean reuseObjects) throws IOException {
    setInput(job, query, query.getDataStore(), reuseObjects);
  }

  /**
   * Sets the input parameters for the job
   * @param job the job to set the properties for
   * @param query the query to get the inputs from
   * @param dataStore the datastore as the input
   * @param reuseObjects whether to reuse objects in serialization
   * @throws IOException
   */
  public static <K1, V1 extends Persistent> void setInput(Job job
      , Query<K1,V1> query, DataStore<K1,V1> dataStore, boolean reuseObjects)
  throws IOException {

    Configuration conf = job.getConfiguration();

    GoraMapReduceUtils.setIOSerializations(conf, reuseObjects);

    job.setInputFormatClass(GoraInputFormat.class);
    GoraInputFormat.setQuery(job, query);
  }
  
  /**
   * Sets the input parameters for the job
   * @param job the job to set the properties for
   * @param dataStoreClass the datastore class
   * @param inKeyClass Map input key class
   * @param inValueClass Map input value class
   * @param reuseObjects whether to reuse objects in serialization
   * @throws IOException
   */
  public static <K1, V1 extends Persistent> void setInput(
      Job job, 
      Class<? extends DataStore<K1,V1>> dataStoreClass, 
      Class<K1> inKeyClass, 
      Class<V1> inValueClass,
      boolean reuseObjects)
  throws IOException {

    DataStore<K1,V1> store = DataStoreFactory.getDataStore(dataStoreClass
        , inKeyClass, inValueClass, job.getConfiguration());
    setInput(job, store.newQuery(), store, reuseObjects);
  }
}
