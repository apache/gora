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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.gora.avro.PersistentDatumReader;
import org.apache.gora.avro.PersistentDatumWriter;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;

/**
 * A Base class for {@link DataStore}s.
 */
public abstract class DataStoreBase<K, T extends Persistent>
implements DataStore<K, T> {

  protected BeanFactory<K, T> beanFactory;

  protected Class<K> keyClass;
  protected Class<T> persistentClass;

  /** The schema of the persistent class*/
  protected Schema schema;

  /** A map of field names to Field objects containing schema's fields*/
  protected Map<String, Field> fieldMap;

  protected Configuration conf;

  protected boolean autoCreateSchema;

  protected Properties properties;

  protected PersistentDatumReader<T> datumReader;

  protected PersistentDatumWriter<T> datumWriter;

  public DataStoreBase() {
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws IOException {
    setKeyClass(keyClass);
    setPersistentClass(persistentClass);
    if(this.beanFactory == null)
      this.beanFactory = new BeanFactoryImpl<K, T>(keyClass, persistentClass);
    schema = this.beanFactory.getCachedPersistent().getSchema();
    fieldMap = AvroUtils.getFieldMap(schema);

    autoCreateSchema = DataStoreFactory.getAutoCreateSchema(properties, this);
    this.properties = properties;

    datumReader = new PersistentDatumReader<T>(schema, false);
    datumWriter = new PersistentDatumWriter<T>(schema, false);
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
public T get(K key) throws IOException {
    return get(key, null);
  };

  /**
   * Checks whether the fields argument is null, and if so
   * returns all the fields of the Persistent object, else returns the
   * argument.
   */
  protected String[] getFieldsToQuery(String[] fields) {
    if(fields != null) {
      return fields;
    }
    return beanFactory.getCachedPersistent().getFields();
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  protected Configuration getOrCreateConf() {
    if(conf == null) {
      conf = new Configuration();
    }
    return conf;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void readFields(DataInput in) throws IOException {
    try {
      Class<K> keyClass = (Class<K>) Class.forName(Text.readString(in));
      Class<T> persistentClass = (Class<T>)Class.forName(Text.readString(in));
      initialize(keyClass, persistentClass, DataStoreFactory.properties);

    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Text.writeString(out, getKeyClass().getCanonicalName());
    Text.writeString(out, getPersistentClass().getCanonicalName());
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof DataStoreBase) {
      @SuppressWarnings("rawtypes")
	  DataStoreBase that = (DataStoreBase) obj;
      EqualsBuilder builder = new EqualsBuilder();
      builder.append(this.keyClass, that.keyClass);
      builder.append(this.persistentClass, that.persistentClass);
      return builder.isEquals();
    }
    return false;
  }

  @Override
  /** Default implementation deletes and recreates the schema*/
  public void truncateSchema() throws IOException {
    deleteSchema();
    createSchema();
  }

  /**
   * Returns the name of the schema to use for the persistent class. If the mapping schema name is
   * provided it is returned first, else the properties file is searched, and the default schema name is
   * returned if found. Else, the class name, without the package, of the persistent class is returned.
   * @param mappingSchemaName the name of the schema as read from the mapping file
   * @param persistentClass persistent class
   */
  protected String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    String schemaName = DataStoreFactory.getDefaultSchemaName(properties, this);
    if(schemaName != null) {
      return schemaName;
    }

    if(mappingSchemaName != null) {
      return mappingSchemaName;
    }

    return StringUtils.getClassname(persistentClass);
  }
}
