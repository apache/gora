package org.gora.cassandra.query;

import org.gora.persistency.Persistent;
import org.gora.query.impl.QueryBase;
import org.gora.store.DataStore;

public class CassandraQuery<K, T extends Persistent>
extends QueryBase<K, T> {

  public CassandraQuery() {
    super(null);
  }

  public CassandraQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }
}
