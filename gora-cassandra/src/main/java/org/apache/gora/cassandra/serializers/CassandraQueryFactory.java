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
package org.apache.gora.cassandra.serializers;

import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.mapping.annotations.UDT;
import org.apache.avro.Schema;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.ClusterKeyField;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;
import org.apache.gora.cassandra.bean.PartitionKeyField;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is used create Cassandra Queries.
 */
class CassandraQueryFactory {

  private static final Logger LOG = LoggerFactory.getLogger(CassandraQueryFactory.class);

  /**
   * This method returns the CQL query to create key space.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/create_keyspace_r.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
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

  /**
   * This method returns the CQL query to table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/create_table_r.html
   * <p>
   * Trick : To have a consistency of the order of the columns, first we append partition keys, second cluster keys and finally other columns.
   * It's very much needed to follow the same order in other CRUD operations as well.
   *
   * @param mapping Cassandra mapping {@link CassandraMapping}
   * @return CQL Query
   */
  static String getCreateTableQuery(CassandraMapping mapping) {
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("CREATE TABLE IF NOT EXISTS ").append(mapping.getKeySpace().getName()).append(".").append(mapping.getCoreName()).append(" (");
    CassandraKey cassandraKey = mapping.getCassandraKey();
    // appending Cassandra Persistent columns into db schema
    processFieldsForCreateTableQuery(mapping.getFieldList(), false, stringBuffer);

    if (cassandraKey != null) {
      processFieldsForCreateTableQuery(cassandraKey.getFieldList(), true, stringBuffer);
      List<PartitionKeyField> partitionKeys = cassandraKey.getPartitionKeyFields();
      if (partitionKeys != null) {
        stringBuffer.append(", PRIMARY KEY (");
        boolean isCommaNeededToApply = false;
        for (PartitionKeyField keyField : partitionKeys) {
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

  private static void processFieldsForCreateTableQuery(List<Field> fields, boolean isCommaNeeded, StringBuilder stringBuilder) {
    for (Field field : fields) {
      if (isCommaNeeded) {
        stringBuilder.append(", ");
      }
      stringBuilder.append(field.getColumnName()).append(" ").append(field.getType());
      boolean isStaticColumn = Boolean.parseBoolean(field.getProperty("static"));
      boolean isPrimaryKey = Boolean.parseBoolean(field.getProperty("primarykey"));
      if (isStaticColumn) {
        stringBuilder.append(" STATIC");
      }
      if (isPrimaryKey) {
        stringBuilder.append("  PRIMARY KEY ");
      }
      isCommaNeeded = true;
    }
  }

  /**
   * This method returns the CQL query to drop table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/drop_table_r.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @return CQL query
   */
  static String getDropTableQuery(CassandraMapping mapping) {
    return "DROP TABLE IF EXISTS " + mapping.getKeySpace().getName() + "." + mapping.getCoreName();
  }

  /**
   * This method returns the CQL query to drop key space.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/drop_keyspace_r.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @return CQL query
   */
  static String getDropKeySpaceQuery(CassandraMapping mapping) {
    return "DROP KEYSPACE IF EXISTS " + mapping.getKeySpace().getName();
  }

  /**
   * This method returns the CQL query to truncate (removes all the data) in the table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/truncate_r.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @return CQL query
   */
  static String getTruncateTableQuery(CassandraMapping mapping) {
    return QueryBuilder.truncate(mapping.getKeySpace().getName(), mapping.getCoreName()).getQueryString();
  }

  /**
   * This method return the CQL query to insert data in to the table.
   * refer : http://docs.datastax.com/en/cql/3.1/cql/cql_reference/insert_r.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @param fields  available fields
   * @return CQL Query
   */
  static String getInsertDataQuery(CassandraMapping mapping, List<String> fields) {
    String[] columnNames = getColumnNames(mapping, fields);
    String[] objects = new String[fields.size()];
    Arrays.fill(objects, "?");
    return QueryBuilder.insertInto(mapping.getKeySpace().getName(), mapping.getCoreName()).values(columnNames, objects).getQueryString();
  }

  /**
   * This method return the CQL query to delete a persistent in the table.
   * refer : http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlDelete.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @param fields  filed list to be deleted
   * @return CQL Query
   */
  static String getDeleteDataQuery(CassandraMapping mapping, List<String> fields) {
    String[] columnNames = getColumnNames(mapping, fields);
    String[] objects = new String[fields.size()];
    Arrays.fill(objects, "?");
    Delete delete = QueryBuilder.delete().from(mapping.getKeySpace().getName(), mapping.getCoreName());
    return processKeys(columnNames, delete);
  }

  private static String processKeys(String[] columnNames, BuiltStatement delete) {
    BuiltStatement query = null;
    boolean isWhereNeeded = true;
    for (String columnName : columnNames) {
      if (isWhereNeeded) {
        if (delete instanceof Delete) {
          query = ((Delete) delete).where(QueryBuilder.eq(columnName, "?"));
        } else {
          query = ((Select) delete).where(QueryBuilder.eq(columnName, "?"));
        }
        isWhereNeeded = false;
      } else {
        if (delete instanceof Delete) {
          query = ((Delete.Where) query).and(QueryBuilder.eq(columnName, "?"));
        } else {
          query = ((Select.Where) query).and(QueryBuilder.eq(columnName, "?"));
        }
      }
    }
    return query != null ? query.getQueryString() : null;
  }

  /**
   * This method returns the CQL Select query to retrieve data from the table.
   * refer: http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlSelect.html
   *
   * @param mapping   Cassandra Mapping {@link CassandraMapping}
   * @param keyFields key fields
   * @return CQL Query
   */
  static String getSelectObjectQuery(CassandraMapping mapping, List<String> keyFields) {
    Select select = QueryBuilder.select().from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    String[] columnNames = getColumnNames(mapping, keyFields);
    return processKeys(columnNames, select);
  }

  /**
   * This method returns CQL Select query to retrieve data from the table with given fields.
   * This method is used for Avro Serialization
   * refer: http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlSelect.html
   *
   * @param mapping   Cassandra Mapping {@link CassandraMapping}
   * @param fields    Given fields to retrieve
   * @param keyFields key fields
   * @return CQL Query
   */
  static String getSelectObjectWithFieldsQuery(CassandraMapping mapping, String[] fields, List<String> keyFields) {
    Select select = QueryBuilder.select(getColumnNames(mapping, Arrays.asList(fields))).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    String[] columnNames = getColumnNames(mapping, keyFields);
    return processKeys(columnNames, select);
  }

  /**
   * This method returns CQL Select query to check if a key exists. This method
   * is used for Avro Serialization refer:
   * http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlSelect.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @param keyFields key fields
   * @return CQL Query
   */
  static String getCheckExistsQuery(CassandraMapping mapping, List<String> keyFields) {
    Select select = QueryBuilder.select().countAll().from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    String[] columnNames = getColumnNames(mapping, keyFields);
    return processKeys(columnNames, select);
  }

  /**
   * This method returns CQL Select query to retrieve data from the table with given fields.
   * This method is used for Native Serialization
   * refer: http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlSelect.html
   *
   * @param mapping Cassandra Mapping {@link CassandraMapping}
   * @param fields  Given fields to retrieve
   * @return CQL Query
   */
  static String getSelectObjectWithFieldsQuery(CassandraMapping mapping, String[] fields) {
    String cqlQuery = null;
    String[] columnNames = getColumnNames(mapping, Arrays.asList(fields));
    Select select = QueryBuilder.select(columnNames).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    CassandraKey cKey = mapping.getCassandraKey();
    if (cKey != null) {
      Select.Where query = null;
      boolean isWhereNeeded = true;
      for (Field field : cKey.getFieldList()) {
        if (isWhereNeeded) {
          query = select.where(QueryBuilder.eq(field.getColumnName(), "?"));
          isWhereNeeded = false;
        } else {
          query = query.and(QueryBuilder.eq(field.getColumnName(), "?"));
        }
      }
      cqlQuery = query != null ? query.getQueryString() : null;
    } else {
      for (Field field : mapping.getFieldList()) {
        boolean isPrimaryKey = Boolean.parseBoolean(field.getProperty("primarykey"));
        if (isPrimaryKey) {
          cqlQuery = select.where(QueryBuilder.eq(field.getColumnName(), "?")).getQueryString();
          break;
        }
      }
    }
    return cqlQuery;
  }


  /**
   * This method returns CQL Query for execute method. This CQL contains a Select Query to retrieve data from the table
   *
   * @param mapping        Cassandra Mapping {@link CassandraMapping}
   * @param cassandraQuery Query {@link CassandraQuery}
   * @param objects        object list
   * @return CQL Query
   */
  static String getExecuteQuery(CassandraMapping mapping, Query cassandraQuery, List<Object> objects, String[] fields) {
    long limit = cassandraQuery.getLimit();
    Select select = QueryBuilder.select(getColumnNames(mapping, Arrays.asList(fields))).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (limit > 0) {
      select = select.limit((int) limit);
    }
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    return processQuery(cassandraQuery, select, mapping, objects);
  }

  private static String processQuery(Query cassandraQuery, BuiltStatement select, CassandraMapping mapping, List<Object> objects) {
    String primaryKey = null;
    BuiltStatement query = null;
    Object startKey = cassandraQuery.getStartKey();
    Object endKey = cassandraQuery.getEndKey();
    Object key = cassandraQuery.getKey();
    boolean isWhereNeeded = true;
    if (key != null) {
      if (mapping.getCassandraKey() != null) {
        ArrayList<String> cassandraKeys = new ArrayList<>();
        ArrayList<Object> cassandraValues = new ArrayList<>();
        AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
        String[] columnKeys = getColumnNames(mapping, cassandraKeys);
        for (int i = 0; i < cassandraKeys.size(); i++) {
          if (isWhereNeeded) {
            if (select instanceof Select) {
              query = ((Select) select).where(QueryBuilder.eq(columnKeys[i], "?"));
            } else if (select instanceof Delete) {
              query = ((Delete) select).where(QueryBuilder.eq(columnKeys[i], "?"));
            } else {
              query = ((Update.Assignments) select).where(QueryBuilder.eq(columnKeys[i], "?"));
            }
            objects.add(cassandraValues.get(i));
            isWhereNeeded = false;
          } else {
            if (select instanceof Select) {
              query = ((Select.Where) query).and(QueryBuilder.eq(columnKeys[i], "?"));
            } else if (select instanceof Delete) {
              query = ((Delete.Where) query).and(QueryBuilder.eq(columnKeys[i], "?"));
            } else {
              query = ((Update.Where) query).and(QueryBuilder.eq(columnKeys[i], "?"));
            }
            objects.add(cassandraValues.get(i));
          }
        }
      } else {
        primaryKey = getPKey(mapping.getFieldList());
        if (select instanceof Select) {
          query = ((Select) select).where(QueryBuilder.eq(primaryKey, "?"));
        } else if (select instanceof Delete) {
          query = ((Delete) select).where(QueryBuilder.eq(primaryKey, "?"));
        } else {
          query = ((Update.Assignments) select).where(QueryBuilder.eq(primaryKey, "?"));
        }
        objects.add(key);
      }
    } else {
      if (startKey != null) {
        if (mapping.getCassandraKey() != null) {
          ArrayList<String> cassandraKeys = new ArrayList<>();
          ArrayList<Object> cassandraValues = new ArrayList<>();
          AvroCassandraUtils.processKeys(mapping, startKey, cassandraKeys, cassandraValues);
          String[] columnKeys = getColumnNames(mapping, cassandraKeys);
          for (int i = 0; i < cassandraKeys.size(); i++) {
            if (isWhereNeeded) {
              if (select instanceof Select) {
                query = ((Select) select).where(QueryBuilder.gte(columnKeys[i], "?"));
              } else if (select instanceof Delete) {
                /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
                throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
              } else {
                query = ((Update.Assignments) select).where(QueryBuilder.gte(columnKeys[i], "?"));
              }
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              if (select instanceof Select) {
                query = ((Select.Where) query).and(QueryBuilder.gte(columnKeys[i], "?"));
              } else if (select instanceof Delete) {
                       /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
                throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
              } else {
                query = ((Update.Where) query).and(QueryBuilder.gte(columnKeys[i], "?"));
              }
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = getPKey(mapping.getFieldList());
          if (select instanceof Select) {
            query = ((Select) select).where(QueryBuilder.gte(primaryKey, "?"));
          } else if (select instanceof Delete) {
                           /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
            throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
          } else {
            query = ((Update.Assignments) select).where(QueryBuilder.gte(primaryKey, "?"));
          }
          objects.add(startKey);
          isWhereNeeded = false;
        }
      }
      if (endKey != null) {
        if (mapping.getCassandraKey() != null) {
          ArrayList<String> cassandraKeys = new ArrayList<>();
          ArrayList<Object> cassandraValues = new ArrayList<>();
          AvroCassandraUtils.processKeys(mapping, endKey, cassandraKeys, cassandraValues);
          String[] columnKeys = getColumnNames(mapping, cassandraKeys);
          for (int i = 0; i < cassandraKeys.size(); i++) {
            if (isWhereNeeded) {
              if (select instanceof Select) {
                query = ((Select) select).where(QueryBuilder.lte(columnKeys[i], "?"));
              } else if (select instanceof Delete) {
                               /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
                throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
              } else {
                query = ((Update.Assignments) select).where(QueryBuilder.lte(columnKeys[i], "?"));
              }
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              if (select instanceof Select) {
                query = ((Select.Where) query).and(QueryBuilder.lte(columnKeys[i], "?"));
              } else if (select instanceof Delete) {
                               /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
                throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
              } else {
                query = ((Update.Where) query).and(QueryBuilder.lte(columnKeys[i], "?"));
              }
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = primaryKey != null ? primaryKey : getPKey(mapping.getFieldList());
          if (isWhereNeeded) {
            if (select instanceof Select) {
              query = ((Select) select).where(QueryBuilder.lte(primaryKey, "?"));
            } else if (select instanceof Delete) {
                             /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                 */
              throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
            } else {
              query = ((Update.Assignments) select).where(QueryBuilder.lte(primaryKey, "?"));
            }
          } else {
            if (select instanceof Select) {
              query = ((Select.Where) query).and(QueryBuilder.lte(primaryKey, "?"));
            } else if (select instanceof Delete) {
              /*
                According to the JIRA https://issues.apache.org/jira/browse/CASSANDRA-7651 this has been fixed, but It seems this not fixed yet.
                */
              throw new RuntimeException("Delete by Query is not suppoted for Key Ranges.");
            } else {
              query = ((Update.Where) query).and(QueryBuilder.lte(primaryKey, "?"));
            }
          }
          objects.add(endKey);
        }
      }
    }
    if (startKey == null && endKey == null && key == null) {
      return select.getQueryString();
    }
    return query != null ? query.getQueryString() : null;
  }

  private static String[] getColumnNames(CassandraMapping mapping, List<String> fields) {
    ArrayList<String> columnNames = new ArrayList<>();
    for (String field : fields) {
      Field fieldBean = mapping.getFieldFromFieldName(field);
      CassandraKey cassandraKey = mapping.getCassandraKey();
      Field keyBean = null;
      if (cassandraKey != null) {
        keyBean = cassandraKey.getFieldFromFieldName(field);
      }
      if (fieldBean != null) {
        columnNames.add(fieldBean.getColumnName());
      } else if (keyBean != null) {
        columnNames.add(keyBean.getColumnName());
      } else {
        LOG.warn("{} field is ignored, couldn't find relevant field in the persistent mapping", field);
      }
    }
    return columnNames.toArray(new String[0]);
  }

  private static String getPKey(List<Field> fields) {
    for (Field field : fields) {
      boolean isPrimaryKey = Boolean.parseBoolean(field.getProperty("primarykey"));
      if (isPrimaryKey) {
        return field.getColumnName();
      }
    }
    return null;
  }

  /**
   * This method returns CQL Qeury for DeleteByQuery method.
   * refer: http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlDelete.html
   *
   * @param mapping        Cassandra Mapping {@link CassandraMapping}
   * @param cassandraQuery Cassandra Query {@link CassandraQuery}
   * @param objects        field values
   * @return CQL Query
   */
  static String getDeleteByQuery(CassandraMapping mapping, Query cassandraQuery, List<Object> objects) {
    String[] columns = null;
    if (cassandraQuery.getFields() != null) {
      columns = getColumnNames(mapping, Arrays.asList(cassandraQuery.getFields()));
    }
    Delete delete;
    if (columns != null) {
      delete = QueryBuilder.delete(columns).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    } else {
      delete = QueryBuilder.delete().from(mapping.getKeySpace().getName(), mapping.getCoreName());
    }
    return processQuery(cassandraQuery, delete, mapping, objects);
  }

  /**
   * This method returns the CQL Query for UpdateByQuery method
   * refer : http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlUpdate.html
   *
   * @param mapping        Cassandra mapping {@link CassandraMapping}
   * @param cassandraQuery Cassandra Query {@link CassandraQuery}
   * @param objects        field Objects list
   * @return CQL Query
   */
  static String getUpdateByQueryForAvro(CassandraMapping mapping, Query cassandraQuery, List<Object> objects, Schema schema) {
    Update update = QueryBuilder.update(mapping.getKeySpace().getName(), mapping.getCoreName());
    Update.Assignments updateAssignments = null;
    if (cassandraQuery instanceof CassandraQuery) {
      String[] columnNames = getColumnNames(mapping, Arrays.asList(cassandraQuery.getFields()));
        for (String column : columnNames) {
          updateAssignments = update.with(QueryBuilder.set(column, "?"));
          Field field = mapping.getFieldFromColumnName(column);
          Object value = ((CassandraQuery) cassandraQuery).getUpdateFieldValue(field.getFieldName());
          try {
            Schema schemaField = schema.getField(field.getFieldName()).schema();
            objects.add(AvroCassandraUtils.getFieldValueFromAvroBean(schemaField, schemaField.getType(), value, field));
          } catch (NullPointerException e) {
            throw new RuntimeException(field + " field couldn't find in the class " + mapping.getPersistentClass() + ".");
          }
        }
    } else {
      throw new RuntimeException("Please use Cassandra Query object to invoke, UpdateByQuery method.");
    }
    return processQuery(cassandraQuery, updateAssignments, mapping, objects);
  }


  /**
   * This method returns the CQL Query for UpdateByQuery method
   * refer : http://docs.datastax.com/en/cql/3.3/cql/cql_reference/cqlUpdate.html
   *
   * @param mapping        Cassandra mapping {@link CassandraMapping}
   * @param cassandraQuery Cassandra Query {@link CassandraQuery}
   * @param objects        field Objects list
   * @return CQL Query
   */
  static String getUpdateByQueryForNative(CassandraMapping mapping, Query cassandraQuery, List<Object> objects) {
    Update update = QueryBuilder.update(mapping.getKeySpace().getName(), mapping.getCoreName());
    Update.Assignments updateAssignments = null;
    if (cassandraQuery instanceof CassandraQuery) {
      String[] columnNames = getColumnNames(mapping, Arrays.asList(cassandraQuery.getFields()));
        for (String column : columnNames) {
          updateAssignments = update.with(QueryBuilder.set(column, "?"));
          objects.add(((CassandraQuery) cassandraQuery).getUpdateFieldValue(mapping.getFieldFromColumnName(column).getFieldName()));
        }
    } else {
      throw new RuntimeException("Please use Cassandra Query object to invoke, UpdateByQuery method.");
    }
    return processQuery(cassandraQuery, updateAssignments, mapping, objects);
  }


  private static void populateFieldsToQuery(Schema schema, StringBuilder builder) throws Exception {
    switch (schema.getType()) {
      case INT:
        builder.append("int");
        break;
      case MAP:
        builder.append("map<text,");
        populateFieldsToQuery(schema.getValueType(), builder);
        builder.append(">");
        break;
      case ARRAY:
        builder.append("list<");
        populateFieldsToQuery(schema.getElementType(), builder);
        builder.append(">");
        break;
      case LONG:
        builder.append("bigint");
        break;
      case FLOAT:
        builder.append("float");
        break;
      case DOUBLE:
        builder.append("double");
        break;
      case BOOLEAN:
        builder.append("boolean");
        break;
      case BYTES:
        builder.append("blob");
        break;
      case RECORD:
        builder.append("frozen<").append(schema.getName()).append(">");
        break;
      case STRING:
      case FIXED:
      case ENUM:
        builder.append("text");
        break;
      case UNION:
        for (Schema unionElementSchema : schema.getTypes()) {
          if (unionElementSchema.getType().equals(Schema.Type.RECORD)) {
            String recordName = unionElementSchema.getName();
            if (!builder.toString().contains(recordName)) {
              builder.append("frozen<").append(recordName).append(">");
            } else {
              LOG.warn("Same Field Type can't be mapped recursively. This is not supported with Cassandra UDT types, Please use byte dataType for recursive mapping.");
              throw new Exception("Same Field Type has mapped recursively");
            }
            break;
          } else if (!unionElementSchema.getType().equals(Schema.Type.NULL)) {
            populateFieldsToQuery(unionElementSchema, builder);
            break;
          }
        }
        break;
    }
  }

  static void processRecord(Schema recordSchema, StringBuilder stringBuilder) {
    boolean isCommaNeeded = false;
    for (Schema.Field field : recordSchema.getFields()) {
      if (isCommaNeeded) {
        stringBuilder.append(", ");
      }
      String fieldName = field.name();
      stringBuilder.append(fieldName).append(" ");
      try {
        populateFieldsToQuery(field.schema(), stringBuilder);
        isCommaNeeded = true;
      } catch (Exception e) {
        int i = stringBuilder.indexOf(fieldName);
        if (i != -1) {
          stringBuilder.delete(i, i + fieldName.length());
          isCommaNeeded = false;
        }
      }
    }
  }

  static String getCreateUDTTypeForNative(CassandraMapping mapping, Class persistentClass, String udtType, String fieldName) throws NoSuchFieldException {
    StringBuilder stringBuffer = new StringBuilder();
    Class udtClass = persistentClass.getDeclaredField(fieldName).getType();
    UDT annotation = (UDT) udtClass.getAnnotation(UDT.class);
    if (annotation != null) {
      stringBuffer.append("CREATE TYPE IF NOT EXISTS ").append(mapping.getKeySpace().getName()).append(".").append(udtType).append(" (");
      boolean isCommaNeeded = false;
      for (java.lang.reflect.Field udtField : udtClass.getDeclaredFields()) {
        com.datastax.driver.mapping.annotations.Field fieldAnnotation = udtField.getDeclaredAnnotation(com.datastax.driver.mapping.annotations.Field.class);
        if (fieldAnnotation != null) {
          if (isCommaNeeded) {
            stringBuffer.append(", ");
          }
          if (!fieldAnnotation.name().isEmpty()) {
            stringBuffer.append(fieldAnnotation.name()).append(" ");
          } else {
            stringBuffer.append(udtField.getName()).append(" ");
          }
          stringBuffer.append(dataType(udtField, null));
          isCommaNeeded = true;
        }
      }
      stringBuffer.append(")");
    } else {
      throw new RuntimeException("");
    }
    return stringBuffer.toString();
  }

  static String getCreateUDTTypeForAvro(CassandraMapping mapping, String udtType, Schema fieldSchema) {
    StringBuilder stringBuffer = new StringBuilder();
    stringBuffer.append("CREATE TYPE IF NOT EXISTS ").append(mapping.getKeySpace().getName()).append(".").append(udtType).append(" (");
    CassandraQueryFactory.processRecord(fieldSchema, stringBuffer);
    stringBuffer.append(")");
    return stringBuffer.toString();
  }

  private static String dataType(java.lang.reflect.Field field, Type fieldType) {
    String type;
    if (field != null) {
      type = field.getType().getName();
    } else {
      type = fieldType.getTypeName();
    }
    String dataType;
    String s = type;
    if (s.equals("java.lang.String") || s.equals("java.lang.CharSequence")) {
      dataType = "text";
    } else if (s.equals("int") || s.equals("java.lang.Integer")) {
      dataType = "int";
    } else if (s.equals("double") || s.equals("java.lang.Double")) {
      dataType = "double";
    } else if (s.equals("float") || s.equals("java.lang.Float")) {
      dataType = "float";
    } else if (s.equals("boolean") || s.equals("java.lang.Boolean")) {
      dataType = "boolean";
    } else if (s.equals("java.util.UUID")) {
      dataType = "uuid";
    } else if (s.equals("java.lang.Long")) {
      dataType = "bigint";
    } else if (s.equals("java.math.BigDecimal")) {
      dataType = "decimal";
    } else if (s.equals("java.net.InetAddress")) {
      dataType = "inet";
    } else if (s.equals("java.math.BigInteger")) {
      dataType = "varint";
    } else if (s.equals("java.nio.ByteBuffer")) {
      dataType = "blob";
    } else if (s.contains("Map")) {
      ParameterizedType mapType;
      if (field != null) {
        mapType = (ParameterizedType) field.getGenericType();
      } else {
        mapType = (ParameterizedType) fieldType;
      }
      Type value1 = mapType.getActualTypeArguments()[0];
      Type value2 = mapType.getActualTypeArguments()[1];
      dataType = "map<" + dataType(null, value1) + "," + dataType(null, value2) + ">";
    } else if (s.contains("List")) {
      ParameterizedType listType;
      if (field != null) {
        listType = (ParameterizedType) field.getGenericType();
      } else {
        listType = (ParameterizedType) fieldType;
      }
      Type value = listType.getActualTypeArguments()[0];
      dataType = "list<" + dataType(null, value) + ">";
    } else if (s.contains("Set")) {
      ParameterizedType setType;
      if (field != null) {
        setType = (ParameterizedType) field.getGenericType();
      } else {
        setType = (ParameterizedType) fieldType;
      }
      Type value = setType.getActualTypeArguments()[0];
      dataType = "set<" + dataType(null, value) + ">";
    } else {
      throw new RuntimeException("Unsupported Cassandra DataType");
    }
    return dataType;
  }

}
