/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.ClusterKeyField;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;
import org.apache.gora.cassandra.bean.PartitionKeyField;
import org.apache.gora.persistency.Persistent;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This Class reads the Cassandra Mapping file and create tha Cassandra Mapping object.
 * {@link org.apache.gora.cassandra.store.CassandraMapping}
 */
public class CassandraMappingBuilder<K, T extends Persistent> {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraMappingBuilder.class);

  private CassandraStore dataStore;

  /**
   *
   * @param fileName mapping fileName
   * @return All the Cassandra Mappings in the mapping file
   * @throws Exception
   */
  @SuppressWarnings("all")
  public List<CassandraMapping> readMappingFile(File fileName) throws Exception {
    List<CassandraMapping> mappings = new ArrayList<>();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(fileName);

    List<Element> keyspaces = doc.getRootElement().getChildren("keyspace");
    List<Element> classes = doc.getRootElement().getChildren("class");
    List<Element> keys = doc.getRootElement().getChildren("cassandraKey");
    for (Element classElement : classes) {
      CassandraMapping mapping = new CassandraMapping();
      processClass(mapping, classElement);
      mappings.add(mapping);
    }

    for (CassandraMapping mapping : mappings) {
      for (Element keySpace : keyspaces) {
        String keySpaceName = keySpace.getAttributeValue("name");
        if (keySpaceName.equals(mapping.getProperty("keyspace"))) {
          processKeySpace(mapping, keySpace, keySpaceName);
          break;
        }
      }

      for (Element cassandraKey : keys) {
        String cassandraKeyName = cassandraKey.getAttributeValue("name");
        if (mapping.getProperty("keyClass").equals(cassandraKeyName)) {
          processCassandraKeys(mapping, cassandraKey, cassandraKeyName);
        }
      }
      mapping.finalized();
    }
    return mappings;
  }


  public CassandraMappingBuilder() {
  }

  /**
   * Constructor for builder to create the mapper.
   *
   * @param store Cassandra Store
   */
  CassandraMappingBuilder(final CassandraStore<K, T> store) {
    this.dataStore = store;
  }

  /**
   * In this method we reads the mapping file and creates the Cassandra Mapping.
   *
   * @param filename mapping file name
   * @return @{@link CassandraMapping}
   * @throws IOException
   */
  @SuppressWarnings("all")
  CassandraMapping readMapping(String filename) throws Exception {
    CassandraMapping cassandraMapping = new CassandraMapping();
    Class keyClass = dataStore.getKeyClass();
    Class persistentClass = dataStore.getPersistentClass();
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(getClass().getClassLoader().getResourceAsStream(filename));

    List<Element> keyspaces = doc.getRootElement().getChildren("keyspace");
    List<Element> classes = doc.getRootElement().getChildren("class");
    List<Element> keys = doc.getRootElement().getChildren("cassandraKey");

    boolean classMatched = false;
    for (Element classElement : classes) {
      if (classElement.getAttributeValue("keyClass").equals(
              keyClass.getCanonicalName())
              && classElement.getAttributeValue("name").equals(
              persistentClass.getCanonicalName())) {

        classMatched = true;
        processClass(cassandraMapping, classElement);
        cassandraMapping.setKeyClass(dataStore.getKeyClass());
        cassandraMapping.setPersistentClass(dataStore.getPersistentClass());
        break;
      }
      LOG.warn("Check that 'keyClass' and 'name' parameters in gora-solr-mapping.xml "
              + "match with intended values. A mapping mismatch has been found therefore "
              + "no mapping has been initialized for class mapping at position "
              + " {} in mapping file.", classes.indexOf(classElement));
    }
    if (!classMatched) {
      throw new RuntimeException("Check that 'keyClass' and 'name' parameters in " + filename + " no mapping has been initialized for " + persistentClass + "class mapping");
    }

    String keyspaceName = cassandraMapping.getProperty("keyspace");
    if (keyspaceName != null) {
      KeySpace keyspace;
      for (Element keyspaceElement : keyspaces) {
        if (keyspaceName.equals(keyspaceElement.getAttributeValue("name"))) {
          processKeySpace(cassandraMapping, keyspaceElement, keyspaceName);
          break;
        }
      }
    } else {
      throw new RuntimeException("Couldn't find KeySpace in the Cassandra mapping. Please configure the cassandra mapping correctly.");
    }

    for (Element key : keys) {
      if (keyClass.getName().equals(key.getAttributeValue("name"))) {
        processCassandraKeys(cassandraMapping, key, keyClass.getName());
        break;
      }
    }

    cassandraMapping.finalized();
    return cassandraMapping;
  }

  private void  processClass(CassandraMapping cassandraMapping, Element classElement) {
    String tableName = classElement.getAttributeValue("table");
    cassandraMapping.setCoreName(tableName);

    List classAttributes = classElement.getAttributes();
    for (Object anAttributeList : classAttributes) {
      Attribute attribute = (Attribute) anAttributeList;
      String attributeName = attribute.getName();
      String attributeValue = attribute.getValue();
      cassandraMapping.addProperty(attributeName, attributeValue);
    }

    List<Element> fields = classElement.getChildren("field");

    for (Element field : fields) {
      Field cassandraField = new Field();

      List fieldAttributes = field.getAttributes();
      processAttributes(fieldAttributes, cassandraField);
      cassandraMapping.addCassandraField(cassandraField);
    }
  }


  private void processKeySpace(CassandraMapping cassandraMapping, Element keyspaceElement, String keyspaceName) {
    KeySpace keyspace = new KeySpace();
    List fieldAttributes = keyspaceElement.getAttributes();
    for (Object attributeObject : fieldAttributes) {
      Attribute attribute = (Attribute) attributeObject;
      String attributeName = attribute.getName();
      String attributeValue = attribute.getValue();
      switch (attributeName) {
        case "name":
          keyspace.setName(attributeValue);
          break;
        case "durableWrite":
          keyspace.setDurableWritesEnabled(Boolean.parseBoolean(attributeValue));
          break;
        default:
          LOG.warn("{} attribute is Unsupported or Invalid, in {} Cassandra KeySpace. Please configure the cassandra mapping correctly.", new Object[]{attributeName, keyspaceName});
          break;
      }
    }
    Element placementStrategy = keyspaceElement.getChild("placementStrategy");
    switch (KeySpace.PlacementStrategy.valueOf(placementStrategy.getAttributeValue("name"))) {
      case SimpleStrategy:
        keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.SimpleStrategy);
        keyspace.setReplicationFactor(getReplicationFactor(placementStrategy));
        break;
      case NetworkTopologyStrategy:
        List<Element> dataCenters = placementStrategy.getChildren("datacenter");
        keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.NetworkTopologyStrategy);
        for (Element dataCenter : dataCenters) {
          String dataCenterName = dataCenter.getAttributeValue("name");
          keyspace.addDataCenter(dataCenterName, getReplicationFactor(dataCenter));
        }
        break;
    }
    cassandraMapping.setKeySpace(keyspace);
  }

  private void processCassandraKeys(CassandraMapping cassandraMapping, Element key, String keyName) {
    CassandraKey cassandraKey = new CassandraKey(keyName);
    Element partitionKeys = key.getChild("partitionKey");
    Element clusterKeys = key.getChild("clusterKey");
    List<Element> partitionKeyFields = partitionKeys.getChildren("field");
    List<Element> partitionCompositeKeyFields = partitionKeys.getChildren("compositeKey");
    // process non composite partition keys
    for (Element partitionKeyField : partitionKeyFields) {
      PartitionKeyField fieldKey = new PartitionKeyField();
      List fieldAttributes = partitionKeyField.getAttributes();
      processAttributes(fieldAttributes, fieldKey);
      cassandraKey.addPartitionKeyField(fieldKey);
    }
    // process composite partitions keys
    for (Element partitionCompositeKeyField : partitionCompositeKeyFields) {
      PartitionKeyField compositeFieldKey = new PartitionKeyField();
      compositeFieldKey.setComposite(true);
      List<Element> compositeKeyFields = partitionCompositeKeyField.getChildren("field");
      for (Element partitionKeyField : compositeKeyFields) {
        PartitionKeyField fieldKey = new PartitionKeyField();
        List fieldAttributes = partitionKeyField.getAttributes();
        processAttributes(fieldAttributes, fieldKey);
        compositeFieldKey.addField(fieldKey);
      }
      cassandraKey.addPartitionKeyField(compositeFieldKey);
    }

    //process cluster keys
    List<Element> clusterKeyFields = clusterKeys.getChildren("key");
    for (Element clusterKeyField : clusterKeyFields) {
      ClusterKeyField keyField = new ClusterKeyField();
      List fieldAttributes = clusterKeyField.getAttributes();
      for (Object anAttributeList : fieldAttributes) {
        Attribute attribute = (Attribute) anAttributeList;
        String attributeName = attribute.getName();
        String attributeValue = attribute.getValue();
        switch (attributeName) {
          case "column":
            keyField.setColumnName(attributeValue);
            break;
          case "order":
            keyField.setOrder(ClusterKeyField.Order.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
            break;
          default:
            LOG.warn("{} attribute is Unsupported or Invalid, in {} Cassandra Key. Please configure the cassandra mapping correctly.", new Object[]{attributeName, keyName});
            break;
        }
      }
      cassandraKey.addClusterKeyField(keyField);
    }
    cassandraMapping.setCassandraKey(cassandraKey);
  }

  private void processAttributes(List<Element> attributes, Field fieldKey) {
    for (Object anAttributeList : attributes) {
      Attribute attribute = (Attribute) anAttributeList;
      String attributeName = attribute.getName();
      String attributeValue = attribute.getValue();
      switch (attributeName) {
        case "name":
          fieldKey.setFieldName(attributeValue);
          break;
        case "column":
          fieldKey.setColumnName(attributeValue);
          break;
        case "type":
          // replace UDT into frozen
          if (attributeValue.contains("udt(")) {
            attributeValue = attributeValue.replace("udt(", "frozen(");
          }
          fieldKey.setType(attributeValue.replace("(", "<").replace(")", ">"));
          if (fieldKey.getType().equalsIgnoreCase("udt")) {
            throw new RuntimeException("Invalid udt type, Please enter dataType for udt with a unique name for particular user define data type, like udt(metadata).");
          }
          break;
        default:
          fieldKey.addProperty(attributeName, attributeValue);
          break;
      }
    }
  }

  private static int getReplicationFactor(Element element) {
    String value = element.getAttributeValue("replication_factor");
    if (value == null) {
      return 1;
    } else {
      return Integer.parseInt(value);
    }
  }

}
