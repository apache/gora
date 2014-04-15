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

/**
 * Driver to set up an embedded MongoDB database instance for use in our
 * unit tests. We use embedded mongodb which is available from 
 * https://github.com/flapdoodle-oss/embedmongo.flapdoodle.de
 */
package org.apache.gora.mongodb;

import com.mongodb.Mongo;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.process.runtime.Network;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.mongodb.store.MongoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoraMongodbTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(GoraMongodbTestDriver.class);

  private MongodExecutable _mongodExe;
  private MongodProcess _mongod;
  private MongoClient _mongo;

  
  /**
   * Constructor for this class.
   */
  public GoraMongodbTestDriver() {
    super(MongoStore.class);
  }
  
  /**
   * Initiate the MongoDB server on the default port
   */
  @Override
  public void setUpClass() throws Exception {
    super.setUpClass();
      MongodStarter runtime = MongodStarter.getDefaultInstance();

      int port = 27017;
      IMongodConfig mongodConfig = new MongodConfigBuilder()
              .version(Version.Main.PRODUCTION)
              .net(new Net(port, Network.localhostIsIPv6()))
              .build();

    log.info("Starting embedded Mongodb server on the default port: {}" + port);
    try {

        _mongodExe = runtime.prepare(mongodConfig);
      _mongod = _mongodExe.start();

       _mongo = new MongoClient("localhost", port);
    } catch (Exception e) {
      log.error("Error starting embedded Mongodb server... tearing down test driver.");
      tearDownClass();
    }
  }

  /**
   * Tear the server down 
   */
  @Override
  public void tearDownClass() throws Exception {
    log.info("Shutting down mongodb server...");
    super.tearDownClass();
    _mongod.stop();
    _mongodExe.stop();
  }

  public Mongo getMongo() {
    return _mongo;
  }

}
