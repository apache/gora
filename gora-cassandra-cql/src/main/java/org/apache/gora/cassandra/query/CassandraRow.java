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

package org.apache.gora.cassandra.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by madhawa on 7/1/17.
 */
public class CassandraRow<K> extends ArrayList<CassandraColumn> {
  private static final long serialVersionUID = -7620939600192859652L;
  private K key;

  public K getKey() {
    return this.key;
  }

  public void setKey(K key) {
    this.key = key;
  }

  /**
   * Gets a specific CassandraColumn within a row using its name
   *
   * @param cassandraColumnName columnName
   * @return CassandraColumn
   */
  public CassandraColumn getCassandraColumn(String cassandraColumnName) {
    for (CassandraColumn cColumn : this) {
      if (cassandraColumnName.equals(cColumn.getName())) {
        return cColumn;
      }
    }
    return null;
  }

  /**
   *
   * @return
   */
  public String[] getFields() {
    List<String> columnNames = new ArrayList<>();
    for (CassandraColumn cColumn : this) {
      columnNames.add(cColumn.getName());
    }
    String[] columnNameArray = new String[columnNames.size()];
    columnNameArray = columnNames.toArray(columnNameArray);
    return columnNameArray;
  }

  /**
   *
   * @return
   */
  public Object[] getValues() {
    List<Object> columnValues = new ArrayList<>();
    for (CassandraColumn cColumn : this) {
      columnValues.add(cColumn.getValue());
    }
    return columnValues.toArray();
  }
}
