/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.serializers;

import com.datastax.driver.core.ColumnMetadata;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.gora.cassandra.query.CassandraColumn;
import org.apache.gora.cassandra.query.CassandraRow;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * This class contains the operations relates to Avro Serialization
 */
class AvroSerializer<K, T extends PersistentBase> extends CassandraSerializer {


  /**
   * Default schema index with value "0" used when AVRO Union data types are stored
   */
  public static final int DEFAULT_UNION_SCHEMA = 0;

  AvroSerializer(CassandraClient cassandraClient, Class<K> keyClass, Class<T> persistentClass, CassandraMapping mapping) {
    super(cassandraClient, keyClass, persistentClass, mapping);
  }

  @Override
  public Persistent get(Object key, String[] fields) {
    return null;
  }

  @Override
  public void put(Object key, Persistent value) {

  }

  @Override
  public Persistent get(Object key) {
    return null;
  }

  @Override
  public boolean delete(Object key) {
    return false;
  }


  @Override
  public Result execute(DataStore dataStore,Query query) {
    return null;
  }

  @Override
  public long deleteByQuery(Query query) {
    return 0;
  }

  @Override
  public boolean updateByQuery(Query query) {
    return false;
  }
}
