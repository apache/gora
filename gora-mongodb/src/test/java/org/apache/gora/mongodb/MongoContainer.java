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

import com.mongodb.ServerAddress;
import org.testcontainers.containers.FixedHostPortGenericContainer;

/**
 * Use {@link FixedHostPortGenericContainer}
 * from <a href="https://www.testcontainers.org/">Testcontainers.org</a> project
 * to handle a MongoDB docker container.
 */
public class MongoContainer extends FixedHostPortGenericContainer<MongoContainer> {

    public static final int MONGO_PORT = 27017;

    public MongoContainer(String version) {
        super("mongo:" + version);
        withExposedPorts(MONGO_PORT);
    }

    public ServerAddress getServerAddress() {
        String ipAddress = getContainerIpAddress();
        int port = getMongoPort();
        return new ServerAddress(ipAddress, port);
    }

    public int getMongoPort() {
        return getMappedPort(MONGO_PORT);
    }
}
