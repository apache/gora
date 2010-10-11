
package org.gora.sql.store;

import org.gora.sql.store.SqlTypeInterface.JdbcType;

public class Column {

  public static enum MappingStrategy {
    SERIALIZED,
    JOIN_TABLE,
    SECONDARY_TABLE,
  }

  private String tableName;
  private String name;
  private JdbcType jdbcType;
  private String sqlType;
  private boolean isPrimaryKey;
  private int length = -1;
  private int scale = -1;
  private MappingStrategy mappingStrategy;

  //index, not-null, default-value

  public Column() {
  }

  public Column(String name) {
    this.name = name;
  }

  public Column(String name, boolean isPrimaryKey, JdbcType jdbcType, String sqlType
      , int length, int scale) {
    this.name = name;
    this.isPrimaryKey = isPrimaryKey;
    this.jdbcType = jdbcType;
    this.length = length;
    this.scale = scale;
    this.mappingStrategy = MappingStrategy.SERIALIZED;
    this.sqlType = sqlType == null ? jdbcType.getSqlType() : sqlType;
  }

  public Column(String name, boolean isPrimaryKey, JdbcType jdbcType
      , int length, int scale) {
    this(name, isPrimaryKey, jdbcType, null, length, scale);
  }
  
  public Column(String name, boolean isPrimaryKey) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JdbcType getJdbcType() {
    return jdbcType;
  }

  public void setJdbcType(JdbcType jdbcType) {
    this.jdbcType = jdbcType;
  }

  public String getSqlType() {
    return sqlType;
  }
  
  public void setSqlType(String sqlType) {
    this.sqlType = sqlType;
  }
  
  public void setLength(int length) {
    this.length = length;
  }

  public int getLength() {
    return length;
  }

  public int getScale() {
    return scale;
  }

  public void setScale(int scale) {
    this.scale = scale;
  }

  public int getScaleOrLength() {
    return length > 0 ? length : scale;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public MappingStrategy getMappingStrategy() {
    return mappingStrategy;
  }

  public void setMappingStrategy(MappingStrategy mappingStrategy) {
    this.mappingStrategy = mappingStrategy;
  }

  public boolean isPrimaryKey() {
    return isPrimaryKey;
  }
}
