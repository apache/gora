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
package org.apache.gora.hbase.store;

import java.util.Arrays;

/**
 * Store family, qualifier tuple 
 */
class HBaseColumn {
  
  final byte[] family;
  final byte[] qualifier;
  
  public HBaseColumn(byte[] family, byte[] qualifier) {
    this.family = family==null ? null : Arrays.copyOf(family, family.length);
    this.qualifier = qualifier==null ? null : 
      Arrays.copyOf(qualifier, qualifier.length);
  }
  
  /**
   * @return the family (internal array returned; do not modify)
   */
  public byte[] getFamily() {
    return family;
  }

  /**
   * @return the qualifer (internal array returned; do not modify)
   */
  public byte[] getQualifier() {
    return qualifier;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(family);
    result = prime * result + Arrays.hashCode(qualifier);
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
    HBaseColumn other = (HBaseColumn) obj;
    if (!Arrays.equals(family, other.family))
      return false;
    if (!Arrays.equals(qualifier, other.qualifier))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "HBaseColumn [family=" + Arrays.toString(family) + ", qualifier="
        + Arrays.toString(qualifier) + "]";
  }
  
  
}
