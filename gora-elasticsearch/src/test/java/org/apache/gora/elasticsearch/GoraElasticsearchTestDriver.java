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
package org.apache.gora.elasticsearch;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.elasticsearch.store.ElasticsearchStore;
import org.apache.gora.elasticsearch.utils.ElasticsearchConstants;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Helper class for third part tests using gora-elasticsearch backend.
 *
 * @see GoraTestDriver
 */
public class GoraElasticsearchTestDriver extends GoraTestDriver {

    private static final String DOCKER_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:7.10.1";
    private ElasticsearchContainer elasticsearchContainer;

    /**
     * Constructor for this class.
     */
    public GoraElasticsearchTestDriver() {
        super(ElasticsearchStore.class);
        elasticsearchContainer = new ElasticsearchContainer(DOCKER_IMAGE);
    }

    /**
     * Initiate the Elasticsearch server on the default port.
     */
    @Override
    public void setUpClass() throws Exception {
        elasticsearchContainer.start();
        log.info("Setting up Elasticsearch test driver");

        int port = elasticsearchContainer.getMappedPort(ElasticsearchConstants.DEFAULT_PORT);
        String host = elasticsearchContainer.getContainerIpAddress();
        conf.set(ElasticsearchConstants.PROP_PORT, String.valueOf(port));
        conf.set(ElasticsearchConstants.PROP_HOST, host);
    }

    /**
     * Tear the server down.
     */
    @Override
    public void tearDownClass() throws Exception {
        elasticsearchContainer.close();
        log.info("Tearing down Elasticsearch test driver");
    }
}
