/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.examples.generated;

public class Metadata extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Metadata\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"version\",\"type\":\"int\",\"default\":0},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"default\":null}]}");
  private static final long serialVersionUID = -7097391446015721734L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    VERSION(0, "version"),
    DATA(1, "data"),
    ;
    /**
     * Field's index.
     */
    private int index;

    /**
     * Field's name.
     */
    private String name;

    /**
     * Field's constructor
     * @param index field's index.
     * @param name field's name.
     */
    Field(int index, String name) {this.index=index;this.name=name;}

    /**
     * Gets field's index.
     * @return int field's index.
     */
    public int getIndex() {return index;}

    /**
     * Gets field's name.
     * @return String field's name.
     */
    public String getName() {return name;}

    /**
     * Gets field's attributes to string.
     * @return String field's attributes to string.
     */
    public String toString() {return name;}
  };

  public static final String[] _ALL_FIELDS = {
          "version",
          "data",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return Metadata._ALL_FIELDS.length;
  }

  private int version;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> data;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
      case 0: return version;
      case 1: return data;
      default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
      case 0: version = (java.lang.Integer)(value); break;
      case 1: data = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
      default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'version' field.
   */
  public java.lang.Integer getVersion() {
    return version;
  }

  /**
   * Sets the value of the 'version' field.
   * @param value the value to set.
   */
  public void setVersion(java.lang.Integer value) {
    this.version = value;
    setDirty(0);
  }

  /**
   * Checks the dirty status of the 'version' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isVersionDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'data' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getData() {
    return data;
  }

  /**
   * Sets the value of the 'data' field.
   * @param value the value to set.
   */
  public void setData(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.data = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(1);
  }

  /**
   * Checks the dirty status of the 'data' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataDirty() {
    return isDirty(1);
  }

  /** Creates a new Metadata RecordBuilder */
  public static org.apache.gora.examples.generated.Metadata.Builder newBuilder() {
    return new org.apache.gora.examples.generated.Metadata.Builder();
  }

  /** Creates a new Metadata RecordBuilder by copying an existing Builder */
  public static org.apache.gora.examples.generated.Metadata.Builder newBuilder(org.apache.gora.examples.generated.Metadata.Builder other) {
    return new org.apache.gora.examples.generated.Metadata.Builder(other);
  }

  /** Creates a new Metadata RecordBuilder by copying an existing Metadata instance */
  public static org.apache.gora.examples.generated.Metadata.Builder newBuilder(org.apache.gora.examples.generated.Metadata other) {
    return new org.apache.gora.examples.generated.Metadata.Builder(other);
  }

  private static java.nio.ByteBuffer deepCopyToReadOnlyBuffer(
          java.nio.ByteBuffer input) {
    java.nio.ByteBuffer copy = java.nio.ByteBuffer.allocate(input.capacity());
    int position = input.position();
    input.reset();
    int mark = input.position();
    int limit = input.limit();
    input.rewind();
    input.limit(input.capacity());
    copy.put(input);
    input.rewind();
    copy.rewind();
    input.position(mark);
    input.mark();
    copy.position(mark);
    copy.mark();
    input.position(position);
    copy.position(position);
    input.limit(limit);
    copy.limit(limit);
    return copy.asReadOnlyBuffer();
  }

  /**
   * RecordBuilder for Metadata instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Metadata>
          implements org.apache.avro.data.RecordBuilder<Metadata> {

    private int version;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> data;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.examples.generated.Metadata.SCHEMA$);
    }

    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.examples.generated.Metadata.Builder other) {
      super(other);
    }

    /** Creates a Builder by copying an existing Metadata instance */
    private Builder(org.apache.gora.examples.generated.Metadata other) {
      super(org.apache.gora.examples.generated.Metadata.SCHEMA$);
      if (isValidValue(fields()[0], other.version)) {
        this.version = (java.lang.Integer) data().deepCopy(fields()[0].schema(), other.version);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.data)) {
        this.data = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[1].schema(), other.data);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'version' field */
    public java.lang.Integer getVersion() {
      return version;
    }

    /** Sets the value of the 'version' field */
    public org.apache.gora.examples.generated.Metadata.Builder setVersion(int value) {
      validate(fields()[0], value);
      this.version = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /** Checks whether the 'version' field has been set */
    public boolean hasVersion() {
      return fieldSetFlags()[0];
    }

    /** Clears the value of the 'version' field */
    public org.apache.gora.examples.generated.Metadata.Builder clearVersion() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'data' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getData() {
      return data;
    }

    /** Sets the value of the 'data' field */
    public org.apache.gora.examples.generated.Metadata.Builder setData(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[1], value);
      this.data = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /** Checks whether the 'data' field has been set */
    public boolean hasData() {
      return fieldSetFlags()[1];
    }

    /** Clears the value of the 'data' field */
    public org.apache.gora.examples.generated.Metadata.Builder clearData() {
      data = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public Metadata build() {
      try {
        Metadata record = new Metadata();
        record.version = fieldSetFlags()[0] ? this.version : (java.lang.Integer) defaultValue(fields()[0]);
        record.data = fieldSetFlags()[1] ? this.data : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[1]));
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  public Metadata.Tombstone getTombstone(){
    return TOMBSTONE;
  }

  public Metadata newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();

  public static final class Tombstone extends Metadata implements org.apache.gora.persistency.Tombstone {

    private Tombstone() { }

    /**
     * Gets the value of the 'version' field.
     */
    public java.lang.Integer getVersion() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'version' field.
     * @param value the value to set.
     */
    public void setVersion(java.lang.Integer value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }

    /**
     * Checks the dirty status of the 'version' field. A field is dirty if it represents a change that has not yet been written to the database.
     * @param value the value to set.
     */
    public boolean isVersionDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

    /**
     * Gets the value of the 'data' field.
     */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getData() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'data' field.
     * @param value the value to set.
     */
    public void setData(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }

    /**
     * Checks the dirty status of the 'data' field. A field is dirty if it represents a change that has not yet been written to the database.
     * @param value the value to set.
     */
    public boolean isDataDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }


  }

  private static final org.apache.avro.io.DatumWriter
          DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader
          DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  /**
   * Writes AVRO data bean to output stream in the form of AVRO Binary encoding format. This will transform
   * AVRO data bean from its Java object form to it s serializable form.
   *
   * @param out java.io.ObjectOutput output stream to write data bean in serializable form
   */
  @Override
  public void writeExternal(java.io.ObjectOutput out)
          throws java.io.IOException {
    out.write(super.getDirtyBytes().array());
    DATUM_WRITER$.write(this, org.apache.avro.io.EncoderFactory.get()
            .directBinaryEncoder((java.io.OutputStream) out,
                    null));
  }

  /**
   * Reads AVRO data bean from input stream in it s AVRO Binary encoding format to Java object format.
   * This will transform AVRO data bean from it s serializable form to deserialized Java object form.
   *
   * @param in java.io.ObjectOutput input stream to read data bean in serializable form
   */
  @Override
  public void readExternal(java.io.ObjectInput in)
          throws java.io.IOException {
    byte[] __g__dirty = new byte[getFieldsCount()];
    in.read(__g__dirty);
    super.setDirtyBytes(java.nio.ByteBuffer.wrap(__g__dirty));
    DATUM_READER$.read(this, org.apache.avro.io.DecoderFactory.get()
            .directBinaryDecoder((java.io.InputStream) in,
                    null));
  }

}
