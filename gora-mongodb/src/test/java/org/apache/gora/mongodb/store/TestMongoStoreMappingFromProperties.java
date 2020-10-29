/**
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

package org.apache.gora.mongodb.store;

import com.mongodb.ServerAddress;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.mongodb.MongoContainer;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Test case for loading mappings from properties
 */
public class TestMongoStoreMappingFromProperties {
    private MongoContainer _container;

    @Before
    public void setUp() {
        // Container for MongoStore
        this._container = new MongoContainer("4.2");
        _container.start();
    }

    @Test
    public void testInitialize() throws IOException {
        // Simple mapping XML
        String mappingXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gora-otd>\n" +
                "    <class name=\"org.apache.gora.examples.generated.Employee\" keyClass=\"java.lang.String\" document=\"frontier\">\n" +
                "        <field name=\"name\" docfield=\"name\" type=\"string\"/>\n" +
                "    </class>\n" +
                "</gora-otd>";

        // Initiate the MongoDB server on the default port
        ServerAddress address = _container.getServerAddress();
        int port = address.getPort();
        String host = address.getHost();

        Properties prop = DataStoreFactory.createProps();

        // Store Mongo server "host:port" in Hadoop configuration
        // so that MongoStore will be able to get it latter
        Configuration conf = new Configuration();
        conf.set(MongoStoreParameters.PROP_MONGO_SERVERS, host + ":" + port);

        // Set mapping XML property
        prop.setProperty(MongoStore.XML_MAPPING_DEFINITION, mappingXml);
        MongoStore<String, Employee> mongoStore =
                DataStoreFactory.createDataStore(MongoStore.class, String.class, Employee.class, conf, prop);
        MongoMapping actualMapping = mongoStore.getMapping();

        // Read mapping definition from mappingXml
        MongoMappingBuilder<String, Employee> builder = new MongoMappingBuilder<>(mongoStore);
        builder.fromInputStream(IOUtils.toInputStream(mappingXml, (Charset) null));
        MongoMapping expectedMapping = builder.build();

        Assert.assertEquals(expectedMapping, actualMapping);
    }

    @After
    public void tearDown() {
        _container.stop();
    }
}
