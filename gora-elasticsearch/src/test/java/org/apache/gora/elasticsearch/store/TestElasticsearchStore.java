/*
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
package org.apache.gora.elasticsearch.store;

import org.apache.gora.elasticsearch.GoraElasticsearchTestDriver;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMapping;
import org.apache.gora.elasticsearch.mapping.Field;
import org.apache.gora.elasticsearch.utils.ElasticsearchParameters;
import org.apache.gora.examples.generated.EmployeeInt;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.apache.gora.util.GoraException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Test case for ElasticsearchStore.
 */
public class TestElasticsearchStore extends DataStoreTestBase {

    static {
        setTestDriver(new GoraElasticsearchTestDriver());
    }

    @Test
    public void testInitialize() throws GoraException {
        log.info("test method: testInitialize");

        ElasticsearchMapping mapping = ((ElasticsearchStore) employeeStore).getMapping();

        Map<String, Field> fields = new HashMap<String, Field>() {{
            put("name", new Field("name", new Field.FieldType(Field.DataType.TEXT)));
            put("dateOfBirth", new Field("dateOfBirth", new Field.FieldType(Field.DataType.LONG)));
            put("ssn", new Field("ssn", new Field.FieldType(Field.DataType.TEXT)));
            put("value", new Field("value", new Field.FieldType(Field.DataType.TEXT)));
            put("salary", new Field("salary", new Field.FieldType(Field.DataType.INTEGER)));
            put("boss", new Field("boss", new Field.FieldType(Field.DataType.OBJECT)));
            put("webpage", new Field("webpage", new Field.FieldType(Field.DataType.OBJECT)));
        }};

        Assert.assertEquals("frontier", employeeStore.getSchemaName());
        Assert.assertEquals("frontier", mapping.getIndexName());
        Assert.assertEquals(fields, mapping.getFields());
    }

    @Test
    public void testLoadElasticsearchParameters() throws IOException {
        log.info("test method: testLoadElasticsearchParameters");

        Properties properties = DataStoreFactory.createProps();

        ElasticsearchParameters parameters = ElasticsearchParameters.load(properties, testDriver.getConfiguration());

        Assert.assertEquals("localhost", parameters.getHost());
        Assert.assertEquals("BASIC", parameters.getAuthenticationMethod());
        Assert.assertEquals("elastic", parameters.getUsername());
        Assert.assertEquals("password", parameters.getPassword());
    }

    @Test(expected = GoraException.class)
    public void testInvalidXmlFile() throws Exception {
        log.info("test method: testInvalidXmlFile");

        Properties properties = DataStoreFactory.createProps();
        properties.setProperty(ElasticsearchStore.PARSE_MAPPING_FILE_KEY, "gora-elasticsearch-mapping-invalid.xml");
        properties.setProperty(ElasticsearchStore.XSD_VALIDATION, "true");
        testDriver.createDataStore(String.class, EmployeeInt.class, properties);
    }

    @Test
    public void testXsdValidationParameter() throws GoraException {
        log.info("test method: testXsdValidationParameter");

        Properties properties = DataStoreFactory.createProps();
        properties.setProperty(ElasticsearchStore.PARSE_MAPPING_FILE_KEY, "gora-elasticsearch-mapping-invalid.xml");
        properties.setProperty(ElasticsearchStore.XSD_VALIDATION, "false");
        testDriver.createDataStore(String.class, EmployeeInt.class, properties);
    }
}
