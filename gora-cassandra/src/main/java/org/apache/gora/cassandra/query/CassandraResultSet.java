package org.apache.gora.cassandra.query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * List data structure to keep the order coming from the Cassandra selects.
 */
public class CassandraResultSet extends ArrayList<CassandraRow> {

  /**
   * 
   */
  private static final long serialVersionUID = -7620939600192859652L;

  /**
   * Maps keys to indices in the list.
   */
  private HashMap<String, Integer> indexMap = new HashMap<String, Integer>();

  public CassandraRow getRow(String key) {
    Integer integer = this.indexMap.get(key);
    if (integer == null) {
      return null;
    }
    
    return this.get(integer);
  }

  public void putRow(String key, CassandraRow cassandraRow) {
    this.add(cassandraRow);
    this.indexMap.put(key, this.size()-1);
  } 
  

}