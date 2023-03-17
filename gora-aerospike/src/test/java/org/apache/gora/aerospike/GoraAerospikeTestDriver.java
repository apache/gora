/*
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

package org.apache.gora.aerospike;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.aerospike.store.AerospikeStore;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.testcontainers.containers.GenericContainer;

import java.util.Properties;

/**
 * Helper class for third part tests using gora-aerospike backend.
 *
 * @see GoraTestDriver
 */
public class GoraAerospikeTestDriver extends GoraTestDriver {

  private final GenericContainer aerospikeContainer;

  public GoraAerospikeTestDriver(GenericContainer aerospikeContainer) {
    super(AerospikeStore.class);
    this.aerospikeContainer = aerospikeContainer;
  }

  @Override
  public void setUpClass() throws Exception {
    log.info("Setting up Aerospike test driver");
    conf.set("gora.aerospikestore.server.ip", aerospikeContainer.getContainerIpAddress());
    conf.set("gora.aerospikestore.server.port", aerospikeContainer.getMappedPort(3000).toString());
  }

  @Override
  public void tearDownClass() throws Exception {
    log.info("Tearing down Aerospike test driver");
  }

  /**
   * Instantiate a new {@link DataStore}. Uses 'null' schema.
   *
   * @param keyClass        The key class.
   * @param persistentClass The value class.
   * @return A new store instance.
   * @throws GoraException If an error occur in creating the data store
   */
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
