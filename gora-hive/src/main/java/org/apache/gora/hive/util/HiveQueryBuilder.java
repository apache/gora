/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.hive.util;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.gora.hive.store.HiveMapping;
import org.apache.gora.hive.store.HiveStore;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.avro.AvroObjectInspectorGenerator;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is to build hive SQL queries for schema creation and inserting items into the hive
 * store
 */
public class HiveQueryBuilder {

  private static final Logger LOG = LoggerFactory.getLogger((MethodHandles.lookup().lookupClass()));

  private HiveStore hiveStore;
  private HiveMapping hiveMapping;

  /**
   * common characters for building queries
   **/
  private static final char QUESTION_SYMBOL = '?';
  private static final char QUOTE_SYMBOL = '\'';
  private static final char OPEN_BRACKET_SYMBOL = '(';
  private static final char CLOSE_BRACKET_SYMBOL = ')';
  private static final char COMMA_SYMBOL = ',';
  private static final char SPACE_SYMBOL = ' ';
  private static final char PERIOD_SYMBOL = '.';

  public HiveQueryBuilder(HiveStore hiveStore, HiveMapping hiveMapping) {
    this.hiveStore = hiveStore;
    this.hiveMapping = hiveMapping;
  }

  /**
   * Create hive sql query to create the schema table
   *
   * @param databaseName hive database name
   * @param fieldMap map of avro fields to their field names
   * @return hive sql query
   * @throws GoraException throw if the generation of the sql is failed
   */
  public String buildCreateQuery(String databaseName, Map<String, Field> fieldMap)
      throws GoraException {
    //Initiate create query
    StringBuilder createQuery = new StringBuilder();
    createQuery.append("create table if not exists ")
        .append(databaseName).append(PERIOD_SYMBOL)
        .append(hiveStore.getSchemaName()).append(OPEN_BRACKET_SYMBOL);
    //Create an Avro schema including fields only in mappings
    Schema avroSchema = createAvroSchema(fieldMap);
    try {
      buildColumnType(createQuery, avroSchema);
    } catch (SerDeException e) {
      throw new GoraException("Schema inspection has been failed.", e);
    }
    createQuery.append(CLOSE_BRACKET_SYMBOL);
    return createQuery.toString();
  }

  /**
   * Create hive query to insert persistent item into hive store
   *
   * @param key value of the key
   * @param value item to be stored
   * @param fieldMap schema fields mapping to their names
   * @param parameterList empty list to be filled with parameters of the sql
   * @return parameterized hive sql string.
   * @throws GoraException throw if the generation of the sql is failed
   */
  public String buildInsertQuery(Object key, PersistentBase value, Map<String, Field> fieldMap,
      List<Object> parameterList)
      throws GoraException {
    StringBuilder insert = new StringBuilder();
    StringBuilder fields = new StringBuilder();
    StringBuilder values = new StringBuilder();

    insert.append("insert into").append(SPACE_SYMBOL)
        .append(hiveStore.getSchemaName()).append(OPEN_BRACKET_SYMBOL);
    values.append("select").append(SPACE_SYMBOL);

    //add key value
    fields.append(HiveMapping.DEFAULT_KEY_NAME);
    parameterList.add((key instanceof String) ? key.toString() : key);
    values.append(QUESTION_SYMBOL);

    //add field values
    for (String fieldName : hiveMapping.getFields()) {
      Field field = fieldMap.get(fieldName);
      if (field == null) {
        LOG.warn("{} is skipped as it is not recognised as a field in the schema", fieldName);
        continue;
      }
      Object fieldValue = value.get(field.pos());
      if (value.isDirty(field.name()) && fieldValue != null) {
        Object serializedValue = serializeValue(parameterList, field.schema(), fieldValue);
        if (serializedValue != null && (!(serializedValue instanceof String)
            || !((String) serializedValue).isEmpty())) {
          fields.append(COMMA_SYMBOL).append(SPACE_SYMBOL)
              .append(field.name().toLowerCase(Locale.getDefault()));
          values.append(COMMA_SYMBOL).append(SPACE_SYMBOL)
              .append(serializedValue);
        }
      }
    }
    fields.append(CLOSE_BRACKET_SYMBOL).append(SPACE_SYMBOL);
    insert.append(fields).append(values);
    return insert.toString();
  }

