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

import org.apache.avro.util.Utf8;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.StringTokenizer;


/**
 * DistributedLogManager {@link org.apache.gora.tutorial.log.DistributedLogManager}  is the tutorial class to
 * illustrate the basic distributed features that can be gained when persistent dataStore is used together with
 * cache dataStore similar to {@link org.apache.gora.jcache.store.JCacheStore}. Since Hazelcast provides cache as
 * a service approach, Apache Gora data stores can now be exposed as a data SERVICE when persistent data store is
 * exposed over the JCache store.
 *
 * JCache data store has two modes.
 *
 * 1. Server mode - Participate in Hazelcast cluster as a member. ( Data grid ) and communicates directly
 * with persistent store to full fill cache read/write operations.
 *
 * Add following properties gora.properties file to start JCache store in server mode.
 *
 * gora.cache.datastore.default=org.apache.gora.jcache.store.JCacheStore
 * gora.datastore.jcache.provider=com.hazelcast.cache.impl.HazelcastServerCachingProvider
 * gora.datastore.jcache.hazelcast.config=hazelcast.xml
 *
 * For cluster member network configuration use hazelcast.xml.
 * <p>See Network Configuration on
 * <a href="http://docs.hazelcast.org/docs/3.5/manual/html/networkconfiguration.html">
 * web site</a>for more information.</p>
 *
 * 2. Client mode - DOES not participate in Hazelcast cluster as a member. ( Data grid ) and For cache
 * read/write operations client forwards the requests to hazelcast cluster members which run in SERVER mode.
 *
 * Add following properties gora.properties file to start JCache store in client mode.
 *
 * gora.cache.datastore.default=org.apache.gora.jcache.store.JCacheStore
 * gora.datastore.jcache.provider=com.hazelcast.client.cache.impl.HazelcastClientCachingProvider
 * gora.datastore.jcache.hazelcast.config=hazelcast-client.xml
 *
 * For Hazelcast client configuration use hazelcast-client.xml.
 * <p>See Java Client Configuration on
 * <a href="http://docs.hazelcast.org/docs/3.5/manual/html/javaclientconfiguration.html#java-client-configuration">
 * web site</a>for more information.</p>
 *
 * Sample
 * ------
 * 1. Start DistributedLogManager in SERVER for two or higher instances. ( separate JVMs ).
 * Notice the Hazelcast cluster is well formed by following Hazelcast logs.
 * Members [2] {
 *     Member [127.0.0.1]:5701
 *     Member [127.0.0.1]:5702 this
 * }
 *
 * 2. Start DistributedLogManager in CLIENT mode for one instances.
 * Notice the client correctly connected to the cluster by following Hazelcast logs.
 * Members [2] {
 *     Member [127.0.0.1]:5701
 *     Member [127.0.0.1]:5702
 * }
 * INFO: HazelcastClient[hz.client_0_dev][3.6.4] is CLIENT_CONNECTED
 *
 * 3. Now use CLIENT's command line console to forward cache queries to cluster.
 *
 *  (a) -parse cache <input_log_file> - This will parse logs from logs file and put Pageview data beans to
 *      persistent store via the cache.
 *      Notice following logs
 *      INFO 19:46:34,833 Written data bean to persistent datastore on key 45.
 *      on SERVER instance of DistributedLogManager. Notice the persistent data bean writes are LOAD BALANCED
 *      among SERVER instances.
 *  (b) -parse persistent <input_log_file> - This will write parsed log data beans directly to persistent store.
 *      NOT via cache.
 *  (c) Executing with (a) will create cache entry per each data bean key on each SERVER and CLIENT instances. Since
 *      now data bean ( key/value ) is now loaded to Hazelcast DATA GRID, entries created data beans
 *      are now available to all the SERVER and CLIENT instances. Data beans which were loaded to Hazelcast
 *      DATA Grid can be retrieved from cache so that the latency is reduced compared to when data bean is
 *      direct retrieved from persistent data store.
 *  (d) Executing with (b) will not create cache entries on keys since the data beans were directly put into
 *      to persistent store.
 *      Executing following command
 *      -get <lineNum>
 *      Data will be first loaded from persistent store to cache from one of SERVER instances. Then cache
 *      entry on given key will be created on all SERVER/CLIENT instances.
 *      Notice the persistent data bean load on SINGLE SERVER instance. Only one SERVER instance will handle this work.
 *      INFO 17:13:22,652 Loaded data bean from persistent datastore on key 4.
 *      Notice the cache entry creation on ALL SERVER/CLIENT instances
 *      INFO 17:13:22,656 Cache entry added on key 4.
 *      Once the cache entry is created, data bean is now available to be retrieved from cache without reaching the
 *      persistent store.
 *      Execute the above command consecutively for several times.
 *      -get <lineNum>
 *      Notice there will be NO log entry similar to below
 *      INFO 17:13:22,652 Loaded data bean from persistent datastore on key 4.
 *      Since there will be no data bean load from persistent data store and the data bean is now loaded from
 *      cache.
 *  (e) DistributedLogManager has two Apache Gora data stores instances.
 *      dataStore - which call directly underline persistent data store.
 *      cacheStore - which call same persistent data store via the caching layer.
 *      Simple benchmarking purposes use
 *      -benchmark <startLineNum> <endLineNum> <iterations>
 *      to compare data beans read for two cases. ( Cache layer is present and Not present when executing
 *      consecutive data reads for same data items in nearby intervals )
 *      It generates LOG entries similar to below which indicates time spent for two cases in milliseconds
 *      INFO 17:13:22,652 Direct Backend took 1973 ms
 *      INFO 17:18:49,252 Via Cache took 1923 ms
 *
 * <p>In the data model, keys are the line numbers in the log file,
 * and the values are Pageview objects, generated from
 * <code>gora-tutorial/src/main/avro/pageview.json</code>.
 *
 * <p>See the tutorial.html file in docs or go to the
 * <a href="http://gora.apache.org/docs/current/tutorial.html">
 * web site</a>for more information.</p>
 */

