
package org.gora.sql.statement;

/**
 * A SQL DELETE statement, for generating a Prepared Statement
 */
//new API experiment
public class Delete {

  private String from;
  private Where where;
  
  /**
   * @return the from
   */
  public String from() {
    return from;
  }

  /**
   * @param from the from to set
   */
  public Delete from(String from) {
    this.from = from;
    return this;
  }
  
  public Delete where(Where where) {
    this.where = where;
    return this;
  }
  
  public Where where() {
    if(where == null) {
      where = new Where();
    }
    return where;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("DELETE FROM ");
    builder.append(from);
    
    if(where != null && !where.isEmpty()) {
      builder.append(" WHERE ");
      builder.append(where.toString());
    }
    
    return builder.toString();
  }
}
