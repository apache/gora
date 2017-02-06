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

import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.mapreduce.GoraMapper;
import org.apache.gora.mapreduce.GoraReducer;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that serializing from Map to Reduce holds the dirty state
 */
public class MapReduceSerialization extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(MapReduceSerialization.class);

  public MapReduceSerialization() {
  }

  public MapReduceSerialization(Configuration conf) {
    setConf(conf);
  }

  /**
   * {@link CheckDirtyBitsSerializationMapper} gets a WebPage, sets
   * the URL to the string "hola" and emits it with the same key.
   */
  public static class CheckDirtyBitsSerializationMapper
          extends GoraMapper<String, WebPage, Text, WebPage> {
    @Override
    protected void map(String key, WebPage page, Context context)
            throws IOException, InterruptedException {
      page.setUrl("hola");
      context.write(new Text(key), page);
    }
  }

  /**
   * {@link CheckDirtyBytesSerializationReducer} just take vales and emits
   * them as is.
   */
  public static class CheckDirtyBytesSerializationReducer extends GoraReducer<Text, WebPage,
          String, WebPage> {
    @Override
    protected void reduce(Text key, Iterable<WebPage> values, Context context)
            throws IOException, InterruptedException {
      for (WebPage val : values) {
        LOG.info(key.toString());
        LOG.info(val.toString());
        LOG.info(String.valueOf(val.isDirty()));
        context.write(key.toString(), val);
      }
    }
  }

  /**
   * Creates and returns the {@link Job} for submitting to Hadoop mapreduce.
   *
   * @param inStore  input store on MR jobs runs on
   * @param query    query to select input set run MR
   * @param outStore output store which stores results of MR jobs
   * @return job MR job definition
   * @throws IOException
   */
  public Job createJob(DataStore<String, WebPage> inStore, Query<String, WebPage> query
          , DataStore<String, WebPage> outStore) throws IOException {
    Job job = new Job(getConf());

    job.setJobName("Check serialization of dirty bits");

    job.setNumReduceTasks(1);
    job.setJarByClass(getClass());
    
    /* Mappers are initialized with GoraMapper#initMapper().
     * Instead of the TokenizerMapper defined here, if the input is not 
     * obtained via Gora, any other mapper can be used, such as 
     * Hadoop-MapReduce's WordCount.TokenizerMapper.
     */
    GoraMapper.initMapperJob(job, query, Text.class
            , WebPage.class, CheckDirtyBitsSerializationMapper.class, true);
    
    /* Reducers are initialized with GoraReducer#initReducer().
     * If the output is not to be persisted via Gora, any reducer 
     * can be used instead.
     */
    GoraReducer.initReducerJob(job, outStore, CheckDirtyBytesSerializationReducer.class);

    return job;
  }

  public int mapReduceSerialization(DataStore<String, WebPage> inStore,
                                    DataStore<String, WebPage> outStore)
          throws IOException, InterruptedException, ClassNotFoundException {
    Query<String, WebPage> query = inStore.newQuery();
    query.setFields("url");

    Job job = createJob(inStore, query, outStore);
    return job.waitForCompletion(true) ? 0 : 1;
  }

  @Override
  public int run(String[] args) throws Exception {

    DataStore<String, WebPage> inStore;
    DataStore<String, WebPage> outStore;
    Configuration conf = new Configuration();
    if (args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(dataStoreClass,
              String.class, WebPage.class, conf);
      if (args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(dataStoreClass,
              String.class, WebPage.class, conf);
    } else {
      inStore = DataStoreFactory.getDataStore(String.class, WebPage.class, conf);
      outStore = DataStoreFactory.getDataStore(String.class, WebPage.class, conf);
    }

    return mapReduceSerialization(inStore, outStore);
  }

  // Usage WordCount [<input datastore class> [output datastore class]]
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new MapReduceSerialization(), args);
    System.exit(ret);
  }

}