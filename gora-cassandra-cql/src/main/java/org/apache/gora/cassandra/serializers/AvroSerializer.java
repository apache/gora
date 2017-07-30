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

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.apache.avro.Schema;
import org.apache.commons.lang.ArrayUtils;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.query.CassandraResultSet;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains the operations relates to Avro Serialization.
 */
class AvroSerializer<K, T extends PersistentBase> extends CassandraSerializer {


  private static final Logger LOG = LoggerFactory.getLogger(AvroSerializer.class);

  private DataStore<K, T> cassandraDataStore;

  AvroSerializer(CassandraClient cassandraClient, DataStore<K, T> dataStore, CassandraMapping mapping) {
    super(cassandraClient, dataStore.getKeyClass(), dataStore.getPersistentClass(), mapping);
    this.cassandraDataStore = dataStore;
  }

  @Override
  public Persistent get(Object key, String[] fields) {
    if (fields == null) {
      fields = getFields();
    }
    ArrayList<String> cassandraKeys = new ArrayList<>();
    ArrayList<Object> cassandraValues = new ArrayList<>();
    AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
    String cqlQuery = CassandraQueryFactory.getSelectObjectWithFieldsQuery(mapping, fields, cassandraKeys);
    ResultSet resultSet = this.client.getSession().execute(cqlQuery, cassandraValues.toArray());
    Iterator<Row> iterator = resultSet.iterator();
    ColumnDefinitions definitions = resultSet.getColumnDefinitions();
    T obj = null;
    if (iterator.hasNext()) {
      obj = cassandraDataStore.newPersistent();
      Row row = iterator.next();
      populateValuesToPersistent(row, definitions, obj, fields);
    }
    return obj;
  }

  @Override
  public void put(Object key, Persistent persistent) {
    if (persistent instanceof PersistentBase) {
      if (persistent.isDirty()) {
        PersistentBase persistentBase = (PersistentBase) persistent;
        ArrayList<String> fields = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        AvroCassandraUtils.processKeys(mapping, key, fields, values);
        for (Schema.Field f : persistentBase.getSchema().getFields()) {
          String fieldName = f.name();
          Field field = mapping.getFieldFromFieldName(fieldName);
          if (field == null) {
            LOG.debug("Ignoring {} adding field, {} field can't find in {} mapping", new Object[]{fieldName, fieldName, persistentClass});
            continue;
          }
          if (persistent.isDirty(f.pos()) || mapping.getInlinedDefinedPartitionKey().equals(mapping.getFieldFromFieldName(fieldName))) {
            Object value = persistentBase.get(f.pos());
            value = AvroCassandraUtils.getFieldValueFromAvroBean(f.schema(), f.schema().getType(), value, field);
            values.add(value);
            fields.add(fieldName);
          }
        }
        String cqlQuery = CassandraQueryFactory.getInsertDataQuery(mapping, fields);
        client.getSession().execute(cqlQuery, values.toArray());
      } else {
        LOG.info("Ignored putting persistent bean {} in the store as it is neither "
                + "new, neither dirty.", new Object[]{persistent});
      }
    } else {
      LOG.error("{} Persistent bean isn't extended by {} .", new Object[]{this.persistentClass, PersistentBase.class});
    }
  }

  @Override
  public Persistent get(Object key) {
    ArrayList<String> cassandraKeys = new ArrayList<>();
    ArrayList<Object> cassandraValues = new ArrayList<>();
    AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
    String cqlQuery = CassandraQueryFactory.getSelectObjectQuery(mapping, cassandraKeys);
    ResultSet resultSet = this.client.getSession().execute(cqlQuery, cassandraValues.toArray());
    Iterator<Row> iterator = resultSet.iterator();
    ColumnDefinitions definitions = resultSet.getColumnDefinitions();
    T obj = null;
    if (iterator.hasNext()) {
      obj = cassandraDataStore.newPersistent();
      Row row = iterator.next();
      populateValuesToPersistent(row, definitions, obj, mapping.getFieldNames());
    }
    return obj;
  }

  /**
   * This method wraps result set data in to DataEntry and creates a list of DataEntry.
   **/
  private void populateValuesToPersistent(Row row, ColumnDefinitions columnDefinitions, PersistentBase base, String[] fields) {
    Object paramValue;
    for (String fieldName : fields) {
      Schema.Field avroField = base.getSchema().getField(fieldName);
      Field field = mapping.getFieldFromFieldName(fieldName);
      //to ignore unspecified fields in the mapping
      if (field == null || avroField == null) {
        continue;
      }
      Schema fieldSchema = avroField.schema();
      String columnName = field.getColumnName();
      paramValue = getValue(row, columnDefinitions, columnName);
      Object value = AvroCassandraUtils.getAvroFieldValue(paramValue, fieldSchema);
      base.put(avroField.pos(), value);
    }
  }

