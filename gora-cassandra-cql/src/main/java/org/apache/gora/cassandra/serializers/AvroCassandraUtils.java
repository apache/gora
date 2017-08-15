/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.serializers;

import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.hbase.util.HBaseByteInterface;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class is Utils class for Avro serialization.
 */
class AvroCassandraUtils {

  /**
   * Default schema index with value "0" used when AVRO Union data types are stored.
   */
  private static final int DEFAULT_UNION_SCHEMA = 0;

  private static final Logger LOG = LoggerFactory.getLogger(AvroCassandraUtils.class);

  static void processKeys(CassandraMapping cassandraMapping, Object key, List<String> keys, List<Object> values) {
    CassandraKey cassandraKey = cassandraMapping.getCassandraKey();
    if (cassandraKey != null) {
      if (key instanceof PersistentBase) {
        PersistentBase keyBase = (PersistentBase) key;
        for (Schema.Field field : keyBase.getSchema().getFields()) {
          Field mappedField = cassandraKey.getFieldFromFieldName(field.name());
          if (mappedField != null) {
            keys.add(field.name());
            Object value = keyBase.get(field.pos());
            value = getFieldValueFromAvroBean(field.schema(), field.schema().getType(), value, mappedField);
            values.add(value);
          } else {
            LOG.debug("Ignoring field {}, Since field couldn't find in the {} mapping", new Object[]{field.name(), cassandraMapping.getPersistentClass()});
          }
        }
      } else {
        LOG.error("Key bean isn't extended by {} .", new Object[]{cassandraMapping.getKeyClass(), PersistentBase.class});
      }
    } else {
      keys.add(cassandraMapping.getInlinedDefinedPartitionKey().getFieldName());
      values.add(key);
    }
  }

  /**
   * For every field within an object, we pass in a field schema, Type and value.
   * This enables us to process fields (based on their characteristics)
   * preparing them for persistence.
   *
   * @param fieldSchema the associated field schema
   * @param type        the field type
   * @param fieldValue  the field value.
   * @return field value
   */
  static Object getFieldValueFromAvroBean(Schema fieldSchema, Schema.Type type, Object fieldValue, Field field) {
    switch (type) {
      // Record can be persist with two ways, udt and bytes
      case RECORD:
        if (field.getType().contains("blob")) {
          try {
            byte[] serializedBytes = HBaseByteInterface.toBytes(fieldValue, fieldSchema);
            fieldValue = ByteBuffer.wrap(serializedBytes);
          } catch (IOException e) {
            LOG.error("Error occurred when serializing {} field. {}", new Object[]{field.getFieldName(), e.getMessage()});
          }
        } else {
          throw new RuntimeException("Unsupported Data Type for Record, Currently Supported Data Types are blob and UDT for Records");
        }
        break;
      case MAP:
        Schema valueSchema = fieldSchema.getValueType();
        Schema.Type valuetype = valueSchema.getType();
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<CharSequence, ?> e : ((Map<CharSequence, ?>) fieldValue).entrySet()) {
          String mapKey = e.getKey().toString();
          Object mapValue = e.getValue();
          mapValue = getFieldValueFromAvroBean(valueSchema, valuetype, mapValue, field);
          map.put(mapKey, mapValue);
        }
        fieldValue = map;
        break;
      case ARRAY:
        valueSchema = fieldSchema.getElementType();
        valuetype = valueSchema.getType();
        ArrayList<Object> list = new ArrayList<>();
        for (Object item : (Collection<?>) fieldValue) {
          Object value = getFieldValueFromAvroBean(valueSchema, valuetype, item, field);
          list.add(value);
        }
        fieldValue = list;
        break;
      case UNION:
        // storing the union selected schema, the actual value will
        // be stored as soon as we get break out.
        if (fieldValue != null) {
          int schemaPos = getUnionSchema(fieldValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          Schema.Type unionType = unionSchema.getType();
          fieldValue = getFieldValueFromAvroBean(unionSchema, unionType, fieldValue, field);
        }
        break;
      case STRING:
        fieldValue = fieldValue.toString();
        break;
      default:
        break;
    }
    return fieldValue;
  }

