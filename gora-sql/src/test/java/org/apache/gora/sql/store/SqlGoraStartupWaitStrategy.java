package org.apache.gora.sql.store;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

public class SqlGoraStartupWaitStrategy extends LogMessageWaitStrategy {

    private static final String regEx = ".*port: 3306  MySQL Community Server - GPL.*";

    public SqlGoraStartupWaitStrategy() {
        withRegEx(regEx);
    }
}
