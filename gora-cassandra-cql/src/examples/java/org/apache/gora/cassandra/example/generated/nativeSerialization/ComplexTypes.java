package org.apache.gora.cassandra.example.generated.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.apache.gora.cassandra.persistent.CassandraNativePersistent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sample class for test native cassandra persistent.
 */
@Table(keyspace = "nativeTestKeySpace", name = "documents",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = true)
public class ComplexTypes extends CassandraNativePersistent {

  @Column
  private List<String> listDataType;
  @Column
  private Map<String, String> mapDataType;
  @Column
  private String[] stringArrayDataType;
  @Column
  private int[] intArrayDataType;
  @Column
  private Set<String> setDataType;
  @PartitionKey
  @Column
  private String id;
  @Column
  private List<UUID> listUUIDDataType;

  public ComplexTypes(String id) {
    this.id = id;
  }

  public ComplexTypes() {
  }

  public List<UUID> getListUUIDDataType() {
    return listUUIDDataType;
  }

  public void setListUUIDDataType(List<UUID> listUUIDDataType) {
    this.listUUIDDataType = listUUIDDataType;
  }

  public List<String> getListDataType() {
    return listDataType;
  }

  public void setListDataType(List<String> listDataType) {
    this.listDataType = listDataType;
  }

  public Map<String, String> getMapDataType() {
    return mapDataType;
  }

  public void setMapDataType(Map<String, String> mapDataType) {
    this.mapDataType = mapDataType;
  }

  public String[] getStringArrayDataType() {
    return stringArrayDataType;
  }

  public void setStringArrayDataType(String[] stringArrayDataType) {
    this.stringArrayDataType = stringArrayDataType;
  }

  public int[] getIntArrayDataType() {
    return intArrayDataType;
  }

  public void setIntArrayDataType(int[] intArrayDataType) {
    this.intArrayDataType = intArrayDataType;
  }

  public Set<String> getSetDataType() {
    return setDataType;
  }

  public void setSetDataType(Set<String> setDataType) {
    this.setDataType = setDataType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
