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
/**
 * @author Renato Marroquin
 */
package org.apache.gora.store.ws.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.StringUtils;

/**
 * A Base class for persistent objects{@link DataStore}s.
 */
public abstract class WSDataStoreBase<K, T extends Persistent>
implements DataStore<K, T>{
	
  /**
   * Class of the key to be used
   */
  protected Class<K> keyClass;
  
  /**
   * Class of the persistent object
   */
  protected Class<T> persistentClass;

  /** 
   * The web service provider's name
   */
  private String wsProvider;

  /** 
   * The authentication object to be used for our provider
   */
  protected Object authentication;

  /** 
   * Properties object 
   */
  protected Properties properties;

  /**
   * Default constructor
   */
  public WSDataStoreBase() {
  }

  @Override
  /**
   * Initializes the web services backed data store
   */
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    setKeyClass(keyClass);
    setPersistentClass(persistentClass);
  }

  @Override
  /**
   * Sets the persistent class to be used
   */
  public void setPersistentClass(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  @Override
  /**
   * Gets the persistent class being used
   */
  public Class<T> getPersistentClass() {
    return persistentClass;
  }

  @Override
  /**
   * Gets the key class being used
   */
  public Class<K> getKeyClass() {
    return keyClass;
  }

  @Override
  /**
   * Sets the key class to be used
   */
  public void setKeyClass(Class<K> keyClass) {
    if(keyClass != null)
      this.keyClass = keyClass;
  }

  /**
   * Gets the configuration (authentication) object
   * @return Object containing the authentication values
   */
  public Object getConf() {
    return authentication;
  }
  
  /**
   * Sets the configuration (authentication) object
   */
  public void setConf(Object auth) {
    this.authentication = auth;
  }
  
  /**
   * Reads fields from an object
   * @param obj
   * @throws Exception
   */
  public void readFields(Object obj) throws Exception {
  }

  /**
   * Writes an object
   * @param obj
   * @throws Exception
   */
  public void write(Object obj) throws Exception {
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof WSDataStoreBase) {
      @SuppressWarnings("rawtypes")
      WSDataStoreBase that = (WSDataStoreBase) obj;
      return that.equals(this);
    }
    return false;
  }

  @Override
  /** Default implementation deletes and recreates the schema*/
  public void truncateSchema() {
    deleteSchema();
    createSchema();
  }

  /**
   * Returns the name of the schema to use for the persistent class. 
   * 
   * First the schema name in the defined properties is returned. If null then
   * the provided mappingSchemaName is returned. If this is null too,
   * the class name, without the package, of the persistent class is returned.
   * @param mappingSchemaName the name of the schema as read from the mapping file
   * @param persistentClass persistent class
   */
  protected String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    String schemaName = WSDataStoreFactory.getDefaultSchemaName(properties, this);
    if(schemaName != null) {
      return schemaName;
    }

    if(mappingSchemaName != null) {
      return mappingSchemaName;
    }

    return StringUtils.getClassname(persistentClass);
  }

  /**
   * Gets web service provider name
   * @return
   */
  public String getWSProvider() {
    return wsProvider;
  }

  /**
   * Sets web service provider name
   * @param wsProvider
   */
  public void setWsProvider(String wsProvider) {
    this.wsProvider = wsProvider;
  }
}
