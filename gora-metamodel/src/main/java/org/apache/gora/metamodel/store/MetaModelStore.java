/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.metamodel.store;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.gora.metamodel.query.MetaModelQuery;
import org.apache.gora.metamodel.query.MetaModelResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.metamodel.DataContext;
import org.apache.metamodel.DataContextFactory;
import org.apache.metamodel.UpdateScript;
import org.apache.metamodel.UpdateableDataContext;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;
import org.apache.metamodel.delete.DeleteFrom;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.util.FileHelper;

/**
 * A Gora datastore implementation based on Apache MetaModel.
 * 
 * Apache MetaModel operates on {@link DataContext} objects. The creation of the
 * {@link DataContext} itself is not done within the scope of this class.
 * Rather, this datastore would be portable across all implementations. Consider
 * using {@link DataContextFactory} or add MetaModel-spring and use the
 * DataContextFactoryBean.
 * 
 * TODO: It is still not completely clear how the store and schema is defined.
 *
 * @param <K>
 * @param <T>
 */
public class MetaModelStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private final DataContext _dataContext;
  private final Table _table;
  private final Column _primaryKey;
  private final Column _timestampColumn;

  public MetaModelStore(DataContext dataContext, Table table) {
    this(dataContext, table, null);
  }

  public MetaModelStore(DataContext dataContext, Table table, Column primaryKey) {
    this(dataContext, table, primaryKey, null);
  }

  public MetaModelStore(DataContext dataContext, Table table, Column primaryKey, Column timestampColumn) {
    if (dataContext == null) {
      throw new IllegalArgumentException("DataContext cannot be null");
    }
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null");
    }
    _dataContext = dataContext;
    _table = table;
    if (primaryKey == null) {
      final Column[] primaryKeyCandidates = table.getPrimaryKeys();
      if (primaryKeyCandidates == null || primaryKeyCandidates.length != 1) {
        throw new IllegalArgumentException(
            "No primary key column provided and primary key cannot be automatically detected. Candidates: "
                + Arrays.toString(primaryKeyCandidates));
      }
      _primaryKey = primaryKeyCandidates[0];
    } else {
      _primaryKey = primaryKey;
    }
    _timestampColumn = timestampColumn;
  }

  /**
   * Gets the {@link Table} that this store represents
   * 
   * @return
   */
  public Table getTable() {
    return _table;
  }

  /**
   * Gets the primary key / ID {@link Column} that holds the keys
   * 
   * @return
   */
  public Column getPrimaryKey() {
    return _primaryKey;
  }

  /**
   * Gets the {@link DataContext} that this store is using
   * 
   * @return
   */
  public DataContext getDataContext() {
    return _dataContext;
  }

  /**
   * Gets the {@link Column} that hold timestamp information, if any
   * 
   * @return
   */
  public Column getTimestampColumn() {
    return _timestampColumn;
  }

  public MetaModelStore(DataContext dataContext, String tableName) {
    this(dataContext, dataContext.getTableByQualifiedLabel(tableName));
  }

  @Override
  public String getSchemaName() {
    return _table.getName();
  }

  @Override
  public void createSchema() {
  }

  @Override
  public void deleteSchema() {
  }

  @Override
  public boolean schemaExists() {
    return true;
  }

  @Override
  public T get(K key, String[] fields) {
    final MetaModelQuery<K, T> q = newQuery();
    q.setFields(fields);
    q.setKey(key);

    final Result<K, T> result = q.execute();
    try {
      if (result.next()) {
        return result.get();
      }
      return null;
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new IllegalStateException(e);
    } finally {
      FileHelper.safeClose(result);
    }
  }

  @Override
  public void put(K key, T obj) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean delete(K key) {
    final DeleteFrom delete = new DeleteFrom(_table).where(_primaryKey).eq(key);
    executeUpdate(delete);
    return true;
  }

  private void executeUpdate(UpdateScript updateScript) {
    if (!(_dataContext instanceof UpdateableDataContext)) {
      throw new UnsupportedOperationException("Cannot update read-only DataContext: " + _dataContext);
    }

    final UpdateableDataContext updateableDataContext = (UpdateableDataContext) _dataContext;
    updateableDataContext.executeUpdate(updateScript);
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    if (!(_dataContext instanceof UpdateableDataContext)) {
      throw new UnsupportedOperationException("Cannot update read-only DataContext: " + _dataContext);
    }

    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public MetaModelResult<K, T> execute(Query<K, T> query) {
    if (!(query instanceof MetaModelQuery)) {
      throw new UnsupportedOperationException("Cannot execute non-MetaModel query: " + query);
    }

    final MetaModelQuery<K, T> metaModelQuery = (MetaModelQuery<K, T>) query;
    final DataSet dataSet = metaModelQuery.executeQuery();

    return new MetaModelResult<K, T>(this, metaModelQuery, dataSet);
  }

  @Override
  public MetaModelQuery<K, T> newQuery() {
    return new MetaModelQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
    if (_dataContext instanceof Closeable) {
      FileHelper.safeClose(_dataContext);
    }
  }

  public T newPersistent(Row row) {
    final T persistent = newPersistent();
    final int rowSize = row.size();
    for (int i = 0; i < rowSize; i++) {
      final String name = row.getSelectItems()[i].getColumn().getName();
      final Object value = row.getValue(i);
      persistent.put(name, value);
    }
    return persistent;
  }

}
