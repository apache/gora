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
import org.apache.avro.Schema.Type;
import org.apache.gora.cassandra.store.CassandraStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CassandraResult specific implementation of the {@link org.apache.gora.query.Result}
 * interface.
 */
public class CassandraResult<K, T extends PersistentBase> extends ResultBase<K, T> {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraResult.class);
  
  private int rowNumber;

  /**
   * Result set containing query results
   */
  private CassandraResultSet<K> cassandraResultSet;
  
  /**
   * Maps Cassandra columns to Avro fields.
   */
  private Map<String, String> reverseMap;


  /**
   * Constructor for the result set
   *
   * @param dataStore Data store used
   * @param query     Query used
   */
  public CassandraResult(DataStore<K, T> dataStore, Query<K, T> query) {
    super(dataStore, query);
  }

  /**
   * Gets the next item
   */
  @Override
  protected boolean nextInner() throws IOException {
    if (this.rowNumber < this.cassandraResultSet.size()) {
      updatePersistent();
    }
    ++this.rowNumber;
    return (this.rowNumber <= this.cassandraResultSet.size());
  }
  
  /**
   * Gets the column containing the type of the union type element stored.
   * TODO: This might seem too much of a overhead if we consider that N rows have M columns,
   *       this might have to be reviewed to get the specific column in O(1)
   * @param pFieldName
   * @param pCassandraRow
   * @return
   */
  private CassandraColumn getUnionTypeColumn(String pFieldName, Object[] pCassandraRow){

    for (Object currentPCassandraRow : pCassandraRow) {
      CassandraColumn cColumn = (CassandraColumn) currentPCassandraRow;
      String columnName = StringSerializer.get().fromByteBuffer(cColumn.getName().duplicate());
      if (pFieldName.equals(columnName))
        return cColumn;
    }
    return null;
  }


  /**
   * Load key/value pair from Cassandra row to Avro record.
   * @throws IOException
   */
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
      
      String fieldName = this.reverseMap.get(family + ":" + StringSerializer.get().fromByteBuffer(cassandraColumn.getName().duplicate()));
      
      if (fieldName != null) {
        // get field
        if (!fieldName.contains(CassandraStore.UNION_COL_SUFIX)) {

          int pos = this.persistent.getSchema().getField(fieldName).pos();
          Field field = fields.get(pos);
          Type fieldType = field.schema().getType();
          if (fieldType.equals(Type.UNION)) {
            //getting UNION stored type
            CassandraColumn cc = getUnionTypeColumn(fieldName
                + CassandraStore.UNION_COL_SUFIX, cassandraRow.toArray());
            //creating temporary UNION Field
            Field unionField = new Field(fieldName
                + CassandraStore.UNION_COL_SUFIX, Schema.create(Type.INT),
                null, null);
            // get value of UNION stored type
            cc.setField(unionField);
            Object val = cc.getValue();
            cassandraColumn.setUnionType(Integer.parseInt(val.toString()));
          }

          // get value
          cassandraColumn.setField(field);
          Object value = cassandraColumn.getValue();

          this.persistent.put(pos, value);
          // this field does not need to be written back to the store
          this.persistent.clearDirty(pos);
        }
      } else
        LOG.debug("FieldName was null while iterating CassandraRow and using Avro Union type");
    }

  }

  //TODO Should we remove this method?
  @SuppressWarnings("unused")
  private int getNonNullTypePos(List<Schema> pTypes){
    int iCnt = 0;
    for (Schema sch :  pTypes)
      if (!sch.getName().equals("null"))
        return iCnt;
      else 
        iCnt++;
    return CassandraStore.DEFAULT_UNION_SCHEMA;
  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub
    
  }

  /**
   * Gets the items reading progress
   */
  @Override
  public float getProgress() throws IOException {
    return (((float) this.rowNumber) / this.cassandraResultSet.size());
  }

  /**
   * Set the Result set containing query results
   *
   * @param cassandraResultSet
   */
  public void setResultSet(CassandraResultSet<K> cassandraResultSet) {
    this.cassandraResultSet = cassandraResultSet;
  }

  public void setReverseMap(Map<String, String> reverseMap) {
    this.reverseMap = reverseMap;
  }

}
