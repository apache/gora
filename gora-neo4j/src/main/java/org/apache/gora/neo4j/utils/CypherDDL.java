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
package org.apache.gora.neo4j.utils;

/**
 *
 * Cypher DDL Builder utility for Neo4j.
 */
public class CypherDDL {

  /**
   * Build a CREATE CONSTRAINT statement for node key.
   *
   * @param label Label of the nodes.
   * @param key Node key name.
   * @return Cypher query.
   */
  public static String createNodeKeyConstraint(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("CREATE CONSTRAINT ");
    urlBuilder.append(createNodeKeyConstraintName(label, key));
    urlBuilder.append(" ON ");
    urlBuilder.append("(");
    urlBuilder.append("ds:");
    urlBuilder.append(label);
    urlBuilder.append(")");
    urlBuilder.append(" ASSERT ");
    urlBuilder.append("(");
    urlBuilder.append("ds.");
    urlBuilder.append(key);
    urlBuilder.append(")");
    urlBuilder.append(" IS NODE KEY ");
    return urlBuilder.toString();
  }

  /**
   * Build a CREATE CONSTRAINT statement for EXISTS.
   *
   * @param label Label of the nodes.
   * @param key Property name.
   * @return Cypher query.
   */
  public static String createExistsConstraint(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("CREATE CONSTRAINT ");
    urlBuilder.append(createExistsConstraintName(label, key));
    urlBuilder.append(" ON ");
    urlBuilder.append("(");
    urlBuilder.append("ds:");
    urlBuilder.append(label);
    urlBuilder.append(")");
    urlBuilder.append(" ASSERT ");
    urlBuilder.append(" EXISTS ");
    urlBuilder.append("(");
    urlBuilder.append("ds.");
    urlBuilder.append(key);
    urlBuilder.append(")");
    return urlBuilder.toString();
  }

  /**
   * Build a DROP CONSTRAINT statement for node key.
   *
   * @param label Label of the nodes.
   * @param key Node key name.
   * @return Cypher query.
   */
  public static String dropNodeKeyConstraint(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("DROP CONSTRAINT ");
    urlBuilder.append(createNodeKeyConstraintName(label, key));
    return urlBuilder.toString();
  }

  /**
   * Build a DROP CONSTRAINT statement for exists constraint.
   *
   * @param label Label of the nodes.
   * @param key Property name.
   * @return Cypher query.
   */
  public static String dropExistsConstraint(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("DROP CONSTRAINT ");
    urlBuilder.append(createExistsConstraintName(label, key));
    return urlBuilder.toString();
  }

  /**
   * Generate a name for the NODE KEY constraint.
   *
   * @param label Label of the nodes.
   * @param key Node key name.
   * @return Constraint name.
   */
  public static String createNodeKeyConstraintName(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("gora_nodekey_");
    urlBuilder.append(label);
    urlBuilder.append("_");
    urlBuilder.append(key);
    return urlBuilder.toString();
  }

  /**
   * Generate a name for the EXISTS constraint.
   *
   * @param label Label of the nodes.
   * @param key Property name.
   * @return Constraint name.
   */
  public static String createExistsConstraintName(String label, String key) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("gora_exists_");
    urlBuilder.append(label);
    urlBuilder.append("_");
    urlBuilder.append(key);
    return urlBuilder.toString();
  }

}
