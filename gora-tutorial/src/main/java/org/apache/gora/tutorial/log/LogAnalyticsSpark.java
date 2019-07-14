/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.tutorial.log;

import org.apache.gora.spark.GoraSparkEngine;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.MetricDatum;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

/**
 * LogAnalyticsSpark is the tutorial class to illustrate Gora Spark API. The
 * Spark job reads the web access data stored earlier by the {@link LogManager},
 * and calculates the aggregate daily pageviews. The output of the job is stored
 * in a Gora compatible data store.
 *
 * This class illustrates the same functionality with {@link LogAnalytics} via
 * Spark.
 *
 * <p>
 * See the tutorial.html file in docs or go to the <a
 * href="http://incubator.apache.org/gora/docs/current/tutorial.html"> web
 * site</a>for more information.
 * </p>
 */
public class LogAnalyticsSpark {

  private static final Logger log = LoggerFactory.getLogger(LogAnalyticsSpark.class);

  private static final String USAGE = "LogAnalyticsSpark <input_data_store> <output_data_store>";

  /** The number of milliseconds in a day */
  private static final long DAY_MILIS = 1000 * 60 * 60 * 24;

  /**
   * map function used in calculation
   */
  private static Function<Pageview, Tuple2<Tuple2<String, Long>, Long>> mapFunc = new Function<Pageview, Tuple2<Tuple2<String, Long>, Long>>() {
    @Override
    public Tuple2<Tuple2<String, Long>, Long> call(Pageview pageview)
        throws Exception {
      String url = pageview.getUrl().toString();
      Long day = getDay(pageview.getTimestamp());
      Tuple2<String, Long> keyTuple = new Tuple2<>(url, day);

      return new Tuple2<>(keyTuple, 1L);
    }
  };

    /**
     * reduce function used in calculation
     */
  private static Function2<Long, Long, Long> redFunc = new Function2<Long, Long, Long>() {
    @Override
    public Long call(Long aLong, Long aLong2) throws Exception {
      return aLong + aLong2;
    }
  };

    /**
     * metric function used after map phase
     */
  private static PairFunction<Tuple2<Tuple2<String, Long>, Long>, String, MetricDatum> metricFunc = new PairFunction<Tuple2<Tuple2<String, Long>, Long>, String, MetricDatum>() {
    @Override
    public Tuple2<String, MetricDatum> call(
        Tuple2<Tuple2<String, Long>, Long> tuple2LongTuple2) throws Exception {
      String dimension = tuple2LongTuple2._1()._1();
      long timestamp = tuple2LongTuple2._1()._2();

      MetricDatum metricDatum = new MetricDatum();
      metricDatum.setMetricDimension(dimension);
      metricDatum.setTimestamp(timestamp);

      String key = metricDatum.getMetricDimension().toString();
      key += "_" + Long.toString(timestamp);
      metricDatum.setMetric(tuple2LongTuple2._2());
      return new Tuple2<>(key, metricDatum);
    }
  };

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      log.error(USAGE);
      System.exit(1);
    }

    LogAnalyticsSpark logAnalyticsSpark = new LogAnalyticsSpark();

    try {
      int ret = logAnalyticsSpark.run(args);
      System.exit(ret);
    } catch (Exception ex){
      log.error("Error occurred!");
    }

  }

  /**
   * Rolls up the given timestamp to the day cardinality, so that data can be
   * aggregated daily
   */
  private static long getDay(long timeStamp) {
    return (timeStamp / DAY_MILIS) * DAY_MILIS;
  }

  public int run(String[] args) throws Exception {

    DataStore<Long, Pageview> inStore;
    DataStore<String, MetricDatum> outStore;
    Configuration hadoopConf = new Configuration();

    if (args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(
      dataStoreClass, Long.class, Pageview.class, hadoopConf);
      if (args.length > 1) {
        dataStoreClass = args[1];
      }
        outStore = DataStoreFactory.getDataStore(
        dataStoreClass, String.class, MetricDatum.class, hadoopConf);
      } else {
        inStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, hadoopConf);
        outStore = DataStoreFactory.getDataStore(String.class, MetricDatum.class, hadoopConf);
    }

    //Spark engine initialization
    GoraSparkEngine<Long, Pageview> goraSparkEngine = new GoraSparkEngine<>(Long.class,
        Pageview.class);

    SparkConf sparkConf = new SparkConf().setAppName(
        "Gora Spark Integration Application").setMaster("local");

    Class[] c = new Class[1];
    c[0] = inStore.getPersistentClass();
    sparkConf.registerKryoClasses(c);
    //
    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    JavaPairRDD<Long, Pageview> goraRDD = goraSparkEngine.initialize(sc, inStore);

    long count = goraRDD.count();
    log.info("Total Log Count: {}", count);

    JavaRDD<Tuple2<Tuple2<String, Long>, Long>> mappedGoraRdd = goraRDD
        .values().map(mapFunc);

    JavaPairRDD<String, MetricDatum> reducedGoraRdd = JavaPairRDD
        .fromJavaRDD(mappedGoraRdd).reduceByKey(redFunc).mapToPair(metricFunc);

    log.info("MetricDatum count: {}", reducedGoraRdd.count());

    //Print output for debug purpose
    /*
    Map<String, MetricDatum> metricDatumMap = reducedGoraRdd.collectAsMap();
    for (String key : metricDatumMap.keySet()) {
      System.out.println(key);
    }
    */
    //

    //write output to datastore
    Configuration sparkHadoopConf = goraSparkEngine.generateOutputConf(outStore);
    reducedGoraRdd.saveAsNewAPIHadoopDataset(sparkHadoopConf);
    //

    inStore.close();
    outStore.close();

    log.info("Log completed with success");

    return 1;
  }
}
