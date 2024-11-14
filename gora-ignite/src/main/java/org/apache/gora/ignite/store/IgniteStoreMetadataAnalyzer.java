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
package org.apache.gora.ignite.store;

import avro.shaded.com.google.common.collect.Lists;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.gora.ignite.utils.IgniteSQLBuilder;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;

public class IgniteStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

  private IgniteParameters igniteParameters;
  private Connection connection;

  @Override
  public void initialize() throws GoraException {
    try {
      Properties createProps = DataStoreFactory.createProps();
      igniteParameters = IgniteParameters.load(createProps);
      connection = IgniteStore.acquireConnection(igniteParameters);
    } catch (ClassNotFoundException | SQLException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public String getType() {
    return "IGNITE";
  }

  @Override
  public List<String> getTablesNames() throws GoraException {
    List<String> tabs = Lists.newArrayList();
    try (Statement stmt = connection.createStatement()) {
      ResultSet executeQuery = stmt.executeQuery(IgniteSQLBuilder.createSelectAllTablesNames(igniteParameters.getSchema()));
      while (executeQuery.next()) {
        tabs.add(executeQuery.getString(1));
      }
      executeQuery.close();
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    Collections.sort(tabs);
    return tabs;
  }

  @Override
  public IgniteTableMetadata getTableInfo(String tableName) throws GoraException {
    HashMap<String, String> hmap = new HashMap();
    String pkcl = "";
    String pkdt = "";
    try (Statement stmt = connection.createStatement()) {
      ResultSet executeQuery = stmt.executeQuery(IgniteSQLBuilder.createSelectAllColumnsOfTable(igniteParameters.getSchema(), tableName));
      while (executeQuery.next()) {
        if (executeQuery.getBoolean(3)) {
          pkcl = executeQuery.getString(1);
          pkdt = executeQuery.getString(2);
        } else {
          hmap.put(executeQuery.getString(1), executeQuery.getString(2));
        }

      }
      executeQuery.close();
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    /**
     * Special columns. Ignite uses _KEY column to represent primary key. Ignite
     * uses _VAL column to represent value.
     */
    hmap.remove("_KEY");
    hmap.remove("_VAL");
    return new IgniteTableMetadata(pkcl, pkdt, hmap);
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
