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

public class CassandraMapping {
  
  public static final Logger LOG = LoggerFactory.getLogger(CassandraMapping.class);
  
  private static final String MAPPING_FILE = "gora-cassandra-mapping.xml";
  private static final String KEYSPACE_ELEMENT = "keyspace";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String MAPPING_ELEMENT = "class";
  private static final String COLUMN_ATTRIBUTE = "qualifier";
  private static final String FAMILY_ATTRIBUTE = "family";
  private static final String SUPER_ATTRIBUTE = "type";
  private static final String CLUSTER_ATTRIBUTE = "cluster";
  private static final String HOST_ATTRIBUTE = "host";


  private String hostName;
  private String clusterName;
  private String keyspaceName;
  
  
  /**
   * List of the super column families.
   */
  private List<String> superFamilies = new ArrayList<String>();

  /**
   * Look up the column family associated to the Avro field.
   */
  private Map<String, String> familyMap = new HashMap<String, String>();
  
  /**
   * Look up the column associated to the Avro field.
   */
  private Map<String, String> columnMap = new HashMap<String, String>();

  /**
   * Look up the column family from its name.
   */
  private Map<String, BasicColumnFamilyDefinition> columnFamilyDefinitions = 
		  new HashMap<String, BasicColumnFamilyDefinition>();

  
  /**
   * Simply gets the Cassandra host name.
   * @return hostName
   */
  public String getHostName() {
    return this.hostName;
  }
  
  /**
   * Simply gets the Cassandra cluster (the machines (nodes) 
   * in a logical Cassandra instance) name.
   * Clusters can contain multiple keyspaces. 
   * @return clusterName
   */
  public String getClusterName() {
    return this.clusterName;
  }

  /**
   * Simply gets the Cassandra namespace for ColumnFamilies, typically one per application
   * @return
   */
  public String getKeyspaceName() {
    return this.keyspaceName;
  }

  /**
   * Primary class for loading Cassandra configuration from the 'MAPPING_FILE'.
   */
  public CassandraMapping(Element keyspace, Element mapping) {
    if (keyspace == null) {
    	LOG.warn("Error locating Cassandra Keyspace element!");
    } else {
    	// LOG.info("Located Cassandra Keyspace: '" + KEYSPACE_ELEMENT + "'");
    }
    this.keyspaceName = keyspace.getAttributeValue(NAME_ATTRIBUTE);
    if (this.keyspaceName == null) {
    	LOG.warn("Error locating Cassandra Keyspace name attribute!");
    } else {
    	// LOG.info("Located Cassandra Keyspace name: '" + NAME_ATTRIBUTE + "'");
    }
    this.clusterName = keyspace.getAttributeValue(CLUSTER_ATTRIBUTE);
    if (this.clusterName == null) {
    	LOG.warn("Error locating Cassandra Keyspace cluster attribute!");
    } else {
    	// LOG.info("Located Cassandra Keyspace cluster: '" + CLUSTER_ATTRIBUTE + "'");
    }
    this.hostName = keyspace.getAttributeValue(HOST_ATTRIBUTE);
    if (this.hostName == null) {
    	LOG.warn("Error locating Cassandra Keyspace host attribute!");
    } else {
    	// LOG.info("Located Cassandra Keyspace host: '" + HOST_ATTRIBUTE + "'");
    }
    
    // load column family definitions
    List<Element> elements = keyspace.getChildren();
    for (Element element: elements) {
      BasicColumnFamilyDefinition cfDef = new BasicColumnFamilyDefinition();
      
      String familyName = element.getAttributeValue(NAME_ATTRIBUTE);
      if (familyName == null) {
      	LOG.warn("Error locating column family name attribute!");
      } else {
      	// LOG.info("Located column family name: '" + NAME_ATTRIBUTE + "'");
      }
      String superAttribute = element.getAttributeValue(SUPER_ATTRIBUTE);
      if (superAttribute != null) {
    	// LOG.info("Located super column family");
        this.superFamilies.add(familyName);
        // LOG.info("Added super column family: '" + familyName + "'");
        cfDef.setColumnType(ColumnType.SUPER);
        cfDef.setSubComparatorType(ComparatorType.BYTESTYPE);
      }
      
      cfDef.setKeyspaceName(this.keyspaceName);
      cfDef.setName(familyName);
      cfDef.setComparatorType(ComparatorType.BYTESTYPE);
      cfDef.setDefaultValidationClass(ComparatorType.BYTESTYPE.getClassName());
      
      this.columnFamilyDefinitions.put(familyName, cfDef);

    }
    
    // load column definitions    
    elements = mapping.getChildren();
    for (Element element: elements) {
      String fieldName = element.getAttributeValue(NAME_ATTRIBUTE);
      String familyName = element.getAttributeValue(FAMILY_ATTRIBUTE);
      String columnName = element.getAttributeValue(COLUMN_ATTRIBUTE);
      BasicColumnFamilyDefinition columnFamilyDefinition = this.columnFamilyDefinitions.get(familyName);
      if (columnFamilyDefinition == null) {
        LOG.warn("Family " + familyName + " was not declared in the keyspace.");
      }
      
      this.familyMap.put(fieldName, familyName);
      this.columnMap.put(fieldName, columnName);
      
    }    
  }

  public String getFamily(String name) {
    return this.familyMap.get(name);
  }

  public String getColumn(String name) {
    return this.columnMap.get(name);
  }

  /**
   * Read family super attribute.
   * @param family the family name
   * @return true is the family is a super column family
   */
  public boolean isSuper(String family) {
    return this.superFamilies.indexOf(family) != -1;
  }

  public List<ColumnFamilyDefinition> getColumnFamilyDefinitions() {
    List<ColumnFamilyDefinition> list = new ArrayList<ColumnFamilyDefinition>();
    for (String key: this.columnFamilyDefinitions.keySet()) {
      ColumnFamilyDefinition columnFamilyDefinition = this.columnFamilyDefinitions.get(key);
      ThriftCfDef thriftCfDef = new ThriftCfDef(columnFamilyDefinition);
      list.add(thriftCfDef);
    }
    
    return list;
  }

}
