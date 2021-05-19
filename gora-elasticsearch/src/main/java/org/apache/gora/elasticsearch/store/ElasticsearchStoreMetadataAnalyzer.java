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

import org.apache.gora.elasticsearch.utils.ElasticsearchParameters;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ElasticsearchStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchStoreMetadataAnalyzer.class);

    private RestHighLevelClient elasticsearchClient;

    @Override
    public void initialize() throws GoraException {
        ElasticsearchParameters parameters = ElasticsearchParameters.load(properties, getConf());
        elasticsearchClient = ElasticsearchStore.createClient(parameters);
    }

    @Override
    public String getType() {
        return "ELASTICSEARCH";
    }

    @Override
    public List<String> getTablesNames() throws GoraException {
        GetIndexRequest request = new GetIndexRequest("*");
        GetIndexResponse response;
        try {
            response = elasticsearchClient.indices().get(request, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
        if (response == null) {
            LOG.error("Could not find indices.");
            throw new GoraException("Could not find indices.");
        }
        return Arrays.asList(response.getIndices());
    }

    @Override
    public ElasticsearchStoreCollectionMetadata getTableInfo(String tableName) throws GoraException {
        GetIndexRequest request = new GetIndexRequest(tableName);
        GetIndexResponse getIndexResponse;
        try {
            getIndexResponse = elasticsearchClient.indices().get(request, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
        MappingMetadata indexMappings = getIndexResponse.getMappings().get(tableName);
        Map<String, Object> indexKeysAndTypes = (Map<String, Object>) indexMappings.getSourceAsMap().get("properties");

        List<String> documentTypes = new ArrayList<>();
        List<String> documentKeys = new ArrayList<>();

        for (Map.Entry<String, Object> entry : indexKeysAndTypes.entrySet()) {
            Map<String, Object> subEntry = (Map<String, Object>) entry.getValue();
            documentTypes.add((String) subEntry.get("type"));
            documentKeys.add(entry.getKey());
        }

        ElasticsearchStoreCollectionMetadata collectionMetadata = new ElasticsearchStoreCollectionMetadata();
        collectionMetadata.setDocumentKeys(documentKeys);
        collectionMetadata.setDocumentTypes(documentTypes);
        return collectionMetadata;
    }

    @Override
    public void close() throws IOException {
        if (elasticsearchClient != null) {
            this.elasticsearchClient.close();
        }
    }
}
