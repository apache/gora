/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.kudu.store;

import java.util.Map;

public class KuduTableMetadata {

  //Primary key
  private String primaryKey;
  //Pairs Column Name, Datatype
  private Map<String, String> columns;

  public KuduTableMetadata(String primaryKey, Map<String, String> columns) {
    this.primaryKey = primaryKey;
    this.columns = columns;
  }

  public String getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
  }

  public Map<String, String> getColumns() {
    return columns;
  }

  public void setColumns(Map<String, String> columns) {
    this.columns = columns;
  }

}
