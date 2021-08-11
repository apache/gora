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
package org.apache.gora.elasticsearch.mapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping definitions for Elasticsearch.
 */
public class ElasticsearchMapping {

    private String indexName;
    private Map<String, Field> fields;

    /**
     * Empty constructor for the ElasticsearchMapping class.
     */
    public ElasticsearchMapping() {
        fields = new HashMap<>();
    }

    /**
     * Returns the name of Elasticsearch index linked to the mapping.
     *
     * @return Index's name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Sets the index name of the Elasticsearch mapping.
     *
     * @param indexName Index's name
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Returns a map with all mapped fields.
     *
     * @return Map containing mapped fields
     */
    public Map<String, Field> getFields() {
        return fields;
    }

    /**
     * Add a new field to the mapped fields.
     *
     * @param classFieldName Field name in the persisted class
     * @param field Mapped field from Elasticsearch index
     */
    public void addField(String classFieldName, Field field) {
        fields.put(classFieldName, field);
    }
}
