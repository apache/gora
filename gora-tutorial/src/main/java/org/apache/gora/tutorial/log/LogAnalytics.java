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
package org.apache.gora.tutorial.log;

import java.io.IOException;

import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.mapreduce.GoraMapper;
import org.apache.gora.mapreduce.GoraReducer;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.MetricDatum;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * LogAnalytics is the tutorial class to illustrate Gora MapReduce API. 
 * The analytics mapreduce job reads the web access data stored earlier by the 
 * {@link LogManager}, and calculates the aggregate daily pageviews. The
 * output of the job is stored in a Gora compatible data store. 
 * 
 * <p>See the tutorial.html file in docs or go to the 
 * <a href="http://incubator.apache.org/gora/docs/current/tutorial.html"> 
 * web site</a>for more information.</p>
 */
public class LogAnalytics extends Configured implements Tool {

  private static final Logger log = LoggerFactory.getLogger(LogAnalytics.class);
  
  /** The number of miliseconds in a day */
  private static final long DAY_MILIS = 1000 * 60 * 60 * 24;
    
  /**
   * The Mapper takes Long keys and Pageview objects, and emits 
   * tuples of &lt;url, day&gt; as keys and 1 as values. Input values are 
   * read from the input data store.
   * Note that all Hadoop serializable classes can be used as map output key and value.
   */
  public static class LogAnalyticsMapper extends GoraMapper<Long, Pageview, TextLong,
      LongWritable> {
    
    private LongWritable one = new LongWritable(1L);
  
    private TextLong tuple;
    
    @Override
    protected void setup(Context context) throws IOException ,InterruptedException {
      tuple = new TextLong();
      tuple.setKey(new Text());
      tuple.setValue(new LongWritable());
    };
    
    @Override
    protected void map(Long key, Pageview pageview, Context context)
        throws IOException ,InterruptedException {
      
      Utf8 url = pageview.getUrl();
      long day = getDay(pageview.getTimestamp());
      
      tuple.getKey().set(url.toString());
      tuple.getValue().set(day);
      
      context.write(tuple, one);
    };
    
    /** Rolls up the given timestamp to the day cardinality, so that 
     * data can be aggregated daily */
    private long getDay(long timeStamp) {
      return (timeStamp / DAY_MILIS) * DAY_MILIS; 
    }
  }
  
  /**
   * The Reducer receives tuples of &lt;url, day&gt; as keys and a list of 
   * values corresponding to the keys, and emits a combined keys and
   * {@link MetricDatum} objects. The metric datum objects are stored 
   * as job outputs in the output data store.
   */
  public static class LogAnalyticsReducer extends GoraReducer<TextLong, LongWritable,
      String, MetricDatum> {
    
    private MetricDatum metricDatum = new MetricDatum();
    
    @Override
    protected void reduce(TextLong tuple, Iterable<LongWritable> values, Context context)
      throws IOException ,InterruptedException {
      
      long sum = 0L; //sum up the values
      for(LongWritable value: values) {
        sum+= value.get();
      }
      
      String dimension = tuple.getKey().toString();
      long timestamp = tuple.getValue().get();
      
      metricDatum.setMetricDimension(new Utf8(dimension));
      metricDatum.setTimestamp(timestamp);
      
      String key = metricDatum.getMetricDimension().toString();
      key += "_" + Long.toString(timestamp);
      metricDatum.setMetric(sum);
      
      context.write(key, metricDatum);
    };
  }
  
  /**
   * Creates and returns the {@link Job} for submitting to Hadoop mapreduce.
   * @param inStore
   * @param outStore
   * @param numReducer
   * @return
   * @throws IOException
   */
  public Job createJob(DataStore<Long, Pageview> inStore,
      DataStore<String, MetricDatum> outStore, int numReducer) throws IOException {
    Job job = new Job(getConf());
    job.setJobName("Log Analytics");
    log.info("Creating Hadoop Job: " + job.getJobName());
    job.setNumReduceTasks(numReducer);
    job.setJarByClass(getClass());

    /* Mappers are initialized with GoraMapper.initMapper() or 
     * GoraInputFormat.setInput()*/
    GoraMapper.initMapperJob(job, inStore, TextLong.class, LongWritable.class,
        LogAnalyticsMapper.class, true);

    /* Reducers are initialized with GoraReducer#initReducer().
     * If the output is not to be persisted via Gora, any reducer 
     * can be used instead. */
    GoraReducer.initReducerJob(job, outStore, LogAnalyticsReducer.class);
    
    return job;
  }
  
  @Override
  public int run(String[] args) throws Exception {
    
    DataStore<Long, Pageview> inStore;
    DataStore<String, MetricDatum> outStore;
    Configuration conf = new Configuration();

    if(args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.
          getDataStore(dataStoreClass, Long.class, Pageview.class, conf);
      if(args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.
          getDataStore(dataStoreClass, String.class, MetricDatum.class, conf);
    } else {
	    inStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, conf);
	    outStore = DataStoreFactory.getDataStore(String.class, MetricDatum.class, conf);
    }
    
    Job job = createJob(inStore, outStore, 3);
    boolean success = job.waitForCompletion(true);
    
    inStore.close();
    outStore.close();
    
    log.info("Log completed with " + (success ? "success" : "failure"));
    
    return success ? 0 : 1;
  }
  
  private static final String USAGE = "LogAnalytics <input_data_store> <output_data_store>";
  
  public static void main(String[] args) throws Exception {
    if(args.length < 2) {
      System.err.println(USAGE);
      System.exit(1);
    }
    //run as any other MR job
    int ret = ToolRunner.run(new LogAnalytics(), args);
    System.exit(ret);
  }
  
}
