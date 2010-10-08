
package org.gora.sql.query;

import org.gora.persistency.Persistent;
import org.gora.query.impl.QueryBase;
import org.gora.sql.store.SqlStore;

/**
 * Query implementation covering SQL queries
 */
public class SqlQuery<K, T extends Persistent> extends QueryBase<K, T> {

  public SqlQuery() {
    super(null);
  }

  public SqlQuery(SqlStore<K, T> dataStore) {
    super(dataStore);
  }

}
