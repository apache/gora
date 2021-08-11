package org.apache.gora.sql.store;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.sql.query.SqlQuery;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.jooq.CreateTableColumnStep;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.jooq.impl.DSL.primaryKey;

public class SqlStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    private DSLContext dslContext;
    private SqlStoreParameters sqlStoreParameters;
    private SqlMapping sqlMapping;
    private Connection connection;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        super.initialize(keyClass, persistentClass, properties);

        try {
            sqlStoreParameters = SqlStoreParameters.load(properties);
            String url = "jdbc:mysql://"+sqlStoreParameters.getServerHost()+":" + sqlStoreParameters.getServerPort()
                    + "/"+sqlStoreParameters.getDatabaseName();

            connection = DriverManager.getConnection(url,
                    sqlStoreParameters.getUserName(), sqlStoreParameters.getUserPassword());

            dslContext = DSL.using(connection, SQLDialect.MYSQL);
            dslContext.createDatabaseIfNotExists(sqlStoreParameters.getDatabaseName()).execute();

            SqlMappingBuilder<K, T> builder = new SqlMappingBuilder<>(this);
            sqlMapping = builder.fromFile(sqlStoreParameters.getMappingFile()).build();

        } catch (SQLException e) {
            LOG.error("Error while initializing SQL dataStore: {}",
                    new Object[]{e.getMessage()});
            throw new RuntimeException(e);
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
                    step = step.column(entry.getKey(), SQLDataType.INTEGER);

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
        if(schemaExists())
            dslContext.dropTable(getSchemaName()).execute();

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
            LOG.error("Error occurred while flushing data to RethinkDB : ", ex);
        }
    }
}
