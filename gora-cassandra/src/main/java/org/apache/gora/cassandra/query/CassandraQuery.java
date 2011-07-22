package org.apache.gora.cassandra.query;

import java.util.List;
import java.util.Map;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.QueryBase;
import org.apache.gora.store.DataStore;

public class CassandraQuery<K, T extends Persistent> extends QueryBase<K, T> {

  private Query<K, T> query;
  
  /**
   * Maps Avro fields to Cassandra columns.
   */
  private Map<String, List<String>> familyMap;
  
  public CassandraQuery() {
    super(null);
  }
  public CassandraQuery(DataStore<K, T> dataStore) {
    super(dataStore);
  }
  public void setFamilyMap(Map<String, List<String>> familyMap) {
    this.familyMap = familyMap;
  }
  public Map<String, List<String>> getFamilyMap() {
    return familyMap;
  }
  
  /**
   * @param family the family name
   * @return an array of the query column names belonging to the family
   */
  public String[] getColumns(String family) {
    
    List<String> columnList = familyMap.get(family);
    String[] columns = new String[columnList.size()];
    for (int i = 0; i < columns.length; ++i) {
      columns[i] = columnList.get(i);
    }
    return columns;
  }
  public Query<K, T> getQuery() {
    return query;
  }
  public void setQuery(Query<K, T> query) {
    this.query = query;
  }
  
  

}
