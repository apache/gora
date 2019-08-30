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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.util.Utf8;
import org.apache.gora.hive.store.HiveStore;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class parses a given result set into a persistent schema object
 */
public class HiveResultParser {

  private static final Logger LOG = LoggerFactory.getLogger((MethodHandles.lookup().lookupClass()));

  private HiveStore<?, ?> hiveStore;

  public HiveResultParser(HiveStore<?, ?> hiveStore) {
    this.hiveStore = hiveStore;
  }

  /**
   * Parsing a value based on given schema
   * @param schema schema value to be used
   * @param value value to be parsed
   * @return parsed object value
   * @throws GoraException throw if parsing the object is failed
   */
  public Object parseSchema(Schema schema, Object value) throws GoraException {
    if (value == null) {
      return null;
    }
    final Type type = schema.getType();
    switch (type) {
      case INT:
        return (value instanceof String) ? Integer.parseInt(String.valueOf(value)) : value;
      case STRING:
        return new Utf8(String.valueOf(value));
      case ENUM:
        return AvroUtils.getEnumValue(schema, String.valueOf(value));
      case FLOAT:
        return (value instanceof String) ? Float.parseFloat(String.valueOf(value)) : value;
      case DOUBLE:
        return (value instanceof String) ? Double.parseDouble(String.valueOf(value)) : value;
      case LONG:
        return (value instanceof String) ? Long.parseLong(String.valueOf(value)) : value;
      case BOOLEAN:
        return (value instanceof String) ? Boolean.parseBoolean(String.valueOf(value)) : value;
      case BYTES:
        return parseBytes(value);
      case ARRAY:
        return parseArray(value, schema);
      case MAP:
        return parseMap(value, schema);
      case UNION:
        return parseUnion(value, schema);
      case RECORD:
        return parseRecord(value, schema);
      default:
        return value;
    }
  }

  /**
   * Parse a given bytes value into a ByteBuffer. When bytes are stored using a complex parent data
   * type like map<string, bytes>, a quotation mark (') is added to the front and trailer of the
   * byte string it will be removed here
   *
   * @param value to parsed
   * @return {@link ByteBuffer} parsed byte buffer
   */
  private Object parseBytes(Object value) {
    if (value == null) {
      return null;
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Parsing byte string :{}", value);
    }
    String byteString = value.toString();
    if (value instanceof byte[]) {
      byte[] byteArray = (byte[]) value;
      byteString = new String(byteArray, Charset.defaultCharset());
      if ('\'' == byteString.charAt(0)) {
        byteString = byteString.substring(1);
      }
      if ('\'' == byteString.charAt(byteString.length() - 1)) {
        byteString = byteString.substring(0, byteString.length() - 1);
      }
    }
    return ByteBuffer.wrap(byteString.getBytes(Charset.defaultCharset()));
  }

  /**
   * Arrays are returned as json array strings and this will parse them back to lists.
   *
   * @param value json array string
   * @return {@link List}parsed list
   * @throws GoraException throw if parsing array values is failed
   */
  private Object parseArray(Object value, Schema schema) throws GoraException {
    if (value == null) {
      return null;
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Parsing json array : {}", value);
    }
    Schema elementSchema = schema.getElementType();
    JSONArray jsonArray = new JSONArray(String.valueOf(value));
    List<Object> valueList = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      valueList.add(parseSchema(elementSchema, jsonArray.get(i)));
    }
    return valueList;
  }

  /**
   * Maps are returned as json objects and this will parse them back to map objects
   *
   * @param value String of json object
   * @param schema Map schema to be used for parsing map values
   * @return {@link Map} Parsed map
   * @throws GoraException throw if parsing map entries is failed
   */
  private Object parseMap(Object value, Schema schema) throws GoraException {
    if (value == null) {
      return null;
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Parsing json object:{} as a map", value);
    }
    Schema valueSchema = schema.getValueType();
    JSONObject jsonObject = new JSONObject(String.valueOf(value));
    Map<CharSequence, Object> valueMap = new HashMap<>();
    Iterator<String> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      valueMap.put(new Utf8(key), parseSchema(valueSchema, jsonObject.get(key)));
    }
    return valueMap;
  }

  /**
   * This parses the given object into union typed object. If there is only one not-null sub-type,
   * the values parsed according to that schema regarless of this union schema. If there are more
   * than one not-null sub-types, the object is consided to be a json string with one field
   * representing the position of the type of the given value among the the list of sub-types.<br>
   * Ex: if the schema is union<String, Array<int>>, the object will be {2, '[1,2,3]'}
   *
   * @param value union object
   * @param schema union schema to be used for parsing map values
   * @return {@link Object} Parsed object
   * @throws GoraException throw if parsing union value is failed
   */
  private Object parseUnion(Object value, Schema schema) throws GoraException {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Parsing json object:{} as a union", value);
    }
    List<Schema> typeSchemaList = schema.getTypes();
    Schema primarySchema = null;
    if (typeSchemaList.size() == 2 && HiveQueryBuilder.isNullable(schema)) {
      primarySchema = HiveQueryBuilder.getValidSchema(schema);
    } else {
      JSONObject unionObject = new JSONObject(String.valueOf(value));
      int position = Integer.parseInt(unionObject.keys().next());
      for (Schema typeSchema : typeSchemaList) {
        if (!(Type.NULL.equals(typeSchema.getType())) && (position-- == 0)) {
          primarySchema = typeSchema;
          break;
        }
      }
    }
    if (primarySchema == null) {
      return null;
    } else {
      return parseSchema(primarySchema, value);
    }
  }

  /**
   * Parse a json object to a persistent record.
   *
   * @param value json object string
   * @param schema record schema to be used for parsing the object
   * @return persistent object
   * @throws GoraException throw if parsing record value is failed
   */
  private Object parseRecord(Object value, Schema schema) throws GoraException {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Parsing json object:{} as a record", value);
    }
    Class<?> clazz;
    try {
      clazz = ClassLoadingUtils.loadClass(schema.getFullName());
    } catch (ClassNotFoundException e) {
      throw new GoraException(e);
    }
    @SuppressWarnings("unchecked") final PersistentBase record = (PersistentBase) new BeanFactoryImpl(
        hiveStore.getKeyClass(), clazz).newPersistent();
    JSONObject recordObject = new JSONObject(String.valueOf(value));
    for (Field recField : schema.getFields()) {
      Schema innerSchema = recField.schema();
      if (recordObject.has(recField.name())) {
        record.put(recField.pos(), parseSchema(innerSchema, recordObject.get(recField.name())));
      }
    }
    return record;
  }
}
