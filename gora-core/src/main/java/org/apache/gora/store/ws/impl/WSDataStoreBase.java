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

package org.apache.gora.store.ws.impl;

import java.util.Properties;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.StringUtils;

/**
 * A Base class for Avro persistent {@link DataStore}s.
 */
public abstract class WSDataStoreBase<K, T extends Persistent>
implements DataStore<K, T>{
	
  //protected BeanFactory<K, T> beanFactory;

  protected Class<K> keyClass;
  protected Class<T> persistentClass;

  /** The web service provider's name*/
  private String wsProvider;

  /** A map of field names to Field objects containing schema's fields 
  protected Map<String, Field> fieldMap; */

  /** The authentication object to be used for our provider*/
  protected Object authentication;

  /** Properties object */
  protected Properties properties;
  
  //TODO see if a webservice database will have these persistent datums
  //protected PersistentDatumReader<T> datumReader;
  //protected PersistentDatumWriter<T> datumWriter;

  public WSDataStoreBase() {
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws Exception {
    setKeyClass(keyClass);
    setPersistentClass(persistentClass);
    //TODO See if we need to create a factory to manage our beans
    //if(this.beanFactory == null)
    //  this.beanFactory = new BeanFactoryImpl<K, T>(keyClass, persistentClass);
  }

  @Override
  public void setPersistentClass(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  @Override
  public Class<T> getPersistentClass() {
    return persistentClass;
  }

  @Override
  public Class<K> getKeyClass() {
    return keyClass;
  }

  @Override
  public void setKeyClass(Class<K> keyClass) {
    if(keyClass != null)
      this.keyClass = keyClass;
  }
  /*
  @Override
  public K newKey() throws IOException {
    try {
      return beanFactory.newKey();
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public T newPersistent() throws IOException {
    try {
      return beanFactory.newPersistent();
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void setBeanFactory(BeanFactory<K, T> beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public BeanFactory<K, T> getBeanFactory() {
    return beanFactory;
  }

  @Override
  public T get(K key) throws Exception {
    return get(key, getFieldsToQuery(null));
  };
*/
  /**
   * Checks whether the fields argument is null, and if so
   * returns all the fields of the Persistent object, else returns the
   * argument.
   */
  /*protected String[] getFieldsToQuery(String[] fields) {
    if(fields != null) {
      return fields;
    }
    return beanFactory.getCachedPersistent().getFields();
  }*/

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

  //@Override
  //@SuppressWarnings("unchecked")
  public void readFields(Object in) throws Exception {
  /*  try {
      Class<K> keyClass = (Class<K>) ClassLoadingUtils.loadClass(Text.readString(in));
      Class<T> persistentClass = (Class<T>)ClassLoadingUtils.loadClass(Text.readString(in));
      Properties props = WritableUtils.readProperties(in);
      initialize(keyClass, persistentClass, props);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
    */
  }

  //@Override
  public void write(Object obj) throws Exception {
    /*Text.writeString(out, getKeyClass().getCanonicalName());
    Text.writeString(out, getPersistentClass().getCanonicalName());
    WritableUtils.writeProperties(out, properties);
    */
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof WSDataStoreBase) {
      @SuppressWarnings("rawtypes")
	  WSDataStoreBase that = (WSDataStoreBase) obj;
      return that.equals(obj);
     /* EqualsBuilder builder = new EqualsBuilder();
      builder.append(this.keyClass, that.keyClass);
      builder.append(this.persistentClass, that.persistentClass);
      return builder.isEquals();*/
    }
    return false;
  }

  @Override
  /** Default implementation deletes and recreates the schema*/
  public void truncateSchema() throws Exception {
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

  public String getWSProvider() {
	return wsProvider;
  }

  public void setWsProvider(String wsProvider) {
	this.wsProvider = wsProvider;
  }
}
