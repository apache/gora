/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the"
 * License"); you may not use this file except in compliance
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

package org.apache.gora.scylladb.store;

import org.apache.gora.scylladb.GoraScyllaDBTestDriver;
import org.apache.gora.examples.WebPageDataCreator;
import org.apache.gora.examples.generated.WebPage;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.store.DataStoreTestUtil;
import org.apache.gora.util.GoraException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static org.apache.gora.examples.WebPageDataCreator.SORTED_URLS;
import static org.apache.gora.examples.WebPageDataCreator.URLS;
import static org.apache.gora.store.DataStoreTestUtil.assertEmptyResults;
import static org.apache.gora.store.DataStoreTestUtil.assertNumResults;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Testing class for all standard gora-scylladb functionality.
 * We extend DataStoreTestBase enabling us to run the entire base test
 * suite for Gora.
 * <p>
 * Test Avro Serialization for ScyllaDBStore.
 */
public class TestScyllaDBStore extends DataStoreTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TestScyllaDBStore.class);
    private static Properties properties;
    private static GoraScyllaDBTestDriver testDriver;

    public TestScyllaDBStore() {
        testDriver = new GoraScyllaDBTestDriver();
        setProperties();
        testDriver.setParameters(properties);
        setTestDriver(testDriver);
    }

    private static void setProperties() {
        properties = new Properties();
        properties.setProperty(ScyllaDBStoreParameters.SCYLLADB_SERVERS, testDriver.getHostname());
        properties.setProperty(ScyllaDBStoreParameters.PORT, "9042");
        properties.setProperty(ScyllaDBStoreParameters.SCYLLADB_SERIALIZATION_TYPE, "avro");
        properties.setProperty("gora.scylladbstore.mapping.file", "avro/gora-scylladb-mapping.xml");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Ignore()
    @Override
    public void testGetPartitions() throws IOException {
    }

    private void preConfiguration() throws GoraException {
        if (webPageStore.schemaExists()) {
            webPageStore.truncateSchema();
        } else {
            webPageStore.createSchema();
        }
    }

    @Test
    public void testQuery() throws Exception {
        preConfiguration();
        log.info("test method: testQuery");
        DataStoreTestUtil.testQueryWebPages(webPageStore);
    }

    @Test
    public void testQueryStartKey() throws Exception {
        preConfiguration();
        log.info("test method: testQueryStartKey");
        DataStoreTestUtil.testQueryWebPageStartKey(webPageStore);
    }

    @Test
    public void testQueryEndKey() throws Exception {
        preConfiguration();
        log.info("test method: testQueryEndKey");
        DataStoreTestUtil.testQueryWebPageEndKey(webPageStore);
    }

    @Test
    public void testQueryKeyRange() throws Exception {
        preConfiguration();
        log.info("test method: testQueryKetRange");
        DataStoreTestUtil.testQueryWebPageKeyRange(webPageStore);
    }

    @Test
    public void testDelete() throws Exception {
        preConfiguration();
        log.info("test method: testDelete");
        DataStoreTestUtil.testDelete(webPageStore);
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        preConfiguration();
        log.info("test method: testDeleteByQuery");
        DataStore store = webPageStore;
        Query<String, WebPage> query;
        //test 1 - delete all
        WebPageDataCreator.createWebPageData(store);

        query = store.newQuery();

        assertNumResults(store.newQuery(), URLS.length);
        store.deleteByQuery(query);
        store.flush();
        assertEmptyResults(store.newQuery());
        store.truncateSchema();
    }

    @Test
    public void testDeleteByQueryFields() throws Exception {
        preConfiguration();
        log.info("test method: testQueryByQueryFields");
        //test 5 - delete all with some fields
        WebPageDataCreator.createWebPageData(webPageStore);
        Query query = webPageStore.newQuery();
        query.setFields("outlinks", "parsedContent", "content");

        for (String SORTED_URL : SORTED_URLS) {
            query.setKey(SORTED_URL);
            webPageStore.deleteByQuery(query);
            WebPage page = webPageStore.get(SORTED_URL);
            assertNotNull(page);
            assertNotNull(page.getUrl());
            assertEquals(page.getUrl().toString(), SORTED_URL);
            assertEquals("Map of Outlinks should have a size of '0' as the deleteByQuery "
                    + "not only removes the data but also the data structure.", 0, page.getOutlinks().size());
            assertEquals(0, page.getParsedContent().size());
            if (page.getContent() != null) {
                LOG.info("url:" + page.getUrl().toString());
                LOG.info("limit:" + page.getContent().limit());
            } else {
                assertNull(page.getContent());
            }
        }
    }

    @Ignore("Type 3 Union is not supported for Scylla")
    public void testGet3UnionField() {
    }
}
