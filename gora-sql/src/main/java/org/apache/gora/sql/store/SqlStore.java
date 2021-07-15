package org.apache.gora.sql.store;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

public class SqlStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    private DSLContext dslContext;
    private SqlStoreParameters sqlStoreParameters;
    private SqlMapping sqlMapping;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        //super.initialize(keyClass, persistentClass, properties);

        try {
            sqlStoreParameters = SqlStoreParameters.load(properties);
            Class.forName(sqlStoreParameters.getSqlDriverName());
            Connection connection = DriverManager.getConnection(sqlStoreParameters.getServerHost(),
                    sqlStoreParameters.getUserName(), sqlStoreParameters.getUserPassword());

            dslContext = DSL.using(connection, SQLDialect.MYSQL);
            dslContext.createDatabaseIfNotExists(sqlStoreParameters.getDatabaseName()).execute();

            SqlMappingBuilder<K, T> builder = new SqlMappingBuilder<>(this);
            sqlMapping = builder.fromFile(sqlStoreParameters.getMappingFile()).build();
            if (!schemaExists()) {
                createSchema();
            }

        } catch (Exception e) {
            LOG.error("Error while initializing SQL dataStore: {}",
                    new Object[]{e.getMessage()});
            throw new RuntimeException(e);
        }


    }

    @Override
    public String getSchemaName() {
        return null;
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
            dslContext.createTableIfNotExists(sqlMapping.getTableClass())
                    .execute();
        } catch (Exception e) {
            throw new GoraException(e);
        }


    }

    @Override
    public void deleteSchema() throws GoraException {

    }

    @Override
    public boolean schemaExists() throws GoraException {
        String collectionIdentifier = sqlMapping.getTableClass();
        try {
            return dslContext
                    .meta()
                    .getTables()
                    .stream()
                    .anyMatch(table -> table.equals(collectionIdentifier));
        } catch (Exception e) {
            throw new GoraException(e);
        }
    }

    @Override
    public boolean exists(K key) throws GoraException {
        return false;
    }

    @Override
    public T get(K key, String[] fields) throws GoraException {
        return null;
    }

    @Override
    public void put(K key, T obj) throws GoraException {

    }

    @Override
    public boolean delete(K key) throws GoraException {
        return false;
    }

    @Override
    public long deleteByQuery(Query<K, T> query) throws GoraException {
        return 0;
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) throws GoraException {
        return null;
    }

    @Override
    public Query<K, T> newQuery() {
        return null;
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

    }
}
