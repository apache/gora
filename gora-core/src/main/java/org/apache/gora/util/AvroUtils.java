/**
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

package org.apache.gora.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.gora.persistency.Persistent;

/**
 * An utility class for Avro related tasks
 */
public class AvroUtils {

  /**
   * Returns a map of field name to Field for schema's fields.
   */
  public static Map<String, Field> getFieldMap(Schema schema) {
    List<Field> fields = schema.getFields();
    HashMap<String, Field> fieldMap = new HashMap<String, Field>(fields.size());
    for (Field field : fields) {
      fieldMap.put(field.name(), field);
    }
    return fieldMap;
  }

  @SuppressWarnings("unchecked")
  public static Object getEnumValue(Schema schema, String symbol) {
    return Enum.valueOf(ReflectData.get().getClass(schema), symbol);
  }

  public static Object getEnumValue(Schema schema, int enumOrdinal) {
    String symbol = schema.getEnumSymbols().get(enumOrdinal);
    return getEnumValue(schema, symbol);
  }

  /**
   * Returns the schema of the class
   */
  public static Schema getSchema(Class<? extends Persistent> clazz)
      throws SecurityException, NoSuchFieldException, IllegalArgumentException,
      IllegalAccessException {

    java.lang.reflect.Field field = clazz.getDeclaredField("SCHEMA$");
    return (Schema) field.get(null);
  }

  /**
   * Return the field names from a persistent object
   * 
   * @param persistent
   *          the persistent object to get the fields names from
   * @return the field names
   */
  public static String[] getPersistentFieldNames(Persistent persistent) {
    return getSchemaFieldNames(persistent.getSchema());
  }

  /**
   * Return the field names from a schema object
   * 
   * @param persistent
   *          the persistent object to get the fields names from
   * @return the field names
   */
  public static String[] getSchemaFieldNames(Schema schema) {
    List<Field> fields = schema.getFields();
    String[] fieldNames = new String[fields.size() - 1];
    for (int i = 0; i < fieldNames.length; i++) {
      fieldNames[i] = fields.get(i + 1).name();
    }
    return fieldNames;
  }

  public static <T extends Persistent> T deepClonePersistent(T persistent) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    BinaryEncoder enc = EncoderFactory.get().binaryEncoder(bos, null);
    SpecificDatumWriter<Persistent> writer = new SpecificDatumWriter<Persistent>(
        persistent.getSchema());
    try {
      writer.write(persistent, enc);
      enc.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to serialize avro object to byte buffer - "
              + "please report this issue to the Gora bugtracker "
              + "or your administrator.");
    }
    byte[] value = bos.toByteArray();
    Decoder dec = DecoderFactory.get().binaryDecoder(value, null);
    @SuppressWarnings("unchecked")
    SpecificDatumReader<T> reader = new SpecificDatumReader<T>(
        (Class<T>) persistent.getClass());
    try {
      return reader.read(null, dec);
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to deserialize avro object from byte buffer - "
              + "please report this issue to the Gora bugtracker "
              + "or your administrator.");
    }

  }

}
