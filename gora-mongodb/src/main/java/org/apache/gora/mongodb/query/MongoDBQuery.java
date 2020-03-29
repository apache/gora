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
package org.apache.gora.mongodb.query;

import com.mongodb.client.model.Projections;
import org.apache.gora.mongodb.store.MongoMapping;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

/**
 * MongoDB specific implementation of the {@link Query} interface.
 * 
 * @author Fabien Poulard fpoulard@dictanova.com
 */
public class MongoDBQuery<K, T extends PersistentBase> extends QueryBase<K, T> {

  public MongoDBQuery() {
    super(null);
  }

  public MongoDBQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }

  /**
   * Compute the query itself. Only make use of the keys for querying.
   * 
   * @return a {@link Document} corresponding to the query
   */
  public static Bson toDBQuery(Query<?, ?> query) {

    if ((query.getStartKey() != null) && (query.getEndKey() != null)
        && query.getStartKey().equals(query.getEndKey())) {
      return eq("_id", query.getStartKey());
    } else {
      List<Bson> filters = new ArrayList<>();
      if (query.getStartKey() != null) {
        filters.add(gte("_id", query.getStartKey()));
      }
      if (query.getEndKey() != null) {
        filters.add(lte("_id", query.getEndKey()));
      }
      return filters.isEmpty() ? new Document() : and(filters);
    }
  }

  /**
   * Compute the projection of the query, that is the fields that will be
   * retrieved from the database.
   * 
   * @return a {@link Document} corresponding to the list of field to be
   *         retrieved with the associated boolean
   */
  public static Bson toProjection(String[] fields, MongoMapping mapping) {
    List<String> dbFields = Stream.of(fields)
            .map(mapping::getDocumentField)
            .filter(dbField -> dbField != null && !dbField.isEmpty())
            .collect(Collectors.toList());
    return Projections.include(dbFields);
  }
}
