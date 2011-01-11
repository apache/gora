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

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.FileBackedDataStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * {@link OutputFormat} for Hadoop jobs that want to store the job outputs 
 * to a Gora store. 
 * <p>
 * Hadoop jobs can be either configured through static 
 * <code>setOutput()</code> methods, or if the job is not map-only from {@link GoraReducer}.
 * @see GoraReducer 
 */
public class GoraOutputFormat<K, T extends Persistent>
  extends OutputFormat<K, T> {

  public static final String DATA_STORE_CLASS = "gora.outputformat.datastore.class";

  public static final String OUTPUT_KEY_CLASS   = "gora.outputformat.key.class";

  public static final String OUTPUT_VALUE_CLASS = "gora.outputformat.value.class";

  @Override
  public void checkOutputSpecs(JobContext context)
  throws IOException, InterruptedException { }

  @Override
  public OutputCommitter getOutputCommitter(TaskAttemptContext context)
  throws IOException, InterruptedException {
    return new NullOutputCommitter();
  }

  private void setOutputPath(DataStore<K,T> store, TaskAttemptContext context) {
    if(store instanceof FileBackedDataStore) {
      FileBackedDataStore<K, T> fileStore = (FileBackedDataStore<K, T>) store;
      String uniqueName = FileOutputFormat.getUniqueFile(context, "part", "");

      //if file store output is not set, then get the output from FileOutputFormat
      if(fileStore.getOutputPath() == null) {
        fileStore.setOutputPath(FileOutputFormat.getOutputPath(context).toString());
      }

      //set the unique name of the data file
      String path = fileStore.getOutputPath();
      fileStore.setOutputPath( path + Path.SEPARATOR  + uniqueName);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public RecordWriter<K, T> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    Class<? extends DataStore<K,T>> dataStoreClass
      = (Class<? extends DataStore<K,T>>) conf.getClass(DATA_STORE_CLASS, null);
    Class<K> keyClass = (Class<K>) conf.getClass(OUTPUT_KEY_CLASS, null);
    Class<T> rowClass = (Class<T>) conf.getClass(OUTPUT_VALUE_CLASS, null);
    final DataStore<K, T> store =
      DataStoreFactory.createDataStore(dataStoreClass, keyClass, rowClass);

    setOutputPath(store, context);

    return new GoraRecordWriter(store, context);
  }

  /**
   * Sets the output parameters for the job
   * @param job the job to set the properties for
   * @param dataStore the datastore as the output
   * @param reuseObjects whether to reuse objects in serialization
   */
  public static <K, V extends Persistent> void setOutput(Job job,
      DataStore<K,V> dataStore, boolean reuseObjects) {
    setOutput(job, dataStore.getClass(), dataStore.getKeyClass()
        , dataStore.getPersistentClass(), reuseObjects);
  }

  /**
   * Sets the output parameters for the job 
   * @param job the job to set the properties for
   * @param dataStoreClass the datastore class
   * @param keyClass output key class
   * @param persistentClass output value class
   * @param reuseObjects whether to reuse objects in serialization
   */
  @SuppressWarnings("rawtypes")
  public static <K, V extends Persistent> void setOutput(Job job,
      Class<? extends DataStore> dataStoreClass,
      Class<K> keyClass, Class<V> persistentClass,
      boolean reuseObjects) {

    Configuration conf = job.getConfiguration();

    GoraMapReduceUtils.setIOSerializations(conf, reuseObjects);

    job.setOutputFormatClass(GoraOutputFormat.class);
    job.setOutputKeyClass(keyClass);
    job.setOutputValueClass(persistentClass);
    conf.setClass(GoraOutputFormat.DATA_STORE_CLASS, dataStoreClass,
        DataStore.class);
    conf.setClass(GoraOutputFormat.OUTPUT_KEY_CLASS, keyClass, Object.class);
    conf.setClass(GoraOutputFormat.OUTPUT_VALUE_CLASS,
        persistentClass, Persistent.class);
  }
}
