package org.apache.gora.sql.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.sql.store.SqlMapping;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.sql.store.SqlStoreParameters;
import org.apache.gora.store.DataStore;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectField;
import org.jooq.SelectQuery;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class SqlQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

    public SqlQuery(DataStore<K, T> dataStore) {
        super(dataStore);
    }

    private DSLContext dslContext;

    public SqlQuery() {
        super(null);
    }

    private SelectQuery<Record> query;
    private List<SelectField<?>> selectFields = new ArrayList<>();

    public SelectQuery<Record> populateSqlQuery(final SqlMapping sqlMapping,
                                                final String[] fields,
                                                final SqlStoreParameters sqlStoreParameters,
                                                final String[] schemaFields) {

        dslContext = SqlStore.getJooQConfiguration(sqlStoreParameters);
        query = dslContext.selectQuery();
        selectFields.add(field("id"));
        if (fields.length == schemaFields.length) {
            for (String k : fields) {
                String dbFieldName = k;
                if (dbFieldName != null && dbFieldName.length() > 0) {
                    selectFields.add(field(dbFieldName));
                }
            }
        } else {
            for (String k : fields) {
                String dbFieldName = k;
                if (dbFieldName != null && dbFieldName.length() > 0) {
                    selectFields.add(field(dbFieldName));
                }
            }

        }

        if ((this.getStartKey() != null) && (this.getEndKey() != null)
                && this.getStartKey().equals(this.getEndKey())) {
            query.addFrom(table(sqlMapping.getTableClass()));
            query.addSelect(selectFields);
            query.addConditions(field("id").eq(this.getStartKey()));


        } else if (this.getStartKey() != null || this.getEndKey() != null) {
            if (this.getStartKey() != null
                    && this.getEndKey() == null) {
                query.addFrom(table(sqlMapping.getTableClass()));
                query.addSelect(selectFields);
                query.addConditions(field("id").eq(this.getStartKey()));
            } else if (this.getEndKey() != null
                    && this.getStartKey() == null) {
                query.addFrom(table(sqlMapping.getTableClass()));
                query.addSelect(selectFields);
                query.addConditions(field("id").eq(this.getEndKey()));
            } else {

                query.addFrom(table(sqlMapping.getTableClass()));
                query.addSelect(selectFields);
                query.addConditions(field("id").eq(this.getStartKey()));
                query.addConditions(field("id").eq(this.getEndKey()));
            }
        } else {
            query.addFrom(table(sqlMapping.getTableClass()));
            query.addSelect(selectFields);
        }

        return query;
    }

}