  /**
   * Given an object and the object schema this function obtains,
   * from within the UNION schema, the position of the type used.
   * If no data type can be inferred then we return a default value
   * of position 0.
   *
   * @param pValue       Object
   * @param pUnionSchema avro Schema
   * @return the unionSchemaPosition.
   */
  private static int getUnionSchema(Object pValue, Schema pUnionSchema) {
    int unionSchemaPos = 0;
//    String valueType = pValue.getClass().getSimpleName();
    for (Schema currentSchema : pUnionSchema.getTypes()) {
      Schema.Type schemaType = currentSchema.getType();
      if (pValue instanceof CharSequence && schemaType.equals(Schema.Type.STRING))
        return unionSchemaPos;
      else if (pValue instanceof ByteBuffer && schemaType.equals(Schema.Type.BYTES))
        return unionSchemaPos;
      else if (pValue instanceof Integer && schemaType.equals(Schema.Type.INT))
        return unionSchemaPos;
      else if (pValue instanceof Long && schemaType.equals(Schema.Type.LONG))
        return unionSchemaPos;
      else if (pValue instanceof Double && schemaType.equals(Schema.Type.DOUBLE))
        return unionSchemaPos;
      else if (pValue instanceof Float && schemaType.equals(Schema.Type.FLOAT))
        return unionSchemaPos;
      else if (pValue instanceof Boolean && schemaType.equals(Schema.Type.BOOLEAN))
        return unionSchemaPos;
      else if (pValue instanceof Map && schemaType.equals(Schema.Type.MAP))
        return unionSchemaPos;
      else if (pValue instanceof List && schemaType.equals(Schema.Type.ARRAY))
        return unionSchemaPos;
      else if (pValue instanceof Persistent && schemaType.equals(Schema.Type.RECORD))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.STRING))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.INT))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.LONG))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.DOUBLE))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.FLOAT))
        return unionSchemaPos;
      else if (pValue != null && ByteBuffer.class.isAssignableFrom(pValue.getClass()) && schemaType.equals(Schema.Type.RECORD))
        return unionSchemaPos;
      unionSchemaPos++;
    }
    // if we weren't able to determine which data type it is, then we return the default
    return DEFAULT_UNION_SCHEMA;
  }

  static Object getAvroFieldValue(Object value, Schema schema) {
    Object result;
    switch (schema.getType()) {

      case MAP:
        Map<String, Object> rawMap = (Map<String, Object>) value;
        Map<Utf8, Object> utf8ObjectHashMap = new HashMap<>();
        if (rawMap == null) {
          result = new DirtyMapWrapper(utf8ObjectHashMap);
          break;
        }
        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
          Schema innerSchema = schema.getValueType();
          Object obj = getAvroFieldValue(e.getValue(), innerSchema);
          if (e.getKey().getClass().getSimpleName().equalsIgnoreCase("Utf8")) {
            utf8ObjectHashMap.put((Utf8) e.getKey(), obj);
          } else {
            utf8ObjectHashMap.put(new Utf8((String) e.getKey()), obj);
          }
        }
        result = new DirtyMapWrapper<>(utf8ObjectHashMap);
        break;

      case ARRAY:
        List<Object> rawList = (List<Object>) value;
        List<Object> objectArrayList = new ArrayList<>();
        if (rawList == null) {
          return new DirtyListWrapper(objectArrayList);
        }
        for (Object item : rawList) {
          Object obj = getAvroFieldValue(item, schema.getElementType());
          objectArrayList.add(obj);
        }
        result = new DirtyListWrapper<>(objectArrayList);
        break;

      case RECORD:
        if (value != null && ByteBuffer.class.isAssignableFrom(value.getClass())) {
          ByteBuffer buffer = (ByteBuffer) value;
          byte[] arr = new byte[buffer.remaining()];
          buffer.get(arr);
          try {
            result = (PersistentBase) HBaseByteInterface.fromBytes(schema, arr);
          } catch (IOException e) {
            LOG.error("Error occurred while deserialize the Record. :" + e.getMessage());
            result = null;
          }
        } else {
          result = (PersistentBase) value;
        }
        break;

      case UNION:
        int index = getUnionSchema(value, schema);
        Schema resolvedSchema = schema.getTypes().get(index);
        result = getAvroFieldValue(value, resolvedSchema);
        break;

      case ENUM:
        result = org.apache.gora.util.AvroUtils.getEnumValue(schema, (String) value);
        break;

      case BYTES:
        if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = value;
        } else {
          result = ByteBuffer.wrap((byte[]) value);
        }
        break;

      case STRING:
        if (value instanceof org.apache.avro.util.Utf8) {
          result = value;
        } else if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = new Utf8(((ByteBuffer) value).array());
        } else {
          result = new Utf8((String) value);
        }
        break;

      case INT:
        if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = ((ByteBuffer) value).getInt();
        } else {
          result = value;
        }
        break;

      case FLOAT:
        if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = ((ByteBuffer) value).getFloat();
        } else {
          result = value;
        }
        break;

      case DOUBLE:
        if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = ((ByteBuffer) value).getDouble();
        } else {
          result = value;
        }
        break;

      case LONG:
        if (ByteBuffer.class.isAssignableFrom(value.getClass())) {
          result = ((ByteBuffer) value).getLong();
        } else {
          result = value;
        }
        break;

      default:
        result = value;
    }
    return result;
  }

  static Class getRelevantClassForCassandraDataType(String dataType) {
    switch (dataType) {
      case "ascii":
      case "text":
      case "varchar":
        return String.class;
      case "blob":
        return ByteBuffer.class;
      case "int":
        return Integer.class;
      case "double":
        return Double.class;
      case "bigint":
      case "counter":
        return Long.class;
      case "decimal":
        return BigDecimal.class;
      case "float":
        return Float.class;
      case "boolean":
        return Boolean.class;
      case "inet":
        return InetAddress.class;
      case "varint":
        return BigInteger.class;
      case "uuid":
        return UUID.class;
      case "timestamp":
        return Date.class;
      default:
        throw new RuntimeException("Invalid Cassandra DataType");
    }
  }

}
