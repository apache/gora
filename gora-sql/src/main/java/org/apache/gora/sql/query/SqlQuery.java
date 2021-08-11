package org.apache.gora.sql.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;

public class SqlQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

    public SqlQuery(DataStore<K, T> dataStore) {
        super(dataStore);
    }
    public SqlQuery() {
        super(null);
    }


}
