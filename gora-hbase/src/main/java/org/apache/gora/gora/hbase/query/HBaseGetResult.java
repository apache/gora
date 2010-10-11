
package org.gora.hbase.query;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.gora.hbase.store.HBaseStore;
import org.gora.persistency.Persistent;
import org.gora.query.Query;

/**
 * An {@link HBaseResult} based on the result of a HBase {@link Get} query.
 */
public class HBaseGetResult<K, T extends Persistent> extends HBaseResult<K,T> {

  private Result result;
  
  public HBaseGetResult(HBaseStore<K, T> dataStore, Query<K, T> query
      , Result result) {
    super(dataStore, query);
    this.result = result;
  }

  @Override
  public float getProgress() throws IOException {
    return key == null ? 0f : 1f;
  }

  @Override
  public boolean nextInner() throws IOException {
    if(result == null || result.getRow() == null 
        || result.getRow().length == 0) {
      return false;
    }
    if(key == null) {
      readNext(result);
      return key != null;
    }
    
    return false;
  }

  @Override
  public void close() throws IOException {
  }
}
