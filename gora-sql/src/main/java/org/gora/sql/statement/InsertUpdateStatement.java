package org.gora.sql.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.avro.Schema;
import org.gora.persistency.Persistent;
import org.gora.sql.store.Column;
import org.gora.sql.store.SqlMapping;
import org.gora.sql.store.SqlStore;

public abstract class InsertUpdateStatement<K, V extends Persistent> {

  protected class ColumnData {
    protected Object object;
    protected Schema schema;
    protected Column column;

    protected ColumnData(Object object, Schema schema, Column column) {
      this.object = object;
      this.schema = schema;
      this.column = column;
    }
  }

  protected SortedMap<String, ColumnData> columnMap = new TreeMap<String, ColumnData>();

  protected String tableName;

  protected SqlMapping mapping;

  protected SqlStore<K, V> store;

  public InsertUpdateStatement(SqlStore<K, V> store, SqlMapping mapping, String tableName) {
    this.store = store;
    this.mapping = mapping;
    this.tableName = tableName;
  }

  public void setObject(Object object, Schema schema, Column column) {
    columnMap.put(column.getName(), new ColumnData(object, schema, column));
  }

  public abstract PreparedStatement toStatement(Connection connection)
  throws SQLException;
}
