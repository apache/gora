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

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.sql.store.SqlMapping;
import org.apache.gora.sql.store.SqlStore;
import org.apache.gora.sql.store.SqlStore.DBVendor;

public class InsertUpdateStatementFactory {

  public static <K, T extends PersistentBase>
  InsertUpdateStatement<K, T> createStatement(SqlStore<K, T> store,
      SqlMapping mapping, DBVendor dbVendor) {
    switch(dbVendor) {
      case MYSQL:
        return new MySqlInsertUpdateStatement<K, T>(store, mapping, mapping.getTableName());
      case HSQL:
        return new HSqlInsertUpdateStatement<K, T>(store, mapping, mapping.getTableName());
      case GENERIC:
      default :
        throw new RuntimeException("Database is not supported yet.");    
    }
  }
}
