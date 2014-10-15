package org.apache.gora.metamodel.query;

import java.io.IOException;

import org.apache.gora.metamodel.store.MetaModelStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.ResultBase;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;

/**
 * Implementation of {@link Result} for {@link MetaModelStore}.
 *
 * @param <K>
 * @param <T>
 */
public final class MetaModelResult<K, T extends PersistentBase> extends ResultBase<K, T> {

  private final DataSet _dataSet;

  public MetaModelResult(MetaModelStore<K, T> dataStore, MetaModelQuery<K, T> query, DataSet dataSet) {
    super(dataStore, query);
    _dataSet = dataSet;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    // not available
    return 0;
  }

  @Override
  public MetaModelStore<K, T> getDataStore() {
    return (MetaModelStore<K, T>) super.getDataStore();
  }

  @Override
  protected boolean nextInner() throws IOException {
    final boolean next = _dataSet.next();
    if (next) {
      final Row row = _dataSet.getRow();
      persistent = getDataStore().newPersistent(row);
    } else {
      persistent = null;
    }
    return next;
  }

  @Override
  public void close() throws IOException {
    super.close();
    _dataSet.close();
  }
}
