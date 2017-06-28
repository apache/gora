package org.apache.gora.cassandra.example.generated.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.apache.gora.cassandra.persistent.CassandraNativePersistent;

import java.util.Date;
import java.util.UUID;

/**
 * Created by madhawa on 6/23/17.
 */

@Table(keyspace = "nativeTestKeySpace", name = "users",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
public class User extends CassandraNativePersistent {
  @PartitionKey
  @Column(name = "user_id")
  private UUID userId;
  @Column(name = "name")
  private String name;
  @Column(name = "dob")
  private Date dateOfBirth;

  @Transient
  private boolean dirty;

  public User() {

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

  public UUID getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }
}
