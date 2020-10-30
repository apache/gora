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
package org.apache.gora.mongodb.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class with the collection info returned by MongoStoreMetadataAnalyzer with the information
 * about a MongoStore collection.
 * <p>
 * - Collection document keys
 */
public class MongoStoreCollectionMetadata {

    private List<String> documentKeys = new ArrayList<>();
    private List<String> documentTypes = new ArrayList<>();

    /**
     * Collection document keys present in a given collection at MongoStore
     */
    public List<String> getDocumentKeys() {
        return documentKeys;
    }

    public void setDocumentKeys(List<String> documentKeys) {
        this.documentKeys = documentKeys;
    }

    /**
     * Collection document types present in a given collection at MongoStore
     */
    public List<String> getDocumentTypes() {
        return documentTypes;
    }

    public void setDocumentTypes(List<String> documentTypes) {
        this.documentTypes = documentTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoStoreCollectionMetadata that = (MongoStoreCollectionMetadata) o;
        return documentKeys.equals(that.documentKeys) &&
                documentTypes.equals(that.documentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentKeys, documentTypes);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MongoStoreCollectionMetadata.class.getSimpleName() + "[", "]")
                .add("collectionDocumentKeys=" + documentKeys)
                .add("collectionDocumentTypes=" + documentTypes)
                .toString();
    }
}