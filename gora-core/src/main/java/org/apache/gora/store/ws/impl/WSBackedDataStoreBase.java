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
import org.apache.gora.store.FileBackedDataStore;
import org.apache.gora.store.WebServiceBackedDataStore;
import org.apache.gora.util.OperationNotSupportedException;

/**
 * Base implementations for {@link FileBackedDataStore} methods.
 */
public abstract class WSBackedDataStoreBase<K, T extends Persistent>
  extends WSDataStoreBase<K, T> implements WebServiceBackedDataStore<K, T> {

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws Exception {
    super.initialize(keyClass, persistentClass, properties);
    //if(properties != null) {
    //}
  }
  @Override
  public Result<K, T> execute(Query<K, T> query) throws Exception {
   /* if(query instanceof PartitionWSQueryImpl) {
        return executePartial((FileSplitPartitionQuery<K, T>) query);
    } else {*/
      return executeQuery(query);
    //}
  }

  /**
   * Executes a normal Query reading the whole data. #execute() calls this function
   * for non-PartitionQuery's.
   */
  protected abstract Result<K,T> executeQuery(Query<K,T> query)
    throws Exception;

  /**
   * Executes a PartitialQuery, reading the data between start and end.
   */
  //protected abstract Result<K,T> executePartial(FileSplitPartitionQuery<K,T> query)
  //  throws Exception;

  @Override
  public void flush() throws Exception {
  }

  @Override
  public void createSchema() throws Exception{
  }

  @Override
  public void deleteSchema() throws Exception {
    throw new OperationNotSupportedException("delete schema is not supported for " +
    		"file backed data stores");
  }

  @Override
  public boolean schemaExists() throws Exception {
    return true;
  }

  @Override
  public void write(Object out) throws Exception {
    super.write(out);
    //if(wsProvider != null)
      // make write request
    
  }

  @Override
  public void readFields(Object in) throws Exception {
    super.readFields(in);
    //if(wsProvider != null)
    // make read request
  }

  // TODO this could be close connection
  @Override
  public void close() throws IOException, InterruptedException, Exception {
  }
}
