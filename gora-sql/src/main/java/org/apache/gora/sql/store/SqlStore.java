package org.apache.gora.sql.store;

import org.apache.avro.Schema;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.sql.query.SqlQuery;
import org.apache.gora.sql.query.SqlResult;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import static org.jooq.impl.DSL.*;

public class SqlStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    private static DSLContext dslContext;
    private SqlStoreParameters sqlStoreParameters;
    private SqlMapping sqlMapping;
    private static Connection connection;

    public static DSLContext getJooQConfiguration(SqlStoreParameters sqlStoreParameters) {
        String url = "jdbc:mysql://" + sqlStoreParameters.getServerHost() + ":" + sqlStoreParameters.getServerPort()
                + "/" + sqlStoreParameters.getDatabaseName();
        try {
            connection = DriverManager.getConnection(url,
                    sqlStoreParameters.getUserName(), sqlStoreParameters.getUserPassword());

            dslContext = DSL.using(connection, SQLDialect.MYSQL);
            return dslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        super.initialize(keyClass, persistentClass, properties);

        try {
            sqlStoreParameters = SqlStoreParameters.load(properties);
            getJooQConfiguration(sqlStoreParameters);
            dslContext.createDatabaseIfNotExists(sqlStoreParameters.getDatabaseName()).execute();

            SqlMappingBuilder<K, T> builder = new SqlMappingBuilder<>(this);
            sqlMapping = builder.fromFile(sqlStoreParameters.getMappingFile()).build();
        } catch (Exception e) {
            LOG.error("Error while initializing SQL dataStore: {}",
                    new Object[]{e.getMessage()});
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getSchemaName() {
        return sqlMapping.getTableClass();
    }

    @Override
    public String getSchemaName(final String mappingSchemaName,
                                final Class<?> persistentClass) {
        return super.getSchemaName(mappingSchemaName, persistentClass);
    }

    @Override
    public void createSchema() throws GoraException {
        if (schemaExists()) {
            return;
        }
        try {
            Map<String, SqlMapping.SQLDataType> allColumns = sqlMapping.getAllColumns();
            Iterator<Map.Entry<String, SqlMapping.SQLDataType>> iterator = allColumns.entrySet().iterator();
            CreateTableColumnStep step = dslContext.createTableIfNotExists(sqlMapping.getTableClass());
            step = step.column(sqlMapping.getPrimaryKey(), org.jooq.impl.SQLDataType.VARCHAR.length(50));

            while (iterator.hasNext()) {
                Map.Entry<String, SqlMapping.SQLDataType> entry = iterator.next();

                if (entry.getValue() == SqlMapping.SQLDataType.VARCHAR)
                    step = step.column(entry.getKey(), org.jooq.impl.SQLDataType.VARCHAR);
                if (entry.getValue() == SqlMapping.SQLDataType.INTEGER)
                    step = step.column(entry.getKey(), SQLDataType.BIGINT);
                if (entry.getValue() == SqlMapping.SQLDataType.BLOB)
                    step = step.column(entry.getKey(), SQLDataType.BLOB);

            }
            step.constraints(
                    primaryKey(sqlMapping.getPrimaryKey())
            ).execute();

        } catch (Exception e) {
            throw new GoraException(e);
        }


    }

    @Override
    public void deleteSchema() throws GoraException {
        try {
            if (schemaExists())
                dslContext.dropTable(getSchemaName()).execute();
            //dslContext.dropSchema(getSchemaName()).execute();
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public boolean schemaExists() throws GoraException {
        String collectionIdentifier = sqlMapping.getTableClass();
        try {
            return dslContext
                    .meta()
                    .getTables()
                    .stream()
                    .anyMatch(table -> table.getName().equalsIgnoreCase(collectionIdentifier));
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public boolean exists(K key) throws GoraException {
        Boolean isExists = dslContext.fetchExists(dslContext.selectOne()
                .from(table(sqlMapping.getTableClass()))
                .where(field("id").eq(key)));

        return isExists;
    }

    @Override
    public T get(K key, String[] fields) throws GoraException {
        List<SelectField<?>> selectFields = new ArrayList<>();
        try {
            Boolean isExists = dslContext.fetchExists(dslContext.selectOne()
                    .from(table(sqlMapping.getTableClass()))
                    .where(field("id").eq(key)));
            if (isExists) {
                String[] dbFields = getFieldsToQuery(fields);
                for (String k : fields) {
                    String dbFieldName = k;
                    if (dbFieldName != null && dbFieldName.length() > 0) {
                        selectFields.add(field(dbFieldName));
                    }
                }
                org.jooq.Result<Record> result = dslContext.select(selectFields)
                        .from(table(sqlMapping.getTableClass()))
                        .where(field("id").eq(key)).fetch();

                return convertSqlTableToAvroBean(result.get(0), dbFields);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    public T convertSqlTableToAvroBean(Record record, String[] dbFields) throws GoraException {
        T persistent = newPersistent();
        Object value;
        byte[] byteStream;
        for (String f : dbFields) {
            Schema.Field field = fieldMap.get(f);
            Schema fieldSchema = field.schema();
            if (fieldSchema.getType() == Schema.Type.STRING || fieldSchema.getType() == Schema.Type.UNION) {
                value = record.getValue(field(f));
            } else {
                try {
                    Blob blob = (Blob) record.getValue(field(f));
                    byteStream = ((Blob) blob).getBytes(1, (int) ((Blob) blob).length());
                    String b = new String(byteStream, "UTF-8");
                    value = b;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
            persistent.put(field.pos(), value);
        }
        persistent.clearDirty();
        return persistent;
    }

    @Override
    public void put(K key, T obj) throws GoraException {
        if (obj.isDirty()) {
            org.jooq.Result<Record> result = dslContext.select()
                    .from(table(sqlMapping.getTableClass()))
                    .where(field("id").eq(key)).fetch();
            Map<String, Object> insertFields = convertAvroBeanToSqlTable(key, obj);
            Iterator<Map.Entry<String, Object>> iterator = insertFields.entrySet().iterator();
            InsertQuery<?> step = dslContext.insertQuery(DSL.table(sqlMapping.getTableClass()));
            UpdateQuery<Record> updateStep = dslContext.updateQuery(table(sqlMapping.getTableClass()));

            if (!result.isEmpty()) {
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    updateStep.addValue(field(entry.getKey()), entry.getValue());
                }
                updateStep.addConditions(field("id").eq(key));
                updateStep.execute();
            } else {

                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    step.addValue(field(entry.getKey()), entry.getValue());
                }
                step.newRecord();
                step.execute();

            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.info("Ignored putting persistent bean {} in the store as it is neither "
                        + "new, neither dirty.", new Object[]{obj});
            }
        }
    }

    @Override
    public boolean delete(K key) throws GoraException {
        dslContext.delete(table(sqlMapping.getTableClass()))
                .where(field("id").eq(key))
                .execute();
        return true;
    }

    @Override
    public long deleteByQuery(Query<K, T> query) throws GoraException {
        return 0;
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) throws GoraException {
        String[] fields = getFieldsToQuery(query.getFields());
        SqlQuery dataStoreQuery;
        if (query instanceof SqlQuery) {
            dataStoreQuery = ((SqlQuery) query);
        } else {
            dataStoreQuery = (SqlQuery) ((PartitionQueryImpl<K, T>) query).getBaseQuery();
        }
        try {
            org.jooq.Result<Record> result = dataStoreQuery.populateSqlQuery(sqlMapping, fields, sqlStoreParameters, getFields()).fetch();
            return new SqlResult<>(this, query, result, getFields());
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public Query<K, T> newQuery() {
        return new SqlQuery<>(this);
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        return null;
    }

    @Override
    public void flush() throws GoraException {
    }

    @Override
    public void close() {
        try {
            flush();
        } catch (Exception ex) {
            LOG.error("Error occurred while flushing data SQL store : ", ex);
        }
    }

    private Map<String, Object> convertAvroBeanToSqlTable(final K key, final T persistent) {
        Map<String, Object> result = new HashMap<>();
        Object value = null;
        for (Schema.Field f : persistent.getSchema().getFields()) {
            if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
                if (f.schema().getType() == Schema.Type.STRING) {
                    value = persistent.get(f.pos()).toString();
                } else
                    value = persistent.get(f.pos()).toString().getBytes(StandardCharsets.UTF_8);
            }
            result.put(f.name(), value);
        }
        result.put("id", key.toString());
        return result;
    }
}
