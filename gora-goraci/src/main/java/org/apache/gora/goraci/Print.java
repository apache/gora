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

import java.math.BigInteger;

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
 * A stand alone program that prints out portions of a list created by {@link Generator}
 */
public class Print extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(Print.class);
  
  public int run(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("s", "start", true, "start key");
    options.addOption("e", "end", true, "end key");
    options.addOption("l", "limit", true, "number to print");
    
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

    DataStore<Long,CINode> store = DataStoreFactory.getDataStore(Long.class, CINode.class, new Configuration());
    
    Query<Long,CINode> query = store.newQuery();
    
    if (cmd.hasOption("s"))
      query.setStartKey(new BigInteger(cmd.getOptionValue("s"), 16).longValue());
    
    if (cmd.hasOption("e"))
      query.setEndKey(new BigInteger(cmd.getOptionValue("e"), 16).longValue());
    
    if (cmd.hasOption("l"))
      query.setLimit(Integer.parseInt(cmd.getOptionValue("l")));
    else
      query.setLimit(100);

    Result<Long,CINode> rs = store.execute(query);

    while (rs.next()) {
      CINode node = rs.get();
      LOG.info("%016x:%016x:%012d:%s\n {} {} {} {}", new Object[] {rs.getKey(), 
        node.getPrev(), node.getCount(), node.getClient()});

    }
    
    store.close();
    
    return 0;
  }
  
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new Print(), args);
    System.exit(ret);
  }
}
