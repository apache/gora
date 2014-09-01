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
package org.apache.gora.goraci.generated;  
@SuppressWarnings("all")
public class CINode extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"CINode\",\"namespace\":\"org.apache.gora.goraci.generated\",\"fields\":[{\"name\":\"prev\",\"type\":\"long\"},{\"name\":\"client\",\"type\":\"string\"},{\"name\":\"count\",\"type\":\"long\"}]}");

  /** Enum containing all data bean's fields. */
  public static enum Field {
    PREV(0, "prev"),
    CLIENT(1, "client"),
    COUNT(2, "count"),
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
  "prev",
  "client",
  "count",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return CINode._ALL_FIELDS.length;
  }

  private long prev;
  private java.lang.CharSequence client;
  private long count;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return prev;
    case 1: return client;
    case 2: return count;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: prev = (java.lang.Long)(value); break;
    case 1: client = (java.lang.CharSequence)(value); break;
    case 2: count = (java.lang.Long)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'prev' field.
   */
  public java.lang.Long getPrev() {
    return prev;
  }

  /**
   * Sets the value of the 'prev' field.
   * @param value the value to set.
   */
  public void setPrev(java.lang.Long value) {
    this.prev = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'prev' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPrevDirty(java.lang.Long value) {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'client' field.
   */
  public java.lang.CharSequence getClient() {
    return client;
  }

  /**
   * Sets the value of the 'client' field.
   * @param value the value to set.
   */
  public void setClient(java.lang.CharSequence value) {
    this.client = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'client' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isClientDirty(java.lang.CharSequence value) {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'count' field.
   */
  public java.lang.Long getCount() {
    return count;
  }

  /**
   * Sets the value of the 'count' field.
   * @param value the value to set.
   */
  public void setCount(java.lang.Long value) {
    this.count = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'count' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCountDirty(java.lang.Long value) {
    return isDirty(2);
  }

  /** Creates a new CINode RecordBuilder */
  public static org.apache.gora.goraci.generated.CINode.Builder newBuilder() {
    return new org.apache.gora.goraci.generated.CINode.Builder();
  }
  
  /** Creates a new CINode RecordBuilder by copying an existing Builder */
  public static org.apache.gora.goraci.generated.CINode.Builder newBuilder(org.apache.gora.goraci.generated.CINode.Builder other) {
    return new org.apache.gora.goraci.generated.CINode.Builder(other);
  }
  
  /** Creates a new CINode RecordBuilder by copying an existing CINode instance */
  public static org.apache.gora.goraci.generated.CINode.Builder newBuilder(org.apache.gora.goraci.generated.CINode other) {
    return new org.apache.gora.goraci.generated.CINode.Builder(other);
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
   * RecordBuilder for CINode instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<CINode>
    implements org.apache.avro.data.RecordBuilder<CINode> {

    private long prev;
    private java.lang.CharSequence client;
    private long count;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.goraci.generated.CINode.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.goraci.generated.CINode.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing CINode instance */
    private Builder(org.apache.gora.goraci.generated.CINode other) {
            super(org.apache.gora.goraci.generated.CINode.SCHEMA$);
      if (isValidValue(fields()[0], other.prev)) {
        this.prev = (java.lang.Long) data().deepCopy(fields()[0].schema(), other.prev);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.client)) {
        this.client = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.client);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.count)) {
        this.count = (java.lang.Long) data().deepCopy(fields()[2].schema(), other.count);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'prev' field */
    public java.lang.Long getPrev() {
      return prev;
    }
    
    /** Sets the value of the 'prev' field */
    public org.apache.gora.goraci.generated.CINode.Builder setPrev(long value) {
      validate(fields()[0], value);
      this.prev = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'prev' field has been set */
    public boolean hasPrev() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'prev' field */
    public org.apache.gora.goraci.generated.CINode.Builder clearPrev() {
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'client' field */
    public java.lang.CharSequence getClient() {
      return client;
    }
    
    /** Sets the value of the 'client' field */
    public org.apache.gora.goraci.generated.CINode.Builder setClient(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.client = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'client' field has been set */
    public boolean hasClient() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'client' field */
    public org.apache.gora.goraci.generated.CINode.Builder clearClient() {
      client = null;
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'count' field */
    public java.lang.Long getCount() {
      return count;
    }
    
    /** Sets the value of the 'count' field */
    public org.apache.gora.goraci.generated.CINode.Builder setCount(long value) {
      validate(fields()[2], value);
      this.count = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'count' field has been set */
    public boolean hasCount() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'count' field */
    public org.apache.gora.goraci.generated.CINode.Builder clearCount() {
      fieldSetFlags()[2] = false;
      return this;
    }
    
    @Override
    public CINode build() {
      try {
        CINode record = new CINode();
        record.prev = fieldSetFlags()[0] ? this.prev : (java.lang.Long) defaultValue(fields()[0]);
        record.client = fieldSetFlags()[1] ? this.client : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.count = fieldSetFlags()[2] ? this.count : (java.lang.Long) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public CINode.Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  public CINode newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends CINode implements org.apache.gora.persistency.Tombstone {
  
      private Tombstone() { }
  
	  		  /**
	   * Gets the value of the 'prev' field.
		   */
	  public java.lang.Long getPrev() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'prev' field.
		   * @param value the value to set.
	   */
	  public void setPrev(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'prev' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPrevDirty(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'client' field.
		   */
	  public java.lang.CharSequence getClient() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'client' field.
		   * @param value the value to set.
	   */
	  public void setClient(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'client' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isClientDirty(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'count' field.
		   */
	  public java.lang.Long getCount() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'count' field.
		   * @param value the value to set.
	   */
	  public void setCount(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'count' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCountDirty(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
		  
  }
  
}

