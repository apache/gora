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

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.Pipeline;
import org.apache.gora.jet.JetEngine;
import org.apache.gora.jet.JetInputOutputFormat;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.MetricDatum;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;

import java.util.Map;

import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.aggregate.AggregateOperations.groupingBy;

/**
 * LogAnalyticsJet is the tutorial class to illustrate Gora's Hazelcast Jet API.
 * The analytics jet reads the web access data stored earlier by the
 * {@link LogManager}, and calculates the aggregate daily pageviews. The
 * output is stored in a Gora compatible data store.
 *
 * <p>See the tutorial.html file in docs or go to the
 * <a href="http://gora.apache.org/current/tutorial.html">
 * web site</a>for more information.</p>
 */
public class LogAnalyticsJet {

  private static DataStore<Long, Pageview> inStore;
  private static DataStore<String, MetricDatum> outStore;

  /**
   * The number of miliseconds in a day
   */
  private static final long DAY_MILIS = 1000 * 60 * 60 * 24;

  /**
   * In the main method pageviews are fetched though the jet source connector.
   * Then those are grouped by url and day. Then a counting aggregator is
   * applied to calculate the aggregated daily pageviews. Then the result is
   * output through the jet sink connector to a gora compatible data store.
   */
  public static void main(String[] args) throws Exception{

    Configuration conf =  new Configuration();

    inStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, conf);
    outStore = DataStoreFactory.getDataStore(String.class, MetricDatum.class, conf);

    Query<Long, Pageview> query = inStore.newQuery();
    JetEngine<Long, Pageview, String, MetricDatum> jetEngine = new JetEngine<>();

    Pipeline p = Pipeline.create();
    p.drawFrom(jetEngine.createDataSource(inStore, query))
        .groupingKey(e -> e.getValue().getUrl().toString())
        .aggregate(groupingBy(e -> getDay(e.getValue().getTimestamp()), counting()))
        .map(e -> {
          MetricDatum metricDatum = new MetricDatum();
          String url = e.getKey();
          for (Map.Entry<Long, Long> item : e.getValue().entrySet()) {
            long timeStamp = item.getKey();
            long sum = item.getKey();
            metricDatum.setTimestamp(timeStamp);
            metricDatum.setMetric(sum);
          }
          metricDatum.setMetricDimension(url);
          return new JetInputOutputFormat<String, MetricDatum>(url + "_" + "ip", metricDatum);
        })
        .peek()
        .drainTo(jetEngine.createDataSink(outStore));

    JetInstance jet =  Jet.newJetInstance();
    try {
      jet.newJob(p).join();
    } finally {
      Jet.shutdownAll();
    }
  }

  /**
   * Rolls up the given timestamp to the day cardinality, so that
   * data can be aggregated daily
   */
  private static long getDay(long timeStamp) {
    return (timeStamp / DAY_MILIS) * DAY_MILIS;
  }

}
