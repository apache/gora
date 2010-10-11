package org.gora.cassandra.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CassandraMapping {

  private String keySpace;

  private Map<String, Boolean> families =
    new HashMap<String, Boolean>();

  public String getKeySpace() {
    return keySpace;
  }

  public void setKeySpace(String keySpace) {
    this.keySpace = keySpace;
  }

  public Set<String> getColumnFamilies() {
    return families.keySet();
  }

  public void addColumnFamily(String columnFamily, boolean isSuper) {
    families.put(columnFamily, isSuper);
  }

  public boolean isColumnFamilySuper(String columnFamily) {
    return families.get(columnFamily);
  }
}
