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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MongoStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

    private MongoDatabase mongoDatabase;
    private MongoClient mongoClient;

    @Override
    public void initialize() {
        MongoStoreParameters parameters = MongoStoreParameters.load(properties, getConf());
        mongoClient = MongoStore.getClient(parameters);
        mongoDatabase = mongoClient.getDatabase(parameters.getDbname());
    }

    @Override
    public String getType() {
        return "MONGODB";
    }

    @Override
    public List<String> getTablesNames() throws GoraException {
        try {
            return mongoDatabase.listCollectionNames().into(new ArrayList<>());
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public MongoStoreCollectionMetadata getTableInfo(String tableName) {
        MongoStoreCollectionMetadata collectionMetadata = new MongoStoreCollectionMetadata();
        Document document = mongoDatabase.getCollection(tableName).find().first();
        collectionMetadata.getCollectionDocumentKeys().addAll(document.keySet());
        collectionMetadata.getCollectionDocumentTypes().addAll(document.values()
                .stream().map(Object::getClass).collect(Collectors.toList()));
        return collectionMetadata;
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            this.mongoClient.close();
        }
    }
}