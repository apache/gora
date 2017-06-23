package org.apache.gora.cassandra.test.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;
import org.apache.gora.persistency.impl.PersistentBase;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by madhawa on 6/23/17.
 */

@Table(keyspace = "ks", name = "users",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
public class User implements Persistent {
  @PartitionKey
  @Column(name = "user_id")
  private UUID userId;
  @Column(name = "name")
  private String name;
  @Column(name = "dob")
  private Date dateOfBirth;

  public User () {

  }
  public User(UUID userId, String name, Date dateOfBirth) {
    this.userId = userId;
    this.name = name;
    this.dateOfBirth = dateOfBirth;
  }


  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  @Override
  public void clear() {
  }

  @Override
  public boolean isDirty(int fieldIndex) {
    return false;
  }

  @Override
  public boolean isDirty(String field) {
    return false;
  }

  @Override
  public void setDirty() {

  }

  @Override
  public void setDirty(int fieldIndex) {

  }

  @Override
  public void setDirty(String field) {

  }

  @Override
  public void clearDirty(int fieldIndex) {

  }

  @Override
  public void clearDirty(String field) {

  }





  @Override
  public Tombstone getTombstone() {
    return null;
  }

  @Override
  public List<Schema.Field> getUnmanagedFields() {
    return null;
  }

  @Override
  public Persistent newInstance() {
    return new User();
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void clearDirty() {

  }
}
