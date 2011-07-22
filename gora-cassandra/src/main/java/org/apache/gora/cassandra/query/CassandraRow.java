package org.apache.gora.cassandra.query;

import java.util.ArrayList;

/**
 * List of key value pairs representing a row, tagged by a key.
 */
public class CassandraRow extends ArrayList<CassandraColumn> {

  /**
   * 
   */
  private static final long serialVersionUID = -7620939600192859652L;
  private String key;

  public String getKey() {
    return this.key;
  }

  public void setKey(String key) {
    this.key = key;
  }

}
