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
package org.apache.gora.store.impl;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;

/**
 * Metadata analyzer base class for one backend. The hierarchy based on this class
 * exposes some details of the backend.
 * This class is responsible of creating the connection to the backend with the given configuration,
 * getting the information about what Persistent entities could be created for that
 * backend based on the tables/namespaces/whatever of the backend , and telling what
 * fields exists in that table.
 * 
 * Notice that the method {@link #getTableInfo(String persistentName)} returns an Object,
 * but each class extending this abstract class (for each backend) will return a specific
 * class with the information. 
 */
public abstract class DataStoreMetadataAnalyzer implements Configurable {

  /**
   * Configuration from Hadoop/HBase/...
   */
  protected Configuration conf;

  /**
   * Properties for the data store.
   */
  protected Properties properties;
  
  /**
   * After been set the configuratin, this method is called.
   * This method should create the connection to the backend
   */
  public abstract void initialize() throws GoraException ;
  
  /**
   * This method allows to know the type of the backend to which it is connected.
   * Although this could be known at runtime when getting the instance from the DataStoreMetadataFactory,
   * it can be useful to have this method.
   * 
   * @return The backend type. Recommended in CAPITAL LETTERS
   */
  public abstract String getType() ;
  
  /**
   * Gets the list of tables/namespaces/entities, that could be accesed as a Persistent Entity, from the backend.
   * 
   * @return A list of the tables/namespaces/entities of the backend
   */
  public abstract List<String> getTablesNames() throws GoraException;
  
  /**
   * Returns the information of a table at the backend, information that could help building a schema
   * or mapping. Each backend has to implement a proper subclass adapted to the specific backend.
   * 
   * @return A specific subclass depending on the backend type
   */
  public abstract Object getTableInfo(String tableName) throws GoraException ;
  
  public abstract void close() throws IOException ;
  
  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }
  
  @Override
  public Configuration getConf() {
    return conf;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }
}
