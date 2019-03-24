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
package org.apache.gora.store;

import java.util.Properties;

import org.apache.gora.store.impl.DataStoreMetadataAnalyzer;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.ReflectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create Metadata Analyzers for the backends.
 * It uses the properties from gora.properties, but can be configured with other properties.
 * 
 * The returned Metadata Analyzer defaults to *StoreMetadataAnalyzer, but can be configured. By default detects the backend storage
 * taking it from the Gora Properties (which defaults to gora.properties).
 */
public class DataStoreMetadataFactory {

  public static final Logger log = LoggerFactory.getLogger(DataStoreMetadataFactory.class);

  private DataStoreMetadataFactory() {
  }

  /**
   * Creates a metadata analyzer given the Hadoop Configuration instance. Uses the default gora.properties where it takes the
   * base name from the default store to infer the metadata analyzer to create.
   * @throws GoraException
   * @throws ClassNotFoundException - Exception thrown when does not exists a suitable metadata analyzer class
   */
  public static DataStoreMetadataAnalyzer createAnalyzer(Configuration configuration) throws GoraException, ClassNotFoundException {
    return createAnalyzer(configuration, DataStoreFactory.createProps());
  }
  
  /**
   * Creates a metadata analyzer given the Hadoop Configuration instance and the gora properties to use. It uses this properties
   * to infer the metadata analyzer to create.
   * @throws GoraException
   * @throws ClassNotFoundException - Exception thrown when does not exists a suitable metadata analyzer class
   */
  public static DataStoreMetadataAnalyzer createAnalyzer(Configuration configuration, Properties properties) throws GoraException, ClassNotFoundException {    
    String metadataAnalyzerClassName = DataStoreFactory.getDefaultDataStore(properties) + "MetadataAnalyzer";
    return createAnalyzer(metadataAnalyzerClassName, configuration, properties) ;
  }

  /**
   * Creates the metadata analyzer with the name given. 
   * @throws GoraException
   * @throws ClassNotFoundException - Exception thrown when does not exists a suitable metadata analyzer class
   */
  public static DataStoreMetadataAnalyzer createAnalyzer(String metadataAnalyzerClassName, Configuration configuration) throws GoraException, ClassNotFoundException {
    return createAnalyzer(metadataAnalyzerClassName, configuration, DataStoreFactory.createProps()) ;
  }

  /**
   * Main factory method that creates a Metadata Analyzer given a metadata analyzer class name, a Hadoop Configuration instance and Gora Properties
   * @throws GoraException
   * @throws ClassNotFoundException - Exception thrown when does not exists a suitable metadata analyzer class
   */
  public static DataStoreMetadataAnalyzer createAnalyzer(String metadataAnalyzerClassName, Configuration configuration, Properties properties) throws GoraException, ClassNotFoundException {
    try {
      DataStoreMetadataAnalyzer metadataAnalyzer = (DataStoreMetadataAnalyzer) ReflectionUtils.newInstance(metadataAnalyzerClassName);
      metadataAnalyzer.setConf(configuration);
      metadataAnalyzer.initialize();
      return metadataAnalyzer;
    } catch (ClassNotFoundException e) {
      throw e ;
    } catch (GoraException e) {
      throw e ;
    } catch (Exception e) {
      throw new GoraException(e);
    }    
  }
  
}
