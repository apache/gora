/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.elasticsearch.store;

import org.apache.gora.elasticsearch.mapping.ElasticsearchMapping;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMappingBuilder;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

/**
 * Implementation of a Apache Elasticsearch data store to be used by Apache Gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class ElasticsearchStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    public static final Logger LOG = LoggerFactory.getLogger(ElasticsearchStore.class);
    private static final String DEFAULT_MAPPING_FILE = "gora-elasticsearch-mapping.xml";
    private static final String PARSE_MAPPING_FILE_KEY = "gora.elasticsearch.mapping.file";
    private static final String XML_MAPPING_DEFINITION = "gora.mapping";

    /**
     * Elasticsearch client
     */
    private RestHighLevelClient client;

    /**
     * Mapping definition for Elasticsearch
     */
    private ElasticsearchMapping elasticsearchMapping;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        try {
            LOG.debug("Initializing Elasticsearch store");
            super.initialize(keyClass, persistentClass, properties);
            ElasticsearchMappingBuilder<K, T> builder = new ElasticsearchMappingBuilder<>(this);
            InputStream mappingStream;
            if (properties.containsKey(XML_MAPPING_DEFINITION)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("{} = {}", XML_MAPPING_DEFINITION, properties.getProperty(XML_MAPPING_DEFINITION));
                }
                mappingStream = org.apache.commons.io.IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
            } else {
                mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
            }
            builder.readMappingFile(mappingStream);
            elasticsearchMapping = builder.getElasticsearchMapping();
            client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
            LOG.info("Elasticsearch store was successfully initialized.");
        } catch (Exception ex) {
            LOG.error("Error while initializing Elasticsearch store", ex);
            throw new GoraException(ex);
        }
    }

    public ElasticsearchMapping getMapping() {
        return elasticsearchMapping;
    }

    @Override
    public String getSchemaName() {
        return elasticsearchMapping.getIndexName();
    }

    @Override
    public String getSchemaName(final String mappingSchemaName, final Class<?> persistentClass) {
        return super.getSchemaName(mappingSchemaName, persistentClass);
    }

    @Override
    public void createSchema() throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteSchema() throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean schemaExists() throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean exists(K key) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T get(K key, String[] fields) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void put(K key, T obj) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean delete(K key) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long deleteByQuery(Query<K, T> query) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Query<K, T> newQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void flush() throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        try {
            client.close();
            LOG.info("Elasticsearch datastore destroyed successfully.");
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
