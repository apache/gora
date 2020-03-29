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

import com.mongodb.ServerAddress;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.mongodb.MongoContainer;
import org.apache.gora.mongodb.store.MongoStore;
import org.apache.gora.mongodb.store.MongoStoreParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.io.IOException;
import java.time.Duration;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Driver to set up an embedded MongoDB database instance for use in our  * unit tests.
 * This class is specially written to automate authentication mechanisms.
 */
class GoraMongodbAuthenticationTestDriver extends GoraTestDriver {
    private static final Logger log = LoggerFactory.getLogger(GoraMongodbAuthenticationTestDriver.class);
    private MongoContainer _container;
    private final String adminUsername = "madhawa";
    private final String adminPassword = "123";
    private final String useVersion;
    private final String authMechanisms;

    GoraMongodbAuthenticationTestDriver(String authMechanisms, String useVersion) {
        super(MongoStore.class);
        this.authMechanisms = authMechanisms;
        this.useVersion = useVersion;
    }

    private void doStart() throws Exception {
        try {
            log.info("Starting the embedded Mongodb server");
            startWithAuth();
            if (authMechanisms.equals("SCRAM-SHA-1")) {
                setSCRAM_SHA_1Credentials();
            }
            // Store Mongo server "host:port" in Hadoop configuration
            // so that MongoStore will be able to get it latter
            ServerAddress address = _container.getServerAddress();
            int port = address.getPort();
            String host = address.getHost();

            conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, host + ":" + port);
            conf.set(MongoStoreParameters.PROP_MONGO_DB, "admin");
            conf.set(MongoStoreParameters.PROP_MONGO_AUTHENTICATION_TYPE, authMechanisms);
            conf.set(MongoStoreParameters.PROP_MONGO_LOGIN, adminUsername);
            conf.set(MongoStoreParameters.PROP_MONGO_SECRET, adminPassword);
        } catch (Exception e) {
            log.error("Error starting embedded Mongodb server... tearing down test driver.");
            tearDownClass();
        }
    }

    private void startWithAuth() throws IOException {
        try {
            prepareExecutable();
            _container.start();
        } catch (Exception e) {
            log.error("Error starting embedded Mongodb server... tearing down test driver.");
            tearDownClass();
        }
    }

    private void prepareExecutable() throws IOException {
        _container = new MongoContainer(useVersion);
        // https://hub.docker.com/_/mongo
        // These variables, used in conjunction, create a new user and set that user's password.
        // This user is created in the admin authentication database
        // and given the role of root, which is a "superuser" role.
        _container.withEnv("MONGO_INITDB_ROOT_USERNAME", adminUsername);
        _container.withEnv("MONGO_INITDB_ROOT_PASSWORD", adminPassword);

        // To enable authentication, MongoDB will have to restart itself
        // so wait for at least 5 sec
        _container.withMinimumRunningDuration(Duration.ofSeconds(5));

        // https://docs.mongodb.com/manual/tutorial/enable-authentication/
        // https://docs.mongodb.com/manual/reference/parameters/#param.authenticationMechanisms
        _container.withCommand("--auth", "--setParameter", "authenticationMechanisms=" + authMechanisms);
    }

    private void setSCRAM_SHA_1Credentials() throws Exception {
        final String scriptText1 = "db.adminCommand({authSchemaUpgrade: 1});\n";
        runScriptAndWait(scriptText1, "admin", adminUsername, adminPassword);
    }

    private void runScriptAndWait(String scriptText, String dbName, String username, String password)
            throws InterruptedException, IOException {
        final StringBuilder builder = new StringBuilder("mongo --quiet");
        if (!isEmpty(username)) {
            builder.append(" --username ").append(username);
        }
        if (!isEmpty(password)) {
            builder.append(" --password ").append(password);
        }
        if (!isEmpty(dbName)) {
            builder.append(" ").append(dbName);
        }
        builder.append(" --eval '").append(scriptText).append("'");

        Container.ExecResult res = _container.execInContainer("/bin/bash", "-c", builder.toString());
        if (!isEmpty(res.getStderr())) {
            log.error("Unable to run script on Mongodb server {}: {}", scriptText, res.getStderr());
            throw new IOException(res.getStderr());
        }
    }

    @Override
    public void setUpClass() throws Exception {
        doStart();
    }

    /**
     * Tear the server down
     */
    @Override
    public void tearDownClass() {
        log.info("Shutting down mongodb server...");
        _container.stop();
    }
}
