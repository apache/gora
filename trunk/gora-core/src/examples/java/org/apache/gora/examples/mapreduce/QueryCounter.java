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

package org.apache.gora.examples.mapreduce;

import java.io.IOException;

import org.apache.gora.mapreduce.GoraMapper;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.gora.util.ClassLoadingUtils;

/**
 * Example Hadoop job to count the row of a gora {@link Query}.
 */
public class QueryCounter<K, T extends Persistent> extends Configured implements Tool {

  public static final String COUNTER_GROUP = "QueryCounter";
  public static final String ROWS = "ROWS";

  public QueryCounter(Configuration conf) {
    setConf(conf);
  }

  public static class QueryCounterMapper<K, T extends Persistent>
  extends GoraMapper<K, T
    , NullWritable, NullWritable> {

    @Override
    protected void map(K key, T value,
        Context context) throws IOException ,InterruptedException {

      context.getCounter(COUNTER_GROUP, ROWS).increment(1L);
    };
  }

  /** Returns the Query to count the results of. Subclasses can
   * override this function to customize the query.
   * @return the Query object to count the results of.
   */
  public Query<K, T> getQuery(DataStore<K,T> dataStore) {
    Query<K,T> query = dataStore.newQuery();
    return query;
  }

  /**
   * Creates and returns the {@link Job} for submitting to Hadoop mapreduce.
   * @param dataStore
   * @param query
   * @return
   * @throws IOException
   */
  public Job createJob(DataStore<K,T> dataStore, Query<K,T> query) throws IOException {
    Job job = new Job(getConf());

    job.setJobName("QueryCounter");
    job.setNumReduceTasks(0);
    job.setJarByClass(getClass());
    /* Mappers are initialized with GoraMapper.initMapper()*/
    GoraMapper.initMapperJob(job, query, dataStore, NullWritable.class
        , NullWritable.class, QueryCounterMapper.class, true);

    job.setOutputFormatClass(NullOutputFormat.class);
    return job;
  }


  /**
   * Returns the number of results to the Query
   */
  public long countQuery(DataStore<K,T> dataStore, Query<K,T> query) throws Exception {
    Job job = createJob(dataStore, query);
    job.waitForCompletion(true);

    return job.getCounters().findCounter(COUNTER_GROUP, ROWS).getValue();
  }

  /**
   * Returns the number of results to the Query obtained by the
   * {@link #getQuery(DataStore)} method.
   */
  public long countQuery(DataStore<K,T> dataStore) throws Exception {
    Query<K,T> query = getQuery(dataStore);

    Job job = createJob(dataStore, query);
    job.waitForCompletion(true);

    return job.getCounters().findCounter(COUNTER_GROUP, ROWS).getValue();
  }

  @SuppressWarnings("unchecked")
  @Override
  public int run(String[] args) throws Exception {

    if(args.length < 2) {
      System.err.println("Usage QueryCounter <keyClass> <persistentClass> [dataStoreClass]");
      return 1;
    }

    Class<K> keyClass = (Class<K>) ClassLoadingUtils.loadClass(args[0]);
    Class<T> persistentClass = (Class<T>) ClassLoadingUtils.loadClass(args[1]);

    DataStore<K,T> dataStore;
    Configuration conf = new Configuration();

    if(args.length > 2) {
      Class<? extends DataStore<K,T>> dataStoreClass
          = (Class<? extends DataStore<K, T>>) Class.forName(args[2]);
      dataStore = DataStoreFactory.getDataStore(dataStoreClass, keyClass, persistentClass, conf);
    }
    else {
      dataStore = DataStoreFactory.getDataStore(keyClass, persistentClass, conf);
    }

    long results = countQuery(dataStore);

    System.out.println("Number of result to the query:" + results);

    return 0;
  }


  @SuppressWarnings("rawtypes")
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new QueryCounter(new Configuration()), args);
    System.exit(ret);
  }
}
