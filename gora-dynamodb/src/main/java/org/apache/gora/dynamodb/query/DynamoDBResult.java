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
 * @author Renato Marroquin Mogrovejo
 */

package org.apache.gora.dynamodb.query;

import java.io.IOException;
import java.util.List;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.ResultWSBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBResult<K, T extends Persistent> extends ResultWSBase<K, T> {

  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBResult.class);
  
  /**
   * Result set containing query results
   */
  private List<T> dynamoDBResultSet;

  /**
   * Constructor for the result set
   * @param dataStore	Data store used
   * @param query		Query used
   * @param objList		Objects obtained from querying
   */
  public DynamoDBResult(DataStore<K, T> dataStore, Query<K, T> query, List<T> objList) {
    super(dataStore, query);
    LOG.debug("DynamoDB result created.");
    this.setResultSet(objList);
  }

  /**
   * Sets the resulting objects within the class
   * @param objList
   */
  public void setResultSet(List<T> objList) {
    this.dynamoDBResultSet = objList;
    this.limit = objList.size();
  }

  /**
   * Gets the items reading progress
   */
  public float getProgress() throws IOException, InterruptedException, Exception {
    if (this.limit <= 0 || this.offset <= 0)
      return 0;
    return this.limit/this.offset;
  }

  /**
   * Gets the next item
   */
  protected boolean nextInner() throws Exception {
    if (offset < 0 || offset > ( dynamoDBResultSet.size() - 1))
      return false;
    persistent = dynamoDBResultSet.get((int) this.offset);
    return true;
  }

  @Override
  public void close() throws IOException {
  // TODO Auto-generated method stub
  }

}
