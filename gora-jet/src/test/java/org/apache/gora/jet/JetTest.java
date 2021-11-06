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
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.Functions.wholeItem;
import static org.junit.Assert.assertEquals;

/**
 * Test case for jet sink and source connectors.
 */
public class JetTest {

  private static DataStore<Long, ResultPageView> dataStoreOut;
  private static Query<Long, Pageview> query = null;

  private static HBaseTestingUtility utility;

  @BeforeClass
  public static void insertData() throws Exception {
    utility = new HBaseTestingUtility();
    utility.startMiniCluster();

    dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, utility.getConfiguration());

    ResultPageView resultPageView = new ResultPageView();
    resultPageView.setIp("88.240.129.183");
    resultPageView.setTimestamp(123L);
    resultPageView.setUrl("I am the the one");

    ResultPageView resultPageView1 = new ResultPageView();
    resultPageView1.setIp("87.240.129.170");
    resultPageView1.setTimestamp(124L);
    resultPageView1.setUrl("How are you");

    ResultPageView resultPageView2 = new ResultPageView();
    resultPageView2.setIp("88.240.129.183");
    resultPageView2.setTimestamp(124L);
    resultPageView2.setUrl("This is the jet engine");

    dataStoreOut.put(1L,resultPageView);
    dataStoreOut.put(2L,resultPageView1);
    dataStoreOut.put(3L,resultPageView2);
    dataStoreOut.flush();

  }

  @Test
  public void testNewJetSource() throws Exception {

    DataStore<Long, Pageview> dataStoreIn;

    dataStoreIn = DataStoreFactory.getDataStore(Long.class, Pageview.class, utility.getConfiguration());

    dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, utility.getConfiguration());

    query = dataStoreIn.newQuery();
    query.setStartKey(0L);
    query.setEndKey(55L);

    JetEngine<Long, Pageview, Long, ResultPageView> jetEngine = new JetEngine<>();
    BatchSource<JetInputOutputFormat<Long, Pageview>> fileSource = jetEngine.createDataSource(dataStoreIn, query);
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

    Query<Long, ResultPageView> query = dataStoreOut.newQuery();
    Result<Long, ResultPageView> result = query.execute();
    int noOfOutputRecords = 0;
    String ip = "";
    while (result.next()) {
      noOfOutputRecords++;
    }
    assertEquals(3, noOfOutputRecords);
  }

  @Test
  public void jetWordCount() throws GoraException {
    dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, utility.getConfiguration());

    Query<Long, ResultPageView> query = dataStoreOut.newQuery();
    JetEngine<Long, ResultPageView, Long, ResultPageView> jetEngine = new JetEngine<>();

    Pattern delimiter = Pattern.compile("\\W+");
    Pipeline p = Pipeline.create();
    p.drawFrom(jetEngine.createDataSource(dataStoreOut, query))
        .flatMap(e -> traverseArray(delimiter.split(e.getValue().getUrl().toString())))
        .filter(word -> !word.isEmpty())
        .groupingKey(wholeItem())
        .aggregate(counting())
        .drainTo(Sinks.map("COUNTS"));
    JetInstance jet =  Jet.newJetInstance();;
    jet.newJob(p).join();
    IMap<String, Long> counts = jet.getMap("COUNTS");
    assertEquals(3L, (long)counts.get("the"));
  }

  @Test
  public void jetWordCountExtended() throws GoraException {
    dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, utility.getConfiguration());

    Query<Long, ResultPageView> query = dataStoreOut.newQuery();
    JetEngine<Long, ResultPageView, Long, ResultPageView> jetEngine = new JetEngine<>();

    Pattern delimiter = Pattern.compile("\\W+");
    Pipeline p = Pipeline.create();
    p.drawFrom(jetEngine.createDataSource(dataStoreOut, query))
            .flatMap(e -> traverseArray(delimiter.split(e.getValue().getUrl().toString())))
            .filter(word -> !word.isEmpty())
            .groupingKey(wholeItem())
            .aggregate(counting())
            .drainTo(Sinks.map("COUNTS"));
    JetInstance jet =  Jet.newJetInstance();;
    jet.newJob(p).join();
    IMap<String, Long> counts = jet.getMap("COUNTS");
    for(Map.Entry<String, Long> entry: counts.entrySet()){
      System.out.println("########### Entry ; "+entry.getKey()+ "   "+entry.getValue());
    }

    assertEquals(3L, (long)counts.get("the"));
    assertEquals(1L, (long)counts.get("This"));
    assertEquals(1L, (long)counts.get("is"));
    assertEquals(1L, (long)counts.get("jet"));
    assertEquals(1L, (long)counts.get("engine"));
    assertEquals(1L, (long)counts.get("How"));
    assertEquals(1L, (long)counts.get("are"));
    assertEquals(1L, (long)counts.get("you"));
    assertEquals(1L, (long)counts.get("I"));
    assertEquals(1L, (long)counts.get("am"));
    assertEquals(1L, (long)counts.get("one"));
  }
}
