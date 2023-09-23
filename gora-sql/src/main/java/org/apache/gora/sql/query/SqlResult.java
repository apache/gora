package org.apache.gora.sql.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.store.DataStore;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public class SqlResult<K, T extends PersistentBase> extends ResultBase<K, T> {
    private Result<Record> resultSet;
    private int size;
    private Iterator<Record> resultSetIterator;
    private String[] dbFields;
    private static final Logger log = LoggerFactory.getLogger(SqlResult.class);

    public SqlResult(DataStore<K, T> dataStore, Query<K, T> query) {
        super(dataStore, query);
    }

    public SqlResult(DataStore<K, T> dataStore,
                     Query<K, T> query,
                     Result<Record> resultSet,
                     String[] dbFields) {
        super(dataStore, query);
        this.resultSet = resultSet;
        this.resultSetIterator = resultSet.iterator();
        this.size = resultSet.size();
        this.dbFields = dbFields;
    }

    public SqlStore<K, T> getDataStore() {
        return (SqlStore<K, T>) super.getDataStore();
    }
    @Override
    public float getProgress() throws IOException, InterruptedException {
        if (resultSet == null) {
            return 0;
        } else if (size == 0) {
            return 1;
        } else {
            return offset / (float) size;
        }
    }

    @Override
    public int size() {
        int totalSize = size;
        int intLimit = (int) this.limit;
        return intLimit > 0 && totalSize > intLimit ? intLimit : totalSize;
    }

    @Override
    protected boolean nextInner() throws IOException {
        if (!resultSetIterator.hasNext()) {
            return false;
        }

        Record obj =resultSetIterator.next();
        key = (K) obj.getValue("id");
        persistent = ((SqlStore<K, T>) getDataStore())
                .convertSqlTableToAvroBean(obj, dbFields);
        return persistent != null;

    }
}
