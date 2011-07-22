package org.apache.gora.cassandra.query;

import org.apache.avro.Schema.Field;


/**
 * Represents a unit of data: a key value pair tagged by a family name
 */
public abstract class CassandraColumn {
  public static final int SUB = 0;
  public static final int SUPER = 1;
  
  private String family;
  private int type;
  private Field field;
  
  public String getFamily() {
    return family;
  }
  public void setFamily(String family) {
    this.family = family;
  }
  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
  }
  public void setField(Field field) {
    this.field = field;
  }
  
  protected Field getField() {
    return this.field;
  }
  
  public abstract String getName();
  public abstract Object getValue();
  

}
