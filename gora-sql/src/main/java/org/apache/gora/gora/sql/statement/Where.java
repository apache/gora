package org.gora.sql.statement;

/**
 * A WHERE clause in an SQL statement
 */
public class Where {

  private StringBuilder builder;

  public Where() {
    builder = new StringBuilder();
  }

  public Where(String where) {
    builder = new StringBuilder(where == null ? "" : where);
  }

  /** Adds a part to the Where clause connected with AND */
  public void addPart(String part) {
    if (builder.length() > 0) {
      builder.append(" AND ");
    }
    builder.append(part);
  }

  public void equals(String name, String value) {
    addPart(name + " = " + value);
  }

  public void lessThan(String name, String value) {
    addPart(name + " < " + value);
  }
  
  public void lessThanEq(String name, String value) {
    addPart(name + " <= " + value);
  }
  
  public void greaterThan(String name, String value) {
    addPart(name + " > " + value);
  }
  
  public void greaterThanEq(String name, String value) {
    addPart(name + " >= " + value);
  }
  
  public boolean isEmpty() {
    return builder.length() == 0;
  }
  
  @Override
  public String toString() {
    return builder.toString();
  }
}
