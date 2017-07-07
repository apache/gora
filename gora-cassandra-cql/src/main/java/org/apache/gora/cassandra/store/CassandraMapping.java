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

  private CassandraKey cassandraKey;

  private Map<String, String> tableProperties;

  private KeySpace keySpace;

  private List<Field> fieldList;

  private String coreName;

  public KeySpace getKeySpace() {
    return keySpace;
  }

  public void setKeySpace(KeySpace keySpace) {
    this.keySpace = keySpace;
  }

  public List<Field> getFieldList() {
    return fieldList;
  }

  public Field getField(String field) {
    for (Field field1 : fieldList) {
      if (field1.getFieldName().equals(field)) {
        return field1;
      }
    }
    return null;
  }

  public String[] getFieldNames() {
    List<String> fieldNames = new ArrayList<>(fieldList.size());
    for (Field field : fieldList) {
      fieldNames.add(field.getFieldName());
    }
    String[] fieldNameArray = new String[fieldNames.size()];
    return fieldNames.toArray(fieldNameArray);
  }

  public CassandraKey getCassandraKey() {
    return cassandraKey;
  }

  public void setCassandraKey(CassandraKey cassandraKey) {
    this.cassandraKey = cassandraKey;
  }

  public CassandraMapping() {
    this.fieldList = new ArrayList<>();
    this.tableProperties = new HashMap<>();
  }

  public void setCoreName(String coreName) {
    this.coreName = coreName;
  }

  public String getCoreName() {
    return coreName;
  }

  public void addCassandraField(Field field) {
    this.fieldList.add(field);
  }

  public void addProperty(String key, String value) {
        this.tableProperties.put(key,value);
  }

  public String getProperty(String key) {
    return this.tableProperties.get(key);
  }
}
