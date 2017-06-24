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

import java.util.List;
import java.util.Map;

/**
 * This class is used create Cassandra Queries.
 */
class CassandraQueryFactory {

  /**
   * This method returns the CQL query to create key space.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/create_keyspace_r.html
   *
   * @param mapping Cassandra Mapping
   * @return CQL Query
   */
  static String getCreateKeySpaceQuery(CassandraMapping mapping) {
    KeySpace keySpace = mapping.getKeySpace();
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("CREATE KEYSPACE IF NOT EXISTS ").append(keySpace.getName()).append(" WITH REPLICATION = { 'class' : ");
    KeySpace.PlacementStrategy placementStrategy = keySpace.getPlacementStrategy();
    stringBuffer.append("'").append(placementStrategy).append("'").append(", ").append("'");
    switch (placementStrategy) {
      case SimpleStrategy:
        stringBuffer.append("replication_factor").append("'").append(" : ").append(keySpace.getReplicationFactor()).append(" }");
        break;
      case NetworkTopologyStrategy:
        boolean isCommaNeeded = false;
        for (Map.Entry<String, Integer> entry : keySpace.getDataCenters().entrySet()) {
          if (isCommaNeeded) {
            stringBuffer.append(", '");
          }
          stringBuffer.append(entry.getKey()).append("'").append(" : ").append(entry.getValue());
          isCommaNeeded = true;
        }
        stringBuffer.append(" }");
        break;
    }

    if (keySpace.isDurableWritesEnabled()) {
      stringBuffer.append(" AND DURABLE_WRITES = ").append(keySpace.isDurableWritesEnabled());
    }
    return stringBuffer.toString();
  }

  static String getCreateTableQuery(CassandraMapping mapping) {
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("CREATE TABLE IF NOT EXISTS ").append(mapping.getKeySpace().getName()).append(".").append(mapping.getCoreName()).append(" (");
    boolean isCommaNeeded = false;
    CassandraKey cassandraKey = mapping.getCassandraKey();
    // appending Cassandra key columns into db schema
    if (cassandraKey != null) {
      for (PartitionKeyField partitionKeyField : cassandraKey.getPartitionKeyFields()) {
        if (partitionKeyField.isComposite()) {
          for (Field compositeField : partitionKeyField.getFields()) {
            stringBuffer = processFields(stringBuffer, compositeField, isCommaNeeded);
          }

        } else {
          stringBuffer = processFields(stringBuffer, partitionKeyField, isCommaNeeded);
        }
        isCommaNeeded = true;
      }
      for (ClusterKeyField clusterKeyField : cassandraKey.getClusterKeyFields()) {
        stringBuffer = processFields(stringBuffer, clusterKeyField, isCommaNeeded);
      }
    }
    // appending Other columns
    for (Field field : mapping.getFieldList()) {
      if (isCommaNeeded) {
        stringBuffer.append(", ");
      }
      stringBuffer.append(field.getColumnName()).append(" ").append(field.getType());
      boolean isStaticColumn = Boolean.parseBoolean(field.getProperty("static"));
      boolean isPrimaryKey = Boolean.parseBoolean(field.getProperty("primarykey"));
      if (isStaticColumn) {
        stringBuffer.append(" STATIC");
      }
      if (isPrimaryKey) {
        stringBuffer.append("  PRIMARY KEY ");
      }
      isCommaNeeded = true;
    }

    if (cassandraKey != null) {
      List<PartitionKeyField> pkey = cassandraKey.getPartitionKeyFields();
      if (pkey != null) {
        stringBuffer.append(", PRIMARY KEY (");
        boolean isCommaNeededToApply = false;
        for (PartitionKeyField keyField : pkey) {
          if (isCommaNeededToApply) {
            stringBuffer.append(",");
          }
          if (keyField.isComposite()) {
            stringBuffer.append("(");
            boolean isCommaNeededHere = false;
            for (Field field : keyField.getFields()) {
              if (isCommaNeededHere) {
                stringBuffer.append(", ");
              }
              stringBuffer.append(field.getColumnName());
              isCommaNeededHere = true;
            }
            stringBuffer.append(")");
          } else {
            stringBuffer.append(keyField.getColumnName());
          }
          isCommaNeededToApply = true;
        }
        stringBuffer.append(")");
      }
    }

    stringBuffer.append(")");
    boolean isWithNeeded = true;
    if (Boolean.parseBoolean(mapping.getProperty("compactStorage"))) {
      stringBuffer.append(" WITH COMPACT STORAGE ");
      isWithNeeded = false;
    }

    String id = mapping.getProperty("id");
    if (id != null) {
      if (isWithNeeded) {
        stringBuffer.append(" WITH ");
      } else {
        stringBuffer.append(" AND ");
      }
      stringBuffer.append("ID = '").append(id).append("'");
      isWithNeeded = false;
    }
    if (cassandraKey != null) {
      List<ClusterKeyField> clusterKeyFields = cassandraKey.getClusterKeyFields();
      if (clusterKeyFields != null) {
        if (isWithNeeded) {
          stringBuffer.append(" WITH ");
        } else {
          stringBuffer.append(" AND ");
        }
        stringBuffer.append(" CLUSTERING ORDER BY (");
        boolean isCommaNeededToApply = false;
        for (ClusterKeyField keyField : clusterKeyFields) {
          if (isCommaNeededToApply) {
            stringBuffer.append(", ");
          }
          stringBuffer.append(keyField.getColumnName()).append(" ");
          if (keyField.getOrder() != null) {
            stringBuffer.append(keyField.getOrder());
          }
          isCommaNeededToApply = true;
        }
        stringBuffer.append(")");
      }
    }
    return stringBuffer.toString();
  }

  private static StringBuilder processFields(StringBuilder stringBuilder, Field field, boolean isCommaNeeded) {
    if (isCommaNeeded) {
      stringBuilder.append(", ");
    }
    stringBuilder.append(field.getColumnName()).append(" ").append(field.getType());
    boolean isStaticColumn = Boolean.parseBoolean(field.getProperty("static"));
    if (isStaticColumn) {
      stringBuilder.append(" STATIC");
    }
    return stringBuilder;
  }

  /**
   * This method returns the CQL query to drop table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/drop_table_r.html
   *
   * @param mapping Cassandra Mapping
   * @return CQL query
   */
  static String getDropTableQuery(CassandraMapping mapping) {
    return "DROP TABLE IF EXISTS " + mapping.getKeySpace().getName() + "." + mapping.getCoreName();
  }

  /**
   * This method returns the CQL query to drop key space.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/drop_keyspace_r.html
   *
   * @param mapping Cassandra Mapping
   * @return CQL query
   */
  static String getDropKeySpaceQuery(CassandraMapping mapping) {
    return "DROP KEYSPACE IF EXISTS " + mapping.getKeySpace().getName();
  }

  /**
   * This method returns the CQL query to truncate (removes all the data) in the table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/truncate_r.html
   *
   * @param mapping Cassandra Mapping
   * @return CQL query
   */
  static String getTruncateTableQuery(CassandraMapping mapping) {
    return "TRUNCATE TABLE " + mapping.getKeySpace().getName() + "." + mapping.getCoreName();
  }
}
