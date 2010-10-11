package org.gora.sql.statement;

import org.gora.persistency.Persistent;
import org.gora.sql.store.SqlMapping;
import org.gora.sql.store.SqlStore;
import org.gora.sql.store.SqlStore.DBVendor;

public class InsertUpdateStatementFactory {

  public static <K, T extends Persistent>
  InsertUpdateStatement<K, T> createStatement(SqlStore<K, T> store,
      SqlMapping mapping, DBVendor dbVendor) {
    switch(dbVendor) {
      case MYSQL:
        return new MySqlInsertUpdateStatement<K, T>(store, mapping, mapping.getTableName());
      case HSQL:
        return new HSqlInsertUpdateStatement<K, T>(store, mapping, mapping.getTableName());
      case GENERIC:
      default :
        throw new RuntimeException("Database is not supported yet.");    
    }
  }
}
