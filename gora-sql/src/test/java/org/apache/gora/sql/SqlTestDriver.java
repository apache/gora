package org.apache.gora.sql;

import org.apache.gora.GoraTestDriver;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.sql.store.SqlStoreParameters;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Properties;

public class SqlTestDriver extends GoraTestDriver {

    private static Logger log = LoggerFactory.getLogger(SqlTestDriver.class);

    //private GenericContainer sqlContainer;
    private GenericContainer sqlContainer;

    public SqlTestDriver(GenericContainer sqlContainer) {
        super(SqlStore.class);
        this.sqlContainer = sqlContainer;
    }


    public SqlTestDriver() {
        super(SqlStore.class);
    }

    @Override
    public void setUpClass() throws Exception {
        log.info("Setting up MySQL test driver");
        conf.set(SqlStoreParameters.SQL_SERVER_HOST, sqlContainer.getContainerIpAddress());
        conf.set(SqlStoreParameters.SQL_SERVER_PORT,
                sqlContainer.getMappedPort(3306).toString());
        log.info("MySQL Embedded Server started successfully.");
    }

    @Override
    public void tearDownClass() throws Exception {
        log.info("MySQL Embedded server instance terminated successfully.");
    }

    @Override
    public <K, T extends Persistent> DataStore<K, T> createDataStore(Class<K> keyClass,
                                                                     Class<T> persistentClass) throws GoraException {
        // Override properties (reads from gora.properties) using test container configuration (defined in #setUpClass)
        Properties props = DataStoreFactory.createProps();
        props.setProperty(SqlStoreParameters.SQL_SERVER_HOST,
                conf.get(SqlStoreParameters.SQL_SERVER_HOST));
        props.setProperty(SqlStoreParameters.SQL_SERVER_PORT,
                conf.get(SqlStoreParameters.SQL_SERVER_PORT));
        final DataStore<K, T> dataStore = DataStoreFactory
                .createDataStore((Class<? extends DataStore<K, T>>) dataStoreClass, keyClass,
                        persistentClass, conf, props);
        dataStores.add(dataStore);
        log.info("Datastore for {} was added.", persistentClass);
        return dataStore;
    }
}
