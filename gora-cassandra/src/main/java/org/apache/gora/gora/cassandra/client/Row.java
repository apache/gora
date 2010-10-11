package org.gora.cassandra.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.gora.util.ByteUtils;

public class Row {

  private String key;

  private Map<String, Map<String, Map<String, byte[]>>> data;

  /** this is a placeholder as a "supercolumn" for all normal columns */
  private static final String EMPTY_COLUMN = "";

  public Row(String key) {
    this.key = key;
    data = new HashMap<String, Map<String,Map<String,byte[]>>>();
  }

  private Map<String, byte[]> getOrCreate(String family, String superColumn) {
    Map<String, Map<String, byte[]>> familyMap = data.get(family);
    if (familyMap == null) {
      familyMap = new HashMap<String, Map<String,byte[]>>();
      data.put(family, familyMap);
    }
    Map<String, byte[]> map = familyMap.get(superColumn);
    if (map == null) {
      map = new HashMap<String, byte[]>();
      familyMap.put(superColumn, map);
    }
    return map;
  }

  /*package*/ void addColumnOrSuperColumn(String columnFamily
      , byte[] superColumn, ColumnOrSuperColumn csc) {
    if (superColumn == null) {
      Map<String, byte[]> map = getOrCreate(columnFamily, EMPTY_COLUMN);
      map.put(ByteUtils.toString(csc.column.name), csc.column.value);
    } else {
      String superColumnStr = ByteUtils.toString(superColumn);
      Map<String, byte[]> map = getOrCreate(columnFamily, superColumnStr);
      map.put(ByteUtils.toString(csc.column.name), csc.column.value);
    }
  }

  public String getKey() {
    return key;
  }

  public Map<String, byte[]> getColumn(String columnFamily) {
    return getSuperColumn(columnFamily, EMPTY_COLUMN);
  }

  public Map<String, byte[]> getSuperColumn(String columnFamily, String superColumn) {
    Map<String, Map<String, byte[]>> map = data.get(columnFamily);
    if (map == null) {
      return null;
    }
    return map.get(superColumn);
  }

  public byte[] get(String columnFamily, String superColumn, String column) {
    Map<String, byte[]> map = getSuperColumn(columnFamily, superColumn);
    if (map == null) {
      return null;
    }
    return map.get(column);
  }

  public byte[] get(String columnFamily, String column) {
    Map<String, byte[]> map = getColumn(columnFamily);
    if (map == null) {
      return null;
    }
    return map.get(column);
  }

  @Override
  public String toString() {
    return key + "::" + data.toString();
  }
}
