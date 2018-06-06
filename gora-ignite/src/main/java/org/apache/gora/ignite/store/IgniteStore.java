/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.ignite.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Ignite data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class IgniteStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(IgniteStore.class);
  private static final String PARSE_MAPPING_FILE_KEY = "gora.ignite.mapping.file";
  private static final String DEFAULT_MAPPING_FILE = "gora-ignite-mapping.xml";
  private IgniteParameters igniteParameters;
  private IgniteMapping igniteMapping;
  private Connection connection;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {

    try {
      super.initialize(keyClass, persistentClass, properties);
      IgniteMappingBuilder builder = new IgniteMappingBuilder(this);
      builder.readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      igniteMapping = builder.getIgniteMapping();
      igniteParameters = IgniteParameters.load(properties, conf);
      connection = acquiereConnection();
      LOG.info("Ignite store was successfully initialized");
    } catch (ClassNotFoundException | SQLException ex) {
      LOG.error("Error while initializing Ignite store", ex);
      throw new GoraException(ex);
    }
  }

  private Connection acquiereConnection() throws ClassNotFoundException, SQLException {
    Class.forName("org.apache.ignite.IgniteJdbcThinDriver");
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("jdbc:ignite:thin://");
    urlBuilder.append(igniteParameters.getHost());
    if (igniteParameters.getPort() != null) {
      urlBuilder.append(":" + igniteParameters.getPort());
    }
    if (igniteParameters.getSchema() != null) {
      urlBuilder.append("/" + igniteParameters.getSchema());
    }
    if (igniteParameters.getUser() != null) {
      urlBuilder.append(";" + igniteParameters.getUser());
    }
    if (igniteParameters.getPassword() != null) {
      urlBuilder.append(";" + igniteParameters.getPassword());
    }
    if (igniteParameters.getAdditionalConfigurations() != null) {
      urlBuilder.append(igniteParameters.getAdditionalConfigurations());
    }
    Connection conn = DriverManager.getConnection(urlBuilder.toString());
    return conn;
  }

  @Override
  public String getSchemaName() {
    return igniteMapping.getTableName();
  }

  @Override
  public String getSchemaName(final String mappingSchemaName,
      final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  @Override
  public void createSchema() throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteSchema() throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean schemaExists() throws GoraException {
    boolean exists = false;
    try (Statement stmt = connection.createStatement()) {
      MessageFormat messageFormat = new MessageFormat("select * from {0} limit 0", Locale.getDefault());
      ResultSet executeQuery = stmt.executeQuery(messageFormat.format(igniteMapping.getTableName()));
      executeQuery.close();
      exists = true;
    } catch (SQLException ex) {
      /**
       * a 42000 error code is thrown by Ignite when a non-existent table
       * queried. More details:
       * https://apacheignite-sql.readme.io/docs/jdbc-error-codes
       */
      if (ex.getSQLState() != null && ex.getSQLState().equals("42000")) {
        exists = false;
      } else {
        throw new GoraException(ex);
      }
    }
    return exists;
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void put(K key, T obj) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean delete(K key) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Query<K, T> newQuery() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void flush() throws GoraException {
    try {
      connection.commit();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void close() {
    try {
      connection.close();
      LOG.info("Ignite datastore destroyed successfully.");
    } catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

}
