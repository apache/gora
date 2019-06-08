/*
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
package org.apache.gora.kudu.store;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.gora.kudu.mapping.KuduMapping;
import org.apache.gora.kudu.mapping.KuduMappingBuilder;
import org.apache.gora.kudu.utils.KuduParameters;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Apache Kudu data store to be used by Apache Gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class KuduStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(KuduStore.class);
  private static final String PARSE_MAPPING_FILE_KEY = "gora.kudu.mapping.file";
  private static final String DEFAULT_MAPPING_FILE = "gora-kudu-mapping.xml";
  private KuduParameters kuduParameters;
  private KuduMapping kuduMapping;
  private KuduClient client;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);
      KuduMappingBuilder<K, T> builder = new KuduMappingBuilder<K, T>(this);
      builder.readMappingFile(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      kuduMapping = builder.getKuduMapping();
      kuduParameters = KuduParameters.load(properties, getConf());
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

      LOG.info("Kudu store was successfully initialized");
      if (!schemaExists()) {
        createSchema();
      }
    } catch (Exception ex) {
      LOG.error("Error while initializing Ignite store", ex);
      throw new GoraException(ex);
    }
  }

  @Override
  public String getSchemaName() {
    return kuduMapping.getTableName();
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
    try {
      client.deleteTable(kuduMapping.getTableName());
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    try {
      return client.tableExists(kuduMapping.getTableName());
    } catch (KuduException ex) {
      throw new GoraException(ex);
    }
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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void close() {
    try {
      client.close();
      LOG.info("Kudu datastore destroyed successfully.");
    } catch (KuduException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }

}
