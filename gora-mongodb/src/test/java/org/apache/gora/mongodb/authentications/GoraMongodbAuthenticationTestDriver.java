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
package org.apache.gora.mongodb.authentications;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.mongodb.store.MongoStoreParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Driver to set up an embedded MongoDB database instance for use in our  * unit tests.
 * This class is specially written to automate authentication mechanisms.
 */
class GoraMongodbAuthenticationTestDriver extends GoraTestDriver {
    private static final Logger log = LoggerFactory.getLogger(GoraMongodbAuthenticationTestDriver.class);
    private GenericContainer _container;
    private static final String adminUsername = "madhawa";
    private static final String adminPassword = "123";
    private final String authMechanisms;

    GoraMongodbAuthenticationTestDriver(String authMechanisms, GenericContainer container) {
        super(MongoStore.class);
        this.authMechanisms = authMechanisms;
        this._container = container;
    }

    @Override
    public void setUpClass() throws Exception {
        log.info("Starting the embedded Mongodb server");

        // Store Mongo server "host:port" in Hadoop configuration
        // so that MongoStore will be able to get it latter
        int port = _container.getMappedPort(27017);
        String host = _container.getContainerIpAddress();

        conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, host + ":" + port);
        conf.set(MongoStoreParameters.PROP_MONGO_DB, "admin");
        conf.set(MongoStoreParameters.PROP_MONGO_AUTHENTICATION_TYPE, authMechanisms);
        conf.set(MongoStoreParameters.PROP_MONGO_LOGIN, adminUsername);
        conf.set(MongoStoreParameters.PROP_MONGO_SECRET, adminPassword);
    }

    public static GenericContainer mongoContainer(String authMechanisms, String useVersion) {
        GenericContainer _container = new GenericContainer(useVersion).withExposedPorts(27017);

        // https://hub.docker.com/_/mongo
        // These variables, used in conjunction, create a new user and set that user's password.
        // This user is created in the admin authentication database
        // and given the role of root, which is a "superuser" role.
        _container.withEnv("MONGO_INITDB_ROOT_USERNAME", adminUsername);
        _container.withEnv("MONGO_INITDB_ROOT_PASSWORD", adminPassword);

        // To enable authentication, MongoDB will have to restart itself
        int restartCount = 2;
        _container.waitingFor(
                Wait.forLogMessage("(?i).*waiting for connections.*", restartCount)
        );

        // https://docs.mongodb.com/manual/tutorial/enable-authentication/
        // https://docs.mongodb.com/manual/reference/parameters/#param.authenticationMechanisms
        _container.withCommand("--auth", "--setParameter", "authenticationMechanisms=" + authMechanisms);

        return _container;
    }

    /**
     * Tear the server down
     */
    @Override
    public void tearDownClass() {
    }
}
