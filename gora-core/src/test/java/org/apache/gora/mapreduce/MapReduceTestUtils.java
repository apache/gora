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

package org.apache.gora.mapreduce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema.Field;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.TokenDatum;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.examples.mapreduce.QueryCounter;
import org.apache.gora.examples.mapreduce.WordCount;
import org.apache.gora.examples.spark.SparkWordCount;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.hadoop.conf.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapReduceTestUtils {

  private static final Logger log = LoggerFactory.getLogger(MapReduceTestUtils.class);
  
  /** 
   * Tests by running the {@link org.apache.gora.examples.mapreduce.QueryCounter} 
   * mapreduce job 
   */
  public static void testCountQuery(DataStore<String, WebPage> dataStore, Configuration conf)
      throws Exception {

    ((DataStoreBase<String, WebPage>)dataStore).setConf(conf);
    
    //create input
    WebPageDataCreator.createWebPageData(dataStore);

    QueryCounter<String,WebPage> counter = new QueryCounter<>(conf);
    Query<String,WebPage> query = dataStore.newQuery();
    List<Field> fields = WebPage.SCHEMA$.getFields();
    String[] fieldNames = new String[fields.size() - 1];
    for(int i = 0; i< fieldNames.length; i++){
      fieldNames[i] = fields.get(i+1).name();
    }
    query.setFields(fieldNames);
    
    dataStore.close();

    //run the job
    log.info("running count query job");
    long result = counter.countQuery(query);
    log.info("finished count query job");
    
    //assert results
    assertEquals(WebPageDataCreator.URLS.length, result);
  }
 
  public static void testWordCount(Configuration conf, DataStore<String,WebPage> inStore, DataStore<String,
      TokenDatum> outStore) throws Exception {
    //Datastore now has to be a Hadoop based datastore
    ((DataStoreBase<String,WebPage>)inStore).setConf(conf);
    ((DataStoreBase<String,TokenDatum>)outStore).setConf(conf);
    
    //create input
    WebPageDataCreator.createWebPageData(inStore);
    
    //run the job
    WordCount wordCount = new WordCount(conf);
    wordCount.wordCount(inStore, outStore);
    
    //assert results
    HashMap<String, Integer> actualCounts = new HashMap<>();
    for(String content : WebPageDataCreator.CONTENTS) {
      if (content != null) {
        for(String token:content.split(" ")) {
          Integer count = actualCounts.get(token);
          if(count == null) 
            count = 0;
          actualCounts.put(token, ++count);
        }
      }
    }
    for(Map.Entry<String, Integer> entry:actualCounts.entrySet()) {
      assertTokenCount(outStore, entry.getKey(), entry.getValue()); 
    }
  }

  public static void testSparkWordCount(Configuration conf, DataStore<String,WebPage> inStore, DataStore<String,
      TokenDatum> outStore) throws Exception {
    //Datastore now has to be a Hadoop based datastore
    ((DataStoreBase<String,WebPage>)inStore).setConf(conf);
    ((DataStoreBase<String,TokenDatum>)outStore).setConf(conf);

    //create input
    WebPageDataCreator.createWebPageData(inStore);

    //run Spark
    SparkWordCount wordCount = new SparkWordCount();
    wordCount.wordCount(inStore, outStore);

    //assert results
    HashMap<String, Integer> actualCounts = new HashMap<>();
    for(String content : WebPageDataCreator.CONTENTS) {
      if (content != null) {
        for(String token:content.split(" ")) {
          Integer count = actualCounts.get(token);
          if(count == null)
            count = 0;
          actualCounts.put(token, ++count);
        }
      }
    }
    for(Map.Entry<String, Integer> entry:actualCounts.entrySet()) {
      assertTokenCount(outStore, entry.getKey(), entry.getValue());
    }
  }
  
  private static void assertTokenCount(DataStore<String, TokenDatum> outStore,
      String token, int count) throws Exception {
    TokenDatum datum = outStore.get(token, null);
    assertNotNull("token:" + token + " cannot be found in datastore", datum);
    assertEquals("count for token:" + token + " is wrong", count, datum.getCount().intValue());
  }
}
