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
  private Map<String, BasicColumnFamilyDefinition> columnFamilyDefinitions = new HashMap<String, BasicColumnFamilyDefinition>();

  public String getHostName() {
    return this.hostName;
  }

  public String getClusterName() {
    return this.clusterName;
  }

  public String getKeyspaceName() {
    return this.keyspaceName;
  }


  @SuppressWarnings("unchecked")
  public void loadConfiguration() throws JDOMException, IOException {
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document = saxBuilder.build(getClass().getClassLoader().getResourceAsStream(MAPPING_FILE));
    Element root = document.getRootElement();
    
    Element keyspace = root.getChild(KEYSPACE_ELEMENT);
    this.keyspaceName = keyspace.getAttributeValue(NAME_ATTRIBUTE);
    this.clusterName = keyspace.getAttributeValue(CLUSTER_ATTRIBUTE);
    this.hostName = keyspace.getAttributeValue(HOST_ATTRIBUTE);
    
    // load column family definitions
    List<Element> elements = keyspace.getChildren();
    for (Element element: elements) {
      BasicColumnFamilyDefinition cfDef = new BasicColumnFamilyDefinition();
      
      String familyName = element.getAttributeValue(NAME_ATTRIBUTE);
      
      String superAttribute = element.getAttributeValue(SUPER_ATTRIBUTE);
      if (superAttribute != null) {
        this.superFamilies.add(familyName);
        cfDef.setColumnType(ColumnType.SUPER);
        cfDef.setSubComparatorType(ComparatorType.UTF8TYPE);
      }
      
      cfDef.setKeyspaceName(this.keyspaceName);
      cfDef.setName(familyName);
      cfDef.setComparatorType(ComparatorType.UTF8TYPE);
      cfDef.setDefaultValidationClass(ComparatorType.UTF8TYPE.getClassName());
      
      this.columnFamilyDefinitions.put(familyName, cfDef);

    }
    
    // load column definitions    
    Element mapping = root.getChild(MAPPING_ELEMENT);
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
