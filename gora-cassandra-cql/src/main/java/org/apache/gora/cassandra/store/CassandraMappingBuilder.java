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

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * This Class reads the Cassandra Mapping file and create tha Cassandra Mapping object.
 * {@link org.apache.gora.cassandra.store.CassandraMapping}
 */
class CassandraMappingBuilder<K, T extends Persistent> {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraMappingBuilder.class);

  private CassandraStore dataStore;


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
  CassandraMapping readMapping(String filename) throws IOException {
    CassandraMapping cassandraMapping = new CassandraMapping();
    Class keyClass = dataStore.getKeyClass();
    Class persistentClass = dataStore.getPersistentClass();
    try {
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
          String tableName = classElement.getAttributeValue("table");
          cassandraMapping.setCoreName(tableName);
          cassandraMapping.setKeyClass(dataStore.getKeyClass());
          cassandraMapping.setPersistentClass(dataStore.getPersistentClass());

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
            keyspace = new KeySpace();
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
            break;
          }

        }

      } else {
        throw new RuntimeException("Couldn't find KeySpace in the Cassandra mapping. Please configure the cassandra mapping correctly.");
      }

      for (Element key : keys) {
        if (keyClass.getName().equals(key.getAttributeValue("name"))) {
          CassandraKey cassandraKey = new CassandraKey(keyClass.getName());
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
                  LOG.warn("{} attribute is Unsupported or Invalid, in {} Cassandra Key. Please configure the cassandra mapping correctly.", new Object[]{attributeName, keyClass});
                  break;
              }
            }
            cassandraKey.addClusterKeyField(keyField);
          }
          cassandraMapping.setCassandraKey(cassandraKey);
        }
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return cassandraMapping;
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
          fieldKey.setType(attributeValue.replace("(", "<").replace(")", ">"));
          break;
        default:
          fieldKey.addProperty(attributeName, attributeValue);
          break;
      }
    }
  }

  private int getReplicationFactor(Element element) {
    String value  = element.getAttributeValue("replication_factor");
    if(value == null) {
      return 1;
    } else {
      return Integer.parseInt(value);
    }
  }

}
