/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.aerospike.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.util.Utf8;
import org.apache.gora.aerospike.query.AerospikeQuery;
import org.apache.gora.aerospike.query.AerospikeQueryResult;
import org.apache.gora.aerospike.query.AerospikeResultRecord;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

/**
 * Implementation of a Aerospike data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class AerospikeStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(AerospikeStore.class);

  private static final String PARSE_MAPPING_FILE_KEY = "gora.aerospike.mapping.file";

  private static final String DEFAULT_MAPPING_FILE = "gora-aerospike-mapping.xml";

  private AerospikeClient aerospikeClient;

  private AerospikeParameters aerospikeParameters;

  /**
   * {@inheritDoc}
   * In initializing the aerospike datastore, read the mapping file, sets the basic
   * aerospike specific parameters and creates the client with the user defined policies
   *
   * @param keyClass        key class
   * @param persistentClass persistent class
   * @param properties      properties
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);

    try {
      AerospikeMappingBuilder aerospikeMappingBuilder = new AerospikeMappingBuilder();
      aerospikeMappingBuilder
              .readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE), keyClass,
                      persistentClass);
      aerospikeParameters = new AerospikeParameters(aerospikeMappingBuilder.getAerospikeMapping(),
              properties, getConf());
      ClientPolicy policy = new ClientPolicy();
      policy.writePolicyDefault = aerospikeParameters.getAerospikeMapping().getWritePolicy();
      policy.readPolicyDefault = aerospikeParameters.getAerospikeMapping().getReadPolicy();
  
      // 'SendKey' property is enabled by default as the key is needed in query execution
      policy.readPolicyDefault.sendKey = true;
      policy.writePolicyDefault.sendKey = true;
  
      // Set the credentials for servers with restricted access
      if (aerospikeParameters.getUsername() != null) {
        policy.user = aerospikeParameters.getUsername();
      }
      if (aerospikeParameters.getPassword() != null) {
        policy.password = aerospikeParameters.getPassword();
      }
  
      aerospikeClient = new AerospikeClient(policy, aerospikeParameters.getHost(),
              aerospikeParameters.getPort());
      aerospikeParameters.setServerSpecificParameters(aerospikeClient);
      aerospikeParameters.validateServerBinConfiguration(persistentClass.getFields());
      LOG.info("Aerospike Gora datastore initialized successfully.");
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Aerospike, being a schemaless database does not support explicit schema creation through the
   * provided java client. When the records are added to the database, the schema is created on
   * the fly. Thus, schema related functionality is unavailable in gora-aerospike module.
   *
   * @return null
   */
  @Override
  public String getSchemaName() {
    return null;
  }

  /**
   * Aerospike, being a schemaless database does not support explicit schema creation through the
   * provided java client. When the records are added to the database, the schema is created on
   * the fly. Thus, schema creation functionality is unavailable in gora-aerospike module.
   */
  @Override
  public void createSchema() throws GoraException {
  }

  /**
   * Aerospike, being a schemaless database does not support explicit schema creation through the
   * provided java client. When the records are added to the database, the schema is created on
   * the fly. Thus, schema deletion functionality is unavailable in gora-aerospike module.
   */
  @Override
  public void deleteSchema() throws GoraException {
  }

  /**
   * Aerospike, being a schemaless database does not support explicit schema creation through the
   * provided java client. When the records are added to the database, the schema is created on
   * the fly. Thus, schema exists functionality is unavailable in gora-aerospike module.
   */
  @Override
  public boolean schemaExists() throws GoraException {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @param key    the key of the object
   * @param fields the fields required in the object. Pass null, to retrieve all fields
   * @return the Object corresponding to the key or null if it cannot be found
   */
  @Override
  public T get(K key, String[] fields) throws GoraException {

    try {
      Key recordKey = getAerospikeKey(key);
      fields = getFieldsToQuery(fields);
  
      Record record = aerospikeClient
              .get(aerospikeParameters.getAerospikeMapping().getReadPolicy(), recordKey, fields);
      
      if (record == null) {
        return null;
      }
      return createPersistentInstance(record, fields);
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean exists(K key) throws GoraException {
    try {
      Key recordKey = getAerospikeKey(key);
      return aerospikeClient.exists(aerospikeParameters.getAerospikeMapping().getReadPolicy(), recordKey);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Method to insert the persistent objects with the given key to the aerospike database server.
   * In writing the records, the policy defined in the mapping file is used to decide on the
   * behaviour of transaction handling.
   *
   * @param key        key of the object
   * @param persistent object to be persisted
   */
  @Override
  public void put(K key, T persistent) throws GoraException {

    try {
      Key recordKey = getAerospikeKey(key);
  
      List<Field> fields = persistent.getSchema().getFields();
  
      for (int i = 0; i < fields.size(); i++) {
        if (!persistent.isDirty(i)) {
          continue;
        }
        Object persistentValue = persistent.get(i);
  
        String mappingBinName = aerospikeParameters.getAerospikeMapping().getBinMapping()
                .get(fields.get(i).name());
        if (mappingBinName == null) {
          LOG.error("Aerospike mapping for field {}#{} not found. Wrong gora-aerospike-mapping.xml?",
                  persistent.getClass().getName(), fields.get(i).name());
          throw new RuntimeException(
                  "Aerospike mapping for field [" + persistent.getClass().getName() + "#" + fields
                          .get(i).name() + "] not found. Wrong gora-aerospike-mapping.xml?");
        }
        Bin bin;
        if (persistentValue != null) {
          bin = new Bin(mappingBinName,
                  getSerializableValue(persistentValue, fields.get(i).schema()));
        } else {
          bin = Bin.asNull(mappingBinName);
        }
        aerospikeClient
                .put(aerospikeParameters.getAerospikeMapping().getWritePolicy(), recordKey, bin);
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key of the object
   * @return whether the object was successfully deleted
   */
  @Override
  public boolean delete(K key) throws GoraException {
    try {
      Key recordKey = getAerospikeKey(key);
      return aerospikeClient
              .delete(aerospikeParameters.getAerospikeMapping().getWritePolicy(), recordKey);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param query matching records to this query will be deleted
   * @return the number of deleted records
   */
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    int deleteCount = 0;
    try {
      Result<K, T> result = query.execute();
      while (result.next()) {
        if (aerospikeClient.delete(null, getAerospikeKey(result.getKey()))) {
          deleteCount++;
        }
      }
      return deleteCount;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param query the query to execute.
   * @return the query result
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {

    List<AerospikeResultRecord> resultRecords = new ArrayList<>();
    String namespace = aerospikeParameters.getAerospikeMapping().getNamespace();
    String set = aerospikeParameters.getAerospikeMapping().getSet();

    try {
      // Query execution without any keys
      if (query.getStartKey() == null && query.getEndKey() == null) {
  
        try (RecordSet recordSet = aerospikeClient.query(null, getStatement(namespace, set))) {
          while (recordSet.next()) {
            AerospikeResultRecord aerospikeRecord = new AerospikeResultRecord(recordSet.getKey(),
                    recordSet.getRecord());
            resultRecords.add(aerospikeRecord);
          }
        }
      }
  
      // Query execution for single key
      else if (query.getKey() != null) {
        Key key = getAerospikeKey(query.getKey());
        Record record = aerospikeClient.get(null, key);
        if (record != null) {
          resultRecords.add(new AerospikeResultRecord(key, record));
        }
      }
  
      // Query execution for key ranges
      // ToDo: Implement Query execution for key ranges
      //    else if (query.getStartKey() != null && query.getEndKey() != null) {
      //
      //      // the key range filtering at the gora side, which is not a better solution
      //      String lowerBound  = query.getStartKey().toString();
      //      String upperBound  = query.getEndKey().toString();
      //
      //      try (RecordSet recordSet = aerospikeClient.query(null, getStatement(namespace, set))) {
      //        while (recordSet.next()) {
      //          Key key = recordSet.getKey();
      //          Record record = recordSet.getRecord();
      //
      //          String input = key.userKey.toString();
      //          boolean isSpecifiedRange =  input.compareToIgnoreCase(lowerBound) >= 0 && input
      //                  .compareToIgnoreCase(upperBound) <= 0;
      //
      //          if (isSpecifiedRange) {
      //            AerospikeResultRecord aerospikeRecord = new AerospikeResultRecord(key, record);
      //            resultRecords.add(aerospikeRecord);
      //          }
      //
      //        }
      //      }
      //    }
      return new AerospikeQueryResult<>(this, query, resultRecords, getFieldsToQuery(null));
    } catch (Exception e) {
      throw new GoraException(e) ;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return the new query corresponding to aerospike
   */
  @Override
  public Query<K, T> newQuery() {
    return new AerospikeQuery<>(this);
  }

  /**
   * {@inheritDoc}
   * As the Aerospike does not support query key ranges as at the moment, only the single partition
   * is retrieved with this method.
   *
   * @param query the query to execute.
   * @return the list of partitions, one partion at the list as at the moment
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query);
    partitionQuery.setConf(getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  @Override
  public void flush() throws GoraException {
  }

  /**
   * Method to create a Aerospike specific statement
   *
   * @param namespace the namespace
   * @param set       the set
   * @return the statement
   */
  private Statement getStatement(String namespace, String set) {
    Statement stmt = new Statement();
    stmt.setNamespace(namespace);
    stmt.setSetName(set);
    return stmt;
  }

  /**
   * Method to close aerospike client connections to database server nodes
   */
  @Override
  public void close() {
    aerospikeClient.close();
    LOG.info("Aerospike Gora datastore destroyed successfully.");
  }

  /**
   * Method to get the aerospike key from the provided K
   *
   * @param key persistent key
   * @return aerospike key for the record
   */
  private Key getAerospikeKey(K key) {
    Value keyValue;
    if (keyClass.getSimpleName().equalsIgnoreCase("string")) {
      keyValue = Value.get(key.toString());
    } else {
      keyValue = Value.get(key);
    }

    return new Key(aerospikeParameters.getAerospikeMapping().getNamespace(),
            aerospikeParameters.getAerospikeMapping().getSet(), keyValue);
  }

  /**
   * Method to get the value serializable in database from the Avro persistent object
   *
   * @param object persistent object
   * @param schema schema of the persistent object
   * @return serializable value
   */
  private Value getSerializableValue(Object object, Schema schema) {

    Value value = null;
    switch (schema.getType()) {
      case UNION:
        if (object != null) {
          int schemaPos = getUnionSchema(object, schema);
          Schema unionSchema = schema.getTypes().get(schemaPos);
          value = getSerializableValue(object, unionSchema);
        }
        break;
      case STRING:
        value = Value.get(object.toString());
        break;
      case BYTES:
        value = Value.get(((ByteBuffer) object).array());
        break;
      case MAP:
        Map<Object, Object> newMap = new HashMap<>();
        Map<?, ?> fieldMap = (Map<?, ?>) object;

        for (Map.Entry<?, ?> entry : fieldMap.entrySet()) {
          newMap.put(entry.getKey().toString(),
                  getSerializableValue(fieldMap.get(entry.getKey()), schema.getValueType()));
        }
        value = Value.get(newMap);
        break;
      case ARRAY:
        List<Object> objectList = new ArrayList<>();
        for (Object obj : (List<Object>) object) {
          objectList.add(getSerializableValue(obj, schema.getElementType()));
        }
        value = Value.get(objectList);
        break;
      default:
        value = Value.get(object);
        break;
    }
    return value;
  }

  /**
   * Method to create a persistent object given the retrieved record
   * from Aerospike database
   *
   * @param record record retrieved from database
   * @param fields fields
   * @return persistent object created
   * @throws GoraException 
   */
  public T createPersistentInstance(Record record, String[] fields) throws GoraException {

    T persistent = newPersistent();
    for (String field : fields) {
      setPersistentField(field, record, persistent);
    }
    persistent.clearDirty();
    return persistent;
  }

  /**
   * Method to set a field in the persistent object
   *
   * @param fieldName  field name
   * @param record     record retrieved from database
   * @param persistent persistent object for the field to be set
   */
  private void setPersistentField(String fieldName, Record record, T persistent) {

    String binName = aerospikeParameters.getAerospikeMapping().getBinName(fieldName);
    if (binName == null) {
      LOG.error("Aerospike mapping for field {} not found. Wrong gora-aerospike-mapping.xml",
              fieldName);
      throw new RuntimeException("Aerospike mapping for field [" + fieldName + "] not found. "
              + "Wrong gora-aerospike-mapping.xml?");
    }
    if (record.bins.get(fieldName) == null) {
      return;
    }
    String binDataType = record.bins.get(fieldName).getClass().getSimpleName();
    Object binValue = record.bins.get(binName);

    persistent.put(fieldName,
            getDeserializedObject(binValue, binDataType, fieldMap.get(fieldName).schema()));
  }

  /**
   * Method to get Avro mapped persistent object from the record retrieved from the database
   *
   * @param binValue    value retrieved from database
   * @param binDataType data type of the database value
   * @param schema      corresponding schema in the persistent class
   * @return persistent object
   */
  private Object getDeserializedObject(Object binValue, String binDataType, Schema schema) {

    Object result;
    switch (schema.getType()) {

      case MAP:
        Map<String, Object> rawMap = (Map<String, Object>) binValue;
        Map<Utf8, Object> deserializableMap = new HashMap<>();
        if (rawMap == null) {
          result = new DirtyMapWrapper(deserializableMap);
          break;
        }
        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
          Schema innerSchema = schema.getValueType();
          Object obj = getDeserializedObject(e.getValue(), e.getValue().getClass().getSimpleName(),
                  innerSchema);
          if (e.getKey().getClass().getSimpleName().equalsIgnoreCase("Utf8")) {
            deserializableMap.put((Utf8) e.getKey(), obj);
          } else {
            deserializableMap.put(new Utf8((String) e.getKey()), obj);
          }
        }
        result = new DirtyMapWrapper<>(deserializableMap);
        break;

      case ARRAY:
        List<Object> rawList = (List<Object>) binValue;
        List<Object> deserializableList = new ArrayList<>();
        if (rawList == null) {
          return new DirtyListWrapper(deserializableList);
        }
        for (Object item : rawList) {
          Object obj = getDeserializedObject(item, item.getClass().getSimpleName(),
                  schema.getElementType());
          deserializableList.add(obj);
        }
        result = new DirtyListWrapper<>(deserializableList);
        break;

      case RECORD:
        result = (PersistentBase) binValue;
        break;

      case UNION:
        int index = getUnionSchema(binValue, schema);
        Schema resolvedSchema = schema.getTypes().get(index);
        result = getDeserializedObject(binValue, binDataType, resolvedSchema);
        break;

      case ENUM:
        result = AvroUtils.getEnumValue(schema, (String) binValue);
        break;

      case BYTES:
        result = ByteBuffer.wrap((byte[]) binValue);
        break;

      case STRING:
        if (binValue instanceof org.apache.avro.util.Utf8)
          result = binValue;
        else
          result = new Utf8((String) binValue);
        break;

      case INT:
        if (binDataType.equalsIgnoreCase("long")) {
          result = Math.toIntExact((Long) binValue);
        } else {
          result = binValue;
        }
        break;

      default:
        result = binValue;
    }
    return result;
  }

  /**
   * Method to retrieve the corresponding schema type index of a particular object having UNION
   * schema. As UNION type can have one or more types and at a given instance, it holds an object
   * of only one type of the defined types, this method is used to figure out the corresponding
   * instance's
   * schema type index.
   *
   * @param instanceValue value that the object holds
   * @param unionSchema   union schema containing all of the data types
   * @return the unionSchemaPosition corresponding schema position
   */
  private int getUnionSchema(Object instanceValue, Schema unionSchema) {
    int unionSchemaPos = 0;
    for (Schema currentSchema : unionSchema.getTypes()) {
      Schema.Type schemaType = currentSchema.getType();
      if (instanceValue instanceof CharSequence && schemaType.equals(Schema.Type.STRING)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof ByteBuffer && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Integer && schemaType.equals(Schema.Type.INT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Long && schemaType.equals(Schema.Type.LONG)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Double && schemaType.equals(Schema.Type.DOUBLE)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Float && schemaType.equals(Schema.Type.FLOAT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Boolean && schemaType.equals(Schema.Type.BOOLEAN)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Map && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof List && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Persistent && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      unionSchemaPos++;
    }
    return 0;
  }
}
