/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
@SuppressWarnings("all")
public class TokenDatum extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TokenDatum\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"count\",\"type\":\"int\",\"default\":0}]}");
  private static final long serialVersionUID = -6468894522296148608L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    COUNT(0, "count"),
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
  "count",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return TokenDatum._ALL_FIELDS.length;
  }

  private int count;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return count;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: count = (java.lang.Integer)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'count' field.
   */
  public java.lang.Integer getCount() {
    return count;
  }

  /**
   * Sets the value of the 'count' field.
   * @param value the value to set.
   */
  public void setCount(java.lang.Integer value) {
    this.count = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'count' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCountDirty() {
    return isDirty(0);
  }

  /** Creates a new TokenDatum RecordBuilder */
  public static org.apache.gora.examples.generated.TokenDatum.Builder newBuilder() {
    return new org.apache.gora.examples.generated.TokenDatum.Builder();
  }
  
  /** Creates a new TokenDatum RecordBuilder by copying an existing Builder */
  public static org.apache.gora.examples.generated.TokenDatum.Builder newBuilder(org.apache.gora.examples.generated.TokenDatum.Builder other) {
    return new org.apache.gora.examples.generated.TokenDatum.Builder(other);
  }
  
  /** Creates a new TokenDatum RecordBuilder by copying an existing TokenDatum instance */
  public static org.apache.gora.examples.generated.TokenDatum.Builder newBuilder(org.apache.gora.examples.generated.TokenDatum other) {
    return new org.apache.gora.examples.generated.TokenDatum.Builder(other);
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
   * RecordBuilder for TokenDatum instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<TokenDatum>
    implements org.apache.avro.data.RecordBuilder<TokenDatum> {

    private int count;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.examples.generated.TokenDatum.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.examples.generated.TokenDatum.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing TokenDatum instance */
    private Builder(org.apache.gora.examples.generated.TokenDatum other) {
            super(org.apache.gora.examples.generated.TokenDatum.SCHEMA$);
      if (isValidValue(fields()[0], other.count)) {
        this.count = (java.lang.Integer) data().deepCopy(fields()[0].schema(), other.count);
        fieldSetFlags()[0] = true;
      }
    }

    /** Gets the value of the 'count' field */
    public java.lang.Integer getCount() {
      return count;
    }
    
    /** Sets the value of the 'count' field */
    public org.apache.gora.examples.generated.TokenDatum.Builder setCount(int value) {
      validate(fields()[0], value);
      this.count = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'count' field has been set */
    public boolean hasCount() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'count' field */
    public org.apache.gora.examples.generated.TokenDatum.Builder clearCount() {
      fieldSetFlags()[0] = false;
      return this;
    }
    
    @Override
    public TokenDatum build() {
      try {
        TokenDatum record = new TokenDatum();
        record.count = fieldSetFlags()[0] ? this.count : (java.lang.Integer) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public TokenDatum.Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  public TokenDatum newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends TokenDatum implements org.apache.gora.persistency.Tombstone {
  
      private Tombstone() { }
  
	  		  /**
	   * Gets the value of the 'count' field.
		   */
	  public java.lang.Integer getCount() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'count' field.
		   * @param value the value to set.
	   */
	  public void setCount(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'count' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCountDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
		  
  }

  private static final org.apache.avro.io.DatumWriter
          DATUM_WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);
  private static final org.apache.avro.io.DatumReader
          DATUM_READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override
  public void writeExternal(java.io.ObjectOutput out)
          throws java.io.IOException {
    out.write(super.getDirtyBytes().array());
    DATUM_WRITER$.write
            (this, org.apache.avro.io.EncoderFactory.get()
                    .directBinaryEncoder((java.io.OutputStream) out,
                            null));
  }

  @Override
  public void readExternal(java.io.ObjectInput in)
          throws java.io.IOException {
    byte[] __g__dirty = new byte[getFieldsCount()];
    in.read(__g__dirty);
    super.setDirtyBytes(java.nio.ByteBuffer.wrap(__g__dirty));
    DATUM_READER$.read
            (this, org.apache.avro.io.DecoderFactory.get()
                    .directBinaryDecoder((java.io.InputStream) in,
                            null));
  }
  
}

