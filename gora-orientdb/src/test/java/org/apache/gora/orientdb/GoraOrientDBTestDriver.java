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

package org.apache.gora.orientdb;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.orientdb.store.OrientDBStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver to set up an embedded OrientDB database instance for Gora
 * dataStore specific integration tests.
 */
public class GoraOrientDBTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(GoraOrientDBTestDriver.class);
  private static final String SERVER_DIRECTORY = "./target/db";
  private static final String SERVER_CONFIGURATION = "/orientdb-server-config.xml";
  private OServer server;

  public GoraOrientDBTestDriver() {
    super(OrientDBStore.class);
  }

  /**
   * Initialize embedded OrientDB server instance as per the gora-orientdb-mapping.xml
   * server configuration file.
   */
  @Override
  public void setUpClass() throws Exception {
    server = OServerMain.create();
    server.setServerRootDirectory(SERVER_DIRECTORY);
    server.startup(getClass().getResourceAsStream(SERVER_CONFIGURATION));
    server.activate();
    log.info("OrientDB Embedded Server started successfully.");
  }

  /**
   * Terminate embedded OrientDB server.
   */
  @Override
  public void tearDownClass() throws Exception {
    server.shutdown();
    log.info("OrientDB Embedded Server terminated successfully.");
  }

  @Override
  public <K, T extends Persistent> DataStore<K, T>
  createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
    OrientDBStore store = (OrientDBStore) super.createDataStore(keyClass, persistentClass);
    return store;
  }

}
