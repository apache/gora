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
import java.util.List;
import java.util.Properties;

import org.apache.gora.persistency.BeanFactory;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.util.GoraException;

/**
 * DataStore handles actual object persistence. Objects can be persisted,
 * fetched, queried or deleted by the DataStore methods. DataStores can be
 * constructed by an instance of {@link DataStoreFactory}.
 *
 * <p> DataStores implementations should be thread safe.</p>
 * <p><a><b>Note:</b></a> Results of updates ({@link #put(Object, Persistent)},
 * {@link #delete(Object)} and {@link #deleteByQuery(Query)} operations) are
 * guaranteed to be visible to subsequent get / execute operations ONLY
 * after a subsequent call to {@link #flush()}. Additionally, exception
 * handling is largely DataStore specific and is not largely dealt
 * with from within this interface.
 * </p>
 *
 * @param <K> the class of keys in the datastore.
 * @param <T> the class of persistent objects in the datastore.
 */
public interface DataStore<K, T extends Persistent> {

  /**
   * Initializes this DataStore.
   * @param keyClass the class of the keys.
   * @param persistentClass the class of the persistent objects.
   * @param properties extra metadata.
   * @throws GoraException If any error occurred.
   */
  void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException;

  /**
   * Sets the class of the keys.
   * @param keyClass the class of keys.
   */
  void setKeyClass(Class<K> keyClass);

  /**
   * Returns the class of the keys.
   * @return class of the keys.
   */
  Class<K> getKeyClass();

  /**
   * Sets the class of the persistent objects.
   * @param persistentClass class of persistent objects.
   */
  void setPersistentClass(Class<T> persistentClass);

  /**
   * Returns the class of the persistent objects.
   * @return class of the persistent objects.
   */
  Class<T> getPersistentClass();

  /**
   * Returns the schema name given to this DataStore.
   * @return schema name.
   */
  String getSchemaName();

  /**
   * Creates the optional schema or table (or similar) in the datastore
   * to hold the objects. If the schema is already created previously,
   * or the underlying data model does not support
   * or need this operation, the operation is ignored.
   * @throws GoraException If any error occurred.
   */
  void createSchema() throws GoraException;

  /**
   * Deletes the underlying schema or table (or similar) in the datastore
   * that holds the objects. This also deletes all the data associated with
   * the schema.
   * If the schema does not exists, this operation is ignored.
   * @throws GoraException If any error occurred.
   */
  void deleteSchema() throws GoraException;

  /**
   * Deletes all the data associated with the schema, but keeps the
   * schema (table or similar) intact.
   * @throws GoraException If any error occurred.
   */
  void truncateSchema() throws GoraException;

  /**
   * Returns whether the schema that holds the data exists in the datastore.
   * @return whether schema exists.
   * @throws GoraException If any error occurred.
   */
  boolean schemaExists() throws GoraException;

  /**
   * Returns a new instance of the key object. If the object cannot be instantiated 
   * (it the class is a Java primitive wrapper, or does not have no-arg 
   * constructor) it throws an exception. Only use this function if you can 
   * make sure that the key class has a no-arg constructor.   
   * @return a new instance of the key object.
   * @throws GoraException If any error occurred.
   */
  K newKey() throws GoraException;

  /**
   * Returns a new instance of the managed persistent object.
   * @return a new instance of the managed persistent object.
   * @throws GoraException If any error occurred.
   */
  T newPersistent() throws GoraException;

  /**
   * Verify whether a key exists in the data store.
   *
   * @param key the key of the object
   * @return true if the key exists, false otherwise
   * @throws GoraException If any error occurred.
   */
  boolean exists(K key) throws GoraException;


	/**
   * Returns the object corresponding to the given key fetching all the fields.
   * @param key the key of the object.
   * @return the Object corresponding to the key or null if it cannot be found.
   * @throws GoraException If any error occurred.
   */
  T get(K key) throws GoraException;

  /**
   * Returns the object corresponding to the given key.
   * @param key the key of the object.
   * @param fields the fields required in the object. Pass null, to retrieve all fields.
   * @return the Object corresponding to the key or null if it cannot be found.
   * @throws GoraException If any error occurred.
   */
  T get(K key, String[] fields) throws GoraException;

  /**
   * Inserts the persistent object with the given key. If an 
   * object with the same key already exists it will silently
   * be replaced. See also the note on 
   * <a href="#visibility">visibility</a>.
   *
   * @param key the key of the object.
   * @param obj the {@link Persistent} object.
   * @throws GoraException If any error occurred.
   */
  void put(K key, T obj) throws GoraException;

  /**
   * Deletes the object with the given key.
   * @param key the key of the object.
   * @return whether the object was successfully deleted.
   * @throws GoraException If any error occurred.
   */
  boolean delete(K key) throws GoraException;

  /**
   * Deletes all the objects matching the query.
   * See also the note on <a href="#visibility">visibility</a>.
   * @param query matching records to this query will be deleted.
   * @return number of deleted records.
   * @throws GoraException If any error occurred.
   */
  long deleteByQuery(Query<K, T> query) throws GoraException;

  /**
   * Executes the given query and returns the results.
   * @param query the query to execute.
   * @return the results as a {@link Result} object.
   * @throws GoraException If any error occurred.
   */
  Result<K, T> execute(Query<K, T> query) throws GoraException;

  /**
   * Constructs and returns a new Query.
   * @return a new Query.
   */
  Query<K, T> newQuery();

  /**
   * Partitions the given query and returns a list of {@link PartitionQuery}s,
   * which will execute on local data.
   * @param query the base query to create the partitions for. If the query
   * is null, then the data store returns the partitions for the default query
   * (returning every object),the {@link Query} to partition.
   * @return a List of PartitionQuery's
   * @throws IOException if there is an error generating partitions
   */
  List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException;

  /**
   * Forces the write caches to be flushed. DataStore implementations may
   * optimize their writing by deferring the actual put / delete operations
   * until this moment.
   * See also the note on <a href="#visibility">visibility</a>.
   * @throws GoraException If any error occurred.
   */
  void flush() throws GoraException;

  /**
   * Sets the {@link BeanFactory} to use by the DataStore.
   * @param beanFactory the BeanFactory to use
   */
  void setBeanFactory(BeanFactory<K,T> beanFactory);

  /**
   * Returns the BeanFactory used by the DataStore
   * @return the BeanFactory used by the DataStore
   */
  BeanFactory<K,T> getBeanFactory();

  /**
   * Close the DataStore. This should release any resources held by the
   * implementation, so that the instance is ready for GC.
   * All other DataStore methods cannot be used after this
   * method was called. Subsequent calls of this method are ignored.
   */
  void close();

}
