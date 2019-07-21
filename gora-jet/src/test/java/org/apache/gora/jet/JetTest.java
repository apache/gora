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
package org.apache.gora.jet;

import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import org.apache.gora.jet.generated.Pageview;
import org.apache.gora.jet.generated.ResultPageView;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.Functions.wholeItem;

/**
 * Test case for jet sink and source connectors.
 */
public class JetTest {

  private static DataStore<Long, Pageview> dataStore;
  private static DataStore<Long, ResultPageView> dataStoreOut;
  static Query<Long, Pageview> query = null;

  @Test
  public void testNewJetSource() {

    Configuration conf =  new Configuration();

    try {
      dataStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, conf);
    } catch (GoraException e) {
      e.printStackTrace();
    }

    try {
      dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, conf);
    } catch (GoraException e) {
      e.printStackTrace();
    }

    query = dataStore.newQuery();
    query.setStartKey(0L);
    query.setEndKey(55L);

    JetEngine<Long, Pageview, Long, ResultPageView> jetEngine = new JetEngine<>();
    BatchSource<JetInputOutputFormat<Long, Pageview>> fileSource = jetEngine.createDataSource(dataStore, query);
    Pipeline p = Pipeline.create();
    p.drawFrom(fileSource)
        .filter(item -> item.getValue().getIp().toString().equals("88.240.129.183"))
        .map(e -> {
          ResultPageView resultPageView = new ResultPageView();
          resultPageView.setIp(e.getValue().getIp());
          resultPageView.setTimestamp(e.getValue().getTimestamp());
          resultPageView.setUrl(e.getValue().getUrl());
          return new JetInputOutputFormat<Long, ResultPageView>(e.getValue().getTimestamp(), resultPageView);
        })
        .drainTo(jetEngine.createDataSink(dataStoreOut));

    JetInstance jet = Jet.newJetInstance();
    Jet.newJetInstance();
    try {
      jet.newJob(p).join();
    } finally {
      Jet.shutdownAll();
    }
  }

  @Test
  public void insertData() {
    try {
      dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, new Configuration());
    } catch (GoraException e) {
      e.printStackTrace();
    }

    ResultPageView resultPageView = new ResultPageView();
    resultPageView.setIp("123");
    resultPageView.setTimestamp(123L);
    resultPageView.setUrl("I am the the one");

    ResultPageView resultPageView1 = new ResultPageView();
    resultPageView1.setIp("123");
    resultPageView1.setTimestamp(123L);
    resultPageView1.setUrl("How are you");

    try {
      dataStoreOut.put(1L,resultPageView);
      dataStoreOut.put(2L,resultPageView1);
      dataStoreOut.flush();
    } catch (GoraException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void jetWordCount() {
    try {
      dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, new Configuration());
    } catch (GoraException e) {
      e.printStackTrace();
    }
    Query<Long, ResultPageView> query = dataStoreOut.newQuery();
    JetEngine<Long, ResultPageView, Long, ResultPageView> jetEngine = new JetEngine<>();

    Pattern delimiter = Pattern.compile("\\W+");
    Pipeline p = Pipeline.create();
    p.drawFrom(jetEngine.createDataSource(dataStoreOut, query))
        .flatMap(e -> traverseArray(delimiter.split(e.getValue().getUrl().toString().toLowerCase())))
        .filter(word -> !word.isEmpty())
        .groupingKey(wholeItem())
        .aggregate(counting())
        .drainTo(Sinks.map("COUNTS"));
    JetInstance jet =  Jet.newJetInstance();;
    try {
      System.out.print("\nCounting words... ");
      jet.newJob(p).join();
      IMap<String, Long> counts = jet.getMap("COUNTS");
      if (counts.get("the") != 2) {
        throw new AssertionError("Wrong count of 'the'");
      }
      System.out.println("Count of 'the' is valid");
    } finally {
      Jet.shutdownAll();
    }
  }
}
