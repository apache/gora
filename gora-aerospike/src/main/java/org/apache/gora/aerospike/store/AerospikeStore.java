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
import java.util.List;
import java.util.Properties;

import com.aerospike.client.*;
import com.aerospike.client.policy.ClientPolicy;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    super.initialize(keyClass, persistentClass, properties);

    AerospikeMappingBuilder aerospikeMappingBuilder = new AerospikeMappingBuilder();
    aerospikeMappingBuilder
            .readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE), keyClass,
                    persistentClass);
    aerospikeParameters = new AerospikeParameters(aerospikeMappingBuilder.getAerospikeMapping(),
            properties);
    ClientPolicy policy = new ClientPolicy();
    policy.writePolicyDefault = aerospikeParameters.getAerospikeMapping().getWritePolicy();
    policy.readPolicyDefault = aerospikeParameters.getAerospikeMapping().getReadPolicy();

    aerospikeClient = new AerospikeClient(aerospikeParameters.getHost(),
            aerospikeParameters.getPort());
    aerospikeParameters.setServerSpecificParameters(aerospikeClient);
    aerospikeParameters.validateServerBinConfiguration(persistentClass.getFields());
    LOG.info("Aerospike Gora datastore initialized successfully.");
  }

  @Override
  public String getSchemaName() {
    return null;
  }

  @Override
  public void createSchema() {
  }

  @Override
  public void deleteSchema() {
  }

  @Override
  public boolean schemaExists() {
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
  public T get(K key, String[] fields) {
    fields = getFieldsToQuery(fields);
    Key recordKey = new Key(aerospikeParameters.getAerospikeMapping().getNamespace(),
            aerospikeParameters.getAerospikeMapping().getSet(), Value.get(key));
    Record record = aerospikeClient
            .get(aerospikeParameters.getAerospikeMapping().getReadPolicy(), recordKey, fields);
    if (record == null) {
      return null;
    }
    return createPersistentInstance(record, fields);
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
  public void put(K key, T persistent) {

    Key recordKey = new Key(aerospikeParameters.getAerospikeMapping().getNamespace(),
            aerospikeParameters.getAerospikeMapping().getSet(), Value.get(key));

    List<Field> fields = persistent.getSchema().getFields();
    for (int i = 0; i < fields.size(); i++) {
      String mappingBinName = aerospikeParameters.getAerospikeMapping().getBinMapping()
              .get(fields.get(i).name());
      if (mappingBinName == null) {
        LOG.error("Aerospike mapping for field {}#{} not found. Wrong gora-aerospike-mapping.xml?",
                persistent.getClass().getName(), fields.get(i).name());
        throw new RuntimeException(
                "Aerospike mapping for field [" + persistent.getClass().getName() + "#" + fields
                        .get(i).name() + "] not found. Wrong gora-aerospike-mapping.xml?");
      }
      Bin bin = getBin(mappingBinName, persistent.get(i), fields.get(i));
      aerospikeClient
              .put(aerospikeParameters.getAerospikeMapping().getWritePolicy(), recordKey, bin);
    }
  }

  @Override
  public boolean delete(K key) {
    return true;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    return null;
  }

  @Override
  public Query<K, T> newQuery() {
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    return null;
  }

  @Override
  public void flush() {
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
   * Aerospike does not support Utf8 format returned from Avro. This method provides those utf8
   * valued bin (column) values as strings for aerospike Value to obtain the corresponding bin
   * value, and returns the Bin (column in RDBMS)
   *
   * @param binName name of the bin
   * @param value   value of the bin
   * @param field   field corresponding to bin
   * @return Bin
   */
  private Bin getBin(String binName, Object value, Field field) {

    boolean isStringType = false;
    if (field.schema().getType().equals(Schema.Type.STRING))
      isStringType = true;
    if (field.schema().getType().equals(Schema.Type.UNION)) {
      for (Schema schema : field.schema().getTypes()) {
        if (schema.getName().equals("string"))
          isStringType = true;
      }
    }

    if (isStringType)
      return new Bin(binName, Value.get(value.toString()));
    else
      return new Bin(binName, Value.get(value));
  }

  /**
   * Method to create a persistent object given the retrieved record
   * from Aerospike database
   *
   * @param record record retrieved from database
   * @param fields fields
   * @return persistent object created
   */
  private T createPersistentInstance(Record record, String[] fields) {

    T persistent = newPersistent();
    for (String field : fields) {
      setPersistentField(field, record, persistent);
    }
    return persistent;
  }

  /**
   * Method to set a field in the persistent object
   *
   * @param field      field name
   * @param record     record retrieved from database
   * @param persistent persistent object for the field to be set
   */
  private void setPersistentField(String field, Record record, T persistent) {

    String binName = aerospikeParameters.getAerospikeMapping().getBinName(field);
    if (binName == null) {
      LOG.error("Aerospike mapping for field {} not found. Wrong gora-aerospike-mapping.xml",
              field);
      throw new RuntimeException("Aerospike mapping for field [" + field + "] not found. "
              + "Wrong gora-aerospike-mapping.xml?");
    }
    Schema.Type fieldDataType = fieldMap.get(field).schema().getType();
    String binDataType = record.bins.get(field).getClass().getSimpleName();
    Object binValue = record.bins.get(binName);

    if (fieldDataType.toString().equalsIgnoreCase(binDataType)) {
      persistent.put(field, record.bins.get(binName));
    } else {
      switch (fieldDataType) {
        case UNION:
          Schema unionSchema = fieldMap.get(field).schema();
          if (unionSchema.getTypes().size() == 2) {
            Schema.Type type0 = unionSchema.getTypes().get(0).getType();
            Schema.Type type1 = unionSchema.getTypes().get(1).getType();

            if ((type0.equals(Schema.Type.NULL)) || (type1.equals(Schema.Type.NULL))) {
              persistent.put(field, binValue);
            }
          }
          break;
        case INT:
          if (binDataType.equalsIgnoreCase("long")) {
            persistent.put(field, Math.toIntExact((Long) binValue));
          }
          break;
        default:
          break;
      }
    }
  }
}
