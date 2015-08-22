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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.IOException;
import java.util.Map;

/**
 * Classic word count example in Gora with Spark.
 */
public class SparkWordCount {
  private static final Logger log = LoggerFactory.getLogger(SparkWordCount.class);

  private static final String USAGE = "SparkWordCount <input_data_store> <output_data_store>";

  /**
   * map function used in calculation
   */
  private static Function<WebPage, Tuple2<String, Long>> mapFunc =
    new Function<WebPage, Tuple2<String, Long>>() {
        @Override
        public Tuple2<String, Long> call(WebPage webPage)
                throws Exception {
          String content = new String(webPage.getContent().array());
          return new Tuple2<>(content, 1L);
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

  public int wordCount(DataStore<String,WebPage> inStore,
    DataStore<String, TokenDatum> outStore) throws IOException {

    //Spark engine initialization
    GoraSparkEngine<String, WebPage> goraSparkEngine = new GoraSparkEngine<>(String.class,
       WebPage.class);

    SparkConf sparkConf = new SparkConf().setAppName(
      "Gora Spark Word Count Application").setMaster("local");

    Class[] c = new Class[1];
    c[0] = inStore.getPersistentClass();
    sparkConf.registerKryoClasses(c);
    //
    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    JavaPairRDD<String, WebPage> goraRDD = goraSparkEngine.initialize(sc, inStore);

    long count = goraRDD.count();
    System.out.println("Total Log Count: " + count);

    JavaRDD<Tuple2<String, Long>> mappedGoraRdd = goraRDD.values().map(mapFunc);

    JavaPairRDD<String, Long> reducedGoraRdd = JavaPairRDD.fromJavaRDD(mappedGoraRdd).reduceByKey(redFunc);

    //Print output for debug purpose
    System.out.println("SparkWordCount debug purpose TokenDatum print starts:");
    Map<String, Long> tokenDatumMap = reducedGoraRdd.collectAsMap();
    for (String key : tokenDatumMap.keySet()) {
      System.out.println(key);
      System.out.println(tokenDatumMap.get(key));
    }
    System.out.println("SparkWordCount debug purpose TokenDatum print ends:");
    //

    //write output to datastore
    Configuration sparkHadoopConf = goraSparkEngine.generateOutputConf(outStore);
    reducedGoraRdd.saveAsNewAPIHadoopDataset(sparkHadoopConf);
    //

    return 1;
  }

  public int run(String[] args) throws Exception {

    DataStore<String,WebPage> inStore;
    DataStore<String, TokenDatum> outStore;
    Configuration hadoopConf = new Configuration();
    if(args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(dataStoreClass, String.class, WebPage.class, hadoopConf);
      if(args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(dataStoreClass,
        String.class, TokenDatum.class, hadoopConf);
      } else {
        inStore = DataStoreFactory.getDataStore(String.class, WebPage.class, hadoopConf);
        outStore = DataStoreFactory.getDataStore(String.class, TokenDatum.class, hadoopConf);
      }

      return wordCount(inStore, outStore);
  }

  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      System.err.println(USAGE);
      System.exit(1);
    }

    SparkWordCount sparkWordCount = new SparkWordCount();

    try {
      int ret = sparkWordCount.run(args);
      System.exit(ret);
    } catch (Exception ex){
      log.error("Error occurred!");
    }
  }
}
