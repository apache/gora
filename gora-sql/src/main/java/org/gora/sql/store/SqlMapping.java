
package org.gora.sql.store;

import java.util.HashMap;

import org.gora.sql.store.SqlTypeInterface.JdbcType;

public class SqlMapping {

  private String tableName;
  private HashMap<String, Column> fields;
  private Column primaryColumn;

  public SqlMapping() {
    fields = new HashMap<String, Column>();
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public void addField(String fieldname, String column) {
    fields.put(fieldname, new Column(column));
  }

  public void addField(String fieldName, String columnName, JdbcType jdbcType,
      String sqlType, int length, int scale) {
    fields.put(fieldName, new Column(columnName, false, jdbcType, sqlType, length, scale));
  }

  public Column getColumn(String fieldname) {
    return fields.get(fieldname);
  }

  public void setPrimaryKey(String columnName, JdbcType jdbcType,
      int length, int scale) {
    primaryColumn = new Column(columnName, true, jdbcType, length, scale);
  }

  public Column getPrimaryColumn() {
    return primaryColumn;
  }

  public String getPrimaryColumnName() {
    return primaryColumn.getName();
  }

  public HashMap<String, Column> getFields() {
    return fields;
  }
}
