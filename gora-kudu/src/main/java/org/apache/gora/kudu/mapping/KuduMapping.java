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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping definitions for Kudu.
 */
public class KuduMapping {

  private String tableName;
  private int numReplicas;
  private Map<String, Column> fields;
  private List<Column> primaryKey;
  private int hashBuckets;
  private List<Map.Entry<String, String>> rangePartitions;

  /**
   * Empty constructor for the KuduMapping class
   */
  public KuduMapping() {
    fields = new HashMap<>();
  }

  /**
   * Returns the name of kudu table linked to the mapping.
   *
   * @return Table's name.
   */
  public String getTableName() {
    return tableName;
  }

  /**
   * Sets the table name of the kudu mapping
   *
   * @param tableName Table's name
   */
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  /**
   * Returns a map with all field-column mappings
   *
   * @return Map containing mapped fields
   */
  public Map<String, Column> getFields() {
    return fields;
  }

  /**
   * Sets field-column mappings
   *
   * @param fields Map containing mapped fields
   */
  public void setFields(Map<String, Column> fields) {
    this.fields = fields;
  }

  /**
   * Returns the primary key's list of columns
   *
   * @return List with columns
   */
  public List<Column> getPrimaryKey() {
    return primaryKey;
  }

  /**
   * Sets the primary key's columns
   *
   * @param primaryKey List with columns
   */
  public void setPrimaryKey(List<Column> primaryKey) {
    this.primaryKey = primaryKey;
  }

  /**
   * Returns the number of hash buckets for the table
   *
   * @return number of buckets
   */
  public int getHashBuckets() {
    return hashBuckets;
  }

  /**
   * Sets the number of hash buckets
   *
   * @param hashBuckets number of hash buckets
   */
  public void setHashBuckets(int hashBuckets) {
    this.hashBuckets = hashBuckets;
  }

  /**
   * Return a list of the range partitions of the table
   *
   * @return list of range partitions (Pair lower and upper bounds)
   */
  public List<Map.Entry<String, String>> getRangePartitions() {
    return rangePartitions;
  }

  /**
   * Sets the list of the range partitions of the table
   *
   * @param rangePartitions list of range partitions (Pair lower and upper
   * bounds)
   */
  public void setRangePartitions(List<Map.Entry<String, String>> rangePartitions) {
    this.rangePartitions = rangePartitions;
  }

  /**
   * Returns the number of replicas for the table
   *
   * @return number of replicas
   */
  public int getNumReplicas() {
    return numReplicas;
  }

  /**
   * Sets the number of replicas for the table
   *
   * @param numReplicas number of replicas
   */
  public void setNumReplicas(int numReplicas) {
    this.numReplicas = numReplicas;
  }

}
