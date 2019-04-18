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
package org.apache.gora.examples.spark;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.spark.GoraSparkEngine;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
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
 * Classic word count example in Gora with Spark.
 */
public class SparkWordCount {
  private static final Logger log = LoggerFactory.getLogger(SparkWordCount.class);

  private static final String USAGE = "SparkWordCount <input_data_store> <output_data_store>";

  /**
   * This method would flattened WebPage data and return an Iterable list of
   * words. The map Function would use this as an input.
   */
  private static Function<WebPage, Iterable<String>> flatMapFun = new Function<WebPage, Iterable<String>>() {
    private static final long serialVersionUID = 1L;
    
    @Override
    public Iterable<String> call(WebPage page) throws Exception {
      String content = "";
      if (page.getContent() != null)
        content = new String(page.getContent().array(), Charset.defaultCharset());
      return Arrays.asList(content.split(" "));
    }
  };

  /**
   * Map function used to map out each word with a count of 1
   */
  private static Function<String, Tuple2<String, Long>> mapFunc = new Function<String, Tuple2<String, Long>>() {
    @Override
    public Tuple2<String, Long> call(String s) throws Exception {
      return new Tuple2<>(s, 1L);
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
   * Convert the key value pair <String, Long> to <String, TokenDatum> as per
   * the specification in the mapping file
   */
  private static PairFunction<Tuple2<String, Long>, String, TokenDatum> metricFunc = new PairFunction<Tuple2<String, Long>, String, TokenDatum>() {
    @Override
    public Tuple2<String, TokenDatum> call(Tuple2<String, Long> line) throws Exception {
      String word = line._1();
      TokenDatum tDatum = new TokenDatum();
      tDatum.setCount(line._2.intValue());
      return new Tuple2<>(word, tDatum);
    }
  };

  public int wordCount(DataStore<String, WebPage> inStore, DataStore<String, TokenDatum> outStore) throws IOException {

    // Spark engine initialization
    GoraSparkEngine<String, WebPage> goraSparkEngine = new GoraSparkEngine<>(String.class, WebPage.class);

    SparkConf sparkConf = new SparkConf().setAppName("Gora Spark Word Count Application").setMaster("local");

    Class[] c = new Class[1];
    c[0] = inStore.getPersistentClass();
    sparkConf.registerKryoClasses(c);
    //
    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    JavaPairRDD<String, WebPage> goraRDD = goraSparkEngine.initialize(sc, inStore);

    JavaPairRDD<String, String> goraRDDFlatenned = goraRDD.flatMapValues(flatMapFun);

    long count = goraRDD.count();
    log.info("Total Web page count: {}", count);

    JavaRDD<Tuple2<String, Long>> mappedGoraRdd = goraRDDFlatenned.values().map(mapFunc);

    JavaPairRDD<String, TokenDatum> reducedGoraRdd = JavaPairRDD.fromJavaRDD(mappedGoraRdd).reduceByKey(redFunc)
        .mapToPair(metricFunc);

    // Print output for debug purpose
    log.info("SparkWordCount debug purpose TokenDatum print starts:");
    Map<String, TokenDatum> tokenDatumMap = reducedGoraRdd.collectAsMap();
    for (String key : tokenDatumMap.keySet()) {
      log.info(key);
      log.info(tokenDatumMap.get(key).toString());
    }
    log.info("SparkWordCount debug purpose TokenDatum print ends:");

    // write output to datastore
    log.info(reducedGoraRdd.collect().toString());
    Configuration sparkHadoopConf = goraSparkEngine.generateOutputConf(outStore);
    reducedGoraRdd.saveAsNewAPIHadoopDataset(sparkHadoopConf);
    return 1;
  }

  public int run(String[] args) throws Exception {

    DataStore<String, WebPage> inStore;
    DataStore<String, TokenDatum> outStore;
    Configuration hadoopConf = new Configuration();
    if (args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(dataStoreClass, String.class, WebPage.class, hadoopConf);
      if (args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(dataStoreClass, String.class, TokenDatum.class, hadoopConf);
    } else {
      inStore = DataStoreFactory.getDataStore(String.class, WebPage.class, hadoopConf);
      outStore = DataStoreFactory.getDataStore(String.class, TokenDatum.class, hadoopConf);
    }

    return wordCount(inStore, outStore);
  }

  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      log.info(USAGE);
      System.exit(1);
    }

    SparkWordCount sparkWordCount = new SparkWordCount();

    try {
      int ret = sparkWordCount.run(args);
      System.exit(ret);
    } catch (Exception ex) {
      log.error("Error occurred!");
    }
  }
}