  private Object getValue(Row row, ColumnDefinitions columnDefinitions, String columnName) {
    Object paramValue;
    Field field = mapping.getFieldFromColumnName(columnName);
    DataType columnType = columnDefinitions.getType(columnName);
    switch (columnType.getName()) {
      case ASCII:
        paramValue = row.getString(columnName);
        break;
      case BIGINT:
        paramValue = row.isNull(columnName) ? null : row.getLong(columnName);
        break;
      case BLOB:
        paramValue = row.isNull(columnName) ? null : row.getBytes(columnName);
        break;
      case BOOLEAN:
        paramValue = row.isNull(columnName) ? null : row.getBool(columnName);
        break;
      case COUNTER:
        paramValue = row.isNull(columnName) ? null : row.getLong(columnName);
        break;
      case DECIMAL:
        paramValue = row.isNull(columnName) ? null : row.getDecimal(columnName);
        break;
      case DOUBLE:
        paramValue = row.isNull(columnName) ? null : row.getDouble(columnName);
        break;
      case FLOAT:
        paramValue = row.isNull(columnName) ? null : row.getFloat(columnName);
        break;
      case INET:
        paramValue = row.isNull(columnName) ? null : row.getInet(columnName).toString();
        break;
      case INT:
        paramValue = row.isNull(columnName) ? null : row.getInt(columnName);
        break;
      case TEXT:
        paramValue = row.getString(columnName);
        break;
      case TIMESTAMP:
        paramValue = row.isNull(columnName) ? null : row.getDate(columnName);
        break;
      case UUID:
        paramValue = row.isNull(columnName) ? null : row.getUUID(columnName);
        break;
      case VARCHAR:
        paramValue = row.getString(columnName);
        break;
      case VARINT:
        paramValue = row.isNull(columnName) ? null : row.getVarint(columnName);
        break;
      case TIMEUUID:
        paramValue = row.isNull(columnName) ? null : row.getUUID(columnName);
        break;
      case LIST:
        String dataType = field.getType();
        dataType = dataType.substring(dataType.indexOf("<") + 1, dataType.indexOf(">"));
        paramValue = row.isNull(columnName) ? null : row.getList(columnName, getRelevantClassForCassandraDataType(dataType));
        break;
      case SET:
        dataType = field.getType();
        dataType = dataType.substring(dataType.indexOf("<") + 1, dataType.indexOf(">"));
        paramValue = row.isNull(columnName) ? null : row.getList(columnName, getRelevantClassForCassandraDataType(dataType));
        break;
      case MAP:
        dataType = field.getType();
        dataType = dataType.substring(dataType.indexOf("<") + 1, dataType.indexOf(">"));
        dataType = dataType.split(",")[1];
        // Avro supports only String for keys
        paramValue = row.isNull(columnName) ? null : row.getMap(columnName, String.class, getRelevantClassForCassandraDataType(dataType));
        break;
      case UDT:
        paramValue = row.isNull(columnName) ? null : row.getUDTValue(columnName);
        break;
      case TUPLE:
        paramValue = row.isNull(columnName) ? null : row.getTupleValue(columnName).toString();
        break;
      case CUSTOM:
        paramValue = row.isNull(columnName) ? null : row.getBytes(columnName);
        break;
      default:
        paramValue = row.getString(columnName);
        break;
    }
    return paramValue;
  }

/*  public Collection<Object> getFieldValues(Object o) {
    UDTValue udtValue = (UDTValue) o;
    UserType type = udtValue.getType();

    Collection<Object> values = new ArrayList<Object>(type.size());

 *//*   for (UserType.Field field : type) {
      udtValue.
      ByteBuffer bytes = udtValue.getBytesUnsafe(field.getName());
      DataType value = field.getType();
      for(DataType type1 : value.getTypeArguments()) {
        type1.
      }
      values.add(value);
    }*//*

    return values;
  }*/


  private Class getRelevantClassForCassandraDataType(String dataType) {
    switch (dataType) {
      //// TODO: 7/25/17 support all the datatypes 
      case "ascii":
      case "text":
      case "varchar":
        return String.class;
      case "blob":
        return ByteBuffer.class;
      default:
        throw new RuntimeException("Invalid Cassandra DataType");
    }
  }

  @Override
  public boolean delete(Object key) {
    ArrayList<String> cassandraKeys = new ArrayList<>();
    ArrayList<Object> cassandraValues = new ArrayList<>();
    AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
    String cqlQuery = CassandraQueryFactory.getDeleteDataQuery(mapping, cassandraKeys);
    ResultSet resultSet = this.client.getSession().execute(cqlQuery, cassandraValues.toArray());
    return resultSet.wasApplied();
  }


  @Override
  public Result execute(DataStore dataStore, Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    String[] fields = query.getFields();
    if (fields != null) {
      fields = (String[]) ArrayUtils.addAll(fields, mapping.getAllKeys());
    } else {
      fields = mapping.getAllFieldsIncludingKeys();
    }
    CassandraResultSet<K, T> cassandraResult = new CassandraResultSet<>(dataStore, query);
    String cqlQuery = CassandraQueryFactory.getExecuteQuery(mapping, query, objectArrayList, fields);
    ResultSet results;
    if (objectArrayList.size() == 0) {
      results = client.getSession().execute(cqlQuery);
    } else {
      results = client.getSession().execute(cqlQuery, objectArrayList.toArray());
    }
    Iterator<Row> iterator = results.iterator();
    ColumnDefinitions definitions = results.getColumnDefinitions();
    T obj;
    K keyObject;
    CassandraKey cassandraKey = mapping.getCassandraKey();
    while (iterator.hasNext()) {
      Row row = iterator.next();
      obj = cassandraDataStore.newPersistent();
      keyObject = cassandraDataStore.newKey();
      populateValuesToPersistent(row, definitions, obj, fields);
      if (cassandraKey != null) {
        populateValuesToPersistent(row, definitions, (PersistentBase) keyObject, cassandraKey.getFieldNames());
      } else {
        Field key = mapping.getInlinedDefinedPartitionKey();
        keyObject = (K) getValue(row, definitions, key.getColumnName());
      }
      cassandraResult.addResultElement(keyObject, obj);
    }
    return cassandraResult;
  }

}
