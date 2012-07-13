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

package org.apache.gora.cassandra.query;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.StringSerializer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.specific.SpecificFixed;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraResult<K, T extends Persistent> extends ResultBase<K, T> {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraResult.class);
  
  private int rowNumber;

  private CassandraResultSet<K> cassandraResultSet;
  
  /**
   * Maps Cassandra columns to Avro fields.
   */
  private Map<String, String> reverseMap;

  public CassandraResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  @Override
  protected boolean nextInner() throws IOException {
    if (this.rowNumber < this.cassandraResultSet.size()) {
      updatePersistent();
    }
    ++this.rowNumber;
    return (this.rowNumber <= this.cassandraResultSet.size());
  }


  /**
   * Load key/value pair from Cassandra row to Avro record.
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private void updatePersistent() throws IOException {
    CassandraRow<K> cassandraRow = this.cassandraResultSet.get(this.rowNumber);
    
    // load key
    this.key = cassandraRow.getKey();
    
    // load value
    Schema schema = this.persistent.getSchema();
    List<Field> fields = schema.getFields();
    
    for (CassandraColumn cassandraColumn: cassandraRow) {
      
      // get field name
      String family = cassandraColumn.getFamily();
      String fieldName = this.reverseMap.get(family + ":" + StringSerializer.get().fromByteBuffer(cassandraColumn.getName()));
      
      // get field
      int pos = this.persistent.getFieldIndex(fieldName);
      Field field = fields.get(pos);
      
      // get value
      cassandraColumn.setField(field);
      Object value = cassandraColumn.getValue();
      
      this.persistent.put(pos, value);
      // this field does not need to be written back to the store
      this.persistent.clearDirty(pos);
    }

  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public float getProgress() throws IOException {
    return (((float) this.rowNumber) / this.cassandraResultSet.size());
  }

  public void setResultSet(CassandraResultSet<K> cassandraResultSet) {
    this.cassandraResultSet = cassandraResultSet;
  }
  
  public void setReverseMap(Map<String, String> reverseMap) {
    this.reverseMap = reverseMap;
  }

}
