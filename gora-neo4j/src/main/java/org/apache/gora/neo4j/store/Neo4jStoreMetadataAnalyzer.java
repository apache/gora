/*
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
package org.apache.gora.neo4j.store;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import jersey.repackaged.com.google.common.collect.Sets;
import org.apache.flink.shaded.guava18.com.google.common.collect.Lists;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;

/**
 * Metadata analyzer for Neo4j
 */
public class Neo4jStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

  private Neo4jParameters neo4jParameters;
  private Connection connection;

  @Override
  public void initialize() throws GoraException {
    try {
      Properties createProps = DataStoreFactory.createProps();
      neo4jParameters = Neo4jParameters.load(createProps, this.getConf());
      connection = Neo4jStore.connectToServer(neo4jParameters);
    } catch (ClassNotFoundException | SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public String getType() {
    return "NEO4J";
  }

  @Override
  public List<String> getTablesNames() throws GoraException {
    List<String> tables = Lists.newArrayList();
    try (PreparedStatement callStatement = this.connection.prepareStatement("SHOW CONSTRAINTS;")) {
      try (ResultSet rs = callStatement.executeQuery()) {
        while (rs.next()) {
          if (rs.getString("type").equals("NODE_KEY")) {
            String[] array = (String[]) rs.getArray("labelsOrTypes").getArray();
            tables.addAll(Lists.newArrayList(array));
          }
        }
      }
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return tables;
  }

  @Override
  public Object getTableInfo(String tableName) throws GoraException {
    String nodeKey = null;
    List<String> properties = Lists.newArrayList();

    try (PreparedStatement callStatement = this.connection.prepareStatement("SHOW CONSTRAINTS;")) {
      try (ResultSet rs = callStatement.executeQuery()) {
        while (rs.next()) {
          if (rs.getString("type").equals("NODE_PROPERTY_EXISTENCE")
                  && Sets.newConcurrentHashSet(Lists.newArrayList((String[]) rs.getArray("labelsOrTypes").getArray())).contains(tableName)) {
            String[] array = (String[]) rs.getArray("properties").getArray();
            properties.addAll(Lists.newArrayList(array));
          }
          if (rs.getString("type").equals("NODE_KEY")
                  && Sets.newConcurrentHashSet(Lists.newArrayList((String[]) rs.getArray("labelsOrTypes").getArray())).contains(tableName)) {
            String[] array = (String[]) rs.getArray("properties").getArray();
            nodeKey = array[0];
          }
        }
      }
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return new Neo4jTableMetadata(nodeKey, properties);
  }

  @Override
  public void close() throws IOException {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

}
