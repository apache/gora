/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import org.apache.gora.benchmark.generated.User;
import org.apache.gora.util.GoraException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.Status;
import com.yahoo.ycsb.StringByteIterator;
import com.yahoo.ycsb.workloads.CoreWorkload;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * The Class GoraClientTest.
 */
public class GoraClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(GoraClientTest.class);
  private GoraBenchmarkClient benchmarkClient;
  private static HashMap<String, ByteIterator> DATA_TO_INSERT;
  private static HashMap<String, ByteIterator> DATA_TO_UPDATE;
  private MongodExecutable mongodExecutable;
  private MongodProcess mongodProcess;
  private MongoClient mongoClient;
  private static boolean isMongoDBSetupDone = false;

  /**
   * Setup MongoDB embed cluster. This function will auto provision a MongoDB
   * embeded cluster which will run locally on port 27017. It is called in the
   * {@link setUp() class} which is executed testUpdate after each test.
   */
  private void setupMongoDBCluster() {
    MongodStarter starter = MongodStarter.getDefaultInstance();
    String bindIp = Constants.LOCALHOST;
    int port = Constants.MONGO_DEFAULT_PORT;
    IMongodConfig mongodConfig = null;
    try {
      mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
          .net(new Net(bindIp, port, Network.localhostIsIPv6())).build();
    } catch (IOException e) {
      LOG.info("There is a problem in configuring MongoDB", e.getMessage(), e);
    }
    this.mongodExecutable = starter.prepare(mongodConfig);
    try {
      LOG.info("Starting MongDB Server on port " + bindIp + ":" + port);
      this.mongodProcess = mongodExecutable.start();
    } catch (IOException e) {
      LOG.info("Cannot Start MongDB Server on port " + bindIp + ":" + port, e.getMessage(), e);
      this.mongodProcess.stop();
      this.mongodExecutable.stop();
      if (this.mongoClient != null)
        this.mongoClient.close();
    }
    this.mongoClient = new MongoClient(bindIp, port);
  }

  /**
   * Sets the up. testUpdate Setup is executed before each test using
   * the @Before annotation of JUnit 4. Use @BeforeClass if you want to execute
   * a code block just once.
   * 
   * @throws Exception
   *           the exception files are auto-generated. I have the code to add
   *           the license file accordingly
   */
  @Before
  public void setUp() throws Exception {
    if (!isMongoDBSetupDone) {
      setupMongoDBCluster();
      isMongoDBSetupDone = true;
    }
    DATA_TO_INSERT = new HashMap<>();
    DATA_TO_UPDATE = new HashMap<>();
    for (int count = 0; count < Constants.TEST_NUMBER_OF_FIELDS; count++) {
      DATA_TO_INSERT.put(Constants.FIELD_PREFIX + count, new StringByteIterator(Constants.TEST_VALUE + count));
      DATA_TO_UPDATE.put(Constants.FIELD_PREFIX + count, new StringByteIterator(Constants.TEST_UPDATED + count));
    }
    Properties p = new Properties();
    p.setProperty(Constants.KEY_CLASS_KEY, Constants.KEY_CLASS_VALUE);
    p.setProperty(Constants.PERSISTENCE_CLASS_KEY, Constants.PERSISTENCE_CLASS_VALUE);
    p.setProperty(CoreWorkload.FIELD_COUNT_PROPERTY, Constants.TEST_NUMBER_OF_FIELDS + "");
    benchmarkClient = new GoraBenchmarkClient();
    benchmarkClient.setProperties(p);
    benchmarkClient.init();
  }

  /**
   * Clean up.
   *
   * @throws Exception
   *           the exception
   */
  @After
  public void cleanUp() throws Exception {
    if (benchmarkClient != null)
      benchmarkClient.cleanup();
    benchmarkClient = null;
  }

  /**
   * Read record.
   *
   * @param key
   *          the key
   * @return the user
   * @throws GoraException
   *           the gora exception
   */
  private User readRecord(String key) throws GoraException {
    User u = benchmarkClient.getDataStore().get(key);
    return u;
  }

  private void insertData() {
    benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_1, DATA_TO_INSERT);
    benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_2, DATA_TO_INSERT);
    benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_3, DATA_TO_INSERT);
  }

  /**
   * Test client initialisation.
   */
  @Test
  public void testClientInitialisation() {
    assertNotNull(benchmarkClient.getDataStore());
  }

  /**
   * files are auto-generated. I have the code to add the license file
   * accordingly Test insert.
   *
   * @throws GoraException
   *           the gora exception
   */
  @Test
  public void testInsert() throws GoraException {
    Status result1 = benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_1, DATA_TO_INSERT);
    Status result2 = benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_2, DATA_TO_INSERT);
    Status result3 = benchmarkClient.insert(Constants.TEST_TABLE, Constants.TEST_KEY_3, DATA_TO_INSERT);
    assertEquals(Status.OK, result1);
    assertEquals(Status.OK, result2);
    assertEquals(Status.OK, result3);
  }

  /**
   * Test read performs a read record test from the database.
   */
  @Test
  public void testRead() {
    insertData();
    HashMap<String, ByteIterator> results = new HashMap<>();
    Set<String> fields = new HashSet<>();// this could be null as well
    Status result = benchmarkClient.read(Constants.TEST_TABLE, Constants.TEST_KEY_1, fields, results);
    assertEquals(Status.OK, result);
    assertEquals(DATA_TO_INSERT.size(), results.size());
    assertEquals(DATA_TO_INSERT.get(Constants.TEST_FIELD_0).toString(), results.get(Constants.TEST_FIELD_0).toString());
    assertEquals(DATA_TO_INSERT.get(Constants.TEST_FIELD_0).toString(), Constants.TEST_VALUE_0);
  }

  /**
   * Test scan. Performs a range scan test for a set of records in the database.
   */
  @Test
  public void testScan() {
    insertData();
    Vector<HashMap<String, ByteIterator>> results = new Vector<HashMap<String, ByteIterator>>();
    Set<String> fields = new HashSet<>();
    Status result = benchmarkClient.scan(Constants.TEST_TABLE, Constants.TEST_KEY_1, 2, fields, results);
    assertEquals(result, Status.OK);
    assertEquals(results.size(), 2);
  }

  /**
   * Test update performs an update record test in the database
   *
   * @throws GoraException
   *           the gora exception
   */
  @Test
  public void testUpdate() throws GoraException {
    insertData();
    Status result = benchmarkClient.update(Constants.TEST_TABLE, Constants.TEST_KEY_1, DATA_TO_UPDATE);
    assertEquals(result, Status.OK);
    if (result == Status.OK) {
      benchmarkClient.getDataStore().flush();
      User u = readRecord(Constants.TEST_KEY_1);
      assertEquals(Constants.TEST_UPDATED_0, u.getField0().toString());
    }
  }

  /**
   * Test mapping file generation.
   */
  @Test
  public void testgenearateMappingFile() {
    GoraBenchmarkUtils.generateMappingFile(Constants.HBASE);
  }

  /**
   * Test AVRO schema schema generation.
   */
  @Test
  public void testgenerateAvroSchema() {
    GoraBenchmarkUtils.generateAvroSchema(Constants.TEST_NUMBER_OF_FIELDS);
  }

  /**
   * Test generate data beans.
   */
  @Test
  public void testGenerateDataBeans() {
    GoraBenchmarkUtils.generateDataBeans();
  }

}
