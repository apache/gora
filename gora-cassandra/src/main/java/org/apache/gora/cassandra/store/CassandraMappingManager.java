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
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String KEYCLASS_ATTRIBUTE = "keyClass";
  private static final String HOST_ATTRIBUTE = "host";
  private static final String CLUSTER_ATTRIBUTE = "cluster";
  // singleton
  private static CassandraMappingManager manager = new CassandraMappingManager();

  public static CassandraMappingManager getManager() {
    return manager;
  }

  /**
  * Objects to maintain mapped keyspaces
  */
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

  public CassandraMapping get(Class<?> persistentClass) {
    String className = persistentClass.getName();
    Element mappingElement = mappingMap.get(className);
    if (mappingElement == null) {
      LOG.error("Mapping element does not exist for className=" + className);
      return null;
    }
    String keyspaceName = mappingElement.getAttributeValue(KEYSPACE_ELEMENT);
    if (LOG.isDebugEnabled()) {
      LOG.debug("className=" + className + " -> keyspaceName=" + keyspaceName);
    }
    Element keyspaceElement = keyspaceMap.get(keyspaceName);
    if (keyspaceElement == null) {
      LOG.error("Keyspace element does not exist for keyspaceName=" + keyspaceName);
      return null;
    }
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
    // get mapping file
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MAPPING_FILE);
    if (inputStream == null){
      LOG.warn("Mapping file '" + MAPPING_FILE + "' could not be found!");
      throw new IOException("Mapping file '" + MAPPING_FILE + "' could not be found!");
    }
    Document document = saxBuilder.build(inputStream);
    if (document == null) {
      LOG.warn("Mapping file '" + MAPPING_FILE + "' could not be found!");
    }
    Element root = document.getRootElement();
    // find cassandra keyspace element
    List<Element> keyspaces = root.getChildren(KEYSPACE_ELEMENT);
    if (keyspaces == null || keyspaces.size() == 0) {
      LOG.error("Error locating Cassandra Keyspace element!");
    }
    else {
      for (Element keyspace : keyspaces) {
        // log name, cluster and host for given keyspace(s)
        String keyspaceName = keyspace.getAttributeValue(NAME_ATTRIBUTE);
        String clusterName = keyspace.getAttributeValue(CLUSTER_ATTRIBUTE);
        String hostName = keyspace.getAttributeValue(HOST_ATTRIBUTE);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Located Cassandra Keyspace: '" + keyspaceName + "' in cluster '" + clusterName + 
          "' on host '" + hostName + "'.");
        }
        if (keyspaceName == null) {
          LOG.error("Error locating Cassandra Keyspace name attribute!");
          continue;
        }
        keyspaceMap.put(keyspaceName, keyspace);
      }
    }
      
    // load column definitions    
    List<Element> mappings = root.getChildren(MAPPING_ELEMENT);
    if (mappings == null || mappings.size() == 0) {
      LOG.error("Error locating Cassandra Mapping class element!");
    }
    else {
      for (Element mapping : mappings) {
        // associate persistent and class names for keyspace(s)
        String className = mapping.getAttributeValue(NAME_ATTRIBUTE);
        String keyClassName = mapping.getAttributeValue(KEYCLASS_ATTRIBUTE);
        String keyspaceName = mapping.getAttributeValue(KEYSPACE_ELEMENT);
        if (LOG.isDebugEnabled()) {
        LOG.debug("Located Cassandra Mapping: keyClass: '" + keyClassName + "' in storage class '" 
          + className + "' for Keyspace '" + keyspaceName + "'.");
        }
        if (className == null) {
          LOG.error("Error locating Cassandra Mapping class name attribute!");
          continue;
        }
        mappingMap.put(className, mapping);
      }
    }
  }
}
