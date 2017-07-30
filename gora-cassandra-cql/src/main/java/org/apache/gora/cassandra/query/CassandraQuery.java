/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.query;

import org.apache.gora.filter.Filter;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.ws.impl.QueryWSBase;
import org.apache.gora.store.DataStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Cassandra specific implementation of the {@link Query} interface.
 */
public class CassandraQuery<K, T extends Persistent> extends QueryWSBase<K, T> {

  private Filter<K, T> filter;
  private boolean localFilterEnabled = true;
  private Map<String, Object> updateFields = new HashMap<>();

  public CassandraQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  @Override
  public Filter<K, T> getFilter() {
    return filter;
  }

  @Override
  public void setFilter(Filter<K, T> filter) {
    this.filter = filter;
  }

  @Override
  public boolean isLocalFilterEnabled() {
    return localFilterEnabled;
  }

  @Override
  public void setLocalFilterEnabled(boolean enable) {
    localFilterEnabled = enable;
  }

  public void addUpdateField(String field, Object newValue) {
    updateFields.put(field, newValue);
  }

  public Object getUpdateFieldValue(String key) {
    return updateFields.get(key);
  }

  @Override
  public String[] getFields() {
    if (updateFields.size() == 0) {
      return super.getFields();
    } else {
      String[] updateFieldsArray = new String[updateFields.size()];
      return updateFields.keySet().toArray(updateFieldsArray);
    }
  }
}
