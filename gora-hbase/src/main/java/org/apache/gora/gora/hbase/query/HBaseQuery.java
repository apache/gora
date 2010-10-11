
package org.gora.hbase.query;

import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.impl.QueryBase;
import org.gora.store.DataStore;

/**
 * HBase specific implementation of the {@link Query} interface.
 */
public class HBaseQuery<K, T extends Persistent> extends QueryBase<K, T> {

  public HBaseQuery() {
    super(null);
  }
  
  public HBaseQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

}
