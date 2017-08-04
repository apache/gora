/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.cassandra.example.generated.nativeSerialization;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;

import java.util.List;

@Table(keyspace = "nativeTestKeySpace", name = "documents",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
public class Document implements Persistent {
  @Column
  Customer customer;
  @PartitionKey
  @Column
  String defaultId;

  public String getDefaultId() {
    return defaultId;
  }

  public void setDefaultId(String defaultId) {
    this.defaultId = defaultId;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
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
    return new Document();
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
