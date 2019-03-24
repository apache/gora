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
package org.apache.gora.cassandra.example.generated.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sample class for native cassandra persistent example.
 */
@Table(keyspace = "nativeTestKeySpace", name = "users",
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

  public User() {

  }

  public User(UUID userId, String name, Date dateOfBirth) {
    this.userId = userId;
    this.name = name;
    this.dateOfBirth = dateOfBirth;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
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
    return new User();
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
  
  @Transient
  @Override
  public Schema getSchema() {
    // TODO Auto-generated method stub
    return null;
  }

  @Transient
  @Override
  public Persistent clone() {
    return this.clone();
  }
  
}
