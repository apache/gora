/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.couchdb.store;

import org.jdom.Element;

import java.util.List;

/**
 * Mapping definitions for CouchDB.
 */
public class CouchDBMapping {

  /**
   * Name of the database this mapping is linked to
   */
  public String databaseName;

  /**
   * CouchDB document description
   */
  public List<Element> fields;

  /**
   * Name of the database this mapping is linked to
   *
   * @return name as String
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Setter for the name of the database
   *
   * @param databaseName name of the database
   */
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

}
