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
package org.apache.gora.neo4j;

import org.apache.gora.neo4j.utils.Neo4jStartupLogWaitStrategy;
import java.time.Duration;
import org.apache.gora.GoraTestDriver;
import org.apache.gora.neo4j.store.Neo4jConstants;
import org.apache.gora.neo4j.store.Neo4jStore;
import org.testcontainers.containers.GenericContainer;

public class GoraNeo4jTestDriver extends GoraTestDriver {

  private static final String DOCKER_IMAGE = "neo4j:enterprise";
  private final GenericContainer neo4jContainer;

  public GoraNeo4jTestDriver() {
    super(Neo4jStore.class);
    GenericContainer container = new GenericContainer(DOCKER_IMAGE)
            .waitingFor(new Neo4jStartupLogWaitStrategy())
            .withStartupTimeout(Duration.ofMinutes(3))
            .withEnv("NEO4J_AUTH", "neo4j/password")
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
    neo4jContainer = container;
  }

  @Override
  public void setUpClass() throws Exception {
    neo4jContainer.start();
    String containerIpAddress = neo4jContainer.getContainerIpAddress();
    String port = neo4jContainer.getMappedPort(7687).toString();
    conf.set(Neo4jConstants.PROPERTY_HOST, containerIpAddress);
    conf.set(Neo4jConstants.PROPERTY_PORT, port);
    log.info("Setting up Neo4j test driver");

  }

  @Override
  public void tearDownClass() throws Exception {
    neo4jContainer.stop();
    log.info("Tearing down Neo4j test driver");
  }

}
