
package org.gora.hbase.query;

import static org.gora.hbase.util.HBaseByteInterface.fromBytes;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Result;
import org.gora.hbase.store.HBaseStore;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.query.impl.ResultBase;

/**
 * Base class for {@link Result} implementations for HBase.  
 */
public abstract class HBaseResult<K, T extends Persistent> 
  extends ResultBase<K, T> {

  public HBaseResult(HBaseStore<K,T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }
  
  @Override
  public HBaseStore<K, T> getDataStore() {
    return (HBaseStore<K, T>) super.getDataStore();
  }
  
  protected void readNext(Result result) throws IOException {
    key = fromBytes(getKeyClass(), result.getRow());
    persistent = getDataStore().newInstance(result, query.getFields());
  }
  
}
