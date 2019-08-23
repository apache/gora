/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.kudu.mapping;

import com.google.inject.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.gora.kudu.store.KuduStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.GoraException;
import org.apache.kudu.Type;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for Mapping definitions of Kudu.
 */
public class KuduMappingBuilder<K, T extends PersistentBase> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Mapping instance being built
   */
  private KuduMapping kuduMapping;

  private final KuduStore<K, T> dataStore;

  /**
   * Constructor for KuduMappingBuilder
   *
   * @param store KuduStore instance
   */
  public KuduMappingBuilder(final KuduStore<K, T> store) {
    this.kuduMapping = new KuduMapping();
    this.dataStore = store;
  }

  /**
   * Returns the Kudu Mapping being built
   *
   * @return Kudu Mapping instance
   */
  public KuduMapping getKuduMapping() {
    return kuduMapping;
  }

  /**
   * Reads Kudu mappings from file
   *
   * @param inputStream Mapping input stream
   * @throws org.apache.gora.util.GoraException Error reading mapping file
   */
  public void readMappingFile(InputStream inputStream) throws GoraException {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      if (inputStream == null) {
        LOG.error("The mapping input stream is null!");
        throw new GoraException("The mapping input stream is null!");
      }
      Document document = saxBuilder.build(inputStream);
      if (document == null) {
        LOG.error("The mapping document is null!");
        throw new GoraException("The mapping document is null!");
      }
      @SuppressWarnings("unchecked")
      List<Element> classes = document.getRootElement().getChildren("class");
      for (Element classElement : classes) {
        if (classElement.getAttributeValue("keyClass").equals(
            dataStore.getKeyClass().getCanonicalName())
            && classElement.getAttributeValue("name").equals(
                dataStore.getPersistentClass().getCanonicalName())) {
          final String tableNameFromMapping = classElement.getAttributeValue("table");
          final String tablenumReplicasMapping = classElement.getAttributeValue("numReplicas");
          String tableName = dataStore.getSchemaName(tableNameFromMapping, dataStore.getPersistentClass());
          kuduMapping.setTableName(tableName);
          kuduMapping.setNumReplicas(Integer.parseInt(tablenumReplicasMapping));
          @SuppressWarnings("unchecked")
          List<Element> tables = document.getRootElement().getChildren("table");
          for (Element tableElement : tables) {
            if (tableElement.getAttributeValue("name").equals(tableNameFromMapping)) {
              @SuppressWarnings("unchecked")
              List<Element> pkColumns = tableElement.getChildren("primaryKey");
              List<Column> pkFields = new ArrayList<>();
              for (Element aPrimaryKey : pkColumns) {
                String columnName = aPrimaryKey.getAttributeValue("column");
                String columnType = aPrimaryKey.getAttributeValue("type");
                Type aDataType = Type.valueOf(columnType);
                if (aDataType == Type.DECIMAL) {
                  int precision = Integer.parseInt(aPrimaryKey.getAttributeValue("precision"));
                  int scale = Integer.parseInt(aPrimaryKey.getAttributeValue("scale"));
                  pkFields.add(new Column(columnName, new Column.FieldType(precision, scale)));
                } else {
                  pkFields.add(new Column(columnName, new Column.FieldType(aDataType)));
                }
              }
              kuduMapping.setPrimaryKey(pkFields);
              Element hashPartition = tableElement.getChild("hashPartition");
              if (hashPartition != null) {
                int numBuckets = Integer.parseInt(hashPartition.getAttributeValue("numBuckets"));
                kuduMapping.setHashBuckets(numBuckets);
              }
              List<Map.Entry<String, String>> ranges = new ArrayList<>();
              @SuppressWarnings("unchecked")
              List<Element> rangePartitions = tableElement.getChildren("rangePartition");
              for (Element rangePartition : rangePartitions) {
                String lower = rangePartition.getAttributeValue("lower");
                String upper = rangePartition.getAttributeValue("upper");
                ranges.add(new AbstractMap.SimpleEntry<>(lower, upper));
              }
              kuduMapping.setRangePartitions(ranges);
            }
          }
          @SuppressWarnings("unchecked")
          List<Element> fields = classElement.getChildren("field");
          Map<String, Column> fieldsMappings = new HashMap<>();
          for (Element field : fields) {
            String fieldName = field.getAttributeValue("name");
            String columnName = field.getAttributeValue("column");
            String columnType = field.getAttributeValue("type");
            Type aDataType = Type.valueOf(columnType);
            if (aDataType == Type.DECIMAL) {
              int precision = Integer.parseInt(field.getAttributeValue("precision"));
              int scale = Integer.parseInt(field.getAttributeValue("scale"));
              fieldsMappings.put(fieldName, new Column(columnName, new Column.FieldType(precision, scale)));
            } else {
              fieldsMappings.put(fieldName, new Column(columnName, new Column.FieldType(aDataType)));
            }
          }
          kuduMapping.setFields(fieldsMappings);
          break;
        }
      }
    } catch (IOException | JDOMException | ConfigurationException e) {
      throw new GoraException(e);
    }
    LOG.info("Gora Kudu mapping file was read successfully.");
  }
}
