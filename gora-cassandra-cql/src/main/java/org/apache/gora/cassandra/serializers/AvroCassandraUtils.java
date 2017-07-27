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
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.gora.cassandra.bean.CassandraKey;
import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    if (cassandraMapping.isPartitionKeyDefined()) {
      if (cassandraKey != null) {
        if (key instanceof PersistentBase) {
          PersistentBase keyBase = (PersistentBase) key;
          for (Schema.Field field : keyBase.getSchema().getFields()) {
            if (cassandraMapping.getFieldFromFieldName(field.name()) != null) {
              keys.add(field.name());
              Object value = keyBase.get(field.pos());
              value = getFieldValueFromAvroBean(field.schema(), field.schema().getType(), value);
              values.add(value);
            } else {
              LOG.debug("Ignoring field {}, Since field couldn't find in the {} mapping", new Object[]{field.name(), cassandraMapping.getPersistentClass()});
            }
          }
        } else {
          LOG.error("Key bean isn't extended by {} .", new Object[]{cassandraMapping.getKeyClass(), PersistentBase.class});
        }
      } else {
        for (Field field : cassandraMapping.getInlinedDefinedPartitionKeys()) {
          keys.add(field.getFieldName());
          values.add(key);
        }
      }
    } else {
      keys.add(cassandraMapping.getDefaultCassandraKey().getFieldName());
      values.add(key.toString());
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
  static Object getFieldValueFromAvroBean(Schema fieldSchema, Schema.Type type, Object fieldValue) {
    switch (type) {
      case RECORD:
        PersistentBase persistent = (PersistentBase) fieldValue;
        PersistentBase newRecord = (PersistentBase) SpecificData.get().newRecord(persistent, persistent.getSchema());
        for (Schema.Field member : fieldSchema.getFields()) {
          if (member.pos() == 0 || !persistent.isDirty()) {
            continue;
          }
          Schema memberSchema = member.schema();
          Schema.Type memberType = memberSchema.getType();
          Object memberValue = persistent.get(member.pos());
          newRecord.put(member.pos(), getFieldValueFromAvroBean(memberSchema, memberType, memberValue));
        }
        fieldValue = newRecord;
        break;
      case MAP:
        Schema valueSchema = fieldSchema.getValueType();
        Schema.Type valuetype = valueSchema.getType();
        HashMap<String, Object> map = new HashMap<>();
        for (Map.Entry<CharSequence, ?> e : ((Map<CharSequence, ?>) fieldValue).entrySet()) {
          String mapKey = e.getKey().toString();
          Object mapValue = e.getValue();
          mapValue = getFieldValueFromAvroBean(valueSchema, valuetype, mapValue);
          map.put(mapKey, mapValue);
        }
        fieldValue = map;
        break;
      case ARRAY:
        valueSchema = fieldSchema.getElementType();
        valuetype = valueSchema.getType();
        ArrayList<Object> list = new ArrayList<>();
        for (Object item : (Collection<?>) fieldValue) {
          Object value = getFieldValueFromAvroBean(valueSchema, valuetype, item);
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
          fieldValue = getFieldValueFromAvroBean(unionSchema, unionType, fieldValue);
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
   * @param pValue Object
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
      unionSchemaPos++;
    }
    // if we weren't able to determine which data type it is, then we return the default
    return DEFAULT_UNION_SCHEMA;
  }

  static String encodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace(".", "\u00B7")
            .replace(":", "\u00FF")
            .replace(";", "\u00FE")
            .replace(" ", "\u00FD")
            .replace("%", "\u00FC")
            .replace("=", "\u00FB");
  }

  static String decodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace("\u00B7", ".")
            .replace("\u00FF", ":")
            .replace("\u00FE", ";")
            .replace("\u00FD", " ")
            .replace("\u00FC", "%")
            .replace("\u00FB", "=");
  }

  static Object getAvroFieldValue(Object value, Schema schema) {
    Object result;
    switch (schema.getType()) {

      case MAP:
        Map<String, Object> rawMap = (Map<String, Object>) value;
        Map<Utf8, Object> deserializableMap = new HashMap<>();
        if (rawMap == null) {
          result = new DirtyMapWrapper(deserializableMap);
          break;
        }
        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
          Schema innerSchema = schema.getValueType();
          Object obj = getAvroFieldValue(e.getValue(), innerSchema);
          if (e.getKey().getClass().getSimpleName().equalsIgnoreCase("Utf8")) {
            deserializableMap.put((Utf8) e.getKey(), obj);
          } else {
            deserializableMap.put(new Utf8((String) e.getKey()), obj);
          }
        }
        result = new DirtyMapWrapper<>(deserializableMap);
        break;

      case ARRAY:
        List<Object> rawList = (List<Object>) value;
        List<Object> deserializableList = new ArrayList<>();
        if (rawList == null) {
          return new DirtyListWrapper(deserializableList);
        }
        for (Object item : rawList) {
          Object obj = getAvroFieldValue(item, schema.getElementType());
          deserializableList.add(obj);
        }
        result = new DirtyListWrapper<>(deserializableList);
        break;

      case RECORD:
        result = (PersistentBase) value;
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
        } else {
          result = new Utf8((String) value);
        }
        break;

      case INT:
        result = value;
        break;

      default:
        result = value;
    }
    return result;
  }

}
