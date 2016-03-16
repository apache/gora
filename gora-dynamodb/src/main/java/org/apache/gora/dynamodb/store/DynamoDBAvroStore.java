/*
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
package org.apache.gora.dynamodb.store;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;

public class DynamoDBAvroStore<K, T extends PersistentBase> extends
DataStoreBase<K, T> implements IDynamoDB<K, T> {

  /**
   * The values are Avro fields pending to be stored.
   *
   * We want to iterate over the keys in insertion order. We don't want to lock
   * the entire collection before iterating over the keys, since in the meantime
   * other threads are adding entries to the map.
   */
  private Map<K, T> buffer = Collections
      .synchronizedMap(new LinkedHashMap<K, T>());

  private DynamoDBStore<K, ? extends Persistent> dynamoDBStoreHandler;

  /**
   * Sets the handler to the main DynamoDB
   * 
   * @param DynamoDBStore
   *          handler to main DynamoDB
   */
  @Override
  public void setDynamoDBStoreHandler(DynamoDBStore<K, T> dynamoHandler) {
    this.dynamoDBStoreHandler = dynamoHandler;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  @Override
  public void createSchema() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean delete(K arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public long deleteByQuery(Query<K, T> arg0) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void deleteSchema() {
    // TODO Auto-generated method stub

  }

  @Override
  public Result<K, T> execute(Query<K, T> arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void flush() {
    // TODO Auto-generated method stub

  }

  @Override
  public T get(K arg0, String[] arg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> arg0)
      throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getSchemaName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Query<K, T> newQuery() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void put(K key, T value) {
    buffer.put(key, value);
  }

  @Override
  public boolean schemaExists() {
    // TODO Auto-generated method stub
    return false;
  }
}