  /**
   * Inspect avro schema and append its fields with their types to the create query.
   *
   * @param createQuery String builder to generate the create query
   * @param schema Avro schema to derive column names and types
   * @throws SerDeException Throw if schema inspection failed
   */
  private void buildColumnType(StringBuilder createQuery, Schema schema) throws SerDeException {
    AvroObjectInspectorGenerator generator = new AvroObjectInspectorGenerator(schema);
    List<TypeInfo> typeInfos = generator.getColumnTypes();
    List<String> names = generator.getColumnNames();
    for (int i = 0; i < names.size(); i++) {
      createQuery.append(names.get(i)).append(" ")
          .append(typeInfos.get(i).getTypeName());
      if (i < names.size() - 1) {
        createQuery.append(COMMA_SYMBOL).append(SPACE_SYMBOL);
      }
    }
  }

  /**
   * Create an avro schema including keys and fields of the mapping.
   *
   * @param fieldMap Avro field map which maps each field to its name
   * @return Genereated avro schema
   * @throws GoraException throws if the schema generation is failed
   */
  private Schema createAvroSchema(Map<String, Field> fieldMap) throws GoraException {
    Class persistentClass = hiveStore.getPersistentClass();
    Schema avroSchema = Schema.createRecord(persistentClass.getSimpleName(), null,
        persistentClass.getPackage().getName(), false);
    List<Field> avroFieldList = new ArrayList<>();

    avroFieldList.add(new Field(HiveMapping.DEFAULT_KEY_NAME, getKeySchema(), null, 1));

    List<String> fieldNames = hiveMapping.getFields();
    for (String fieldName : fieldNames) {
      Field field = fieldMap.get(fieldName);
      if (field == null) {
        throw new GoraException(
            "Could not find a avro field for field name : " + fieldName);
      }
      avroFieldList.add(new Field(field.name(), field.schema(), field.doc(), field.defaultVal()));
    }
    avroSchema.setFields(avroFieldList);
    return avroSchema;
  }

  /**
   * Derive the schema for the key field
   *
   * @return schema for the key field
   * @throws GoraException throw if no schema could identified for the key class
   */
  private Schema getKeySchema() throws GoraException {
    final String keyName = hiveStore.getKeyClass().getSimpleName().toUpperCase(Locale.getDefault());
    try {
      Type keyType = Type.valueOf(keyName);
      return Schema.create(keyType);
    } catch (IllegalArgumentException | AvroRuntimeException e) {
      throw new GoraException("Failed to derive schema for the key class name : " + keyName, e);
    }
  }

  /**
   * Serialize the given value according to its schema. if a primitive type has to be included as a
   * a value, it will be added to the parameter list
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema the value should be serialized based on the schema
   * @param value the value to be serialized
   * @return serialized value
   * @throws GoraException throw if serialization is failed.
   */
  private Object serializeValue(List<Object> parameterList, Schema schema, Object value)
      throws GoraException {
    if (value == null) {
      return getNullValue(parameterList, schema);
    }
    final Type type = schema.getType();
    switch (type) {
      case RECORD:
        return serializeRecord(parameterList, schema, value);
      case MAP:
        return serializeMap(parameterList, schema, value);
      case ARRAY:
        return serializeArray(parameterList, schema, value);
      case UNION:
        return serializeUnion(parameterList, schema, value);
      case STRING:
      case ENUM:
        parameterList.add(value.toString());
        return QUESTION_SYMBOL;
      case BYTES:
        return serializeBytes(parameterList, value);
      default:
        parameterList.add(value);
        return QUESTION_SYMBOL;
    }
  }

  /**
   * A record is handled as a struct in hive context and the sql query for structs are define as
   * named_struct(field1, value1, field2, value2,..). Moreover nested structs are not allowed to be
   * null and they have to be empty structs which all fields represent respective null values.
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema the record schema
   * @param value the record object
   * @return serialized value
   * @throws GoraException throw if serialization is failed.
   */
  private Object serializeRecord(List<Object> parameterList, Schema schema, Object value)
      throws GoraException {
    if (value == null) {
      return getNullValue(parameterList, schema);
    } else if (!(value instanceof PersistentBase)) {
      throw new GoraException("Record value is not a persistent object");
    }
    PersistentBase record = (PersistentBase) value;

    StringBuilder valueBuilder = new StringBuilder();
    valueBuilder.append("named_struct(");

    List<Field> fields = schema.getFields();
    for (int i = 0; i < fields.size(); i++) {
      Field field = fields.get(i);
      valueBuilder.append(QUOTE_SYMBOL).append(field.name()).append(QUOTE_SYMBOL)
          .append(COMMA_SYMBOL).append(SPACE_SYMBOL);
      valueBuilder.append(serializeValue(parameterList, field.schema(), record.get(field.pos())));
      if (i < fields.size() - 1) {
        valueBuilder.append(",");
      }
    }
    return valueBuilder.append(CLOSE_BRACKET_SYMBOL).toString();
  }

