/*
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

package org.apache.gora.hive.store;


import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.gora.util.GoraException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is for reading a given mapping file and building the hive mappings
 *
 * @param <K> Key class
 * @param <T> Persistent class
 */
public class HiveMappingBuilder<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger((MethodHandles.lookup().lookupClass()));

  //Tag names
  private static final String CLASS_TAG = "class";
  private static final String FIELD_TAG = "field";

  //Attribute names
  private static final String KEYCLASS_ATTRIBUTE = "keyClass";
  private static final String TABLE_ATTRIBUTE = "table";
  private static final String NAME_ATTRIBUTE = "name";

  private HiveStore<?, ?> dataStore;

  public HiveMappingBuilder(HiveStore<K, ?> dataStore) {
    this.dataStore = dataStore;
  }

  /**
   * Reading the given file to build the Hive Mappings.
   *
   * @param filename path of the mapping file
   * @return {@link org.apache.gora.hive.store.HiveMapping} hive mappings
   * @throws GoraException exception in reading mappings
   */
  @SuppressWarnings("unchecked")
  public HiveMapping readMappingFile(String filename) throws GoraException {
    HiveMapping mapping = new HiveMapping();
    final Class<T> persistentClass = (Class<T>) dataStore.getPersistentClass();
    final Class<K> keyClass = (Class<K>) dataStore.getKeyClass();
    final SAXBuilder saxBuilder = new SAXBuilder();
    final InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
    if (is == null) {
      throw new GoraException("hive mapping file:" + filename + " could not be loaded");
    }
    final Element root;
    try {
      root = saxBuilder.build(is).getRootElement();
    } catch (JDOMException | IOException e) {
      throw new GoraException("Reading hive mapping file : " + filename + " failed", e);
    }
    List<Element> classElements = root.getChildren(CLASS_TAG);
    if (classElements == null || classElements.isEmpty()) {
      throw new GoraException(
          "Could not find any class definition in the mapping file:" + filename);
    }
    parseClassElements(mapping, persistentClass, keyClass, classElements);
    if (!validateSchema(mapping)) {
      throw new GoraException("Schema validation failed for the mapping file: " + filename);
    }
    return mapping;
  }

  /**
   * Validate hive schema mappings
   *
   * @param mapping Hive mappings
   * @return true if a valid schema. otherwise false
   */
  private boolean validateSchema(HiveMapping mapping) {
    List<String> fields = mapping.getFields();
    if (fields == null || fields.isEmpty()) {
      LOG.error("table should have at least single column");
      return false;
    } else {
      for (String fieldName : fields) {
        if (HiveMapping.DEFAULT_KEY_NAME.equals(fieldName)) {
          LOG.error("\'{}\' is a reserved keyword and cannot be used as a field name",
              HiveMapping.DEFAULT_KEY_NAME);
          return false;
        }
      }
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  private void parseClassElements(HiveMapping hiveMapping, Class<T> persistentClass,
      Class<K> keyClass, List<Element> classElements) {
    for (Element classElement : classElements) {
      String persistentClassName = classElement.getAttributeValue(NAME_ATTRIBUTE);
      String keyClassName = classElement.getAttributeValue(KEYCLASS_ATTRIBUTE);
      //Find a class which matches persistent class name and key class name
      if (persistentClassName != null && keyClassName != null && persistentClassName
          .equals(persistentClass.getName()) && keyClassName
          .equals(keyClass.getName())) {
        hiveMapping.setTableName(dataStore
            .getSchemaName(classElement.getAttributeValue(TABLE_ATTRIBUTE),
                dataStore.getPersistentClass()));
        List<Element> fieldElments = classElement.getChildren(FIELD_TAG);
        parseFieldElements(hiveMapping, fieldElments);
        break;
      }
    }
  }

  private void parseFieldElements(HiveMapping hiveMapping, List<Element> fieldElments) {
    if (fieldElments != null) {
      List<String> fieldNames = new ArrayList<>();
      Map<String, String> columnFieldMap = new HashMap<>();
      for (Element field : fieldElments) {
        String fieldName = field.getAttributeValue(HiveMappingBuilder.NAME_ATTRIBUTE);
        if (fieldName == null || fieldName.isEmpty()) {
          LOG.warn("Field without a name attribute is found and that field will be ignored");
        } else {
          fieldNames.add(fieldName);
          columnFieldMap.put(fieldName.toLowerCase(Locale.getDefault()), fieldName);
        }
      }
      hiveMapping.setFields(fieldNames);
      hiveMapping.setColumnFieldMap(columnFieldMap);
    }
  }
}