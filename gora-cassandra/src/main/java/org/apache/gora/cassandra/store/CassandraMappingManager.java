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

package org.apache.gora.cassandra.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraMappingManager {
  
  public static final Logger LOG = LoggerFactory.getLogger(CassandraMappingManager.class);
  
  private static final String MAPPING_FILE = "gora-cassandra-mapping.xml";
  private static final String KEYSPACE_ELEMENT = "keyspace";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String MAPPING_ELEMENT = "class";
  private static final String COLUMN_ATTRIBUTE = "qualifier";
  private static final String FAMILY_ATTRIBUTE = "family";
  private static final String SUPER_ATTRIBUTE = "type";
  private static final String CLUSTER_ATTRIBUTE = "cluster";
  private static final String HOST_ATTRIBUTE = "host";

  // singleton
  private static CassandraMappingManager manager = new CassandraMappingManager();

  public static CassandraMappingManager getManager() {
    return manager;
  }

  //
  private Map<String, Element> keyspaceMap = null;
  private Map<String, Element>  mappingMap = null;

  private CassandraMappingManager() {
    keyspaceMap = new HashMap<String, Element>();
    mappingMap  = new HashMap<String, Element>();
    try {
      loadConfiguration();
    }
    catch (JDOMException e) {
      LOG.error(e.toString());
    }
    catch (IOException e) {
      LOG.error(e.toString());
    }
  }

  public CassandraMapping get(Class persistentClass) {
    String className = persistentClass.getName();
    Element mappingElement = mappingMap.get(className);
    String keyspaceName = mappingElement.getAttributeValue(KEYSPACE_ELEMENT);
    Element keyspaceElement = keyspaceMap.get(keyspaceName);
    return new CassandraMapping(keyspaceElement, mappingElement);
  }

  /**
   * Primary class for loading Cassandra configuration from the 'MAPPING_FILE'.
   * 
   * @throws JDOMException
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void loadConfiguration() throws JDOMException, IOException {
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(getClass().getClassLoader().getResourceAsStream(MAPPING_FILE));
    if (document == null) {
      LOG.warn("Mapping file '" + MAPPING_FILE + "' could not be found!");
    }
    Element root = document.getRootElement();
    
    List<Element> keyspaces = root.getChildren(KEYSPACE_ELEMENT);
    if (keyspaces == null || keyspaces.size() == 0) {
      LOG.warn("Error locating Cassandra Keyspace element!");
    }
    else {
      LOG.info("Located Cassandra Keyspace: '" + KEYSPACE_ELEMENT + "'");
      for (Element keyspace : keyspaces) {
        String keyspaceName = keyspace.getAttributeValue(NAME_ATTRIBUTE);
        if (keyspaceName == null) {
    	    LOG.warn("Error locating Cassandra Keyspace name attribute!");
        }
    	LOG.info("Located Cassandra Keyspace name: '" + NAME_ATTRIBUTE + "'");
        keyspaceMap.put(keyspaceName, keyspace);
      }
    }
      
    // load column definitions    
    List<Element> mappings = root.getChildren(MAPPING_ELEMENT);
    if (mappings == null || mappings.size() == 0) {
      LOG.warn("Error locating Cassandra Mapping element!");
    }
    else {
      LOG.info("Located Cassandra Mapping: '" + MAPPING_ELEMENT + "'");
      for (Element mapping : mappings) {
        String className = mapping.getAttributeValue(NAME_ATTRIBUTE);
        if (className == null) {
    	    LOG.warn("Error locating Cassandra Mapping class name attribute!");
    	    continue;
        }
    	LOG.info("Located Cassandra Mapping class name: '" + NAME_ATTRIBUTE + "'");
        mappingMap.put(className, mapping);
      }
    }
  }
}
