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
 * Created by madhawa on 6/28/17.
 */
public class CassandraMappingBuilder<K, T extends Persistent> {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraMappingBuilder.class);


  private CassandraStore dataStore;


  /**
   * Constructor for builder to create the mapper.
   *
   * @param store
   */
  public CassandraMappingBuilder(final CassandraStore<K, T> store) {
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
  public CassandraMapping readMapping(String filename) throws IOException {
    CassandraMapping map = new CassandraMapping();
    Class keyClass = dataStore.getKeyClass();
    Class persistentClass = dataStore. getPersistentClass();
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
          map.setCoreName(tableName);

          List classAttributes = classElement.getAttributes();
          for (Object anAttributeList : classAttributes) {
            Attribute attribute = (Attribute) anAttributeList;
            String attributeName = attribute.getName();
            String attributeValue = attribute.getValue();
            map.addProperty(attributeName, attributeValue);
          }

          List<Element> fields = classElement.getChildren("field");

          for (Element field : fields) {
            Field cassandraField = new Field();

            List fieldAttributes = field.getAttributes();
            processAttributes(fieldAttributes, cassandraField);
            map.addCassandraField(cassandraField);
          }
          break;
        }
        LOG.warn("Check that 'keyClass' and 'name' parameters in gora-solr-mapping.xml "
                + "match with intended values. A mapping mismatch has been found therefore "
                + "no mapping has been initialized for class mapping at position "
                + " {} in mapping file.", classes.indexOf(classElement));
      }
      if (!classMatched) {
        LOG.error("Check that 'keyClass' and 'name' parameters in {} no mapping has been initialized for {} class mapping", filename, persistentClass);
      }

      String keyspaceName = map.getProperty("keyspace");
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
                  keyspace.addProperty(attributeName, attributeValue);
                  break;
              }
            }
            Element placementStrategy = keyspaceElement.getChild("placementStrategy");
            switch (KeySpace.PlacementStrategy.valueOf(placementStrategy.getAttributeValue("name"))) {
              case SimpleStrategy:
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.SimpleStrategy);
                keyspace.setReplicationFactor(Integer.parseInt(placementStrategy.getAttributeValue("replication_factor")));
                break;
              case NetworkTopologyStrategy:
                List<Element> dataCenters = placementStrategy.getChildren("datacenter");
                keyspace.setPlacementStrategy(KeySpace.PlacementStrategy.NetworkTopologyStrategy);
                for (Element dataCenter : dataCenters) {
                  String dataCenterName = dataCenter.getAttributeValue("name");
                  Integer dataCenterReplicationFactor = Integer.valueOf(dataCenter.getAttributeValue("replication_factor"));
                  keyspace.addDataCenter(dataCenterName, dataCenterReplicationFactor);
                }
                break;
            }
            map.setKeySpace(keyspace);
            break;
          }

        }

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
          List<Element> clusterKeyFields = clusterKeys.getChildren("field");
          for (Element clusterKeyField : clusterKeyFields) {
            ClusterKeyField keyField = new ClusterKeyField();
            List fieldAttributes = clusterKeyField.getAttributes();
            for (Object anAttributeList : fieldAttributes) {
              Attribute attribute = (Attribute) anAttributeList;
              String attributeName = attribute.getName();
              String attributeValue = attribute.getValue();
              switch (attributeName) {
                case "name":
                  keyField.setFieldName(attributeValue);
                  break;
                case "column":
                  keyField.setColumnName(attributeValue);
                  break;
                case "type":
                  keyField.setType(attributeValue);
                  break;
                case "order":
                  keyField.setOrder(ClusterKeyField.Order.valueOf(attributeValue.toUpperCase(Locale.ENGLISH)));
                  break;
                default:
                  keyField.addProperty(attributeName, attributeValue);
                  break;
              }
            }
            cassandraKey.addClusterKeyField(keyField);
          }
          map.setCassandraKey(cassandraKey);
        }
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return map;
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
          fieldKey.setType(attributeValue);
          break;
        default:
          fieldKey.addProperty(attributeName, attributeValue);
          break;
      }
    }
  }

}
