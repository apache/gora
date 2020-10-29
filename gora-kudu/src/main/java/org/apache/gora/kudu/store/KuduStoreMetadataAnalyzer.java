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
package org.apache.gora.kudu.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.gora.kudu.utils.KuduParameters;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduTable;

public class KuduStoreMetadataAnalyzer extends DataStoreMetadataAnalyzer {

  private KuduClient client;

  @Override
  public void initialize() throws GoraException {
    Properties createProps = DataStoreFactory.createProps();
    KuduParameters kuduParameters = KuduParameters.load(createProps, getConf());
    KuduClient.KuduClientBuilder kuduClientBuilder = new KuduClient.KuduClientBuilder(kuduParameters.getMasterAddresses());
    if (kuduParameters.getBossCount() != null) {
      kuduClientBuilder.bossCount(kuduParameters.getBossCount());
    }
    if (kuduParameters.getDefaultAdminOperationTimeoutMs() != null) {
      kuduClientBuilder.defaultAdminOperationTimeoutMs(kuduParameters.getDefaultAdminOperationTimeoutMs());
    }
    if (kuduParameters.getDefaultOperationTimeoutMs() != null) {
      kuduClientBuilder.defaultOperationTimeoutMs(kuduParameters.getDefaultOperationTimeoutMs());
    }
    if (kuduParameters.getDefaultSocketReadTimeoutMs() != null) {
      kuduClientBuilder.defaultSocketReadTimeoutMs(kuduParameters.getDefaultSocketReadTimeoutMs());
    }
    if (kuduParameters.getWorkerCount() != null) {
      kuduClientBuilder.workerCount(kuduParameters.getWorkerCount());
    }
    if (kuduParameters.isClientStatistics() != null && !kuduParameters.isClientStatistics()) {
      kuduClientBuilder.disableStatistics();
    }
    client = kuduClientBuilder.build();
  }

  @Override
  public String getType() {
    return "KUDU";
  }

  @Override
  public List<String> getTablesNames() throws GoraException {
    try {
      return client.getTablesList().getTablesList();
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public Object getTableInfo(String tableName) throws GoraException {
    try {
      KuduTable openTable = client.openTable(tableName);
      List<ColumnSchema> columns = openTable.getSchema().getColumns();
      HashMap<String, String> columnas = new HashMap();
      String pk = "";
      for (ColumnSchema esquema : columns) {
        columnas.put(esquema.getName(), esquema.getType().getName());
        if (esquema.isKey()) {
          pk = esquema.getName();
        }
      }
      return new KuduTableMetadata(pk, columnas);
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public void close() throws IOException {
    if (client != null) {
      client.close();
    }
  }

}