public class DistributedLogManager {

  private static final Logger log = LoggerFactory.getLogger(DistributedLogManager.class);
  private static final SimpleDateFormat dateFormat
          = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.getDefault());
  private static final String USAGE =
          "            -parse cache|persistent <input_log_file>\n" +
          "            -benchmark <startLineNum> <endLineNum> <iterations>\n" +
          "            -get <lineNum>\n" +
          "            -get <lineNum> <fieldList>\n" +
          "            -query <startLineNum> <endLineNum>\n" +
          "            -delete <lineNum>\n" +
          "            -deleteByQuery <startLineNum> <endLineNum>\n" +
          "            -deleteSchema\n"+
          "            -exit\n";
  private DataStore<Long, Pageview> dataStore;
  private DataStore<Long, Pageview> cacheStore;

  public DistributedLogManager() {
    try {
      init();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void main(String[] args) throws Exception {
    DistributedLogManager manager = new DistributedLogManager();

    BufferedReader input = new BufferedReader(new InputStreamReader(System.in,
            Charset.defaultCharset()));

    for (; ; ) {
      // read next command from commandline
      String command = input.readLine();
      if ("".equals(command) || command == null) {
        // ignore and show new cmd line
        continue;
      }

      args = command.split("\\s+");
      if ("-parse".equals(args[0])) {
        if (args.length == 3) {
          if (args[1].contains("cache")) {
            manager.parse(args[2], true);
          } else if (args[1].contains("persistent")) {
            manager.parse(args[2], false);
          }
        }
      } else if ("-benchmark".equals(args[0])) {
        if (args.length == 4)
          manager.benchmark(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), manager);
      } else if ("-get".equals(args[0])) {
        if (args.length == 2) {
          manager.get(Long.parseLong(args[1]), true);
        } else {
          //field array should be input as comma ',' separated
          String[] fields = args[2].split(",");
          manager.get(Long.parseLong(args[1]), fields);
        }
      } else if ("-query".equals(args[0])) {
        if (args.length == 3)
          manager.query(Long.parseLong(args[1]), Long.parseLong(args[2]));
      } else if ("-delete".equals(args[0])) {
        manager.delete(Long.parseLong(args[1]));
      } else if ("-deleteByQuery".equalsIgnoreCase(args[0])) {
        manager.deleteByQuery(Long.parseLong(args[1]), Long.parseLong(args[2]));
      } else if ("-deleteSchema".equals(args[0])) {
        manager.deleteSchema();
        continue;
      } else if ("-exit".equalsIgnoreCase(args[0])) {
        input.close();
        manager.close();
        System.exit(1);
      } else {
        log.info(USAGE);
      }
    }
  }

  private void init() throws IOException {
    //Data store objects are created from a factory. It is necessary to
    //provide the key and value class. The datastore class is optional,
    //and if not specified it will be read from the properties file

    //this dataStore talks  directly to persistent store
    dataStore = DataStoreFactory.getDataStore(Long.class, Pageview.class,
            new Configuration());
    //this dataStore talks to persistent store via the cache
    cacheStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, new Configuration(), true);
  }

  /**
   * Parses a log file and store the contents at the data store.
   *
   * @param input the input file location
   */
  private void parse(String input, boolean isCacheEnabled) throws Exception {
    log.info("Parsing file: {}", input);
    long lineCount = 0;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(input), Charset.defaultCharset()))) {
      String line = reader.readLine();
      do {
        Pageview pageview = parseLine(line);

        if (pageview != null) {
          //store the pageview
          storePageview(lineCount++, pageview, isCacheEnabled);
        }

        line = reader.readLine();
      } while (line != null);

    }
    log.info("finished parsing file. Total number of log lines: {}", lineCount);
  }

  /**
   * Parses a single log line in combined log format using StringTokenizers
   */
  private Pageview parseLine(String line) throws ParseException {
    StringTokenizer matcher = new StringTokenizer(line);
    //parse the log line
    String ip = matcher.nextToken();
    matcher.nextToken(); //discard
    matcher.nextToken();
    long timestamp = dateFormat.parse(matcher.nextToken("]").substring(2)).getTime();
    matcher.nextToken("\"");
    String request = matcher.nextToken("\"");
    String[] requestParts = request.split(" ");
    String httpMethod = requestParts[0];
    String url = requestParts[1];
    matcher.nextToken(" ");
    int httpStatusCode = Integer.parseInt(matcher.nextToken());
    int responseSize = Integer.parseInt(matcher.nextToken());
    matcher.nextToken("\"");
    String referrer = matcher.nextToken("\"");
    matcher.nextToken("\"");
    String userAgent = matcher.nextToken("\"");
    //construct and return pageview object
    Pageview pageview = new Pageview();
    pageview.setIp(new Utf8(ip));
    pageview.setTimestamp(timestamp);
    pageview.setHttpMethod(new Utf8(httpMethod));
    pageview.setUrl(new Utf8(url));
    pageview.setHttpStatusCode(httpStatusCode);
    pageview.setResponseSize(responseSize);
    pageview.setReferrer(new Utf8(referrer));
    pageview.setUserAgent(new Utf8(userAgent));
    return pageview;
  }

  /**
   * Stores the pageview object with the given key
   */
  private void storePageview(long key, Pageview pageview, boolean isCacheEnabled) throws Exception {
    if (!isCacheEnabled) {
      log.info("Storing Pageview in: " + dataStore.toString());
      dataStore.put(key, pageview);
    } else {
      log.info("Storing Pageview in: " + dataStore.toString());
      cacheStore.put(key, pageview);
    }
  }

  /**
   * Fetches a single pageview object and prints it
   */
  private void get(long key, boolean isCacheEnabled) throws Exception {
    if (!isCacheEnabled) {
      Pageview pageview = dataStore.get(key);
      printPageview(pageview);
    } else {
      Pageview pageview = cacheStore.get(key);
      printPageview(pageview);
    }
  }

  /**
   * Fetches a single pageview object with required fields and prints it
   */
  private void get(long key, String[] fields) throws Exception {
    Pageview pageview = cacheStore.get(key, fields);
    printPageview(pageview);
  }

  /**
   * Queries and prints pageview object that have keys between startKey and endKey
   */
  private void query(long startKey, long endKey) throws Exception {
    Query<Long, Pageview> query = cacheStore.newQuery();
    //set the properties of query
    query.setStartKey(startKey);
    query.setEndKey(endKey);
    Result<Long, Pageview> result = query.execute();
    printResult(result);
  }

  /**
   * Deletes the pageview with the given line number
   */
  private void delete(long lineNum) throws Exception {
    cacheStore.delete(lineNum);
    cacheStore.flush(); //write changes may need to be flushed before
    //they are committed
    log.info("pageview with key: {} deleted", lineNum);
  }

  /**
   * This method illustrates delete by query call
   */
  private void deleteByQuery(long startKey, long endKey) throws Exception {
    //Constructs a query from the dataStore. The matching rows to this query will be deleted
    Query<Long, Pageview> query = cacheStore.newQuery();
    //set the properties of query
    query.setStartKey(startKey);
    query.setEndKey(endKey);
    cacheStore.deleteByQuery(query);
    log.info("pageviews with keys between {} and {} are deleted.", startKey, endKey);
  }

  private void printResult(Result<Long, Pageview> result) throws Exception {

    while (result.next()) { //advances the Result object and breaks if at end
      long resultKey = result.getKey(); //obtain current key
      Pageview resultPageview = result.get(); //obtain current value object
      log.info("{} :", resultKey);
      printPageview(resultPageview);
    }
    log.info("Number of pageviews from the query: {}", result.getOffset());
  }

  /**
   * Pretty prints the pageview object to stdout
   */
  private void printPageview(Pageview pageview) {
    if (pageview == null) {
      log.info("No result to show");
    } else {
      log.info(pageview.toString());
    }
  }

  private void deleteSchema() {
    cacheStore.deleteSchema();
    log.info("Deleted schema on dataStore");
  }

  private void close() throws Exception {
    //It is very important to close the datastore properly, otherwise
    //some data loss might occur.
    if (dataStore != null)
      dataStore.close();
    if (cacheStore != null)
      cacheStore.close();
  }

  /**
   * Simple benchmarking for comparison between when cache layer is present and not present. Purpose is to
   * exploit temporal locality of consecutive data bean reads.
   */
  private boolean benchmark(int start,
                            int end,
                            int iterations,
                            DistributedLogManager manager) throws Exception {
    if (!(start < 10000) && (end < 10000)) {
      return false;
    }
    long startTime;
    long finishTime;
    ArrayList<Integer> entryset= getShuffledEntrySet(start,end);
    startTime = System.currentTimeMillis();
    for (int itr = 0; itr < iterations; itr++) {
      for (int entry : entryset){
        manager.get(entry, false);
      }
    }
    finishTime = System.currentTimeMillis();
    long directDifference = (finishTime - startTime);
    startTime = System.currentTimeMillis();
    for (int itr = 0; itr < iterations; itr++) {
      for (int entry : entryset){
        manager.get(entry, true);
      }
    }
    finishTime = System.currentTimeMillis();
    log.info("Direct Backend took {} ms", directDifference);
    log.info("Via Cache took {} ms", (finishTime - startTime));
    return true;
  }

  private ArrayList<Integer> getShuffledEntrySet(int start, int end) {
    ArrayList<Integer> entrySet = new ArrayList<>();
    for (int counter = start; counter <= end; counter++) {
      entrySet.add(counter);
    }
    Collections.shuffle(entrySet);
    return entrySet;
  }

}
