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

package org.apache.gora.cassandra.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * This Class represents the Cassandra Key.
 */
public class CassandraKey {

  private String name;

  private List<ClusterKeyField> clusterKeyFields;

  private List<PartitionKeyField> partitionKeyFields;

  private List<Field> fieldList;

  public CassandraKey(String name) {
    this.name = name;
    this.fieldList = new ArrayList<>();
    this.partitionKeyFields = new ArrayList<>();
  }

  public String getName() {
    return this.name;
  }

  public List<ClusterKeyField> getClusterKeyFields() {
    return this.clusterKeyFields;
  }

  public List<PartitionKeyField> getPartitionKeyFields() {
    return this.partitionKeyFields;
  }

  public void addPartitionKeyField(PartitionKeyField partitionKeyField) {
    this.partitionKeyFields.add(partitionKeyField);
    if (partitionKeyField.isComposite()) {
      this.fieldList.addAll(partitionKeyField.getFields());
    } else {
      this.fieldList.add(partitionKeyField);
    }
  }

  public void addClusterKeyField(ClusterKeyField clusterKeyField) {
    if (this.clusterKeyFields == null) {
      this.clusterKeyFields = new ArrayList<>();
    }
    this.clusterKeyFields.add(clusterKeyField);
  }

  public String[] getFieldNames() {
    List<String> fieldNames = new ArrayList<>(fieldList.size());
    for (Field field : fieldList) {
      fieldNames.add(field.getFieldName());
    }
    String[] fieldNameArray = new String[fieldNames.size()];
    return fieldNames.toArray(fieldNameArray);
  }

  public Field getFieldFromFieldName(String fieldName) {
    for (Field field1 : fieldList) {
      if (field1.getFieldName().equalsIgnoreCase(fieldName)) {
        return field1;
      }
    }
    return null;
  }

  public List<Field> getFieldList() {
    return this.fieldList;
  }

}
