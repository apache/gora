package org.apache.gora.cassandra.store;

import org.apache.gora.cassandra.GoraCassandraTestDriver;
import org.apache.gora.cassandra.test.nativeSerialization.User;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

/**
 * This class tests Cassandra Store functionality with Cassandra Native Serialization
 */
public class TestCassandraStoreWithNativeSerialization {
  private static GoraCassandraTestDriver testDriver = new GoraCassandraTestDriver();
  private static DataStore<UUID, User> userDataStore;
  private static Properties parameter;

  @BeforeClass
  public static void setUpClass() throws Exception {
    setProperties();
    testDriver.setParameters(parameter);
    testDriver.setUpClass();
    userDataStore = testDriver.createDataStore(UUID.class, User.class);

  }

  private static void setProperties() {
    parameter = new Properties();
    parameter.setProperty(CassandraStoreParameters.CASSANDRA_SERVERS,"localhost");
    parameter.setProperty(CassandraStoreParameters.PORT,"9160");
    parameter.setProperty(CassandraStoreParameters.USE_CASSANDRA_NATIVE_SERIALIZATION, "true");
  }


  @After
  public void tearDown() throws Exception {
    testDriver.tearDown();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    testDriver.tearDownClass();
  }

  @Test
  public void testCreate() {
    UUID id = UUID.randomUUID();
    User user1 = new User(id, "madhawa", Date.from(Instant.now()));
    userDataStore.put(id, user1);
  }
}
