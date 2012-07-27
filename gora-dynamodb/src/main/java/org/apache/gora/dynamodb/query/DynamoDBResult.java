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
  public static final Logger LOG = LoggerFactory.getLogger(DynamoDBResult.class);
  
  private int rowNumber;

  private List<T> dynamoDBResultSet;

  public DynamoDBResult(DataStore<K, T> dataStore, Query<K, T> query, List<T> objList) {
	super(dataStore, query);
	LOG.debug("DynamoDB result created.");
    this.setResultSet(objList);
  }

  public void setResultSet(List<T> objList) {
    this.dynamoDBResultSet = objList;
    this.limit = objList.size();
  }

  public float getProgress() throws IOException, InterruptedException, Exception {
	// TODO Auto-generated method stub
	return 0;
  }

  protected boolean nextInner() throws Exception {
	if (offset < 0 || offset > dynamoDBResultSet.size())
		return false;
	persistent = dynamoDBResultSet.get((int) this.offset);
	return true;
  }

  @Override
  public void close() throws IOException {
	// TODO Auto-generated method stub
  }

}
