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
package org.apache.gora.mongodb.authentications;

import org.apache.gora.mongodb.store.TestMongoStore;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;

import static org.apache.gora.mongodb.authentications.GoraMongodbAuthenticationTestDriver.mongoContainer;

/**
 * Perform {@link TestMongoStore} tests on MongoDB 4.2.x server with SCRAM-SHA-1 Authentication mechanism
 */
public class SCRAM_SHA_1_AuthenticationTest extends TestMongoStore {

    public static final String AUTH_MECHANISMS = "SCRAM-SHA-1";

    @ClassRule
    public final static GenericContainer container = mongoContainer(AUTH_MECHANISMS, "mongo:4.2");

    static {
        setTestDriver(new GoraMongodbAuthenticationTestDriver(AUTH_MECHANISMS, container));
    }
}
