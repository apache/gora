package org.apache.gora.cassandra.query;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraResult<K, T extends Persistent> extends ResultBase<K, T> {
  public static final Logger LOG = LoggerFactory.getLogger(CassandraResult.class);
  
  private int rowNumber;

  private CassandraResultSet cassandraResultSet;
  
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
    CassandraRow cassandraRow = this.cassandraResultSet.get(this.rowNumber);
    
    // load key
    this.key = (K) cassandraRow.getKey();
    
    // load value
    Schema schema = this.persistent.getSchema();
    List<Field> fields = schema.getFields();
    
    for (CassandraColumn cassandraColumn: cassandraRow) {
      
      // get field name
      String family = cassandraColumn.getFamily();
      String fieldName = this.reverseMap.get(family + ":" + cassandraColumn.getName());
      
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

  public void setResultSet(CassandraResultSet cassandraResultSet) {
    this.cassandraResultSet = cassandraResultSet;
  }
  
  public void setReverseMap(Map<String, String> reverseMap) {
    this.reverseMap = reverseMap;
  }

}
