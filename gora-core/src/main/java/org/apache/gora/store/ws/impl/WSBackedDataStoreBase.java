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

import java.io.IOException;
import java.util.Properties;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.WebServiceBackedDataStore;
import org.apache.gora.util.OperationNotSupportedException;

/**
 * Base implementations for {@link WebServiceBackedDataStore} methods.
 */
public abstract class WSBackedDataStoreBase<K, T extends Persistent>
  extends WSDataStoreBase<K, T> implements WebServiceBackedDataStore<K, T> {

  @Override
  /**
   * Initializes a web service backed data store
   * @throws IOException
   */
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
  }

  @Override
  /**
   * Executes a query inside a web service backed data store
   */
  public Result<K, T> execute(Query<K, T> query) {
    try {
      return executeQuery(query);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Executes a normal Query reading the whole data. #execute() calls this function
   * for non-PartitionQuery's.
   */
  protected abstract Result<K,T> executeQuery(Query<K,T> query)
    throws IOException;

  @Override
  /**
   * Flushes objects into the data store
   */
  public void flush() {
  }

  @Override
  /**
   * Creates schema into the data store
   */
  public void createSchema() {
  }

  @Override
  /**
   * Deletes schema from the data store
   */
  public void deleteSchema() {
    throw new OperationNotSupportedException("delete schema is not supported for " +
    		"file backed data stores");
  }

  @Override
  /**
   * Verifies if a schema exists
   */
  public boolean schemaExists() {
    return true;
  }

  @Override
  /**
   * Writes an object into the data
   */
  public void write(Object out) throws Exception {
    super.write(out);
  }

  @Override
  /**
   * Reads fields from an object
   */
  public void readFields(Object in) throws Exception {
    super.readFields(in);
  }

  @Override
  /**
   * Closes the data store
   */
  public void close() {
  }
}
