/**
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
package org.apache.gora.ignite.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.ignite.utils.IgniteSQLBuilder;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Ignite data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class IgniteStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(IgniteStore.class);
  private static final String PARSE_MAPPING_FILE_KEY = "gora.ignite.mapping.file";
  private static final String DEFAULT_MAPPING_FILE = "gora-ignite-mapping.xml";
  private IgniteParameters igniteParameters;
  private IgniteMapping igniteMapping;
  private Connection connection;

  /*
   * Create a threadlocal map for the datum readers and writers, because they
   * are not thread safe, at least not before Avro 1.4.0 (See AVRO-650). When
   * they are thread safe, it is possible to maintain a single reader and writer
   * pair for every schema, instead of one for every thread.
   */
  public static final ConcurrentHashMap<Schema, SpecificDatumReader<?>> readerMap = new ConcurrentHashMap<>();

  public static final ConcurrentHashMap<Schema, SpecificDatumWriter<?>> writerMap = new ConcurrentHashMap<>();

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {

    try {
      super.initialize(keyClass, persistentClass, properties);
      IgniteMappingBuilder builder = new IgniteMappingBuilder(this);
      builder.readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      igniteMapping = builder.getIgniteMapping();
      igniteParameters = IgniteParameters.load(properties, conf);
      connection = acquiereConnection();
      LOG.info("Ignite store was successfully initialized");
    } catch (ClassNotFoundException | SQLException ex) {
      LOG.error("Error while initializing Ignite store", ex);
      throw new GoraException(ex);
    }
  }

  private Connection acquiereConnection() throws ClassNotFoundException, SQLException {
    Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("jdbc:ignite:thin://");
    urlBuilder.append(igniteParameters.getHost());
    if (igniteParameters.getPort() != null) {
      urlBuilder.append(":" + igniteParameters.getPort());
    }
    if (igniteParameters.getSchema() != null) {
      urlBuilder.append("/" + igniteParameters.getSchema());
    }
    if (igniteParameters.getUser() != null) {
      urlBuilder.append(";" + igniteParameters.getUser());
    }
    if (igniteParameters.getPassword() != null) {
      urlBuilder.append(";" + igniteParameters.getPassword());
    }
    if (igniteParameters.getAdditionalConfigurations() != null) {
      urlBuilder.append(igniteParameters.getAdditionalConfigurations());
    }
    Connection conn = DriverManager.getConnection(urlBuilder.toString());
    return conn;
  }

  @Override
  public String getSchemaName() {
    return igniteMapping.getTableName();
  }

  @Override
  public String getSchemaName(final String mappingSchemaName,
      final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  @Override
  public void createSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
          "Impossible to create the schema as no connection has been initiated.");
    }
    if (schemaExists()) {
      return;
    }
    try (Statement stmt = connection.createStatement()) {
      String createTableSQL = IgniteSQLBuilder.createTable(igniteMapping);
      stmt.executeUpdate(createTableSQL);
      LOG.info("Table {} has been created for Ignite instance.",
          igniteMapping.getTableName());
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
          "Impossible to delete the schema as no connection has been initiated.");
    }
    try (Statement stmt = connection.createStatement()) {
      String dropTableSQL = IgniteSQLBuilder.dropTable(igniteMapping.getTableName());
      stmt.executeUpdate(dropTableSQL);
      LOG.info("Table {} has been dropped from Ignite instance.",
          igniteMapping.getTableName());
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    boolean exists = false;
    try (Statement stmt = connection.createStatement()) {
      String tableExistsSQL = IgniteSQLBuilder.tableExists(igniteMapping.getTableName());
      ResultSet executeQuery = stmt.executeQuery(tableExistsSQL);
      executeQuery.close();
      exists = true;
    } catch (SQLException ex) {
      /**
       * a 42000 error code is thrown by Ignite when a non-existent table
       * queried. More details:
       * https://apacheignite-sql.readme.io/docs/jdbc-error-codes
       */
      if (ex.getSQLState() != null && ex.getSQLState().equals("42000")) {
        exists = false;
      } else {
        throw new GoraException(ex);
      }
    }
    return exists;
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    String[] avFields = getFieldsToQuery(fields);
    Object[] keyl = null;
    if (igniteMapping.getPrimaryKey().size() == 1) {
      keyl = new Object[]{key};
    } else {
      //Composite key pending
    }
    //Avro fields to Ignite fields
    List<String> dbFields = new ArrayList<>();
    for (String af : avFields) {
      dbFields.add(igniteMapping.getFields().get(af).getName());
    }
    String selectQuery = IgniteSQLBuilder.selectGet(igniteMapping, dbFields);
    try (PreparedStatement stmt = connection.prepareStatement(selectQuery)) {
      IgniteSQLBuilder.fillSelectStatement(stmt, igniteMapping, keyl);
      ResultSet rs = stmt.executeQuery();
      boolean data = rs.next();
      T resp = null;
      if (data) {
        resp = newInstance(rs, fields);
        if (rs.next()) {
          LOG.warn("Multiple results for primary key {} in the schema {}, ignoring additional rows.", keyl, igniteMapping.getTableName());
        }
      }
      rs.close();
      return resp;
    } catch (SQLException | IOException ex) {
      throw new GoraException(ex);
    }

  }

  public T newInstance(ResultSet rs, String[] fields) throws GoraException, SQLException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      String dbField = igniteMapping.getFields().get(f).getName();
      Object sv = rs.getObject(dbField);
      if (sv == null) {
        continue;
      }
      Object v = deserializeFieldValue(field, fieldSchema, sv, persistent);
      persistent.put(field.pos(), v);
      persistent.setDirty(field.pos());
    }
    return persistent;
  }

  private Object deserializeFieldValue(Schema.Field field, Schema fieldSchema,
      Object igniteValue, T persistent) throws IOException {
    Object fieldValue = null;
    switch (fieldSchema.getType()) {
      case MAP:
      case ARRAY:
      case RECORD:
        @SuppressWarnings("rawtypes") SpecificDatumReader reader = getDatumReader(fieldSchema);
        fieldValue = IOUtils.deserialize((byte[]) igniteValue, reader,
            persistent.get(field.pos()));
        break;
      case ENUM:
        fieldValue = AvroUtils.getEnumValue(fieldSchema, igniteValue.toString());
        break;
      case FIXED:
        break;
      case BYTES:
        fieldValue = ByteBuffer.wrap((byte[]) igniteValue);
        break;
      case STRING:
        fieldValue = new Utf8(igniteValue.toString());
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(igniteValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          fieldValue = deserializeFieldValue(field, unionSchema, igniteValue, persistent);
        } else {
          reader = getDatumReader(fieldSchema);
          fieldValue = IOUtils.deserialize((byte[]) igniteValue, reader,
              persistent.get(field.pos()));
        }
        break;
      default:
        fieldValue = igniteValue;
    }
    return fieldValue;

  }

  @Override
  public void put(K key, T obj) throws GoraException {
    try {
      if (obj.isDirty()) {
        Schema schema = obj.getSchema();
        List<Schema.Field> fields = schema.getFields();
        Map<Column, Object> data = new HashMap<>();
        if (igniteMapping.getPrimaryKey().size() == 1) {
          Column getKey = igniteMapping.getPrimaryKey().get(0);
          data.put(getKey, key);
        } else {
          //Composite keys pending..
        }
        for (Schema.Field field : fields) {
          Column get = igniteMapping.getFields().get(field.name());
          Object v = obj.get(field.pos());
          if (get != null && v != null) {
            Schema fieldSchema = field.schema();
            Object serializedObj = serializeFieldValue(get, fieldSchema, v);
            data.put(get, serializedObj);
          }
        }
        String baseInsertStatement = IgniteSQLBuilder.baseInsertStatement(igniteMapping, data);
        try (PreparedStatement stmt = connection.prepareStatement(baseInsertStatement)) {
          IgniteSQLBuilder.fillInsertStatement(stmt, data);
          stmt.executeUpdate();
        } catch (SQLException ex) {
          throw new GoraException(ex);
        }
      } else {
        LOG.info("Ignored putting object {} in the store as it is neither "
            + "new, neither dirty.", new Object[]{obj});
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean delete(K key) throws GoraException {
    String deleteQuery = null;
    Object[] keyl = null;
    if (igniteMapping.getPrimaryKey().size() == 1) {
      deleteQuery = IgniteSQLBuilder.delete(igniteMapping);
      keyl = new Object[]{key};
    } else {
      //Composite key pending
    }
    try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
      IgniteSQLBuilder.fillDeleteStatement(stmt, igniteMapping, keyl);
      stmt.executeUpdate();
      return true;
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Query<K, T> newQuery() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void flush() throws GoraException {
    //Auto-commit mode by default
  }

  @Override
  public void close() {
    try {
      connection.close();
      LOG.info("Ignite datastore destroyed successfully.");
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

  private Object serializeFieldValue(Column get, Schema fieldSchema, Object fieldValue) {
    Object output = fieldValue;
    switch (fieldSchema.getType()) {
      case ARRAY:
      case MAP:
      case RECORD:
        byte[] data = null;
        try {
          @SuppressWarnings("rawtypes")
          SpecificDatumWriter writer = getDatumWriter(fieldSchema);
          data = IOUtils.serialize(writer, fieldValue);
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
        output = data;
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(fieldValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          output = serializeFieldValue(get, unionSchema, fieldValue);
        } else {
          data = null;
          try {
            @SuppressWarnings("rawtypes")
            SpecificDatumWriter writer = getDatumWriter(fieldSchema);
            data = IOUtils.serialize(writer, fieldValue);
          } catch (IOException e) {
            LOG.error(e.getMessage(), e);
          }
          output = data;
        }
        break;
      case FIXED:
        break;
      case ENUM:
      case STRING:
        output = fieldValue.toString();
        break;
      case BYTES:
        output = ((ByteBuffer) fieldValue).array();
        break;
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
        output = fieldValue;
        break;
      case NULL:
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());

    }
    return output;
  }

  private boolean isNullable(Schema unionSchema) {
    for (Schema innerSchema : unionSchema.getTypes()) {
      if (innerSchema.getType().equals(Schema.Type.NULL)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method to retrieve the corresponding schema type index of a particular
   * object having UNION schema. As UNION type can have one or more types and at
   * a given instance, it holds an object of only one type of the defined types,
   * this method is used to figure out the corresponding instance's schema type
   * index.
   *
   * @param instanceValue value that the object holds
   * @param unionSchema union schema containing all of the data types
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
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      unionSchemaPos++;
    }
    return 0;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumReader getDatumReader(Schema fieldSchema) {
    SpecificDatumReader<?> reader = readerMap.get(fieldSchema);
    if (reader == null) {
      reader = new SpecificDatumReader(fieldSchema);// ignore dirty bits
      SpecificDatumReader localReader = null;
      if ((localReader = readerMap.putIfAbsent(fieldSchema, reader)) != null) {
        reader = localReader;
      }
    }
    return reader;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumWriter getDatumWriter(Schema fieldSchema) {
    SpecificDatumWriter writer = writerMap.get(fieldSchema);
    if (writer == null) {
      writer = new SpecificDatumWriter(fieldSchema);// ignore dirty bits
      writerMap.put(fieldSchema, writer);
    }

    return writer;
  }

}
