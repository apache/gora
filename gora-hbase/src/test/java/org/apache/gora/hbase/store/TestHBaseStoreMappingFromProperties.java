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

package org.apache.gora.hbase.store;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.gora.examples.generated.Employee;
import org.apache.gora.hbase.util.HBaseClusterSingleton;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Test case for loading mappings from properties
 */
public class TestHBaseStoreMappingFromProperties {
    private HBaseClusterSingleton cluster;

    @Before
    public void setUp() {
        // Cluster for HBaseStore
        cluster = HBaseClusterSingleton.build(1);
    }

    @Test
    public void testInitialize() throws IOException {
        // Simple mapping XML
        String mappingXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gora-otd>\n" +
                "\n" +
                "  <table name=\"Employee\">\n" +
                "    <family name=\"info\"/>\n" +
                "  </table>\n" +
                "    <class name=\"org.apache.gora.examples.generated.Employee\" keyClass=\"java.lang.String\" table=\"Employee\">\n" +
                "    <field name=\"name\" family=\"info\" qualifier=\"nm\"/>\n" +
                "    </class>\n" +
                "</gora-otd>";

        Configuration conf = HBaseConfiguration.create(cluster.getHbaseTestingUtil().getConfiguration());
        Properties prop = DataStoreFactory.createProps();

        // Set mapping XML property
        prop.setProperty(HBaseStore.XML_MAPPING_DEFINITION, mappingXml);
        HBaseStore<String, Employee> hBaseStore =
                DataStoreFactory.createDataStore(HBaseStore.class, String.class, Employee.class, conf, prop);
        HBaseMapping actualMapping = hBaseStore.getMapping();

        // Read mapping definition from mappingXml
        HBaseMapping expectedMapping = hBaseStore.readMapping(IOUtils.toInputStream(mappingXml, (Charset) null));

        Assert.assertEquals(expectedMapping, actualMapping);
    }

    @After
    public void tearDown() throws IOException {
        cluster.shutdownMiniCluster();
    }
}