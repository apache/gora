/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.scylladb;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.scylladb.store.ScyllaDBStore;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Properties;

/**
 * Helper class for third party tests using gora-Scylla backend.
 *
 * @see GoraTestDriver for test specifics.
 * This driver is the base for all test cases that require an embedded Scylla
 * server. In this case we draw on Hector's @see EmbeddedServerHelper.
 * It starts (setUp) and stops (tearDown) embedded Scylla server.
 */
public class GoraScyllaDBTestDriver extends GoraTestDriver {
    private static Logger log = LoggerFactory.getLogger(GoraScyllaDBTestDriver.class);

    private static final String DOCKER_IMAGE = "scylladb/scylla:4.0.7";
    private final FixedHostPortGenericContainer scylladbContainer;
    private String baseAddress;
    private Properties properties;

    public GoraScyllaDBTestDriver() {
        super(ScyllaDBStore.class);
        GenericContainer container = new FixedHostPortGenericContainer(DOCKER_IMAGE)
                .withFixedExposedPort(9160, 9160)
                .withFixedExposedPort(9042, 9042)
                .waitingFor(new StartupLogWaitStrategy())
                .withStartupTimeout(Duration.ofMinutes(10))
                .withEnv("developer-mode", "0")
                .withEnv("listen-address", "localhost")
                .withEnv("broadcast-address", "localhost")
                .withEnv("STANDALONE", "true");
        scylladbContainer = (FixedHostPortGenericContainer) container;
    }

    public String getHostname() {
        return scylladbContainer.getContainerIpAddress();
    }


    public void setParameters(Properties parameters) {
        this.properties = parameters;
    }

    @Override
    public <K, T extends Persistent> DataStore<K, T> createDataStore(Class<K> keyClass, Class<T> persistentClass) throws GoraException {
        return DataStoreFactory.createDataStore(ScyllaDBStore.class, keyClass, persistentClass, conf, properties, null);
    }


    /**
     * Starts embedded Scylla server.
     *
     * @throws Exception if an error occurs
     */
    @Override
    public void setUpClass() {
        log.info("Starting embedded Scylla Server...");
        try {
            scylladbContainer.start();
        } catch (Exception e) {
            log.error("Embedded Scylla server start failed!", e);

            // cleanup
            tearDownClass();
        }
    }

    /**
     * Stops embedded Scylla server.
     *
     * @throws Exception if an error occurs
     */
    @Override
    public void tearDownClass() {
        log.info("Shutting down Embedded Scylla server...");
        scylladbContainer.stop();
    }
}
