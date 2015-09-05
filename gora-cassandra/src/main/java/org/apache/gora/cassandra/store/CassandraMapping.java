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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraMapping {
  
  public static final Logger LOG = LoggerFactory.getLogger(CassandraMapping.class);
  
  private static final String NAME_ATTRIBUTE = "name";
  private static final String COLUMN_ATTRIBUTE = "qualifier";
  private static final String FAMILY_ATTRIBUTE = "family";
  private static final String SUPER_ATTRIBUTE = "type";
  private static final String CLUSTER_ATTRIBUTE = "cluster";
  private static final String HOST_ATTRIBUTE = "host";

  private static final String GCGRACE_SECONDS_ATTRIBUTE = "gc_grace_seconds";
  private static final String COLUMNS_TTL_ATTRIBUTE = "ttl";
  private static final String REPLICATION_FACTOR_ATTRIBUTE = "replication_factor"; 	
  private static final String REPLICATION_STRATEGY_ATTRIBUTE = "placement_strategy";
  
  public static final String DEFAULT_REPLICATION_FACTOR = "1";		 
  public static final String DEFAULT_REPLICATION_STRATEGY = "org.apache.cassandra.locator.SimpleStrategy";
  public static final String DEFAULT_COLUMNS_TTL = "0";
  public static final String DEFAULT_GCGRACE_SECONDS = "0";

  private String hostName;
  private String clusterName;
  private String keyspaceName;
  private String keyspaceStrategy;
  private int 	 keyspaceRF;
  
  
  /**
   * List of the super column families.
   */
  private List<String> superFamilies = new ArrayList<>();

  /**
   * Look up the column family associated to the Avro field.
   */
  private Map<String, String> familyMap = new HashMap<>();
  
  /**
   * Look up the column associated to the Avro field.
   */
  private Map<String, String> columnMap = new HashMap<>();

  /**
   * Helps storing attributes defined for each field.
   */
  private Map<String, String> columnAttrMap = new HashMap<>();
  
  /**
   * Look up the column family from its name.
   */
  private Map<String, BasicColumnFamilyDefinition> columnFamilyDefinitions = 
		  new HashMap<>();

  
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
   * gets the replication strategy
   * @return string class name to be used for strategy
   */
  public String getKeyspaceReplicationStrategy() {
	return this.keyspaceStrategy;
  }
  
  /**
   * gets the replication factor
   * @return int replication factor
   */
  public int getKeyspaceReplicationFactor() {
	return this.keyspaceRF;
  }

  /**
   * Primary class for loading Cassandra configuration from the 'MAPPING_FILE'.
   * It should be noted that should the "qualifier" attribute and its associated
   * value be absent from class field definition, it will automatically be set to 
   * the field name value.
   * 
   */
  @SuppressWarnings("unchecked")
  public CassandraMapping(Element keyspace, Element mapping) {
    if (keyspace == null) {
      LOG.error("Keyspace element should not be null!");
      return;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Located Cassandra Keyspace");
      }
    }
    this.keyspaceName = keyspace.getAttributeValue(NAME_ATTRIBUTE);
    if (this.keyspaceName == null) {
      LOG.error("Error locating Cassandra Keyspace name attribute!");
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Located Cassandra Keyspace name: '" + keyspaceName + "'");
      }
    }
    this.clusterName = keyspace.getAttributeValue(CLUSTER_ATTRIBUTE);
    if (this.clusterName == null) {
    	LOG.error("Error locating Cassandra Keyspace cluster attribute!");
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Located Cassandra Keyspace cluster: '" + clusterName + "'");
      }
    }
    this.hostName = keyspace.getAttributeValue(HOST_ATTRIBUTE);
    if (this.hostName == null) {
    	LOG.error("Error locating Cassandra Keyspace host attribute!");
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Located Cassandra Keyspace host: '" + hostName + "'");
      }  
    }
    
    // setting replication strategy
    this.keyspaceStrategy = keyspace.getAttributeValue( REPLICATION_STRATEGY_ATTRIBUTE );
    if( null == this.keyspaceStrategy ) {
    	this.keyspaceStrategy = DEFAULT_REPLICATION_STRATEGY;
    }
	if( LOG.isDebugEnabled() ) {
		LOG.debug( "setting Keyspace replication strategy to " + this.keyspaceStrategy );
	}

	// setting replication factor
	String tmp = keyspace.getAttributeValue( REPLICATION_FACTOR_ATTRIBUTE );	
	if( null == tmp ) {
		tmp = DEFAULT_REPLICATION_FACTOR;
	}
	this.keyspaceRF = Integer.parseInt( tmp );
	if( LOG.isDebugEnabled() ) {
		LOG.debug( "setting Keyspace replication factor to " + this.keyspaceRF );
	}

    
    // load column family definitions
    List<Element> elements = keyspace.getChildren();
    for (Element element: elements) {
      BasicColumnFamilyDefinition cfDef = new BasicColumnFamilyDefinition();
      
      String familyName = element.getAttributeValue(NAME_ATTRIBUTE);
      if (familyName == null) {
      	LOG.error("Error locating column family name attribute!");
      	continue;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Located column family: '" + familyName + "'" );
        }
      }
      String gcgrace_scs = element.getAttributeValue(GCGRACE_SECONDS_ATTRIBUTE);
      if (gcgrace_scs == null) {
        LOG.warn("Error locating gc_grace_seconds attribute for '" + familyName + "' column family");
        LOG.warn("Using gc_grace_seconds default value which is: " + DEFAULT_GCGRACE_SECONDS 
        		+ " and is viable ONLY FOR A SINGLE NODE CLUSTER");
        LOG.warn("please update the gora-cassandra-mapping.xml file to avoid seeing this warning");
      } else {
        if (LOG.isDebugEnabled()) {
        LOG.debug("Located gc_grace_seconds: '" + gcgrace_scs + "'" );
        }
      }

      String superAttribute = element.getAttributeValue(SUPER_ATTRIBUTE);
      if (superAttribute != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Located super column family");
        }
        this.superFamilies.add(familyName);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Added super column family: '" + familyName + "'");
        }
        cfDef.setColumnType(ColumnType.SUPER);
        cfDef.setSubComparatorType(ComparatorType.BYTESTYPE);
      }

      cfDef.setKeyspaceName(this.keyspaceName);
      cfDef.setName(familyName);
      cfDef.setComparatorType(ComparatorType.BYTESTYPE);
      cfDef.setDefaultValidationClass(ComparatorType.BYTESTYPE.getClassName());

      cfDef.setGcGraceSeconds(Integer.parseInt( gcgrace_scs!=null?gcgrace_scs:DEFAULT_GCGRACE_SECONDS));
      this.columnFamilyDefinitions.put(familyName, cfDef);

    }
    
    // load column definitions    
    elements = mapping.getChildren();
    for (Element element: elements) {
      String fieldName = element.getAttributeValue(NAME_ATTRIBUTE);
      String familyName = element.getAttributeValue(FAMILY_ATTRIBUTE);
      String columnName = element.getAttributeValue(COLUMN_ATTRIBUTE);
      String ttlValue = element.getAttributeValue(COLUMNS_TTL_ATTRIBUTE);
      if (fieldName == null) {
       LOG.error("Field name is not declared.");
        continue;
      }
      if (familyName == null) {
        LOG.error("Family name is not declared for \"" + fieldName + "\" field.");
        continue;
      }
      if (columnName == null) {
        LOG.warn("Column name (qualifier) is not declared for \"" + fieldName + "\" field.");
        columnName = fieldName;
      }
      if (ttlValue == null) {
        LOG.warn("TTL value is not defined for \"" + fieldName + "\" field. \n Using default value: " + DEFAULT_COLUMNS_TTL);
      }

      BasicColumnFamilyDefinition columnFamilyDefinition = this.columnFamilyDefinitions.get(familyName);
      if (columnFamilyDefinition == null) {
        LOG.warn("Family " + familyName + " was not declared in the keyspace.");
      }

      this.familyMap.put(fieldName, familyName);
      this.columnMap.put(fieldName, columnName);
      // TODO we should find a way of storing more values into this map
      this.columnAttrMap.put(columnName, ttlValue!=null?ttlValue:DEFAULT_COLUMNS_TTL);
    }
  }

  /**
   * Add new column to the CassandraMapping using the the below parameters
   * @param pFamilyName the column family name
   * @param pFieldName the Avro field from the Schema
   * @param pColumnName the column name within the column family.
   */
  public void addColumn(String pFamilyName, String pFieldName, String pColumnName){
    this.familyMap.put(pFieldName, pFamilyName);
    this.columnMap.put(pFieldName, pColumnName);
  }

  public String getFamily(String name) {
    return this.familyMap.get(name);
  }

  public String getColumn(String name) {
    return this.columnMap.get(name);
  }

  public Map<String,String> getFamilyMap(){
    return this.familyMap;
  }

  public Map<String, String> getColumnsAttribs(){
    return this.columnAttrMap;
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
    List<ColumnFamilyDefinition> list = new ArrayList<>();
    for (String key: this.columnFamilyDefinitions.keySet()) {
      ColumnFamilyDefinition columnFamilyDefinition = this.columnFamilyDefinitions.get(key);
      ThriftCfDef thriftCfDef = new ThriftCfDef(columnFamilyDefinition);
      list.add(thriftCfDef);
    }
    
    return list;
  }

}
