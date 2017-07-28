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

import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import org.apache.avro.Schema;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.ClusterKeyField;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;
import org.apache.gora.cassandra.bean.PartitionKeyField;
import org.apache.gora.cassandra.persistent.CassandraNativePersistent;
import org.apache.gora.cassandra.query.CassandraQuery;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    boolean isCommaNeeded = false;
    CassandraKey cassandraKey = mapping.getCassandraKey();
    // appending Cassandra key columns into db schema
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
    } else {
      if (!stringBuffer.toString().toLowerCase(Locale.ENGLISH).contains("primary key")) {
        Field field = mapping.getDefaultCassandraKey();
        stringBuffer.append(", ").append(field.getFieldName()).append(" ").append(field.getType()).append("  PRIMARY KEY ");
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
    return "TRUNCATE TABLE " + mapping.getKeySpace().getName() + "." + mapping.getCoreName();
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
    Delete.Where query = null;
    boolean isWhereNeeded = true;
    for (String columnName : columnNames) {
      if (isWhereNeeded) {
        query = delete.where(QueryBuilder.eq(columnName, "?"));
        isWhereNeeded = false;
      } else {
        query = query.and(QueryBuilder.eq(columnName, "?"));
      }
    }
    return query.getQueryString();
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
    Select.Where query = null;
    boolean isWhereNeeded = true;
    for (String columnName : columnNames) {
      if (isWhereNeeded) {
        query = select.where(QueryBuilder.eq(columnName, "?"));
        isWhereNeeded = false;
      } else {
        query = query.and(QueryBuilder.eq(columnName, "?"));
      }
    }
    return query.getQueryString();
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
    Select.Where query = null;
    boolean isWhereNeeded = true;
    for (String columnName : columnNames) {
      if (isWhereNeeded) {
        query = select.where(QueryBuilder.eq(columnName, "?"));
        isWhereNeeded = false;
      } else {
        query = query.and(QueryBuilder.eq(columnName, "?"));
      }
    }
    return query.getQueryString();
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
  static String getExecuteQuery(CassandraMapping mapping, Query cassandraQuery, List<Object> objects) {
    String[] fields = cassandraQuery.getFields();
    fields = fields != null ? fields : mapping.getFieldNames();
    Object startKey = cassandraQuery.getStartKey();
    Object endKey = cassandraQuery.getEndKey();
    Object key = cassandraQuery.getKey();
    String primaryKey = null;
    long limit = cassandraQuery.getLimit();
    Select select = QueryBuilder.select(getColumnNames(mapping, Arrays.asList(fields))).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    if (limit > 0) {
      select = select.limit((int) limit);
    }
    if (Boolean.parseBoolean(mapping.getProperty("allowFiltering"))) {
      select.allowFiltering();
    }
    Select.Where query = null;
    boolean isWhereNeeded = true;
    if (key != null) {
      if (mapping.getCassandraKey() != null) {
        ArrayList<String> cassandraKeys = new ArrayList<>();
        ArrayList<Object> cassandraValues = new ArrayList<>();
        AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
        String[] columnKeys = getColumnNames(mapping, cassandraKeys);
        for (int i = 0; i < cassandraKeys.size(); i++) {
          if (isWhereNeeded) {
            query = select.where(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
            isWhereNeeded = false;
          } else {
            query = query.and(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
          }
        }
      } else {
        primaryKey = getPKey(mapping.getFieldList());
        query = select.where(QueryBuilder.eq(primaryKey, "?"));
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
              query = select.where(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = getPKey(mapping.getFieldList());
          query = select.where(QueryBuilder.gte(primaryKey, "?"));
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
              query = select.where(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = primaryKey != null ? primaryKey : getPKey(mapping.getFieldList());
          if (isWhereNeeded) {
            query = select.where(QueryBuilder.lte(primaryKey, "?"));
          } else {
            query = query.and(QueryBuilder.lte(primaryKey, "?"));
          }
          objects.add(endKey);
        }
      }
    }
    if (startKey == null && endKey == null && key == null) {
      return select.getQueryString();
    }
    return query.getQueryString();
  }

  private static String[] getColumnNames(CassandraMapping mapping, List<String> fields) {
    ArrayList<String> columnNames = new ArrayList<>();
    for (String field : fields) {
      Field fieldBean = mapping.getFieldFromFieldName(field);
      if (fieldBean != null) {
        columnNames.add(fieldBean.getColumnName());
      } else {
        if (mapping.getDefaultCassandraKey().getFieldName().equals(field)) {
          columnNames.add(field);
        } else {
          LOG.warn("{} field is ignored, couldn't find relavant field in the persistent mapping", field);
        }
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
    if (!Arrays.equals(cassandraQuery.getFields(), mapping.getFieldNames())) {
      columns = getColumnNames(mapping, Arrays.asList(cassandraQuery.getFields()));
    }
    Object startKey = cassandraQuery.getStartKey();
    Object endKey = cassandraQuery.getEndKey();
    Object key = cassandraQuery.getKey();
    String primaryKey = null;
    Delete delete;
    if (columns != null) {
      delete = QueryBuilder.delete(columns).from(mapping.getKeySpace().getName(), mapping.getCoreName());
    } else {
      delete = QueryBuilder.delete().from(mapping.getKeySpace().getName(), mapping.getCoreName());
    }
    Delete.Where query = null;
    boolean isWhereNeeded = true;
    if (key != null) {
      if (mapping.getCassandraKey() != null) {
        ArrayList<String> cassandraKeys = new ArrayList<>();
        ArrayList<Object> cassandraValues = new ArrayList<>();
        AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
        String[] columnKeys = getColumnNames(mapping, cassandraKeys);
        for (int i = 0; i < cassandraKeys.size(); i++) {
          if (isWhereNeeded) {
            query = delete.where(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
            isWhereNeeded = false;
          } else {
            query = query.and(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
          }
        }
      } else {
        primaryKey = getPKey(mapping.getFieldList());
        query = delete.where(QueryBuilder.eq(primaryKey, "?"));
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
              query = delete.where(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = getPKey(mapping.getFieldList());
          query = delete.where(QueryBuilder.gte(primaryKey, "?"));
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
              query = delete.where(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = primaryKey != null ? primaryKey : getPKey(mapping.getFieldList());
          if (isWhereNeeded) {
            query = delete.where(QueryBuilder.lte(primaryKey, "?"));
          } else {
            query = query.and(QueryBuilder.lte(primaryKey, "?"));
          }
          objects.add(endKey);
        }
      }
    }
    if (startKey == null && endKey == null && key == null) {
      return delete.getQueryString();
    }
    return query.getQueryString();
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
  static String getUpdateByQuery(CassandraMapping mapping, Query cassandraQuery, List<Object> objects) {
    Update update = QueryBuilder.update(mapping.getKeySpace().getName(), mapping.getCoreName());
    Update.Assignments updateAssignments = null;
    if (cassandraQuery instanceof CassandraQuery) {
      String[] columnNames = getColumnNames(mapping, Arrays.asList(cassandraQuery.getFields()));
      if(CassandraNativePersistent.class.isAssignableFrom(mapping.getPersistentClass())) {
        for (String column : columnNames) {
          updateAssignments = update.with(QueryBuilder.set(column, "?"));
          objects.add(((CassandraQuery) cassandraQuery).getUpdateFieldValue(mapping.getFieldFromColumnName(column).getFieldName()));
        }
      } else {
        for (String column : columnNames) {
          updateAssignments = update.with(QueryBuilder.set(column, "?"));
          String field = mapping.getFieldFromColumnName(column).getFieldName();
          Object value = ((CassandraQuery) cassandraQuery).getUpdateFieldValue(field);
          try {
            Schema schema = (Schema) mapping.getPersistentClass().getField("SCHEMA$").get(null);
            Schema schemaField = schema.getField(field).schema();
            objects.add(AvroCassandraUtils.getFieldValueFromAvroBean(schemaField, schemaField.getType(), value));
          } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("SCHEMA$ field can't accessible, Please recompile the Avro schema with goracompiler.");
          } catch (NullPointerException e) {
            throw new RuntimeException(field + " field couldn't find in the class " + mapping.getPersistentClass() + ".");
          }
        }
      }
    }
    String primaryKey = null;
    Update.Where query = null;
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
            query = updateAssignments.where(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
            isWhereNeeded = false;
          } else {
            query = query.and(QueryBuilder.eq(columnKeys[i], "?"));
            objects.add(cassandraValues.get(i));
          }
        }
      } else {
        primaryKey = getPKey(mapping.getFieldList());
        query = updateAssignments.where(QueryBuilder.eq(primaryKey, "?"));
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
              query = updateAssignments.where(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.gte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = getPKey(mapping.getFieldList());
          query = updateAssignments.where(QueryBuilder.gte(primaryKey, "?"));
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
              query = updateAssignments.where(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
              isWhereNeeded = false;
            } else {
              query = query.and(QueryBuilder.lte(columnKeys[i], "?"));
              objects.add(cassandraValues.get(i));
            }
          }
        } else {
          primaryKey = primaryKey != null ? primaryKey : getPKey(mapping.getFieldList());
          if (isWhereNeeded) {
            query = updateAssignments.where(QueryBuilder.lte(primaryKey, "?"));
          } else {
            query = query.and(QueryBuilder.lte(primaryKey, "?"));
          }
          objects.add(endKey);
        }
      }
    }
    if (startKey == null && endKey == null && key == null) {
      return updateAssignments.getQueryString();
    }
    return query.getQueryString();
  }

}
