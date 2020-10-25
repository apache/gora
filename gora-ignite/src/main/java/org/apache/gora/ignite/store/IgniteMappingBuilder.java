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
package org.apache.gora.ignite.store;

import com.google.inject.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.gora.persistency.impl.PersistentBase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for Mapping definitions of Ignite.
 */
public class IgniteMappingBuilder<K, T extends PersistentBase> {

  private static final Logger LOG = LoggerFactory.getLogger(IgniteMappingBuilder.class);
  /**
   * Mapping instance being built
   */
  private IgniteMapping igniteMapping;

  private final IgniteStore<K, T> dataStore;

  /**
   * Constructor for IgniteMappingBuilder
   *
   * @param store IgniteStore instance
   */
  public IgniteMappingBuilder(final IgniteStore<K, T> store) {
    this.igniteMapping = new IgniteMapping();
    this.dataStore = store;
  }

  /**
   * Returns the Ignite Mapping being built
   *
   * @return Ignite Mapping instance
   */
  public IgniteMapping getIgniteMapping() {
    return igniteMapping;
  }

  /**
   * Sets the Ignite Mapping
   *
   * @param igniteMapping Ignite Mapping instance
   */
  public void setIgniteMapping(IgniteMapping igniteMapping) {
    this.igniteMapping = igniteMapping;
  }

  /**
   * Reads Ignite mappings from file
   *
   * @param inputStream Input stream of the mapping
   */
  public void readMappingFile(InputStream inputStream) {
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      if (inputStream == null) {
        LOG.error("The mapping input stream is null!");
        throw new IOException("The mapping input stream is null!");
      }
      Document document = saxBuilder.build(inputStream);
      if (document == null) {
        LOG.error("The mapping document is null!");
        throw new IOException("The mapping document is null!");
      }
      @SuppressWarnings("unchecked")
      List<Element> classes = document.getRootElement().getChildren("class");
      for (Element classElement : classes) {
        if (classElement.getAttributeValue("keyClass").equals(
            dataStore.getKeyClass().getCanonicalName())
            && classElement.getAttributeValue("name").equals(
                dataStore.getPersistentClass().getCanonicalName())) {
          final String tableNameFromMapping = classElement.getAttributeValue("table");
          String tableName = dataStore.getSchemaName(tableNameFromMapping, dataStore.getPersistentClass());
          igniteMapping.setTableName(tableName);
          @SuppressWarnings("unchecked")
          List<Element> prColumns = classElement.getChildren("primarykey");
          List<Column> prFields = new ArrayList<>();
          for (Element aPrimaryKey : prColumns) {
            String name = aPrimaryKey.getAttributeValue("column");
            String type = aPrimaryKey.getAttributeValue("type");
            prFields.add(new Column(name, Column.FieldType.valueOf(type)));
          }
          igniteMapping.setPrimaryKey(prFields);
          @SuppressWarnings("unchecked")
          List<Element> fields = classElement.getChildren("field");
          Map<String, Column> mp = new HashMap<>();
          for (Element field : fields) {
            String fieldName = field.getAttributeValue("name");
            String columnName = field.getAttributeValue("column");
            String columnType = field.getAttributeValue("type");
            mp.put(fieldName, new Column(columnName, Column.FieldType.valueOf(columnType)));
          }
          igniteMapping.setFields(mp);
          break;
        }
      }
    } catch (IOException | JDOMException | ConfigurationException e) {
      throw new RuntimeException(e);
    }
    LOG.info("Gora Ignite mapping file was read successfully.");
  }
}
