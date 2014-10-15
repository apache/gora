package org.apache.gora.metamodel.query;

import org.apache.gora.metamodel.store.MetaModelStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.metamodel.DataContext;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.query.FilterItem;
import org.apache.metamodel.query.LogicalOperator;
import org.apache.metamodel.query.OperatorType;
import org.apache.metamodel.query.Query;
import org.apache.metamodel.query.SelectItem;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;

/**
 * A {@link org.apache.gora.query.Query} implementation for
 * {@link MetaModelStore}.
 * 
 * TODO: Gora Filters are not implemented. Not sure about how "local filters" work.
 *
 * @param <K>
 * @param <T>
 */
public final class MetaModelQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  public MetaModelQuery(MetaModelStore<K, T> store) {
    super(store);
  }

  @Override
  public MetaModelStore<K, T> getDataStore() {
    // overridden to expose MetaModelDatastore
    return (MetaModelStore<K, T>) super.getDataStore();
  }

  @Override
  public MetaModelResult<K, T> execute() {
    // overridden to expose MetaModelResult as return type
    final MetaModelStore<K, T> store = getDataStore();
    return store.execute(this);
  }

  public Query toMetaModelQuery() {
    final MetaModelStore<K, T> store = getDataStore();
    final Table table = store.getTable();

    final Query query = new Query();
    query.from(table);

    final String[] fields = getFields();
    for (String fieldName : fields) {
      query.select(fieldName);
    }

    final SelectItem primaryKeySelectItem = new SelectItem(store.getPrimaryKey(), query.getFromClause().getItem(0));

    final K key = getKey();
    if (key != null) {
      query.where(primaryKeySelectItem, OperatorType.EQUALS_TO, key);
    } else {
      final K startKey = getStartKey();
      if (startKey != null) {
        final FilterItem filter = createGreaterThanOrEqualFilter(primaryKeySelectItem, startKey);
        query.where(filter);
      }

      final K endKey = getEndKey();
      if (endKey != null) {
        final FilterItem filter = createLessThanOrEqualFilter(primaryKeySelectItem, endKey);
        query.where(filter);
      }
    }

    final long limit = getLimit();
    if (limit > 0) {
      query.setMaxRows((int) limit);
    }

    final Column timestampColumn = store.getTimestampColumn();
    final long timestamp = getTimestamp();
    if (timestamp != -1) {
      if (timestampColumn == null) {
        throw new IllegalStateException("Cannot query by timestamp when no timestamp Column is set");
      }
      query.where(timestampColumn, OperatorType.EQUALS_TO, timestamp);
    } else {
      final long startTime = getStartTime();
      final long endTime = getEndTime();

      if (startTime != -1 || endTime != -1) {
        final SelectItem timestampSelectItem = new SelectItem(timestampColumn, query.getFromClause().getItem(0));

        if (startTime != -1) {
          if (timestampColumn == null) {
            throw new IllegalStateException("Cannot query by timestamp when no timestamp Column is set");
          }
          final FilterItem filter = createGreaterThanOrEqualFilter(timestampSelectItem, startTime);
          query.where(filter);
        }

        if (endTime != -1) {
          if (timestampColumn == null) {
            throw new IllegalStateException("Cannot query by timestamp when no timestamp Column is set");
          }
          final FilterItem filter = createLessThanOrEqualFilter(timestampSelectItem, endTime);
          query.where(filter);
        }
      }
    }

    return query;
  }

  private FilterItem createGreaterThanOrEqualFilter(SelectItem selectItem, Object operand) {
    // TODO: In MetaModel 4.3 and onwards there will be a single
    // OperatorType for this. But for now we need a composite FilterItem.
    return new FilterItem(LogicalOperator.OR, new FilterItem(selectItem, OperatorType.EQUALS_TO, operand),
        new FilterItem(selectItem, OperatorType.GREATER_THAN, operand));
  }

  private FilterItem createLessThanOrEqualFilter(SelectItem selectItem, Object operand) {
    // TODO: In MetaModel 4.3 and onwards there will be a single
    // OperatorType for this. But for now we need a composite FilterItem.
    return new FilterItem(LogicalOperator.OR, new FilterItem(selectItem, OperatorType.EQUALS_TO, operand),
        new FilterItem(selectItem, OperatorType.LESS_THAN, operand));
  }

  public DataSet executeQuery() {
    final Query query = toMetaModelQuery();
    final DataContext dataContext = getDataStore().getDataContext();
    return dataContext.executeQuery(query);
  }
}
