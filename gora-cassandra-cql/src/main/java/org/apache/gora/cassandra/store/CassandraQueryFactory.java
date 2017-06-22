package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;

import java.util.Map;

class CassandraQueryFactory {

  static String getCreateKeySpaceQuery(CassandraMapping mapping) {
    KeySpace keySpace = mapping.getKeySpace();
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("CREATE KEYSPACE ").append(keySpace.getName()).append(" WITH REPLICATION = { 'class' : ");
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

    if(keySpace.isDurableWritesEnabled()) {
      stringBuffer.append(" AND DURABLE_WRITES = ").append(keySpace.isDurableWritesEnabled());
    }
    return stringBuffer.toString();
  }

  static String getCreateTableQuery(CassandraMapping mapping) {
    StringBuilder stringBuffer = new StringBuilder();
stringBuffer.append("CREATE TABLE ").append(mapping.getCoreName()).append(" (");
    boolean isCommaNeeded = false;
    for(Field field : mapping.getFieldList()) {
      if(isCommaNeeded) {
        stringBuffer.append(", ");
      }
      stringBuffer.append(field.getColumnName()).append(" ").append(field.getType());
      boolean isStaticColumn = Boolean.parseBoolean(field.getProperty("static"));
      if( isStaticColumn) {
        stringBuffer.append(" STATIC");
      }
      isCommaNeeded =true;
    }

    return stringBuffer.toString();
  }

}
