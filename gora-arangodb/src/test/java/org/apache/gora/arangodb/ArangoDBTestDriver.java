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

package org.apache.gora.arangodb;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.arangodb.store.ArangoDBStore;
import org.apache.gora.arangodb.store.ArangoDBStoreParameters;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Driver to set up an embedded ArangoDB database instance for Gora
 * dataStore specific integration tests.
 */
public class ArangoDBTestDriver extends GoraTestDriver {

  private static Logger log = LoggerFactory.getLogger(ArangoDBTestDriver.class);

  private GenericContainer arangodbContainer;

  public ArangoDBTestDriver(GenericContainer arangodbContainer) {
    super(ArangoDBStore.class);
    this.arangodbContainer = arangodbContainer;
  }


  public ArangoDBTestDriver() {
    super(ArangoDBStore.class);
  }

  /**
   * Initialize embedded ArangoDB server instance as per the gora-arangodb-mapping.xml
   * server configuration file.
   */
  @Override
  public void setUpClass() throws Exception {
    log.info("Setting up ArangoDB test driver");
    conf.set(ArangoDBStoreParameters.ARANGO_DB_SERVER_HOST, "localhost");
    conf.set(ArangoDBStoreParameters.ARANGO_DB_SERVER_PORT,
            arangodbContainer.getMappedPort(8529).toString());
    log.info("ArangoDB Embedded Server started successfully.");
  }

  /**
   * Terminate embedded ArangoDB server.
   */
  @Override
  public void tearDownClass() throws Exception {
    log.info("ArangoDB Embedded server instance terminated successfully.");
  }

  @Override
  public <K, T extends Persistent> DataStore<K, T> createDataStore(Class<K> keyClass,
                                                                   Class<T> persistentClass) throws GoraException {

    final DataStore<K, T> dataStore = DataStoreFactory
            .createDataStore((Class<? extends DataStore<K, T>>) dataStoreClass, keyClass,
                    persistentClass, conf);
    dataStores.add(dataStore);
    log.info("Datastore for {} was added.", persistentClass);
    return dataStore;
  }

}
