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
package org.apache.gora.neo4j.mapping;

/**
 * Neo4j property class.
 */
public class Property {

  /**
   * The name of the property in Neo4j.
   */
  private String name;
  /**
   * The Neo4j Datatype of the property.
   */
  private PropertyTypes sqltype;

  public Property(String name, PropertyTypes sqltype) {
    this.name = name;
    this.sqltype = sqltype;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PropertyTypes getSqltype() {
    return sqltype;
  }

  public void setSqltype(PropertyTypes sqltype) {
    this.sqltype = sqltype;
  }
}
