/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.goraci;

import org.apache.gora.goraci.generated.CINode;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stand alone program that follows a linked list created by {@link Generator} and prints timing info.
 */
public class Walker extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(Walker.class);
  
  private static final String[] PREV_FIELD = new String[] {"prev"};

  public int run(String[] args) throws IOException {
    Options options = new Options();
    options.addOption("n", "num", true, "number of queries");

    GnuParser parser = new GnuParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
      if (cmd.getArgs().length != 0) {
        throw new ParseException("Command takes no arguments");
      }
    } catch (ParseException e) {
      LOG.error("Failed to parse command line {}", e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(getClass().getSimpleName(), options);
      System.exit(-1);
    }
    
    long maxQueries = Long.MAX_VALUE;
    if (cmd.hasOption('n')) {
      maxQueries = Long.parseLong(cmd.getOptionValue("n"));
    }

    DataStore<Long,CINode> store = DataStoreFactory.getDataStore(Long.class, CINode.class, new Configuration());
  
    Random rand = new Random();

    long numQueries = 0;
    
    while (numQueries < maxQueries) {
      CINode node = findStartNode(rand, store);
      numQueries++;
      while (node != null && node.getPrev() >= 0 && numQueries < maxQueries) {
        long prev = node.getPrev();

        long t1 = System.currentTimeMillis();
        node = store.get(prev, PREV_FIELD);
        long t2 = System.currentTimeMillis();
        LOG.info("CQ %d %016x \n {}", new Object [] {t2 - t1, prev});
        numQueries++;
        
        t1 = System.currentTimeMillis();
        node = store.get(prev, PREV_FIELD);
        t2 = System.currentTimeMillis();
        LOG.info("HQ %d %016x \n {}", new Object [] {t2 - t1, prev});
        numQueries++;

      }
    }
    
    store.close();
    return 0;
  }
  
  private static CINode findStartNode(Random rand, DataStore<Long,CINode> store) throws IOException {
    Query<Long,CINode> query = store.newQuery();
    query.setStartKey(rand.nextLong());
    query.setLimit(1);
    query.setFields(PREV_FIELD);
    
    long t1 = System.currentTimeMillis();
    Result<Long,CINode> rs = store.execute(query);
    long t2 = System.currentTimeMillis();
    
    try {
      if (rs.next()) {
        LOG.info("FSR %d %016x\n {}", new Object[] {t2 - t1, rs.getKey()});
        return rs.get();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    LOG.info("FSR {}", new Object [] {(t2 - t1)});
    
    return null;
  }
  
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new Walker(), args);
    System.exit(ret);
  }
  
}
