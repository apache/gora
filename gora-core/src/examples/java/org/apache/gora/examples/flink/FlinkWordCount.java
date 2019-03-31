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
package org.apache.gora.examples.flink;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;
import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.flink.GoraFlinkEngine;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.StringTokenizer;

/**
 * Classic Word count example in Gora with Flink.
 */
public class FlinkWordCount {

  private static final Logger log = LoggerFactory.getLogger(FlinkWordCount.class);

  private static final String USAGE = "FlinkWordCount <input_data_store> <output_data_store>";

  /**
   * Flink Map Function
   */
  public static final class WordCountMapFunction implements FlatMapFunction<Tuple2<String, WebPage>,
          Tuple2<Text, IntWritable>> {

    @Override
    public void flatMap(Tuple2<String, WebPage> in, Collector<Tuple2<Text, IntWritable>> out) {
      IntWritable one = new IntWritable(1);
      Text word = new Text();
      WebPage page = in.getField(1);
      if (page.getContent() != null) {
        String content = new String(page.getContent().array(), Charset.defaultCharset());

        StringTokenizer itr = new StringTokenizer(content);
        while (itr.hasMoreTokens()) {
          word.set(itr.nextToken());
          out.collect(new Tuple2<>(word, one));
        }
      }
    }
  }

  /**
   * Flink Reduce Function
   */
  public static final class WordCountReduceFunction implements
          GroupReduceFunction<Tuple2<Text, IntWritable>, Tuple2<String, TokenDatum>> {

    @Override
    public void reduce(Iterable<Tuple2<Text, IntWritable>> in, Collector<Tuple2<String, TokenDatum>> out) {
      TokenDatum result = new TokenDatum();
      int sum = 0;
      Text currentKey = null;
      for (Tuple2<Text, IntWritable> tuple : in) {
        currentKey = tuple.getField(0);
        IntWritable value = tuple.getField(1);
        sum += value.get();
      }
      result.setCount(sum);
      out.collect(new Tuple2<>(currentKey.toString(), result));
    }
  }

  public int wordCount(DataStore<String, WebPage> inStore,
                       DataStore<String, TokenDatum> outStore,
                       Configuration conf) throws Exception {
    // Create Apache Flink Execution environment
    ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

    // Create Apache Gora - Flink Engine
    GoraFlinkEngine<String, WebPage, String, TokenDatum> flinkEngine =
            new GoraFlinkEngine<>(String.class, WebPage.class, String.class, TokenDatum.class);

    // Create Gora Data source
    DataSet<Tuple2<String, WebPage>> webPages = flinkEngine.createDataSource(env, conf, inStore);

    DataSet<Tuple2<String, TokenDatum>> wordCounts = webPages
            .flatMap(new WordCountMapFunction())
            .groupBy(0)
            .reduceGroup(new WordCountReduceFunction());

    // Register Gora Sink source
    wordCounts.output(flinkEngine.createDataSink(conf, outStore));

    // Execute Flink Word Count Job
    env.execute("Gora Word Count on Apache Flink Engine");

    log.info("Total WebPage Count: {}", webPages.count());
    log.info("Total TokenDatum count: {}", wordCounts.count());

    log.info("Flink Job completed with success");

    return 1;
  }


  public int run(String[] args) throws Exception {

    DataStore<String, WebPage> inStore;
    DataStore<String, TokenDatum> outStore;
    Configuration conf = new Configuration();
    if (args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(dataStoreClass,
              String.class, WebPage.class, conf);
      if (args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(dataStoreClass,
              String.class, TokenDatum.class, conf);
    } else {
      inStore = DataStoreFactory.getDataStore(String.class, WebPage.class, conf);
      outStore = DataStoreFactory.getDataStore(String.class, TokenDatum.class, conf);
    }

    return wordCount(inStore, outStore, conf);
  }

  // Usage FlinkWordCount [input datastore class] [output datastore class]
  public static void main(String[] args) throws Exception {

    if (args.length < 2) {
      log.info(USAGE);
      System.exit(1);
    }

    FlinkWordCount flinkWordCount = new FlinkWordCount();

    try {
      int ret = flinkWordCount.run(args);
      System.exit(ret);
    } catch (Exception ex) {
      log.error("Error occurred while excution of Flink Word Count Sample : ", ex);
    }
  }
}
