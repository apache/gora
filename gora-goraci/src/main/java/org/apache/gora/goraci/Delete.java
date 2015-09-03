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

import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stand alone program that deletes a single node.
 */
public class Delete extends Configured implements Tool {

  private static final Logger LOG = LoggerFactory.getLogger(Delete.class);
  
  public int run(String[] args) throws Exception {
    if (args.length != 1) {
      LOG.info("Usage : {} <node to delete>", Delete.class.getSimpleName());
      return 0;
    }
    
    DataStore<Long,CINode> store = DataStoreFactory.getDataStore(Long.class, CINode.class, new Configuration());
    
    boolean ret = store.delete(new BigInteger(args[0], 16).longValue());
    store.flush();
    
    LOG.info("Delete returned {}", ret);
    
    store.close();

    return ret ? 0 : 1;
  }
  
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new Delete(), args);
    System.exit(ret);
  }
}
