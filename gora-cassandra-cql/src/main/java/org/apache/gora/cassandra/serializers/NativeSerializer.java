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

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.gora.cassandra.persistent.CassandraNativePersistent;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;

/**
 * Created by madhawa on 6/26/17.
 */
public class NativeSerializer<K, T extends CassandraNativePersistent> extends CassandraSerializer {

  private Mapper<T> mapper;




  @Override
  public void put(Object key, Object value) {
    mapper.save((T)value);
  }

  @Override
  public T get(Object key) {
    return mapper.get(key);
  }

  @Override
  public boolean delete(Object key) {
    mapper.delete(key);
    return true;
  }



  public NativeSerializer(CassandraClient cassandraClient, Class<K> keyClass, Class<T> persistentClass, CassandraMapping mapping) {
    super(cassandraClient, keyClass, persistentClass, mapping);
    this.createSchema();
    MappingManager mappingManager = new MappingManager(cassandraClient.getSession());
    mapper = mappingManager.mapper(persistentClass);

  }
}
