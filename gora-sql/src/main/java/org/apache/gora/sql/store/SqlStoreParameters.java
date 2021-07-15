package org.apache.gora.sql.store;

import java.util.Properties;

public class SqlStoreParameters {
    public static final String SQL_MAPPING_FILE = "/home/infaz/projects/gora/gora-sql/src/test/resources/gora-sql-mapping.xml";
    public static final String SQL_SERVER_HOST = "gora.sql.server.host";
    public static final String SQL_SERVER_PORT = "gora.sql.server.port";
    public static final String SQL_USER_USERNAME = "gora.sql.user.username";
    public static final String SQL_USER_PASSWORD = "gora.sql.user.password";
    public static final String SQL_DB_NAME = "gora.sql.database.name";
    public static final String SQL_DRIVER_NAME = "gora.sql.jdbc.driver";

    private String serverHost;
    private String serverPort;
    private String userName;
    private String userPassword;
    private String databaseName;
    private String sqlDriverName;

    public String getMappingFile() {
        return this.SQL_MAPPING_FILE;
    }

    public String getServerHost() {
        return this.serverHost;
    }

    public String getServerPort() {
        return this.serverPort;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getUserPassword() {
        return this.userPassword;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getSqlDriverName() {
        return this.sqlDriverName;
    }

    public SqlStoreParameters(String serverHost,
                              String serverPort,
                              String userName,
                              String userPassword,
                              String databaseName,
                              String sqlDriverName) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.userName = userName;
        this.userPassword = userPassword;
        this.databaseName = databaseName;
        this.sqlDriverName = sqlDriverName;
    }


    public static SqlStoreParameters load(Properties properties) {
        //String propMappingFile = properties.getProperty(SQL_MAPPING_FILE);
        String propServerHost = properties.getProperty(SQL_SERVER_HOST);
        String propServerPort = properties.getProperty(SQL_SERVER_PORT);
        String propUserName = properties.getProperty(SQL_USER_USERNAME);
        String propUserPassword = properties.getProperty(SQL_USER_PASSWORD, "");
        String propDatabaseName = properties.getProperty(SQL_DB_NAME);
        String propSqlDriverName = properties.getProperty(SQL_DRIVER_NAME);
        return new SqlStoreParameters(propServerHost, propServerPort, propUserName,
                propUserPassword, propDatabaseName, propSqlDriverName);
    }
}
