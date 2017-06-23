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

import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;

import java.util.Map;

/**
 * This class is used create Cassandra Queries.
 */
class CassandraQueryFactory {

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
      if(isPrimaryKey) {
        stringBuffer.append("  PRIMARY KEY ");
      }
      isCommaNeeded = true;
    }

    stringBuffer.append(")");
    if(Boolean.parseBoolean(mapping.getProperty("compactStorage"))) {
      stringBuffer.append(" WITH COMPACT STORAGE ");
    } else {
      String id = mapping.getProperty("id");
      if (id != null) {
        stringBuffer.append(" WITH ID = '").append(id).append("'");
      }
    }
    return stringBuffer.toString();
  }

}
