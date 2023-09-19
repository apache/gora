/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.geode;

import org.apache.gora.GoraTestDriver;

import org.apache.gora.geode.store.GeodeStore;
import org.apache.gora.geode.store.GeodeStoreParameters;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.testcontainers.containers.GenericContainer;

import java.util.Properties;

import static org.apache.gora.geode.store.GeodeStoreParameters.GEODE_SERVER_PORT;

/**
 * Helper class for third party tests using gora-geode backend.
 * @see GoraTestDriver for test specifics.
 * This driver is the base for all test cases that require an Geode server.
 * In this case we use docker container. A docker container is run before tests
 * and it is stopped after tests.
 *
 */
public class GoraGeodeTestDriver extends GoraTestDriver {

    private final GenericContainer GeodeContainer;
    private Properties properties = DataStoreFactory.createProps();

    /**
     * Default constructor
     */
    public GoraGeodeTestDriver(GenericContainer GeodeContainer) {
        super(GeodeStore.class);
        this.GeodeContainer = GeodeContainer;
    }

    @Override
    public void setUpClass() {
        log.info("Setting up Geode Test Driver");
        properties.put(GeodeStoreParameters.GEODE_SERVER_HOST, GeodeContainer.getContainerIpAddress());
        properties.put(GEODE_SERVER_PORT, GeodeContainer.getMappedPort(10334).toString());
    }

    @Override
    public void tearDownClass() {
        log.info("Teardown Geode test driver");
    }

    /**
     * Instantiate a new {@link DataStore}. Uses 'null' schema.
     *
     * @param keyClass The key class.
     * @param persistentClass The value class.
     * @return A new store instance.
     * @throws GoraException
     */
    @Override
    public <K, T extends Persistent> DataStore<K, T> createDataStore(Class<K> keyClass, Class<T> persistentClass)
            throws GoraException {

        final DataStore<K, T> dataStore = DataStoreFactory
                .createDataStore((Class<? extends DataStore<K, T>>) dataStoreClass, keyClass, persistentClass, conf,
                        properties);
        dataStores.add(dataStore);
        log.info("Datastore for {} was added.", persistentClass);
        return dataStore;
    }

}
