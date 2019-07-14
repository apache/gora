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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.tutorial.log.generated.Pageview;
import org.apache.hadoop.conf.Configuration;

/**
 * LogManager is the tutorial class to illustrate the basic 
 * {@link DataStore} API usage. The LogManager class is used 
 * to parse the web server logs in combined log format, store the 
 * data in a Gora compatible data store, query and manipulate the stored data.  
 * 
 * <p>In the data model, keys are the line numbers in the log file, 
 * and the values are Pageview objects, generated from 
 * <code>gora-tutorial/src/main/avro/pageview.json</code>.
 * 
 * <p>See the tutorial.html file in docs or go to the 
 * <a href="http://gora.apache.org/docs/current/tutorial.html"> 
 * web site</a>for more information.</p>
 */
public class LogManager {

  private static final Logger log = LoggerFactory.getLogger(LogManager.class);
  
  private DataStore<Long, Pageview> dataStore; 
  
  private static final SimpleDateFormat dateFormat 
    = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.getDefault());
  
  public LogManager() {
    try {
      init();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  private void init() throws IOException {
    //Data store objects are created from a factory. It is necessary to 
    //provide the key and value class. The datastore class is optional, 
    //and if not specified it will be read from the properties file
    dataStore = DataStoreFactory.getDataStore(Long.class, Pageview.class,
            new Configuration());
  }
  
  /**
   * Parses a log file and store the contents at the data store.
   * @param input the input file location
   */
  private void parse(String input) throws Exception {
    log.info("Parsing file: {}", input);
      long lineCount = 0;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(input), Charset.defaultCharset()))) {
        String line = reader.readLine();
        do {
          Pageview pageview = parseLine(line);

          if (pageview != null) {
            //store the pageview
            storePageview(lineCount++, pageview);
          }

          line = reader.readLine();
        } while (line != null);

      }
    log.info("finished parsing file. Total number of log lines: {}", lineCount);
  }
  
  /** Parses a single log line in combined log format using StringTokenizers */
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
  
  /** Stores the pageview object with the given key */
  private void storePageview(long key, Pageview pageview) throws Exception {
	log.info("Storing Pageview in: " + dataStore.toString());
    dataStore.put(key, pageview);
  }
  
  /** Fetches a single pageview object and prints it*/
  private void get(long key) throws Exception {
    Pageview pageview = dataStore.get(key);
    printPageview(pageview);
  }

  /** Fetches a single pageview object with required fields and prints it*/
  private void get(long key, String[] fields) throws Exception {
    Pageview pageview = dataStore.get(key, fields);
    printPageview(pageview);
  }
  
  /** Queries and prints a single pageview object */
  private void query(long key) throws Exception {
    //Queries are constructed from the data store
    Query<Long, Pageview> query = dataStore.newQuery();
    query.setKey(key);
    
    Result<Long, Pageview> result = query.execute(); //Actually executes the query.
    // alternatively dataStore.execute(query); can be used
    
    printResult(result);
  }
  
  /** Queries and prints pageview object that have keys between startKey and endKey*/
  private void query(long startKey, long endKey) throws Exception {
    Query<Long, Pageview> query = dataStore.newQuery();
    //set the properties of query
    query.setStartKey(startKey);
    query.setEndKey(endKey);
    
    Result<Long, Pageview> result = query.execute();
    
    printResult(result);
  }
  
  
  /**Deletes the pageview with the given line number */
  private void delete(long lineNum) throws Exception {
    dataStore.delete(lineNum);
    dataStore.flush(); //write changes may need to be flushed before
                       //they are committed 
    log.info("pageview with key: {} deleted", lineNum);
  }
  
  /** This method illustrates delete by query call */
  private void deleteByQuery(long startKey, long endKey) throws Exception {
    //Constructs a query from the dataStore. The matching rows to this query will be deleted
    Query<Long, Pageview> query = dataStore.newQuery();
    //set the properties of query
    query.setStartKey(startKey);
    query.setEndKey(endKey);
    
    dataStore.deleteByQuery(query);
    log.info("pageviews with keys between {} and {} are deleted.", startKey, endKey);
  }
  
  private void printResult(Result<Long, Pageview> result) throws Exception {
    
    while(result.next()) { //advances the Result object and breaks if at end
      long resultKey = result.getKey(); //obtain current key
      Pageview resultPageview = result.get(); //obtain current value object
      
      log.info("{} :", resultKey);
      printPageview(resultPageview);
    }
    
    log.info("Number of pageviews from the query: {}", result.getOffset());
  }
  
  /** Pretty prints the pageview object to stdout */
  private void printPageview(Pageview pageview) {
    if(pageview == null) {
      log.info("No result to show"); 
    } else {
      log.info(pageview.toString());
    }
  }
  
  private void close() throws Exception {
    //It is very important to close the datastore properly, otherwise
    //some data loss might occur.
    if(dataStore != null)
      dataStore.close();
  }
  
  private static final String USAGE = "LogManager -parse <input_log_file>\n" +
                                      "           -get <lineNum>\n" +
                                      "           -get <lineNum> <fieldList>\n" +
                                      "           -query <lineNum>\n" +
                                      "           -query <startLineNum> <endLineNum>\n" +
  		                              "           -delete <lineNum>\n" +
  		                              "           -deleteByQuery <startLineNum> <endLineNum>\n";
  
  public static void main(String[] args) throws Exception {
    if(args.length < 2) {
      log.error(USAGE);
      System.exit(1);
    }
    
    LogManager manager = new LogManager();
    
    if("-parse".equals(args[0])) {
      manager.parse(args[1]);
    } else if("-get".equals(args[0])) {
      if(args.length == 2) {
        manager.get(Long.parseLong(args[1]));
      } else {
        //field array should be input as comma ',' separated
        String[] fields = args[2].split(",");
        manager.get(Long.parseLong(args[1]), fields);
      }
    } else if("-query".equals(args[0])) {
      if(args.length == 2) 
        manager.query(Long.parseLong(args[1]));
      else 
        manager.query(Long.parseLong(args[1]), Long.parseLong(args[2]));
    } else if("-delete".equals(args[0])) {
      manager.delete(Long.parseLong(args[1]));
    } else if("-deleteByQuery".equalsIgnoreCase(args[0])) {
      manager.deleteByQuery(Long.parseLong(args[1]), Long.parseLong(args[2]));
    } else {
      log.info(USAGE);
      System.exit(1);
    }
    
    manager.close();
  }
  
}
