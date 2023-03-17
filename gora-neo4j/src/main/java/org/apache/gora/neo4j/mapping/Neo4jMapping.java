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

import java.util.Map;

/**
 * Mapping definitions for Neo4j.
 */
public class Neo4jMapping {

  /**
   * Label used for creating new nodes in Neo4j.
   */
  private String label;
  /**
   * The property used as primary key in Neo4j.
   */
  private Property nodeKey;
  /**
   * List of properties used in the Datastore of Neo4j.
   */
  private Map<String, Property> properties;

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Property getNodeKey() {
    return nodeKey;
  }

  public void setNodeKey(Property nodeKey) {
    this.nodeKey = nodeKey;
  }

  public Map<String, Property> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Property> properties) {
    this.properties = properties;
  }

}
