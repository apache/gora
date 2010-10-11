
package org.gora.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.reflect.ReflectData;
import org.gora.persistency.Persistent;

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
