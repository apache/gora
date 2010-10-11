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
 * A WHERE clause in an SQL statement
 */
public class Where {

  private StringBuilder builder;

  public Where() {
    builder = new StringBuilder();
  }

  public Where(String where) {
    builder = new StringBuilder(where == null ? "" : where);
  }

  /** Adds a part to the Where clause connected with AND */
  public void addPart(String part) {
    if (builder.length() > 0) {
      builder.append(" AND ");
    }
    builder.append(part);
  }

  public void equals(String name, String value) {
    addPart(name + " = " + value);
  }

  public void lessThan(String name, String value) {
    addPart(name + " < " + value);
  }
  
  public void lessThanEq(String name, String value) {
    addPart(name + " <= " + value);
  }
  
  public void greaterThan(String name, String value) {
    addPart(name + " > " + value);
  }
  
  public void greaterThanEq(String name, String value) {
    addPart(name + " >= " + value);
  }
  
  public boolean isEmpty() {
    return builder.length() == 0;
  }
  
  @Override
  public String toString() {
    return builder.toString();
  }
}
