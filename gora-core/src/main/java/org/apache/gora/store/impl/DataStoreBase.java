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

import java.io.Closeable;
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
import org.apache.gora.avro.store.AvroStore;
import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.StringUtils;
import org.apache.gora.util.WritableUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Base class for Avro persistent {@link DataStore}s.
 */
public abstract class DataStoreBase<K, T extends PersistentBase>
implements DataStore<K, T>, Configurable, Writable, Closeable {
	
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
  
  public static final Logger LOG = LoggerFactory.getLogger(AvroStore.class);

  public DataStoreBase() {
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
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
  public K newKey() {
    try {
      return beanFactory.newKey();
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      return null;
    }
  }

  @Override
  public T newPersistent() {
    try {
      return beanFactory.newPersistent();
    } catch (Exception ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
      return null;
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
  public T get(K key) {
    return get(key, getFieldsToQuery(null));
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

  @SuppressWarnings("unchecked")
  public void readFields(DataInput in) {
    try {
      Class<K> keyClass = (Class<K>) ClassLoadingUtils.loadClass(Text.readString(in));
      Class<T> persistentClass = (Class<T>)ClassLoadingUtils.loadClass(Text.readString(in));
      Properties props = WritableUtils.readProperties(in);
      initialize(keyClass, persistentClass, props);
    } catch (ClassNotFoundException ex) {
      LOG.error(ex.getMessage());
      LOG.error(ex.getStackTrace().toString());
    } catch (IOException e) {
      LOG.error(e.getMessage());
      LOG.error(e.getStackTrace().toString());
    }
  }

  public void write(DataOutput out) {
    try {
      Text.writeString(out, getKeyClass().getCanonicalName());
      Text.writeString(out, getPersistentClass().getCanonicalName());
      WritableUtils.writeProperties(out, properties);
    } catch (IOException e) {
      LOG.error(e.getMessage());
      LOG.error(e.getStackTrace().toString());
    }
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
  public void truncateSchema() {
    deleteSchema();
    createSchema();
  }

  /**
   * Returns the name of the schema to use for the persistent class. 
   * 
   * The schema name is prefixed with schema.prefix from {@link Configuration}.
   * The schema name in the defined properties is returned. If null then
   * the provided mappingSchemaName is returned. If this is null too,
   * the class name, without the package, of the persistent class is returned.
   * @param mappingSchemaName the name of the schema as read from the mapping file
   * @param persistentClass persistent class
   */
  protected String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    String prefix = getOrCreateConf().get("schema.prefix","");
    
    String schemaName = DataStoreFactory.getDefaultSchemaName(properties, this);
    if(schemaName != null) {
      return prefix+schemaName;
    }

    if(mappingSchemaName != null) {
      return prefix+mappingSchemaName;
    }

    return prefix+StringUtils.getClassname(persistentClass);
  }
}
