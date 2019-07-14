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

import org.apache.avro.util.Utf8;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.gora.flink.GoraFlinkEngine;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.MetricDatum;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * LogAnalyticsFlink is the tutorial class to illustrate Gora's Flink Job API.
 * The analytics Flink Job reads the web access data stored earlier by the
 * {@link LogManager}, and calculates the aggregate daily pageviews. The
 * output of the job is stored in a Gora compatible data store.
 *
 * <p>See the tutorial.html file in docs or go to the
 * <a href="http://gora.apache.org/current/tutorial.html">
 * web site</a>for more information.</p>
 */
public class LogAnalyticsFlink {

  private static final Logger log = LoggerFactory.getLogger(LogAnalyticsFlink.class);

  private static final String USAGE = "LogAnalyticsFlink <input_data_store> <output_data_store>";

  /**
   * The number of miliseconds in a day
   */
  private static final long DAY_MILIS = 1000 * 60 * 60 * 24;

  // *************************************************************************
  //     FLINK FUNCTIONS
  // *************************************************************************

  /**
   * The Map function takes Long keys and Pageview objects, and emits
   * tuples of &lt;url, day&gt; as keys and 1 as values. Input values are
   * read from the input data store.
   * Note that all Hadoop serializable classes can be used as map output key and value.
   */
  public static final class LogAnalyticsMapFunction implements FlatMapFunction<Tuple2<Long, Pageview>,
          Tuple2<TextLong, LongWritable>> {

    @Override
    public void flatMap(Tuple2<Long, Pageview> in, Collector<Tuple2<TextLong, LongWritable>> out) {
      // extract from input tuple
      Pageview pageview = in.getField(1);
      CharSequence url = pageview.getUrl();
      long day = getDay(pageview.getTimestamp());

      // create key for emitting tuple
      TextLong tuple = new TextLong();
      tuple.setKey(new Text());
      tuple.setValue(new LongWritable());
      tuple.getKey().set(url.toString());
      tuple.getValue().set(day);

      // create value for emitting tuple
      LongWritable one = new LongWritable(1L);

      // emit tuple
      out.collect(new Tuple2<>(tuple, one));
    }

    /**
     * Rolls up the given timestamp to the day cardinality, so that
     * data can be aggregated daily
     */
    private long getDay(long timeStamp) {
      return (timeStamp / DAY_MILIS) * DAY_MILIS;
    }

  }

  /**
   * The Reduce function receives tuples of &lt;url, day&gt; as keys and a list of
   * values corresponding to the keys, and emits a combined keys and
   * {@link MetricDatum} objects. The metric datum objects are stored
   * as job outputs in the output data store.
   */
  public static final class LogAnalyticsReduceFunction implements
          GroupReduceFunction<Tuple2<TextLong, LongWritable>, Tuple2<String, MetricDatum>> {

    @Override
    public void reduce(Iterable<Tuple2<TextLong, LongWritable>> in, Collector<Tuple2<String, MetricDatum>> out) {
      Iterator<Tuple2<TextLong, LongWritable>> iterator = in.iterator();

      // Calculate summation of tuples first field values Eg:- tuple.getField(1)
      TextLong currentKey = null;
      long sum = 0L;
      while (iterator.hasNext()) {
        Tuple2<TextLong, LongWritable> tuple = iterator.next();
        currentKey = tuple.getField(0);
        LongWritable value = tuple.getField(1);
        sum += value.get();
      }

      // create value for emitting tuple
      MetricDatum metricDatum = new MetricDatum();
      String dimension = currentKey.getKey().toString();
      long timestamp = currentKey.getValue().get();

      metricDatum.setMetricDimension(new Utf8(dimension));
      metricDatum.setTimestamp(timestamp);

      // create key for emitting tuple
      String key = metricDatum.getMetricDimension().toString();
      key += "_" + Long.toString(timestamp);
      metricDatum.setMetric(sum);

      // emit tuple
      out.collect(new Tuple2<>(key, metricDatum));
    }

  }

  // *************************************************************************
  //     JAVA MAIN FUNCTION
  // *************************************************************************

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      log.error(USAGE);
      System.exit(1);
    }

    LogAnalyticsFlink logAnalyticsFlink = new LogAnalyticsFlink();

    try {
      int ret = logAnalyticsFlink.run(args);
      System.exit(ret);
    } catch (Exception ex) {
      log.error("Error occurred!");
    }

  }

  public int run(String[] args) throws Exception {
    Configuration conf = new Configuration();

    DataStore<Long, Pageview> inStore;
    DataStore<String, MetricDatum> outStore;
    if (args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(
              dataStoreClass, Long.class, Pageview.class, conf);
      if (args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(
              dataStoreClass, String.class, MetricDatum.class, conf);
    } else {
      inStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, conf);
      outStore = DataStoreFactory.getDataStore(String.class, MetricDatum.class, conf);
    }

    // Create Apache Flink Execution environment
    ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

    // Create Apache Gora - Flink Engine
    GoraFlinkEngine<Long, Pageview, String, MetricDatum> flinkEngine =
            new GoraFlinkEngine<>(Long.class, Pageview.class, String.class, MetricDatum.class);

    // Create Gora Data source
    DataSet<Tuple2<Long, Pageview>> logs = flinkEngine.createDataSource(env, conf, inStore);

    DataSet<Tuple2<String, MetricDatum>> metrics = logs
            .flatMap(new LogAnalyticsMapFunction())
            .groupBy(0)
            .reduceGroup(new LogAnalyticsReduceFunction());

    // Register Gora Sink source
    metrics.output(flinkEngine.createDataSink(conf, outStore));

    // Execute Flink Analytic Job
    env.execute("Gora Log Analytics on Apache Flink Engine");

    log.info("Total Log Count: {}", logs.count());
    log.info("MetricDatum count: {}", metrics.count());

    inStore.close();
    outStore.close();

    log.info("Flink Job completed with success");

    return 1;
  }

}
