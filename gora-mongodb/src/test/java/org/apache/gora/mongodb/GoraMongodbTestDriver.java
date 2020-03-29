/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mongodb;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.mongodb.store.MongoStoreParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver to set up an embedded MongoDB database instance for use in our
 * unit tests. We use testcontainers.org project.
 */
public class GoraMongodbTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory
          .getLogger(GoraMongodbTestDriver.class);

  private MongoContainer _container;
  private MongoClient _mongo;

  /**
   * Constructor for this class.
   */
  public GoraMongodbTestDriver(MongoContainer startedContainer) {
    super(MongoStore.class);
    this._container = startedContainer;
  }

  /**
   * Initiate the MongoDB server on the default port
   */
  @Override
  public void setUpClass() {
    ServerAddress address = _container.getServerAddress();
    int port = address.getPort();
    String host = address.getHost();

    // Store Mongo server "host:port" in Hadoop configuration
    // so that MongoStore will be able to get it latter
    conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, host + ":" + port);

    log.info("Starting embedded Mongodb server on {} port.", port);
    try {
      _mongo = new MongoClient(address);
    } catch (Exception e) {
      log.error("Error starting embedded Mongodb server... tearing down test driver.");
      tearDownClass();
    }
  }

  /**
   * Tear the server down
   */
  @Override
  public void tearDownClass() {
    log.info("Shutting down mongodb server...");
    _container.stop();
  }

  public Mongo getMongo() {
    return _mongo;
  }

}
