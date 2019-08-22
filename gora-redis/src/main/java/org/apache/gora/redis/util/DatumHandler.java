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
package org.apache.gora.redis.util;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.IOUtils;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for serialization and deserialization of values from redis.
 */
public class DatumHandler<T extends PersistentBase> {

  public static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ConcurrentHashMap<Schema, SpecificDatumReader<?>> readerMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Schema, SpecificDatumWriter<?>> writerMap = new ConcurrentHashMap<>();

  public DatumHandler() {
  }

  /**
   * Serialize an object
   *
   * @param fieldSchema The avro schema to be used.
   * @param fieldValue The object to be serialized.
   * @return Serialized object.
   */
  @SuppressWarnings("unchecked")
  public Object serializeFieldValue(Schema fieldSchema, Object fieldValue) {
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
          output = serializeFieldValue(unionSchema, fieldValue);
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

  /**
   * Serialize an object as a Map
   *
   * @param fieldSchema The avro schema to be used.
   * @param fieldValue The object to be serialized.
   * @return Serialized object as a map.
   */
  @SuppressWarnings("unchecked")
  public Map<Object, Object> serializeFieldMap(Schema fieldSchema, Object fieldValue) {
    Map<Object, Object> map = new HashMap();
    switch (fieldSchema.getType()) {
      case UNION:
        for (Schema sc : fieldSchema.getTypes()) {
          if (sc.getType() == Schema.Type.MAP) {
            map = serializeFieldMap(sc, fieldValue);
          }
        }
        break;
      case MAP:
        Map<CharSequence, ?> mp = (Map<CharSequence, ?>) fieldValue;
        for (Entry<CharSequence, ?> e : mp.entrySet()) {
          String mapKey = e.getKey().toString();
          Object mapValue = e.getValue();
          mapValue = serializeFieldValue(fieldSchema.getValueType(), mapValue);
          map.put(mapKey, mapValue);
        }
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return map;
  }

  /**
   * Serialize an object as a List
   *
   * @param fieldSchema The avro schema to be used.
   * @param fieldValue The object to be serialized.
   * @return Serialized object as a List.
   */
  @SuppressWarnings("unchecked")
  public List<Object> serializeFieldList(Schema fieldSchema, Object fieldValue) {
    List<Object> serializedList = new ArrayList();
    switch (fieldSchema.getType()) {
      case ARRAY:
        List<?> rawdataList = (List<?>) fieldValue;
        rawdataList.stream().map((lsValue) -> serializeFieldValue(fieldSchema.getElementType(), lsValue)).forEachOrdered((lsValue_) -> {
          serializedList.add(lsValue_);
        });
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return serializedList;
  }

  /**
   * Deserialize an object into a gora bean using avro
   *
   * @param field The field schema.
   * @param fieldSchema The object schema.
   * @param redisValue Object from redis.
   * @param persistent Persistent object
   * @return Deserialized object
   * @throws java.io.IOException Deserialization exception
   */
  @SuppressWarnings("unchecked")
  public Object deserializeFieldValue(Schema.Field field, Schema fieldSchema,
      Object redisValue, T persistent) throws IOException {
    Object fieldValue = null;
    switch (fieldSchema.getType()) {
      case MAP:
      case ARRAY:
      case RECORD:
        @SuppressWarnings("rawtypes") SpecificDatumReader reader = getDatumReader(fieldSchema);
        fieldValue = IOUtils.deserialize((byte[]) redisValue, reader,
            persistent.get(field.pos()));
        break;
      case ENUM:
        fieldValue = AvroUtils.getEnumValue(fieldSchema, redisValue.toString());
        break;
      case FIXED:
        break;
      case BYTES:
        fieldValue = ByteBuffer.wrap((byte[]) redisValue);
        break;
      case STRING:
        fieldValue = new Utf8(redisValue.toString());
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(redisValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          fieldValue = deserializeFieldValue(field, unionSchema, redisValue, persistent);
        } else {
          reader = getDatumReader(fieldSchema);
          fieldValue = IOUtils.deserialize((byte[]) redisValue, reader,
              persistent.get(field.pos()));
        }
        break;
      default:
        fieldValue = redisValue;
    }
    return fieldValue;
  }

  /**
   * Deserialize an Map into a gora bean using avro
   *
   * @param field The field schema.
   * @param fieldSchema The object schema.
   * @param redisMap Map from redis.
   * @param persistent Persistent object
   * @return Deserialized object
   * @throws java.io.IOException Deserialization exception
   */
  @SuppressWarnings("unchecked")
  public Object deserializeFieldMap(Schema.Field field, Schema fieldSchema,
      RMap<Object, Object> redisMap, T persistent) throws IOException {
    Map<Utf8, Object> fieldValue = new HashMap<>();
    switch (fieldSchema.getType()) {
      case UNION:
        for (Schema sc : fieldSchema.getTypes()) {
          if (sc.getType() == Schema.Type.MAP) {
            return deserializeFieldMap(field, sc, redisMap, persistent);
          }
        }
        break;
      case MAP:
        for (Entry<Object, Object> aEntry : redisMap.entrySet()) {
          String key = aEntry.getKey().toString();
          Object value = deserializeFieldValue(field, fieldSchema.getValueType(), aEntry.getValue(), persistent);
          fieldValue.put(new Utf8(key), value);
        }
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return new DirtyMapWrapper<>(fieldValue);
  }

  /**
   * Deserialize an List into a gora bean using avro
   *
   * @param field The field schema.
   * @param fieldSchema The object schema.
   * @param redisList List from redis.
   * @param persistent Persistent object
   * @return Deserialized object
   * @throws java.io.IOException Deserialization exception
   */
  @SuppressWarnings("unchecked")
  public Object deserializeFieldList(Schema.Field field, Schema fieldSchema,
      RList<Object> redisList, T persistent) throws IOException {
    List<Object> fieldValue = new ArrayList<>();
    switch (fieldSchema.getType()) {
      case ARRAY:
        for (Object ob : redisList) {
          Object value = deserializeFieldValue(field, fieldSchema.getElementType(), ob, persistent);
          fieldValue.add(value);
        }
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return new DirtyListWrapper<>(fieldValue);
  }

  /**
   * Gets the Datum reader for a Schema
   *
   * @param fieldSchema The avro schema to be used
   * @return SpecificDatumReader for the schema
   */
  @SuppressWarnings("rawtypes")
  private SpecificDatumReader getDatumReader(Schema fieldSchema) {
    SpecificDatumReader<?> reader = readerMap.get(fieldSchema);
    if (reader == null) {
      reader = new SpecificDatumReader(fieldSchema);
      SpecificDatumReader localReader;
      if ((localReader = readerMap.putIfAbsent(fieldSchema, reader)) != null) {
        reader = localReader;
      }
    }
    return reader;
  }

  /**
   * Gets the Datum writer for a Schema
   *
   * @param fieldSchema The avro schema to be used
   * @return SpecificDatumWriter for the schema
   */
  @SuppressWarnings("rawtypes")
  private SpecificDatumWriter getDatumWriter(Schema fieldSchema) {
    SpecificDatumWriter writer = writerMap.get(fieldSchema);
    if (writer == null) {
      writer = new SpecificDatumWriter(fieldSchema);
      writerMap.put(fieldSchema, writer);
    }
    return writer;
  }

  /**
   * Verify if a schema is Nullable
   *
   * @param unionSchema The schema to be verified
   * @return result
   */
  private boolean isNullable(Schema unionSchema) {
    if (unionSchema.getTypes().stream().anyMatch((innerSchema) -> (innerSchema.getType().equals(Schema.Type.NULL)))) {
      return true;
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

}
