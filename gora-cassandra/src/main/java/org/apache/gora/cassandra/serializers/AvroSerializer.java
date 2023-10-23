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

import com.datastax.driver.core.AbstractGettableData;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.commons.lang3.ArrayUtils;
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
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class contains the operations relates to Avro Serialization.
 */
class AvroSerializer<K, T extends PersistentBase> extends CassandraSerializer {


  private static final Logger LOG = LoggerFactory.getLogger(AvroSerializer.class);

  private DataStore<K, T> cassandraDataStore;

  private Schema persistentSchema;

  AvroSerializer(CassandraClient cassandraClient, DataStore<K, T> dataStore, CassandraMapping mapping) throws GoraException {
    super(cassandraClient, dataStore.getKeyClass(), dataStore.getPersistentClass(), mapping);
    if (PersistentBase.class.isAssignableFrom(dataStore.getPersistentClass())) {
      persistentSchema = ((PersistentBase) dataStore.getBeanFactory().getCachedPersistent()).getSchema();
    } else {
      throw new GoraException("Unsupported persistent class, couldn't able to find the Avro schema.");
    }
    this.cassandraDataStore = dataStore;
    try {
      analyzePersistent();
    } catch (Exception e) {
      throw new GoraException("Error occurred while analyzing the persistent class, :" + e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws Exception
   */
  protected void analyzePersistent() throws Exception {
    userDefineTypeMaps = new HashMap<>();
    for (Field field : mapping.getFieldList()) {
      String fieldType = field.getType();
      if (fieldType.contains("frozen")) {
        String udtType = fieldType.substring(fieldType.indexOf("<") + 1, fieldType.indexOf(">"));
        if (PersistentBase.class.isAssignableFrom(persistentClass)) {
          Schema fieldSchema = persistentSchema.getField(field.getFieldName()).schema();
          if (fieldSchema.getType().equals(Schema.Type.UNION)) {
            for (Schema currentSchema : fieldSchema.getTypes()) {
              if (currentSchema.getType().equals(Schema.Type.RECORD)) {
                fieldSchema = currentSchema;
                break;
              }
            }
          }
          String createQuery = CassandraQueryFactory.getCreateUDTTypeForAvro(mapping, udtType, fieldSchema);
          userDefineTypeMaps.put(udtType, createQuery);
        } else {
          throw new RuntimeException("Unsupported Class for User Define Types, Please use PersistentBase class. field : " + udtType);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param query
   * @return
   */
  @Override
  public boolean updateByQuery(Query query) {
    List<Object> objectArrayList = new ArrayList<>();
    String cqlQuery = CassandraQueryFactory.getUpdateByQueryForAvro(mapping, query, objectArrayList, persistentSchema);
    ResultSet results;
    SimpleStatement statement;
    if (objectArrayList.size() == 0) {
      statement = new SimpleStatement(cqlQuery);
    } else {
      statement = new SimpleStatement(cqlQuery, objectArrayList.toArray());
    }
    if (writeConsistencyLevel != null) {
      statement.setConsistencyLevel(ConsistencyLevel.valueOf(writeConsistencyLevel));
    }
    results = client.getSession().execute(statement);
    return results.wasApplied();
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @param fields
   * @return
   */
  @Override
  public Persistent get(Object key, String[] fields) throws GoraException {
    try {
      if (fields == null) {
        fields = getFields();
      }
      ArrayList<String> cassandraKeys = new ArrayList<>();
      ArrayList<Object> cassandraValues = new ArrayList<>();
      AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
      String cqlQuery = CassandraQueryFactory.getSelectObjectWithFieldsQuery(mapping, fields, cassandraKeys);
      SimpleStatement statement = new SimpleStatement(cqlQuery, cassandraValues.toArray());
      if (readConsistencyLevel != null) {
        statement.setConsistencyLevel(ConsistencyLevel.valueOf(readConsistencyLevel));
      }
      ResultSet resultSet = this.client.getSession().execute(statement);
      Iterator<Row> iterator = resultSet.iterator();
      ColumnDefinitions definitions = resultSet.getColumnDefinitions();
      T obj = null;
      if (iterator.hasNext()) {
        obj = cassandraDataStore.newPersistent();
        AbstractGettableData row = (AbstractGettableData) iterator.next();
        populateValuesToPersistent(row, definitions, obj, fields);
      }
      return obj;
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @param persistent
   */
  @Override
  public void put(Object key, Persistent persistent) throws GoraException {
    try {
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
              String fieldType = field.getType();
              if (fieldType.contains("frozen")) {
                fieldType = fieldType.substring(fieldType.indexOf("<") + 1, fieldType.indexOf(">"));
                UserType userType = client.getSession().getCluster().getMetadata().getKeyspace(mapping.getKeySpace().getName()).getUserType(fieldType);
                UDTValue udtValue = userType.newValue();
                Schema udtSchema = f.schema();
                if (udtSchema.getType().equals(Schema.Type.UNION)) {
                  for (Schema schema : udtSchema.getTypes()) {
                    if (schema.getType().equals(Schema.Type.RECORD)) {
                      udtSchema = schema;
                      break;
                    }
                  }
                }
                PersistentBase udtObjectBase = (PersistentBase) value;
                for (Schema.Field udtField : udtSchema.getFields()) {
                  Object udtFieldValue = AvroCassandraUtils.getFieldValueFromAvroBean(udtField.schema(), udtField.schema().getType(), udtObjectBase.get(udtField.name()), field);
                  if (udtField.schema().getType().equals(Schema.Type.MAP)) {
                    udtValue.setMap(udtField.name(), (Map) udtFieldValue);
                  } else if (udtField.schema().getType().equals(Schema.Type.ARRAY)) {
                    udtValue.setList(udtField.name(), (List) udtFieldValue);
                  } else {
                    udtValue.set(udtField.name(), udtFieldValue, (Class) udtFieldValue.getClass());
                  }
                }
                value = udtValue;
              } else {
                value = AvroCassandraUtils.getFieldValueFromAvroBean(f.schema(), f.schema().getType(), value, field);
              }
              values.add(value);
              fields.add(fieldName);
            }
          }
          String cqlQuery = CassandraQueryFactory.getInsertDataQuery(mapping, fields);
          SimpleStatement statement = new SimpleStatement(cqlQuery, values.toArray());
          if (writeConsistencyLevel != null) {
            statement.setConsistencyLevel(ConsistencyLevel.valueOf(writeConsistencyLevel));
          }
          client.getSession().execute(statement);
        } else {
          LOG.info("Ignored putting persistent bean {} in the store as it is neither "
                  + "new, neither dirty.", new Object[]{persistent});
        }
      } else {
        LOG.error("{} Persistent bean isn't extended by {} .", new Object[]{this.persistentClass, PersistentBase.class});
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key
   * @return
   */
  @Override
  public Persistent get(Object key) throws GoraException {
    try {
      ArrayList<String> cassandraKeys = new ArrayList<>();
      ArrayList<Object> cassandraValues = new ArrayList<>();
      AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
      String cqlQuery = CassandraQueryFactory.getSelectObjectQuery(mapping, cassandraKeys);
      SimpleStatement statement = new SimpleStatement(cqlQuery, cassandraValues.toArray());
      if (readConsistencyLevel != null) {
        statement.setConsistencyLevel(ConsistencyLevel.valueOf(readConsistencyLevel));
      }
      ResultSet resultSet = client.getSession().execute(statement);
      Iterator<Row> iterator = resultSet.iterator();
      ColumnDefinitions definitions = resultSet.getColumnDefinitions();
      T obj = null;
      if (iterator.hasNext()) {
        obj = cassandraDataStore.newPersistent();
        AbstractGettableData row = (AbstractGettableData) iterator.next();
        populateValuesToPersistent(row, definitions, obj, mapping.getFieldNames());
      }
      return obj;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * This method wraps result set data in to DataEntry and creates a list of DataEntry.
   **/
  private void populateValuesToPersistent(AbstractGettableData row, ColumnDefinitions columnDefinitions, PersistentBase base, String[] fields) {
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
      paramValue = getValue(row, columnDefinitions.getType(columnName), columnName, fieldSchema);
      Object value = AvroCassandraUtils.getAvroFieldValue(paramValue, fieldSchema);
      base.put(avroField.pos(), value);
    }
  }

  private Object getValue(AbstractGettableData row, DataType columnType, String columnName, Schema schema) {
    Object paramValue;
    String dataType;
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
        dataType = columnType.getTypeArguments().get(0).toString();
        paramValue = row.isNull(columnName) ? null : row.getList(columnName, AvroCassandraUtils.getRelevantClassForCassandraDataType(dataType));
        break;
      case SET:
        dataType = columnType.getTypeArguments().get(0).toString();
        paramValue = row.isNull(columnName) ? null : row.getList(columnName, AvroCassandraUtils.getRelevantClassForCassandraDataType(dataType));
        break;
      case MAP:
        dataType = columnType.getTypeArguments().get(1).toString();
        // Avro supports only String for keys
        paramValue = row.isNull(columnName) ? null : row.getMap(columnName, String.class, AvroCassandraUtils.getRelevantClassForCassandraDataType(dataType));
        break;
      case UDT:
        paramValue = row.isNull(columnName) ? null : row.getUDTValue(columnName);
        if (paramValue != null) {
          try {
            PersistentBase udtObject = (PersistentBase) SpecificData.newInstance(Class.forName(schema.getFullName()), schema);
            for (Schema.Field f : udtObject.getSchema().getFields()) {
              DataType dType = ((UDTValue) paramValue).getType().getFieldType(f.name());
              Object fieldValue = getValue((UDTValue) paramValue, dType, f.name(), f.schema());
              udtObject.put(f.pos(), fieldValue);
            }
            paramValue = udtObject;
          } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error occurred while populating data to " + schema.getFullName() + " : " + e.getMessage());
          }
        }
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

  /**
   * {@inheritDoc}
   *
   * @param key
   * @return
   */
  @Override
  public boolean delete(Object key) throws GoraException {
    ArrayList<String> cassandraKeys = new ArrayList<>();
    ArrayList<Object> cassandraValues = new ArrayList<>();
    try {
      AvroCassandraUtils.processKeys(mapping, key, cassandraKeys, cassandraValues);
      String cqlQuery = CassandraQueryFactory.getDeleteDataQuery(mapping, cassandraKeys);
      SimpleStatement statement = new SimpleStatement(cqlQuery, cassandraValues.toArray());
      if (writeConsistencyLevel != null) {
        statement.setConsistencyLevel(ConsistencyLevel.valueOf(writeConsistencyLevel));
      }
      ResultSet resultSet = client.getSession().execute(statement);
      return resultSet.wasApplied();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param dataStore
   * @param query
   * @return
   */
  @Override
  public Result execute(DataStore dataStore, Query query) throws GoraException {
    try {
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
      SimpleStatement statement;
      if (objectArrayList.size() == 0) {
        statement = new SimpleStatement(cqlQuery);
      } else {
        statement = new SimpleStatement(cqlQuery, objectArrayList.toArray());
      }
      if (readConsistencyLevel != null) {
        statement.setConsistencyLevel(ConsistencyLevel.valueOf(readConsistencyLevel));
      }
      results = client.getSession().execute(statement);
      Iterator<Row> iterator = results.iterator();
      ColumnDefinitions definitions = results.getColumnDefinitions();
      T obj;
      K keyObject;
      CassandraKey cassandraKey = mapping.getCassandraKey();
      while (iterator.hasNext()) {
        AbstractGettableData row = (AbstractGettableData) iterator.next();
        obj = cassandraDataStore.newPersistent();
        keyObject = cassandraDataStore.newKey();
        populateValuesToPersistent(row, definitions, obj, fields);
        if (cassandraKey != null) {
          populateValuesToPersistent(row, definitions, (PersistentBase) keyObject, cassandraKey.getFieldNames());
        } else {
          Field key = mapping.getInlinedDefinedPartitionKey();
          keyObject = (K) getValue(row, definitions.getType(key.getColumnName()), key.getColumnName(), null);
        }
        cassandraResult.addResultElement(keyObject, obj);
      }
      return cassandraResult;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

}
