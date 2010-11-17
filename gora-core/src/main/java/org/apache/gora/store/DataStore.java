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

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;

/**
 * DataStore handles actual object persistence. Objects can be persisted,
 * fetched, queried or deleted by the DataStore methods. DataStores can be
 * constructed by an instance of {@link DataStoreFactory}.
 *
 * <p> DataStores implementations should be thread safe.
 * @param <K> the class of keys in the datastore
 * @param <T> the class of persistent objects in the datastore
 */
public interface DataStore<K, T extends Persistent> extends Closeable,
  Writable, Configurable {

  /**
   * Initializes this DataStore.
   * @param keyClass the class of the keys
   * @param persistentClass the class of the persistent objects
   * @param properties extra metadata
   * @throws IOException
   */
  public abstract void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws IOException;

  /**
   * Sets the class of the keys
   * @param keyClass the class of keys
   */
  public abstract void setKeyClass(Class<K> keyClass);

  /**
   * Returns the class of the keys
   * @return class of the keys
   */
  public abstract Class<K> getKeyClass();

  /**
   * Sets the class of the persistent objects
   * @param persistentClass class of persistent objects
   */
  public abstract void setPersistentClass(Class<T> persistentClass);

  /**
   * Returns the class of the persistent objects
   * @return class of the persistent objects
   */
  public abstract Class<T> getPersistentClass();

  /**
   * Returns the schema name given to this DataStore
   * @return schema name
   */
  public abstract String getSchemaName();

  /**
   * Creates the optional schema or table (or similar) in the datastore
   * to hold the objects. If the schema is already created previously,
   * or the underlying data model does not support
   * or need this operation, the operation is ignored.
   */
  public abstract void createSchema() throws IOException;

  /**
   * Deletes the underlying schema or table (or similar) in the datastore
   * that holds the objects. This also deletes all the data associated with
   * the schema.
   */
  public abstract void deleteSchema() throws IOException;

  /**
   * Deletes all the data associated with the schema, but keeps the
   * schema (table or similar) intact.
   */
  public abstract void truncateSchema() throws IOException;

  /**
   * Returns whether the schema that holds the data exists in the datastore.
   * @return whether schema exists
   */
  public abstract boolean schemaExists() throws IOException;

  /**
   * Returns a new instance of the key object. If the object cannot be instantiated 
   * (it the class is a Java primitive wrapper, or does not have no-arg 
   * constructor) it throws an exception. Only use this function if you can 
   * make sure that the key class has a no-arg constructor.   
   * @return a new instance of the key object.
   */
  public abstract K newKey() throws IOException;

  /**
   * Returns a new instance of the managed persistent object.
   * @return a new instance of the managed persistent object.
   */
  public abstract T newPersistent() throws IOException;

  /**
   * Returns the object corresponding to the given key fetching all the fields.
   * @param key the key of the object
   * @return the Object corresponding to the key or null if it cannot be found
   */
  public abstract T get(K key) throws IOException;

  /**
   * Returns the object corresponding to the given key.
   * @param key the key of the object
   * @param fields the fields required in the object. Pass null, to retrieve all fields
   * @return the Object corresponding to the key or null if it cannot be found
   */
  public abstract T get(K key, String[] fields) throws IOException;

  /**
   * Inserts the persistent object with the given key.
   */
  public abstract void put(K key, T obj) throws IOException;

  /**
   * Deletes the object with the given key
   * @param key the key of the object
   * @return whether deleted the object successfuly
   */
  public abstract boolean delete(K key) throws IOException;

  /**
   * Deletes all the objects matching the query.
   * @param query matching records to this query will be deleted
   * @return number of deleted records
   */
  public abstract long deleteByQuery(Query<K, T> query) throws IOException;

  /**
   * Executes the given query and returns the results.
   * @param query the query to execute.
   * @return the results as a {@link Result} object.
   */
  public abstract Result<K,T> execute(Query<K, T> query) throws IOException;

  /**
   * Constructs and returns a new Query.
   * @return a new Query.
   */
  public abstract Query<K, T> newQuery();

  /**
   * Partitions the given query and returns a list of {@link PartitionQuery}s,
   * which will execute on local data.
   * @param query the base query to create the partitions for. If the query
   * is null, then the data store returns the partitions for the default query
   * (returning every object)
   * @return a List of PartitionQuery's
   */
  public abstract List<PartitionQuery<K,T>> getPartitions(Query<K,T> query)
    throws IOException;

  /**
   * Forces the write caches to be flushed.
   */
  public abstract void flush() throws IOException;

  /**
   * Sets the {@link BeanFactory} to use by the DataStore.
   * @param beanFactory the BeanFactory to use
   */
  public abstract void setBeanFactory(BeanFactory<K,T> beanFactory);

  /**
   * Returns the BeanFactory used by the DataStore
   * @return the BeanFactory used by the DataStore
   */
  public abstract BeanFactory<K,T> getBeanFactory();

  @Override
  public abstract void close() throws IOException;

  @Override
  public Configuration getConf();

  @Override
  public void setConf(Configuration conf);

  @Override
  public void readFields(DataInput in) throws IOException;

  @Override
  public void write(DataOutput out) throws IOException;

}
