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
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class LogAnalyticsSpark extends Configured implements Tool {

  private static final String USAGE = "LogAnalyticsSpark <input_data_store> <output_data_store>";
  private static LogAnalyticsSpark logAnalyticsSpark = new LogAnalyticsSpark();

  public static void main(String[] args) throws Exception {
      if (args.length < 2) {
        System.err.println(USAGE);
        System.exit(1);
    }
      // run as any other MR job
      int ret = ToolRunner.run(logAnalyticsSpark, args);
      System.exit(ret);
  }

  @Override
  public int run(String[] args) throws Exception {
    GoraSpark<Long, Pageview> goraSpark = new GoraSpark<Long, Pageview>(
      Long.class, Pageview.class);

    SparkConf conf = new SparkConf().setAppName(
        "Gora Integration Application").setMaster("local");
    JavaSparkContext sc = new JavaSparkContext(conf);

    String dataStoreClass = args[0];
    DataStore<Long, Pageview> dataStore = DataStoreFactory.getDataStore(
        dataStoreClass, Long.class, Pageview.class,
    logAnalyticsSpark.getConf());

    JavaPairRDD<Long, org.apache.gora.tutorial.log.generated.Pageview> goraRDD = goraSpark
        .initialize(sc, logAnalyticsSpark.getConf(), dataStore);
    // JavaPairRDD<Long, org.apache.gora.tutorial.log.generated.Pageview>
    // cachedGoraRdd = goraRDD.cache();

    long count = goraRDD.count();
    System.out.println("Total Count: " + count);
    return 1;
  }
}
