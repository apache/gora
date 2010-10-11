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

class CassandraColumn {
  String family;
  String superColumn;
  String column;

  public CassandraColumn(String family, String superColumn, String column) {
    this.family = family;
    this.superColumn = superColumn;
    this.column = column;
  }

  public boolean isSuperColumn() {
    return superColumn != null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((column == null) ? 0 : column.hashCode());
    result = prime * result + ((family == null) ? 0 : family.hashCode());
    result = prime * result
        + ((superColumn == null) ? 0 : superColumn.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CassandraColumn other = (CassandraColumn) obj;
    if (column == null) {
      if (other.column != null)
        return false;
    } else if (!column.equals(other.column))
      return false;
    if (family == null) {
      if (other.family != null)
        return false;
    } else if (!family.equals(other.family))
      return false;
    if (superColumn == null) {
      if (other.superColumn != null)
        return false;
    } else if (!superColumn.equals(other.superColumn))
      return false;
    return true;
  }
}
