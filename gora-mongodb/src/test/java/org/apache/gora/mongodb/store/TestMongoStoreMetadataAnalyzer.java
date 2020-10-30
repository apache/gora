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
package org.apache.gora.mongodb.store;

import com.google.common.collect.Sets;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.apache.gora.mongodb.GoraMongodbTestDriver;
import org.apache.gora.mongodb.MongoContainer;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreMetadataFactory;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.*;

/**
 * Test case for MongoStoreMetadataAnalyzer
 */
public class TestMongoStoreMetadataAnalyzer extends TestMongoStore {
    private DataStoreMetadataAnalyzer storeMetadataAnalyzer;
    private MongoDatabase mongoDatabase;

    @ClassRule
    public final static MongoContainer container = new MongoContainer("4.2");

    static {
        setTestDriver(new GoraMongodbTestDriver(container));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Properties prop = DataStoreFactory.createProps();

        Configuration conf = testDriver.getConfiguration();

        MongoStoreParameters parameters = MongoStoreParameters.load(prop, conf);
        MongoClient mongoClient = MongoStore.getClient(parameters);
        mongoDatabase = mongoClient.getDatabase(parameters.getDbname());

        storeMetadataAnalyzer = DataStoreMetadataFactory.createAnalyzer(conf);
    }

    @Test
    public void testGetType() {
        String actualType = storeMetadataAnalyzer.getType();
        String expectedType = "MONGODB";
        Assert.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetTablesNames() throws GoraException {
        HashSet<String> actualTablesNames = Sets.newHashSet(storeMetadataAnalyzer.getTablesNames());
        HashSet<String> expectedTablesNames = new HashSet<String>() {
            {
                add("frontier");
                add("webpage");
            }
        };
        Assert.assertEquals(expectedTablesNames, actualTablesNames);
    }

    @Test
    public void testGetTableInfo() throws GoraException {
        mongoDatabase.getCollection("frontier").insertOne(new Document(new HashMap<String, Object>() {
            {
                put("name", "Kate");
                put("dateOfBirth", 830563200L);
                put("ssn", "078051120");
                put("value", "varchar");
                put("salary", 70000);
                put("boss", "");
                put("webpage", "");
            }
        }));
        MongoStoreCollectionMetadata actualCollectionMetadata = (MongoStoreCollectionMetadata) storeMetadataAnalyzer.getTableInfo("frontier");

        List<String> expectedDocumentKeys = new ArrayList<String>() {
            {
                add("name");
                add("dateOfBirth");
                add("ssn");
                add("value");
                add("salary");
                add("boss");
                add("webpage");
                add("_id");
            }
        };

        List<String> expectedDocumentTypes = new ArrayList<String>() {
            {
                add("OBJECT_ID");
                add("STRING");
                add("STRING");
                add("INT64");
                add("STRING");
                add("INT32");
                add("STRING");
                add("STRING");
            }
        };

        Assert.assertEquals(expectedDocumentKeys.size(), actualCollectionMetadata.getDocumentTypes().size());
        Assert.assertTrue(expectedDocumentKeys.containsAll(actualCollectionMetadata.getDocumentKeys()));

        Assert.assertEquals(expectedDocumentTypes.size(), actualCollectionMetadata.getDocumentTypes().size());
        Assert.assertTrue(expectedDocumentTypes.containsAll(actualCollectionMetadata.getDocumentTypes()));
    }
}