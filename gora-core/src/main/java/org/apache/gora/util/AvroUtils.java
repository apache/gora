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
import org.apache.gora.persistency.impl.PersistentBase;

/**
 * An utility class for Avro related tasks.
 */
public class AvroUtils {

  /**
   * Returns a map of field name to Field for schema's fields.
   *
   * @param schema the schema object to get the map of field name to Field.
   * @return map of field name to Field.
   */
  public static Map<String, Field> getFieldMap(Schema schema) {
    List<Field> fields = schema.getFields();
    HashMap<String, Field> fieldMap = new HashMap<>(fields.size());
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
   * Returns the schema of the class.
   *
   * @param clazz Class instance of the persistent bean.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws NoSuchFieldException if a field with the specified name is not found.
   * @throws IllegalArgumentException this will not be thrown since <code>field.get(obj)</code> passing obj is ignored
   *         since the SCHEMA field is a static class level variable inside the persistent bean class.
   * @throws IllegalAccessException if the field is inaccessible due to java language access control.
   * @return the schema of persistent bean instance.
   */
  public static Schema getSchema(Class<? extends PersistentBase> clazz)
      throws SecurityException, NoSuchFieldException, IllegalArgumentException,
      IllegalAccessException {

    java.lang.reflect.Field field = clazz.getDeclaredField("SCHEMA$");
    return (Schema) field.get(null);
  }

  /**
   * Return the field names from a persistent object.
   * 
   * @param persistent the persistent object to get the fields names from.
   * @return the field names String array.
   */
  public static String[] getPersistentFieldNames(PersistentBase persistent) {
    return getSchemaFieldNames(persistent.getSchema());
  }

  /**
   * Return the field names from a schema object.
   *
   * @param schema the schema object to get the fields names from.
   * @return the field names String array.
   */
  public static String[] getSchemaFieldNames(Schema schema) {
    List<Field> fields = schema.getFields();
    String[] fieldNames = new String[fields.size()];
    for (int i = 0; i < fieldNames.length; i++) {
      fieldNames[i] = fields.get(i).name();
    }
    return fieldNames;
  }

  /**
   * Utility method for deep clone a given AVRO persistent bean instance.
   *
   * @param persistent source persistent bean instance.
   * @param <T> persistent bean type.
   * @return cloned persistent bean to be returned.
   */
  public static <T extends PersistentBase> T deepClonePersistent(T persistent) {
    final SpecificDatumWriter<PersistentBase> writer = new SpecificDatumWriter<>(persistent.getSchema());
    final byte[] byteData;
    try {
      byteData = IOUtils.serialize(writer, persistent);
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to serialize avro object to byte buffer - "
              + "please report this issue to the Gora bugtracker "
              + "or your administrator.");
    }

    @SuppressWarnings("unchecked")
    final SpecificDatumReader<T> reader = new SpecificDatumReader<>((Class<T>) persistent.getClass());
    try {
      return IOUtils.deserialize(byteData, reader, null);
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to deserialize avro object from byte buffer - "
              + "please report this issue to the Gora bugtracker "
              + "or your administrator.");
    }

  }

}
