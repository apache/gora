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
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the Cassandra Mapping.
 */
public class CassandraMapping {

  private static final String PRIMARY_KEY = "primarykey";
  private CassandraKey cassandraKey;
  private Map<String, String> tableProperties;
  private Class keyClass;
  private Class persistentClass;
  private KeySpace keySpace;
  private List<Field> fieldList;
  private Field inlinedDefinedPartitionKey;
  private String coreName;

  /**
   * Constructor of the class
   */
  CassandraMapping() {
    this.fieldList = new ArrayList<>();
    this.tableProperties = new HashMap<>();
  }

  /**
   * This method returns the KeySpace in the mapping file,
   *
   * @return Key space {@link KeySpace}
   */
  public KeySpace getKeySpace() {
    return keySpace;
  }

  /**
   * This method set the KeySpace in the Cassandra mapping.
   *
   * @param keySpace Key space {@link KeySpace}
   */
  void setKeySpace(KeySpace keySpace) {
    this.keySpace = keySpace;
  }

  /**
   * Thi method returns only the fields which belongs only for the Persistent Object.
   *
   * @return List of Fields
   */
  public List<Field> getFieldList() {
    return fieldList;
  }

  /**
   * This method returns the Persistent Object's Field from the mapping, according to the FieldName.
   *
   * @param fieldName Field Name
   * @return Field {@link Field}
   */
  public Field getFieldFromFieldName(String fieldName) {
    for (Field field1 : fieldList) {
      if (field1.getFieldName().equalsIgnoreCase(fieldName)) {
        return field1;
      }
    }
    return null;
  }

  /**
   * This method returns the Persistent Object's Field from the mapping, according to the ColumnName.
   *
   * @param columnName Column Name
   * @return Field {@link Field}
   */
  public Field getFieldFromColumnName(String columnName) {
    for (Field field1 : fieldList) {
      if (field1.getColumnName().equalsIgnoreCase(columnName)) {
        return field1;
      }
    }
    return null;
  }

  /**
   * This method returns the Field Names
   *
   * @return array of Field Names
   */
  public String[] getFieldNames() {
    List<String> fieldNames = new ArrayList<>(fieldList.size());
    for (Field field : fieldList) {
      fieldNames.add(field.getFieldName());
    }
    String[] fieldNameArray = new String[fieldNames.size()];
    return fieldNames.toArray(fieldNameArray);
  }

  /**
   * This method returns partition keys
   *
   * @return partitionKeys
   */
  public String[] getAllFieldsIncludingKeys() {
    List<String> fieldNames = new ArrayList<>(fieldList.size());
    for (Field field : fieldList) {
      fieldNames.add(field.getFieldName());
    }
    if (cassandraKey != null) {
      for (Field field : cassandraKey.getFieldList()) {
        fieldNames.add(field.getFieldName());
      }
    }
    String[] fieldNameArray = new String[fieldNames.size()];
    return fieldNames.toArray(fieldNameArray);
  }

  /**
   * This method return all the fields which involves with partition keys, Including composite Keys
   * @return field Names
   */
  public String[] getAllKeys() {
    List<String> fieldNames = new ArrayList<>();
    Field keyField = getInlinedDefinedPartitionKey();
    if (cassandraKey != null) {
      for (Field field : cassandraKey.getFieldList()) {
        fieldNames.add(field.getFieldName());
      }
    } else {
      fieldNames.add(keyField.getFieldName());
    }
    String[] fieldNameArray = new String[fieldNames.size()];
    return fieldNames.toArray(fieldNameArray);
  }

  public CassandraKey getCassandraKey() {
    return cassandraKey;
  }

  void setCassandraKey(CassandraKey cassandraKey) {
    this.cassandraKey = cassandraKey;
  }

  public String getCoreName() {
    return coreName;
  }

  void setCoreName(String coreName) {
    this.coreName = coreName;
  }

  void addCassandraField(Field field) {
    this.fieldList.add(field);
  }

  void addProperty(String key, String value) {
    this.tableProperties.put(key, value);
  }

  public String getProperty(String key) {
    return this.tableProperties.get(key);
  }

  private Field getDefaultCassandraKey() {
    Field field = new Field();
    field.setFieldName("defaultId");
    field.setColumnName("defaultId");
    field.setType("varchar");
    field.addProperty("primarykey", "true");
    return field;
  }

  public Class getKeyClass() {
    return keyClass;
  }

  public void setKeyClass(Class keyClass) {
    this.keyClass = keyClass;
  }

  public Class getPersistentClass() {
    return persistentClass;
  }

  void setPersistentClass(Class persistentClass) {
    this.persistentClass = persistentClass;
  }

  /**
   * This method return the Inlined defined Partition Key,
   * If there isn't any inlined define partition keys,
   * this method returns default predefined partition key "defaultId".
   *
   * @return Partition Key {@link Field}
   */
  public Field getInlinedDefinedPartitionKey() {
    if (inlinedDefinedPartitionKey != null) {
      return inlinedDefinedPartitionKey;
    } else {
      for (Field field : fieldList) {
        if (Boolean.parseBoolean(field.getProperty(PRIMARY_KEY))) {
          inlinedDefinedPartitionKey = field;
          break;
        }
      }
      if (inlinedDefinedPartitionKey == null) {
        return getDefaultCassandraKey();
      }
      return inlinedDefinedPartitionKey;
    }
  }

  void finalized() {
    Field field = getInlinedDefinedPartitionKey();
    if (!fieldList.contains(field) && cassandraKey == null) {
      fieldList.add(field);
    }
  }
}
