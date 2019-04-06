/**
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the"
 *License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */
package org.apache.gora.examples.generated;

public class EmployeeInt extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {

  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"EmployeeInt\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"ssn\",\"type\":\"int\",\"default\":0}],\"default\":null}");
  private static final long serialVersionUID = -333122997722160004L;

  /**
   * Enum containing all data bean's fields.
   */
  public static enum Field {
    SSN(0, "ssn"),;
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
     *
     * @param index field's index.
     * @param name field's name.
     */
    Field(int index, String name) {
      this.index = index;
      this.name = name;
    }

    /**
     * Gets field's index.
     *
     * @return int field's index.
     */
    public int getIndex() {
      return index;
    }

    /**
     * Gets field's name.
     *
     * @return String field's name.
     */
    public String getName() {
      return name;
    }

    /**
     * Gets field's attributes to string.
     *
     * @return String field's attributes to string.
     */
    public String toString() {
      return name;
    }
  };

  public static final String[] _ALL_FIELDS = {
    "ssn",};

  /**
   * Gets the total field count.
   *
   * @return int field count
   */
  public int getFieldsCount() {
    return EmployeeInt._ALL_FIELDS.length;
  }

  private java.lang.Integer ssn;

  public org.apache.avro.Schema getSchema() {
    return SCHEMA$;
  }

  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
      case 2:
        return ssn;
      default:
        throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value = "unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
      case 2:
        ssn = (java.lang.Integer) (value);
        break;
      default:
        throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'ssn' field.
   */
  public java.lang.Integer getSsn() {
    return ssn;
  }

  /**
   * Sets the value of the 'ssn' field.
   *
   * @param value the value to set.
   */
  public void setSsn(java.lang.Integer value) {
    this.ssn = value;
    setDirty(2);
  }

  /**
   * Checks the dirty status of the 'ssn' field. A field is dirty if it
   * represents a change that has not yet been written to the database.
   *
   * @param value the value to set.
   */
  public boolean isSsnDirty() {
    return isDirty(2);
  }

  /**
   * Creates a new EmployeeInt RecordBuilder
   */
  public static EmployeeInt.Builder newBuilder() {
    return new EmployeeInt.Builder();
  }

  /**
   * Creates a new EmployeeInt RecordBuilder by copying an existing Builder
   */
  public static EmployeeInt.Builder newBuilder(EmployeeInt.Builder other) {
    return new EmployeeInt.Builder(other);
  }

  /**
   * Creates a new EmployeeInt RecordBuilder by copying an existing EmployeeInt
   * instance
   */
  public static EmployeeInt.Builder newBuilder(EmployeeInt other) {
    return new EmployeeInt.Builder(other);
  }

  @Override
  public EmployeeInt clone() {
    return newBuilder(this).build();
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
   * RecordBuilder for EmployeeInt instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<EmployeeInt>
          implements org.apache.avro.data.RecordBuilder<EmployeeInt> {

    private Integer ssn;

    /**
     * Creates a new Builder
     */
    private Builder() {
      super(EmployeeInt.SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder
     */
    private Builder(EmployeeInt.Builder other) {
      super(other);
    }

    /**
     * Creates a Builder by copying an existing EmployeeInt instance
     */
    private Builder(EmployeeInt other) {
      super(EmployeeInt.SCHEMA$);

      if (isValidValue(fields()[0], other.ssn)) {
        this.ssn = (java.lang.Integer) data().deepCopy(fields()[0].schema(), other.ssn);
        fieldSetFlags()[0] = true;
      }
    }

    /**
     * Gets the value of the 'ssn' field
     */
    public java.lang.Integer getSsn() {
      return ssn;
    }

    /**
     * Sets the value of the 'ssn' field
     */
    public EmployeeInt.Builder setSsn(java.lang.Integer value) {
      validate(fields()[0], value);
      this.ssn = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
     * Checks whether the 'ssn' field has been set
     */
    public boolean hasSsn() {
      return fieldSetFlags()[0];
    }

    /**
     * Clears the value of the 'ssn' field
     */
    public EmployeeInt.Builder clearSsn() {
      ssn = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    public EmployeeInt build() {
      try {
        EmployeeInt record = new EmployeeInt();
        record.ssn = fieldSetFlags()[0] ? this.ssn : (java.lang.Integer) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  public EmployeeInt.Tombstone getTombstone() {
    return TOMBSTONE;
  }

  public EmployeeInt newInstance() {
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();

  public static final class Tombstone extends EmployeeInt implements org.apache.gora.persistency.Tombstone {

    private Tombstone() {
    }

    /**
     * Gets the value of the 'ssn' field.
     */
    public java.lang.Integer getSsn() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'ssn' field.
     *
     * @param value the value to set.
     */
    public void setSsn(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }

    /**
     * Checks the dirty status of the 'ssn' field. A field is dirty if it
     * represents a change that has not yet been written to the database.
     *
     * @param value the value to set.
     */
    public boolean isSsnDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }
  }

  private static final org.apache.avro.io.DatumWriter DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  /**
   * Writes AVRO data bean to output stream in the form of AVRO Binary encoding
   * format. This will transform AVRO data bean from its Java object form to it
   * s serializable form.
   *
   * @param out java.io.ObjectOutput output stream to write data bean in
   * serializable form
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
   * Reads AVRO data bean from input stream in it s AVRO Binary encoding format
   * to Java object format. This will transform AVRO data bean from it s
   * serializable form to deserialized Java object form.
   *
   * @param in java.io.ObjectOutput input stream to read data bean in
   * serializable form
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
