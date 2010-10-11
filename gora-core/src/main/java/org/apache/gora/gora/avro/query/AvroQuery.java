
package org.gora.avro.query;

import org.gora.avro.store.AvroStore;
import org.gora.persistency.Persistent;
import org.gora.query.impl.QueryBase;

/**
 * A simple Query implementation for Avro. Due to the data model, 
 * most of the operations for Query, like setting start,end keys is not 
 * supported. Setting query limit is supported.
 */
public class AvroQuery<K, T extends Persistent> extends QueryBase<K,T> {

  public AvroQuery() {
    super(null);
  }
  
  public AvroQuery(AvroStore<K,T> dataStore) {
    super(dataStore);
  }
  
}
