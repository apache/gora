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
package org.apache.gora.neo4j.store;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.gora.neo4j.mapping.Neo4jMapping;
import org.apache.gora.neo4j.mapping.Neo4jMappingBuilder;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Neo4j data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class Neo4jStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  protected static final String PARSE_MAPPING_FILE_KEY = "gora.neo4j.mapping.file";
  protected static final String DEFAULT_MAPPING_FILE = "gora-neo4j-mapping.xml";
  protected static final String XML_MAPPING_DEFINITION = "gora.mapping";

  public static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Neo4jMapping neo4jMapping;
  private Connection connection;

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file. Initialize is called when then
   * the call to {@link org.apache.gora.store.DataStoreFactory#createDataStore}
   * is made.
   *
   * @param keyClass Gora's key class
   * @param persistentClass Persistent class
   * @param properties Configurations for the data store
   * @throws org.apache.gora.util.GoraException Unexpected exception during
   * initialization
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);

      InputStream mappingStream;
      if (properties.containsKey(XML_MAPPING_DEFINITION)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("{} = {}", XML_MAPPING_DEFINITION, properties.getProperty(XML_MAPPING_DEFINITION));
        }
        mappingStream = IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
      } else {
        mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      }

      Neo4jMappingBuilder mappingBuilder = new Neo4jMappingBuilder(this);
      neo4jMapping = mappingBuilder.readMapping(mappingStream);
      Neo4jParameters load = Neo4jParameters.load(properties, getConf());
      connection = connectToServer(load);
      LOG.info("Neo4j store was successfully initialized");
      if (autoCreateSchema && !schemaExists()) {
        createSchema();
      }
    } catch (IOException | ClassNotFoundException | SQLException ex) {
      throw new GoraException(ex);
    }
  }

  private Connection connectToServer(Neo4jParameters params) throws ClassNotFoundException, GoraException, SQLException {
    Class.forName(Neo4jConstants.DRIVER_NAME);
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("jdbc:neo4j:");
    switch (params.getProtocol()) {
      case "Bolt":
        urlBuilder.append("bolt");
        break;
      case "Bolt+Routing":
        urlBuilder.append("bolt+routing");
        break;
      case "HTTP":
        urlBuilder.append("http");
        break;
      default:
        throw new GoraException("Incorrect protocol");
    }
    urlBuilder.append("://");
    urlBuilder.append(params.getHost());
    if (params.getPort() != null) {
      urlBuilder.append(":").append(params.getPort());
    }
    return DriverManager.getConnection(urlBuilder.toString(), params.getUsername(), params.getPassword());
  }

  @Override
  public String getSchemaName() {
    return this.neo4jMapping.getLabel();
  }

  @Override
  public void createSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
              "Impossible to create the schema as no connection has been initiated.");
    }
    if (schemaExists()) {
      return;
    }
    String create = "CREATE CONSTRAINT "
            + "gora_unique_" + this.neo4jMapping.getLabel() + this.neo4jMapping.getNodeKey().getName()
            + " ON (datastore:" + this.neo4jMapping.getLabel() + ") ASSERT datastore." + this.neo4jMapping.getNodeKey().getName() + " IS UNIQUE";
    try (PreparedStatement stmt = this.connection.prepareStatement(create)) {
      stmt.execute();
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
              "Impossible to delete the schema as no connection has been initiated.");
    }
    if (!schemaExists()) {
      return;
    }
    String delete = "DROP CONSTRAINT "
            + "gora_unique_" + this.neo4jMapping.getLabel() + this.neo4jMapping.getNodeKey().getName();
    try (PreparedStatement stmt = this.connection.prepareStatement(delete)) {
      stmt.execute();
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    boolean exits = false;
    Statement build = Cypher.call("db.constraints").build();
    Renderer defaultRenderer = Renderer.getDefaultRenderer();
    String render = defaultRenderer.render(build);
    try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          if (rs.getString("name").equals("gora_unique_" + this.neo4jMapping.getLabel() + this.neo4jMapping.getNodeKey().getName())) {
            exits = true;
          }
        }
      }
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return exits;
  }

  @Override
  public boolean exists(K key) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
  }

  @Override
  public void close() {
    try {
      connection.close();
      LOG.info("Neo4j datastore destroyed successfully.");
    } catch (SQLException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

}
