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

package org.apache.gora.sql.statement;

/**
 * A SQL DELETE statement, for generating a Prepared Statement
 */
//new API experiment
public class Delete {

  private String from;
  private Where where;
  
  /**
   * @return the from
   */
  public String from() {
    return from;
  }

  /**
   * @param from the from to set
   */
  public Delete from(String from) {
    this.from = from;
    return this;
  }
  
  public Delete where(Where where) {
    this.where = where;
    return this;
  }
  
  public Where where() {
    if(where == null) {
      where = new Where();
    }
    return where;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("DELETE FROM ");
    builder.append(from);
    
    if(where != null && !where.isEmpty()) {
      builder.append(" WHERE ");
      builder.append(where.toString());
    }
    
    return builder.toString();
  }
}
