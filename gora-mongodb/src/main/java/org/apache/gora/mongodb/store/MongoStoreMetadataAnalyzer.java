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

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.apache.gora.mongodb.utils.Utf8Codec;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MongoStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {
    private static CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new Utf8Codec())
    );

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
        collectionMetadata.getDocumentKeys().addAll(document.keySet());
        Collection<BsonValue> values = document.toBsonDocument(null, codecRegistry).values();
        collectionMetadata.getDocumentTypes()
                .addAll(values.stream().map(bson -> bson.getBsonType().toString()).collect(Collectors.toList()));
        return collectionMetadata;
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            this.mongoClient.close();
        }
    }
}