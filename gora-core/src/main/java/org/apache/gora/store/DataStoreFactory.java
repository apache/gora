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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.ReflectionUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;

/**
 * A Factory for {@link DataStore}s. DataStoreFactory instances are thread-safe.
 */
public class DataStoreFactory {

  public static final Logger log = LoggerFactory.getLogger(DataStoreFactory.class);

  public static final String GORA_DEFAULT_PROPERTIES_FILE = "gora.properties";

  public static final String GORA_DEFAULT_DATASTORE_KEY = "gora.datastore.default";

  public static final String GORA = "gora";

  public static final String DATASTORE = "datastore";

  private static final String GORA_DATASTORE = GORA + "." + DATASTORE + ".";

  public static final String AUTO_CREATE_SCHEMA = "autocreateschema";

  public static final String INPUT_PATH  = "input.path";

  public static final String OUTPUT_PATH = "output.path";

  public static final String MAPPING_FILE = "mapping.file";

	public static final String SCHEMA_NAME = "schema.name";

  /**
   * Do not use! Deprecated because it shares system wide state. 
   * Use {@link #createProps()} instead.
   */
  @Deprecated()
  public static final Properties properties = createProps();
  
  /**
   * Creates a new {@link Properties}. It adds the default gora configuration
   * resources. This properties object can be modified and used to instantiate
   * store instances. It is recommended to use a properties object for a single
   * store, because the properties object is passed on to store initialization
   * methods that are able to store the properties as a field.   
   * @return The new properties object.
   */
  public static Properties createProps() {
    try {
    Properties properties = new Properties();
      InputStream stream = DataStoreFactory.class.getClassLoader()
        .getResourceAsStream(GORA_DEFAULT_PROPERTIES_FILE);
      if(stream != null) {
        try {
          properties.load(stream);
          return properties;
        } finally {
          stream.close();
        }
      } else {
        log.warn(GORA_DEFAULT_PROPERTIES_FILE + " not found, properties will be empty.");
      }
      return properties;
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private DataStoreFactory() { }

  private static <K, T extends Persistent> void initializeDataStore(
      DataStore<K, T> dataStore, Class<K> keyClass, Class<T> persistent,
      Properties properties) throws IOException {
    dataStore.initialize(keyClass, persistent, properties);
  }

  /**
   * Instantiate a new {@link DataStore}. Uses default properties. Uses 'null' schema.
   * 
   * @param dataStoreClass The datastore implementation class.
   * @param keyClass The key class.
   * @param persistent The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  public static <D extends DataStore<K,T>, K, T extends Persistent>
  D createDataStore(Class<D> dataStoreClass
      , Class<K> keyClass, Class<T> persistent, Configuration conf) throws GoraException {
    return createDataStore(dataStoreClass, keyClass, persistent, conf, createProps(), null);
  }

  /**
   * Instantiate a new {@link DataStore}. Uses default properties.
   * 
   * @param dataStoreClass The datastore implementation class.
   * @param keyClass The key class.
   * @param persistent The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @param schemaName A default schemaname that will be put on the properties.
   * @return A new store instance.
   * @throws GoraException
   */
  public static <D extends DataStore<K,T>, K, T extends Persistent>
  D createDataStore(Class<D> dataStoreClass , Class<K> keyClass, 
      Class<T> persistent, Configuration conf, String schemaName) throws GoraException {
    return createDataStore(dataStoreClass, keyClass, persistent, conf, createProps(), schemaName);
  }

  /**
   * Instantiate a new {@link DataStore}.
   * 
   * @param dataStoreClass The datastore implementation class.
   * @param keyClass The key class.
   * @param persistent The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @param properties The properties to be used be the store.
   * @param schemaName A default schemaname that will be put on the properties.
   * @return A new store instance.
   * @throws GoraException
   */
  public static <D extends DataStore<K,T>, K, T extends Persistent>
  D createDataStore(Class<D> dataStoreClass, Class<K> keyClass
      , Class<T> persistent, Configuration conf, Properties properties, String schemaName) 
  throws GoraException {
    try {
      setDefaultSchemaName(properties, schemaName);
      D dataStore =
        ReflectionUtils.newInstance(dataStoreClass);
      if ((dataStore instanceof Configurable) && conf != null) {
        ((Configurable)dataStore).setConf(conf);
      }
      initializeDataStore(dataStore, keyClass, persistent, properties);
      return dataStore;

    } catch (GoraException ex) {
      throw ex;
    } catch(Exception ex) {
      throw new GoraException(ex);
    }
  }

  /**
   * Instantiate a new {@link DataStore}. Uses 'null' schema.
   * 
   * @param dataStoreClass The datastore implementation class.
   * @param keyClass The key class.
   * @param persistent The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @param properties The properties to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  public static <D extends DataStore<K,T>, K, T extends Persistent>
  D createDataStore(Class<D> dataStoreClass
      , Class<K> keyClass, Class<T> persistent, Configuration conf, Properties properties) 
  throws GoraException {
    return createDataStore(dataStoreClass, keyClass, persistent, conf, properties, null);
  }

  /**
   * Instantiate a new {@link DataStore}. Uses default properties. Uses 'null' schema.
   * 
   * @param dataStoreClass The datastore implementation class.
   * @param keyClass The key class.
   * @param persistentClass The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  public static <D extends DataStore<K,T>, K, T extends Persistent>
  D getDataStore( Class<D> dataStoreClass, Class<K> keyClass,
      Class<T> persistentClass, Configuration conf) throws GoraException {

    return createDataStore(dataStoreClass, keyClass, persistentClass, conf, createProps(), null);
  }

  /**
   * Instantiate a new {@link DataStore}. Uses default properties. Uses 'null' schema.
   * 
   * @param dataStoreClass The datastore implementation class <i>as string</i>.
   * @param keyClass The key class.
   * @param persistentClass The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  @SuppressWarnings("unchecked")
  public static <K, T extends Persistent> DataStore<K, T> getDataStore(
      String dataStoreClass, Class<K> keyClass, Class<T> persistentClass, Configuration conf)
      throws GoraException {
    try {
      Class<? extends DataStore<K,T>> c
        = (Class<? extends DataStore<K, T>>) Class.forName(dataStoreClass);
      return createDataStore(c, keyClass, persistentClass, conf, createProps(), null);
    } catch(GoraException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
  }

  /**
   * Instantiate a new {@link DataStore}. Uses default properties. Uses 'null' schema.
   * 
   * @param dataStoreClass The datastore implementation class <i>as string</i>.
   * @param keyClass The key class <i>as string</i>.
   * @param persistentClass The value class <i>as string</i>.
   * @param conf {@link Configuration} to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  @SuppressWarnings({ "unchecked" })
  public static <K, T extends Persistent> DataStore<K, T> getDataStore(
      String dataStoreClass, String keyClass, String persistentClass, Configuration conf)
    throws GoraException {

    try {
      Class<? extends DataStore<K,T>> c
          = (Class<? extends DataStore<K, T>>) Class.forName(dataStoreClass);
      Class<K> k = (Class<K>) ClassLoadingUtils.loadClass(keyClass);
      Class<T> p = (Class<T>) ClassLoadingUtils.loadClass(persistentClass);
      return createDataStore(c, k, p, conf, createProps(), null);
    } catch(GoraException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
  }

  /**
   * Instantiate <i>the default</i> {@link DataStore}. Uses default properties. Uses 'null' schema.
   * 
   * @param keyClass The key class.
   * @param persistent The value class.
   * @param conf {@link Configuration} to be used be the store.
   * @return A new store instance.
   * @throws GoraException
   */
  @SuppressWarnings("unchecked")
  public static <K, T extends Persistent> DataStore<K, T> getDataStore(
      Class<K> keyClass, Class<T> persistent, Configuration conf) throws GoraException {
    Properties createProps = createProps();
    Class<? extends DataStore<K, T>> c;
    try {
      c = (Class<? extends DataStore<K, T>>) Class.forName(getDefaultDataStore(createProps));
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
    return createDataStore(c, keyClass, persistent, conf, createProps, null);
  }

  /**
   * Tries to find a property with the given baseKey. First the property
   * key constructed as "gora.&lt;classname&gt;.&lt;baseKey&gt;" is searched.
   * If not found, the property keys for all superclasses is recursively
   * tested. Lastly, the property key constructed as
   * "gora.datastore.&lt;baseKey&gt;" is searched.
   * @return the first found value, or defaultValue
   */
  public static String findProperty(Properties properties
      , DataStore<?, ?> store, String baseKey, String defaultValue) {

    //recursively try the class names until the base class
    Class<?> clazz = store.getClass();
    while(true) {
      String fullKey = GORA + "." + org.apache.gora.util.StringUtils.getClassname(clazz) + "." + baseKey;
      String value = getProperty(properties, fullKey);
      if(value != null) {
        return value;
      }
      //try once with lowercase
      value = getProperty(properties, fullKey.toLowerCase());
      if(value != null) {
        return value;
      }

      if(clazz.equals(DataStoreBase.class)) {
        break;
      }
      clazz = clazz.getSuperclass();
      if(clazz == null) {
        break;
      }
    }
    //try with "datastore"
    String fullKey = GORA + "." + DATASTORE + "." + baseKey;
    String value = getProperty(properties, fullKey);
    if(value != null) {
      return value;
    }
    return defaultValue;
  }

  /**
   * Tries to find a property with the given baseKey. First the property
   * key constructed as "gora.&lt;classname&gt;.&lt;baseKey&gt;" is searched.
   * If not found, the property keys for all superclasses is recursively
   * tested. Lastly, the property key constructed as
   * "gora.datastore.&lt;baseKey&gt;" is searched.
   * @return the first found value, or throws IOException
   */
  public static String findPropertyOrDie(Properties properties
      , DataStore<?, ?> store, String baseKey) throws IOException {
    String val = findProperty(properties, store, baseKey, null);
    if(val == null) {
      throw new IOException("Property with base name \""+baseKey+"\" could not be found, make " +
      		"sure to include this property in gora.properties file");
    }
    return val;
  }

  public static boolean findBooleanProperty(Properties properties
      , DataStore<?, ?> store, String baseKey, String defaultValue) {
    return Boolean.parseBoolean(findProperty(properties, store, baseKey, defaultValue));
  }

  public static boolean getAutoCreateSchema(Properties properties
      , DataStore<?,?> store) {
    return findBooleanProperty(properties, store, AUTO_CREATE_SCHEMA, "true");
  }

  /**
   * Returns the input path as read from the properties for file-backed data stores.
   */
  public static String getInputPath(Properties properties, DataStore<?,?> store) {
    return findProperty(properties, store, INPUT_PATH, null);
  }

  /**
   * Returns the output path as read from the properties for file-backed data stores.
   */
  public static String getOutputPath(Properties properties, DataStore<?,?> store) {
    return findProperty(properties, store, OUTPUT_PATH, null);
  }

  public static String getMappingFile(Properties properties, DataStore<?,?> store
      , String defaultValue) {
    return findProperty(properties, store, MAPPING_FILE, defaultValue);
  }

  private static String getDefaultDataStore(Properties properties) {
    return getProperty(properties, GORA_DEFAULT_DATASTORE_KEY);
  }

  private static String getProperty(Properties properties, String key) {
    return getProperty(properties, key, null);
  }

  private static String getProperty(Properties properties, String key, String defaultValue) {
    if (properties == null) {
      return defaultValue;
    }
    String result = properties.getProperty(key);
    if (result == null) {
      return defaultValue;
    }
    return result;
  }

  /**
   * Set a property
   */
  private static void setProperty(Properties properties, String baseKey, String value) {
    if(value != null) {
      properties.setProperty(GORA_DATASTORE + baseKey, value);
    }
  }

  /**
   * Sets a property for the datastore of the given class
   */
  private static<D extends DataStore<K,T>, K, T extends Persistent>
  void setProperty(Properties properties, Class<D> dataStoreClass, String baseKey, String value) {
    properties.setProperty(GORA+"."+org.apache.gora.util.StringUtils.getClassname(dataStoreClass)+"."+baseKey, value);
  }

  /**
   * Gets the default schema name of a given store class 
   */
  public static String getDefaultSchemaName(Properties properties, DataStore<?,?> store) {
    return findProperty(properties, store, SCHEMA_NAME, null);
  }

  /**
   * Sets the default schema name.
   */
  public static void setDefaultSchemaName(Properties properties, String schemaName) {
    if (schemaName != null) {
      setProperty(properties, SCHEMA_NAME, schemaName);
    }
  }

  /**
   * Sets the default schema name to be used by the datastore of the given class
   */
  public static<D extends DataStore<K,T>, K, T extends Persistent>
  void setDefaultSchemaName(Properties properties, Class<D> dataStoreClass, String schemaName) {
    setProperty(properties, dataStoreClass, SCHEMA_NAME, schemaName);
  }
}
