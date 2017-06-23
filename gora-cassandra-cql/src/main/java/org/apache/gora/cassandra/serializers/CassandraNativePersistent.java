package org.apache.gora.cassandra.serializers;

import com.datastax.driver.mapping.annotations.Transient;
import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.Tombstone;

import java.util.List;

/**
 * This class should be used with Native Cassandra Serialization.
 */
public abstract class CassandraNativePersistent implements Persistent {
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
  public void setDirty(String field) {

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
    return null;
  }

  @Transient
  @Override
  public boolean isDirty() {
    return false;
  }

  @Transient
  @Override
  public void clearDirty() {

  }
}
