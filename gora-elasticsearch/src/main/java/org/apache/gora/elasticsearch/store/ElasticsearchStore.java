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
import org.apache.gora.elasticsearch.utils.ElasticsearchParameters;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
            ElasticsearchParameters parameters = ElasticsearchParameters.load(properties, getConf());
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
            client = createClient(parameters);
            LOG.info("Elasticsearch store was successfully initialized.");
        } catch (Exception ex) {
            LOG.error("Error while initializing Elasticsearch store", ex);
            throw new GoraException(ex);
        }
    }

    private RestHighLevelClient createClient(ElasticsearchParameters parameters) {
        RestClientBuilder clientBuilder = RestClient.builder(new HttpHost(parameters.getHost(), parameters.getPort()));

        // Choosing the authentication method.
        switch (parameters.getAuthenticationMethod()) {
            case "BASIC":
                if (parameters.getUsername() != null && parameters.getPassword() != null) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(parameters.getUsername(), parameters.getPassword()));
                    clientBuilder.setHttpClientConfigCallback(
                            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
                }
                break;
            case "TOKEN":
                if (parameters.getAuthorizationToken() != null) {
                    Header[] defaultHeaders = new Header[]{new BasicHeader("Authorization",
                            parameters.getAuthorizationToken())};
                    clientBuilder.setDefaultHeaders(defaultHeaders);
                }
                break;
            case "APIKEY":
                if (parameters.getApiKeyId() != null && parameters.getApiKeySecret() != null) {
                    String apiKeyAuth = Base64.getEncoder()
                            .encodeToString((parameters.getApiKeyId() + ":" + parameters.getApiKeySecret())
                                    .getBytes(StandardCharsets.UTF_8));
                    Header[] defaultHeaders = new Header[]{new BasicHeader("Authorization", "ApiKey " + apiKeyAuth)};
                    clientBuilder.setDefaultHeaders(defaultHeaders);

                }
                break;
        }

        if (parameters.getConnectTimeout() != 0) {
            clientBuilder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder.setConnectTimeout(parameters.getConnectTimeout()));
        }

        if (parameters.getSocketTimeout() != 0) {
            clientBuilder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder.setSocketTimeout(parameters.getSocketTimeout()));
        }

        if (parameters.getIoThreadCount() != 0) {
            clientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom()
                            .setIoThreadCount(parameters.getIoThreadCount()).build()));
        }
        return new RestHighLevelClient(clientBuilder);
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
        CreateIndexRequest request = new CreateIndexRequest(elasticsearchMapping.getIndexName());
        try {
            if (!client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT)) {
                client.indices().create(request, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public void deleteSchema() throws GoraException {
        DeleteIndexRequest request = new DeleteIndexRequest(elasticsearchMapping.getIndexName());
        try {
            if (client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT)) {
                client.indices().delete(request, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public boolean schemaExists() throws GoraException {
        try {
            return client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public boolean exists(K key) throws GoraException {
        GetRequest getRequest = new GetRequest(elasticsearchMapping.getIndexName(), (String) key);
        getRequest.fetchSourceContext(new FetchSourceContext(false)).storedFields("_none_");
        try {
            return client.exists(getRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
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
        try {
            client.indices().flush(new FlushRequest(), RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
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
