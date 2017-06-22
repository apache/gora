package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.bean.Field;
import org.apache.gora.cassandra.bean.KeySpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by madhawa on 6/22/17.
 */
public class CassandraMapping {

  public KeySpace getKeySpace() {
    return keySpace;
  }

  public void setKeySpace(KeySpace keySpace) {
    this.keySpace = keySpace;
  }

  private KeySpace keySpace;

  public List<Field> getFieldList() {
    return fieldList;
  }

  private List<Field> fieldList;

  private Map<String, String> tableProperties;

  public CassandraMapping() {
    this.fieldList = new ArrayList<>();
    this.tableProperties = new HashMap<>();
  }

  public void setCoreName(String coreName) {
    this.coreName = coreName;
  }

  public String getCoreName() {
    return coreName;
  }

  private String coreName;

  public void addCassandraField(Field field) {
    this.fieldList.add(field);
  }

  public void addProperty(String key, String value) {
        this.tableProperties.put(key,value);
  }

  public String getProperty(String key) {
    return this.tableProperties.get(key);
  }
}
