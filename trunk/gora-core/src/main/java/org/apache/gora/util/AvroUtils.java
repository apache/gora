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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.reflect.ReflectData;
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
    for(Field field: fields) {
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
    throws SecurityException, NoSuchFieldException
    , IllegalArgumentException, IllegalAccessException {
    
    java.lang.reflect.Field field = clazz.getDeclaredField("_SCHEMA");
    return (Schema) field.get(null);
  }
  
}
