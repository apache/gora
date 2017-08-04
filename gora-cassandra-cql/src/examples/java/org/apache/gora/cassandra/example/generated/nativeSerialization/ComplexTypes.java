package org.apache.gora.cassandra.example.generated.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;

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
public class ComplexTypes implements Persistent {

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

  @Transient
  @Override
  public void clear() {

  }

  @Transient
  @Override
  public boolean isDirty(int fieldIndex) {
    return false;
  }

  @Transient
  @Override
  public boolean isDirty(String field) {
    return false;
  }

  @Transient
  @Override
  public void setDirty() {

  }

  @Transient
  @Override
  public void setDirty(int fieldIndex) {

  }

  @Transient
  @Override
  public void clearDirty(int fieldIndex) {

  }

  @Transient
  @Override
  public void clearDirty(String field) {

  }

  @Transient
  @Override
  public Tombstone getTombstone() {
    return null;
  }

  @Transient
  @Override
  public List<Schema.Field> getUnmanagedFields() {
    return null;
  }

  @Transient
  @Override
  public Persistent newInstance() {
    return new ComplexTypes();
  }

  @Transient
  @Override
  public boolean isDirty() {
    return false;
  }

  @Transient
  @Override
  public void setDirty(String field) {

  }

  @Transient
  @Override
  public void clearDirty() {

  }
}
