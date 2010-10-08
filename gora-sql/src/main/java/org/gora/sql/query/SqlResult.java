
package org.gora.sql.query;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.impl.ResultBase;
import org.gora.sql.store.SqlStore;
import org.gora.sql.util.SqlUtils;
import org.gora.store.DataStore;

public class SqlResult<K, T extends Persistent> extends ResultBase<K, T> {

  private ResultSet resultSet;
  private PreparedStatement statement;
  
  public SqlResult(DataStore<K, T> dataStore, Query<K, T> query
      , ResultSet resultSet, PreparedStatement statement) {
    super(dataStore, query);
    this.resultSet = resultSet;
    this.statement = statement;
  }

  @Override
  protected boolean nextInner() throws IOException {
    try {
      if(!resultSet.next()) { //no matching result
        close();
        return false;
      }

      SqlStore<K, T> sqlStore = ((SqlStore<K,T>)dataStore);
      key = sqlStore.readPrimaryKey(resultSet);
      persistent = sqlStore.readObject(resultSet, persistent, query.getFields());

      return true;
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void close() throws IOException {
    SqlUtils.close(resultSet);
    SqlUtils.close(statement);
  }

  @Override
  public float getProgress() throws IOException {
    return 0;
  }
}
