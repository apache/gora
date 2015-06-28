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

import org.apache.gora.spark.GoraSpark;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class LogAnalyticsSpark {

  private static final String USAGE = "LogAnalyticsSpark <input_data_store> <output_data_store>";

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println(USAGE);
      System.exit(1);
    }

    String inStoreClass = args[0];
    String outStoreClass = args[1];

    LogAnalyticsSpark logAnalyticsSpark = new LogAnalyticsSpark();
    int ret = logAnalyticsSpark.run(inStoreClass, outStoreClass);

    System.exit(ret);
  }

  public int run(String inStoreClass, String outStoreClass) throws Exception {
    GoraSpark<Long, Pageview> goraSpark = new GoraSpark<>(Long.class,
        Pageview.class);

    SparkConf sparkConf = new SparkConf().setAppName(
        "Gora Integration Application").setMaster("local");
    JavaSparkContext sc = new JavaSparkContext(sparkConf);

    Configuration hadoopConf = new Configuration();

    DataStore<Long, Pageview> dataStore = DataStoreFactory.getDataStore(
        inStoreClass, Long.class, Pageview.class, hadoopConf);

    JavaPairRDD<Long, Pageview> goraRDD = goraSpark.initializeInput(sc, dataStore);
    // JavaPairRDD<Long, org.apache.gora.tutorial.log.generated.Pageview>
    // cachedGoraRdd = goraRDD.cache();

    long count = goraRDD.count();
    System.out.println("Total Count: " + count);
    return 1;
  }
}
