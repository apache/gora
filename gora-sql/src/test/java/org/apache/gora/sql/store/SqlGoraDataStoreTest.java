package org.apache.gora.sql.store;

import org.apache.gora.sql.SqlTestDriver;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.DataStoreTestBase;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Properties;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class SqlGoraDataStoreTest extends DataStoreTestBase {

    private static final Logger log = LoggerFactory.getLogger(SqlGoraDataStoreTest.class);

    private static final String DOCKER_CONTAINER_NAME = "mysql/mysql-server:latest";

    @ClassRule
    public static GenericContainer sqlContainer = new GenericContainer(DOCKER_CONTAINER_NAME)
            .withEnv("MYSQL_ROOT_HOST", "%")
            .withEnv("MYSQL_ROOT_PASSWORD", SqlStoreParameters.load(DataStoreFactory.createProps()).getUserPassword())
            .withEnv("MYSQL_DATABASE", SqlStoreParameters.load(DataStoreFactory.createProps()).getDatabaseName())
            .waitingFor(new SqlGoraStartupWaitStrategy())
            .withStartupTimeout(Duration.ofSeconds(60));

    @BeforeClass
    public static void setUpClass() throws Exception {
        setTestDriver(new SqlTestDriver(sqlContainer));
        DataStoreTestBase.setUpClass();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        DataStoreTestBase.tearDownClass();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Ignore("3 types union field is not supported by SQL store.")
    @Override
    public void testGet3UnionField() throws Exception {
        //3 types union field is not supported by OrientDBStore.
    }

    @Override
    public void assertSchemaExists(String schemaName) throws Exception {
        if (schemaName == "Employee")
            assertTrue(employeeStore.schemaExists());
        else
            assertFalse(webPageStore.schemaExists());
    }
}
