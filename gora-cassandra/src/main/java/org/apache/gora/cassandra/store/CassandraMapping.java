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
package org.apache.gora.cassandra.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CassandraMapping {

  private String keySpace;

  private Map<String, Boolean> families =
    new HashMap<String, Boolean>();

  public String getKeySpace() {
    return keySpace;
  }

  public void setKeySpace(String keySpace) {
    this.keySpace = keySpace;
  }

  public Set<String> getColumnFamilies() {
    return families.keySet();
  }

  public void addColumnFamily(String columnFamily, boolean isSuper) {
    families.put(columnFamily, isSuper);
  }

  public boolean isColumnFamilySuper(String columnFamily) {
    return families.get(columnFamily);
  }
}