  /**
   * Serialize a given map by serializing its values based on schema.valueType. map values in the
   * sql have to be defined as map(key1, value1, key2, value2,..). If the map value is null or
   * empty, an empty map \"map(null, null_value_type)\" should be returned and it is required to
   * represent the null value of the respective value type in the first value entry. Ex : if the map
   * is binary type. the empty map will be map(null, binary(null))
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema the map schema
   * @param value the map object
   * @return serialized value
   * @throws GoraException throw if serialization is failed.
   */
  private Object serializeMap(List<Object> parameterList, Schema schema, Object value)
      throws GoraException {
    if (value == null) {
      return getNullValue(parameterList, schema);
    } else if (!(value instanceof Map)) {
      throw new GoraException("Value is not an object of java.util.Map for schema type MAP");
    }
    Map<?, ?> map = (Map<?, ?>) value;
    if (map.keySet().isEmpty()) {
      return getNullValue(parameterList, schema);
    }
    //create a map serializing all its entries
    StringBuilder valueBuilder = new StringBuilder();
    valueBuilder.append("map(");
    int size = map.keySet().size();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      valueBuilder.append(QUOTE_SYMBOL).append(entry.getKey().toString()).append(QUOTE_SYMBOL)
          .append(COMMA_SYMBOL).append(SPACE_SYMBOL)
          .append(serializeValue(parameterList, schema.getValueType(), entry.getValue()));
      if (size-- > 1) {
        valueBuilder.append(COMMA_SYMBOL).append(SPACE_SYMBOL);
      }
    }
    return valueBuilder.append(CLOSE_BRACKET_SYMBOL).toString();
  }

  /**
   * Serialize a given list by serializing its values based on schema.elementType. list values in
   * the sql have to be defined as array(value1, value2,..). If the list is null or empty, an empty
   * list \"array(null)\" should be returned and it is required to represent the null value of the
   * respective value type. Ex : if the array is binary type. the empty array will be
   * array(binary(null))
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema the array schema
   * @param value the list object
   * @return serialized value
   * @throws GoraException throw if serialization is failed.
   */
  private Object serializeArray(List<Object> parameterList, Schema schema, Object value)
      throws GoraException {
    if (!(value instanceof List) && value != null) {
      throw new GoraException("Value is not an object of java.util.List for schema type ARRAY");
    }

    List<?> list = (List<?>) value;
    if (list == null || list.isEmpty()) {
      return getNullValue(parameterList, schema);
    } else {
      StringBuilder valueBuilder = new StringBuilder();
      valueBuilder.append("array(");
      for (int i = 0; i < list.size(); i++) {
        valueBuilder.append(serializeValue(parameterList, schema.getElementType(), list.get(i)));
        if (i < list.size() - 1) {
          valueBuilder.append(", ");
        }
      }
      return valueBuilder.append(CLOSE_BRACKET_SYMBOL).toString();
    }
  }

  /**
   * Serialize a given object based on union types. If the union type has only two values and one of
   * them is null type, the schema is considered as a type of the not null type. However if there
   * are more than one not-null types, the value should be defined as create_union(position,
   * serialized_value). Ex : if the field is UNION<String, Float>, the query should be
   * "create_union(1, 'value')". the position is counted using only not-null types.
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema the union schema
   * @param value the value object
   * @return serialized value
   * @throws GoraException throw if serialization is failed.
   */
  private Object serializeUnion(List<Object> parameterList, Schema schema, Object value)
      throws GoraException {
    List<Schema> schemas = schema.getTypes();

    if (schemas.size() == 2 && isNullable(schema)) {
      return serializeValue(parameterList, getValidSchema(schema), value);
    }

    StringBuilder valueBuilder = new StringBuilder();
    int count = 1;
    for (Schema valueSchema : schemas) {
      if (!Type.NULL.equals(valueSchema.getType())) {
        if (isValidType(valueSchema.getType(), value)) {
          valueBuilder.append("create_union(")
              .append(count).append(COMMA_SYMBOL).append(SPACE_SYMBOL)
              .append(serializeValue(parameterList, valueSchema, value))
              .append(CLOSE_BRACKET_SYMBOL);
          return valueBuilder.toString();
        }
        count++;
      }
    }
    throw new GoraException("Serializing union value is failed");
  }

  /**
   * Serialize a given object into a binary. The sql string is defined as "binary(value)"
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param value the byte object
   * @return serialized value
   */
  private Object serializeBytes(List<Object> parameterList, Object value) {
    if (value instanceof ByteBuffer) {
      ByteBuffer clone = ByteBuffer.allocate(((ByteBuffer) value).capacity());
      clone.put((ByteBuffer) value);
      ((ByteBuffer) value).rewind();
      clone.flip();
      value = QUOTE_SYMBOL + Charset.defaultCharset().decode(clone).toString() + QUOTE_SYMBOL;
    }
    parameterList.add(value);
    return "binary(?)";
  }

  /**
   * Returns the first not-null sub-schema from a union schema
   *
   * @param schemas Union schema
   * @return first valid sub-schema
   * @throws GoraException throw if a valid schema is not found
   */
    static Schema getValidSchema(Schema schemas) throws GoraException {
    for (Schema innerSchema : schemas.getTypes()) {
      if (!Type.NULL.equals(innerSchema.getType())) {
        return innerSchema;
      }
    }
    throw new GoraException("Could not find a valid schema");
  }

  /**
   * Generate the null value for a given schema type
   *
   * @param parameterList carries the list of parameters to be injected into sql
   * @param schema schema to get null type
   * @return null value for the schema.type
   * @throws GoraException throw if the null value generation is failed
   */
  private Object getNullValue(List<Object> parameterList, Schema schema) throws GoraException {
    final Type type = schema.getType();
    switch (type) {
      case BYTES:
        return "binary(null)";
      case MAP:
        return "map(null," + getNullValue(parameterList, schema.getValueType())
            + CLOSE_BRACKET_SYMBOL;
      case ARRAY:
        return "array(" + getNullValue(parameterList, schema.getElementType())
            + CLOSE_BRACKET_SYMBOL;
      case UNION:
        return serializeUnion(parameterList, schema, null);
      case RECORD:
        Class<?> clazz;
        try {
          clazz = ClassLoadingUtils.loadClass(schema.getFullName());
        } catch (ClassNotFoundException e) {
          throw new GoraException(e);
        }
        @SuppressWarnings("unchecked") final PersistentBase emptyRecord = (PersistentBase) new BeanFactoryImpl(
            hiveStore.getKeyClass(), clazz).newPersistent();
        return serializeRecord(parameterList, schema, emptyRecord);
      default:
        return null;
    }
  }

  /**
   * Compare the object type and the required schema type and return true if the object type is
   * compatible with the schema type. if the type matches non of the defined types, the object is
   * considered to be  a valid object for any type
   *
   * @param type schema type to be compared
   * @param value object to be compared
   * @return true if object is compatible with the schema type
   */
  private boolean isValidType(Type type, Object value) {
    switch (type) {
      case INT:
        return (value instanceof Integer);
      case LONG:
        return (value instanceof Long);
      case BYTES:
        return (value instanceof Byte[]) || (value instanceof ByteBuffer);
      case NULL:
        return (value == null);
      case STRING:
        return (value instanceof CharSequence);
      case ENUM:
        return (value instanceof Enum);
      case DOUBLE:
        return (value instanceof Double);
      case FLOAT:
        return (value instanceof Float);
      case BOOLEAN:
        return (value instanceof Boolean);
      case MAP:
        return (value instanceof Map);
      case ARRAY:
        return (value instanceof List) || (value != null && value.getClass().isArray());
      case RECORD:
        return (value instanceof PersistentBase);
      default:
        return true;
    }
  }

  /**
   * Check whether the given union schema contains any nullable sub-schemas
   *
   * @param schemas union schema
   * @return true if the list of sub-schemas contain a nullable schema
   */
  static boolean isNullable(Schema schemas) {
    for (Schema innerSchema : schemas.getTypes()) {
      if (innerSchema.getType().equals(Type.NULL)) {
        return true;
      }
    }
    return false;
  }
}
