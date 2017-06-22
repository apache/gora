package org.apache.gora.cassandra.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by madhawa on 6/22/17.
 */
public class Field {

  private String fieldName;

  private String columnName;

  private String type;

  public Field() {
    properties = new HashMap<>(2);
  }

  private Map<String, String> properties;

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public void addProperty(String key, String value) {
    properties.put(key, value);
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getProperty(String key) {
    return this.properties.get(key);
  }
}
