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
package org.apache.gora.cassandra.example.generated.AvroSerialization;  

/** This object created to used as Persistent Object to test cassandra data store */
public class CassandraRecord extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"CassandraRecord\",\"namespace\":\"org.apache.gora.cassandra.example.generated.AvroSerialization\",\"doc\":\"This object created to used as Persistent Object to test cassandra data store\",\"fields\":[{\"name\":\"dataString\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"dataInt\",\"type\":\"int\",\"default\":0},{\"name\":\"dataLong\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"dataDouble\",\"type\":[\"null\",\"double\"],\"default\":null},{\"name\":\"dataBytes\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"arrayInt\",\"type\":{\"type\":\"array\",\"items\":\"int\"},\"default\":null},{\"name\":\"arrayString\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":null},{\"name\":\"arrayLong\",\"type\":{\"type\":\"array\",\"items\":\"long\"},\"default\":null},{\"name\":\"arrayDouble\",\"type\":{\"type\":\"array\",\"items\":\"double\"},\"default\":null},{\"name\":\"mapInt\",\"type\":{\"type\":\"map\",\"values\":\"int\"},\"default\":{}},{\"name\":\"mapString\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"default\":{}},{\"name\":\"mapLong\",\"type\":{\"type\":\"map\",\"values\":\"long\"},\"default\":{}},{\"name\":\"mapDouble\",\"type\":{\"type\":\"map\",\"values\":\"double\"},\"default\":{}}],\"default\":null}");
  private static final long serialVersionUID = -4030705451859358186L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    DATA_STRING(0, "dataString"),
    DATA_INT(1, "dataInt"),
    DATA_LONG(2, "dataLong"),
    DATA_DOUBLE(3, "dataDouble"),
    DATA_BYTES(4, "dataBytes"),
    ARRAY_INT(5, "arrayInt"),
    ARRAY_STRING(6, "arrayString"),
    ARRAY_LONG(7, "arrayLong"),
    ARRAY_DOUBLE(8, "arrayDouble"),
    MAP_INT(9, "mapInt"),
    MAP_STRING(10, "mapString"),
    MAP_LONG(11, "mapLong"),
    MAP_DOUBLE(12, "mapDouble"),
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
  "dataString",
  "dataInt",
  "dataLong",
  "dataDouble",
  "dataBytes",
  "arrayInt",
  "arrayString",
  "arrayLong",
  "arrayDouble",
  "mapInt",
  "mapString",
  "mapLong",
  "mapDouble",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return CassandraRecord._ALL_FIELDS.length;
  }

  private java.lang.CharSequence dataString;
  private int dataInt;
  private java.lang.Long dataLong;
  private java.lang.Double dataDouble;
  private java.nio.ByteBuffer dataBytes;
  private java.util.List<java.lang.Integer> arrayInt;
  private java.util.List<java.lang.CharSequence> arrayString;
  private java.util.List<java.lang.Long> arrayLong;
  private java.util.List<java.lang.Double> arrayDouble;
  private java.util.Map<java.lang.CharSequence,java.lang.Integer> mapInt;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> mapString;
  private java.util.Map<java.lang.CharSequence,java.lang.Long> mapLong;
  private java.util.Map<java.lang.CharSequence,java.lang.Double> mapDouble;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return dataString;
    case 1: return dataInt;
    case 2: return dataLong;
    case 3: return dataDouble;
    case 4: return dataBytes;
    case 5: return arrayInt;
    case 6: return arrayString;
    case 7: return arrayLong;
    case 8: return arrayDouble;
    case 9: return mapInt;
    case 10: return mapString;
    case 11: return mapLong;
    case 12: return mapDouble;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: dataString = (java.lang.CharSequence)(value); break;
    case 1: dataInt = (java.lang.Integer)(value); break;
    case 2: dataLong = (java.lang.Long)(value); break;
    case 3: dataDouble = (java.lang.Double)(value); break;
    case 4: dataBytes = (java.nio.ByteBuffer)(value); break;
    case 5: arrayInt = (java.util.List<java.lang.Integer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 6: arrayString = (java.util.List<java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 7: arrayLong = (java.util.List<java.lang.Long>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 8: arrayDouble = (java.util.List<java.lang.Double>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)value)); break;
    case 9: mapInt = (java.util.Map<java.lang.CharSequence,java.lang.Integer>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 10: mapString = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 11: mapLong = (java.util.Map<java.lang.CharSequence,java.lang.Long>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    case 12: mapDouble = (java.util.Map<java.lang.CharSequence,java.lang.Double>)((value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)value)); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'dataString' field.
   */
  public java.lang.CharSequence getDataString() {
    return dataString;
  }

  /**
   * Sets the value of the 'dataString' field.
   * @param value the value to set.
   */
  public void setDataString(java.lang.CharSequence value) {
    this.dataString = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'dataString' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataStringDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'dataInt' field.
   */
  public java.lang.Integer getDataInt() {
    return dataInt;
  }

  /**
   * Sets the value of the 'dataInt' field.
   * @param value the value to set.
   */
  public void setDataInt(java.lang.Integer value) {
    this.dataInt = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'dataInt' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataIntDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'dataLong' field.
   */
  public java.lang.Long getDataLong() {
    return dataLong;
  }

  /**
   * Sets the value of the 'dataLong' field.
   * @param value the value to set.
   */
  public void setDataLong(java.lang.Long value) {
    this.dataLong = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'dataLong' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataLongDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'dataDouble' field.
   */
  public java.lang.Double getDataDouble() {
    return dataDouble;
  }

  /**
   * Sets the value of the 'dataDouble' field.
   * @param value the value to set.
   */
  public void setDataDouble(java.lang.Double value) {
    this.dataDouble = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'dataDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataDoubleDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'dataBytes' field.
   */
  public java.nio.ByteBuffer getDataBytes() {
    return dataBytes;
  }

  /**
   * Sets the value of the 'dataBytes' field.
   * @param value the value to set.
   */
  public void setDataBytes(java.nio.ByteBuffer value) {
    this.dataBytes = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'dataBytes' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDataBytesDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'arrayInt' field.
   */
  public java.util.List<java.lang.Integer> getArrayInt() {
    return arrayInt;
  }

  /**
   * Sets the value of the 'arrayInt' field.
   * @param value the value to set.
   */
  public void setArrayInt(java.util.List<java.lang.Integer> value) {
    this.arrayInt = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'arrayInt' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isArrayIntDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'arrayString' field.
   */
  public java.util.List<java.lang.CharSequence> getArrayString() {
    return arrayString;
  }

  /**
   * Sets the value of the 'arrayString' field.
   * @param value the value to set.
   */
  public void setArrayString(java.util.List<java.lang.CharSequence> value) {
    this.arrayString = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'arrayString' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isArrayStringDirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'arrayLong' field.
   */
  public java.util.List<java.lang.Long> getArrayLong() {
    return arrayLong;
  }

  /**
   * Sets the value of the 'arrayLong' field.
   * @param value the value to set.
   */
  public void setArrayLong(java.util.List<java.lang.Long> value) {
    this.arrayLong = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(7);
  }
  
  /**
   * Checks the dirty status of the 'arrayLong' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isArrayLongDirty() {
    return isDirty(7);
  }

  /**
   * Gets the value of the 'arrayDouble' field.
   */
  public java.util.List<java.lang.Double> getArrayDouble() {
    return arrayDouble;
  }

  /**
   * Sets the value of the 'arrayDouble' field.
   * @param value the value to set.
   */
  public void setArrayDouble(java.util.List<java.lang.Double> value) {
    this.arrayDouble = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyListWrapper(value);
    setDirty(8);
  }
  
  /**
   * Checks the dirty status of the 'arrayDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isArrayDoubleDirty() {
    return isDirty(8);
  }

  /**
   * Gets the value of the 'mapInt' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.Integer> getMapInt() {
    return mapInt;
  }

  /**
   * Sets the value of the 'mapInt' field.
   * @param value the value to set.
   */
  public void setMapInt(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
    this.mapInt = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(9);
  }
  
  /**
   * Checks the dirty status of the 'mapInt' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMapIntDirty() {
    return isDirty(9);
  }

  /**
   * Gets the value of the 'mapString' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMapString() {
    return mapString;
  }

  /**
   * Sets the value of the 'mapString' field.
   * @param value the value to set.
   */
  public void setMapString(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.mapString = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(10);
  }
  
  /**
   * Checks the dirty status of the 'mapString' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMapStringDirty() {
    return isDirty(10);
  }

  /**
   * Gets the value of the 'mapLong' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.Long> getMapLong() {
    return mapLong;
  }

  /**
   * Sets the value of the 'mapLong' field.
   * @param value the value to set.
   */
  public void setMapLong(java.util.Map<java.lang.CharSequence,java.lang.Long> value) {
    this.mapLong = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(11);
  }
  
  /**
   * Checks the dirty status of the 'mapLong' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMapLongDirty() {
    return isDirty(11);
  }

  /**
   * Gets the value of the 'mapDouble' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.Double> getMapDouble() {
    return mapDouble;
  }

  /**
   * Sets the value of the 'mapDouble' field.
   * @param value the value to set.
   */
  public void setMapDouble(java.util.Map<java.lang.CharSequence,java.lang.Double> value) {
    this.mapDouble = (value instanceof org.apache.gora.persistency.Dirtyable) ? value : new org.apache.gora.persistency.impl.DirtyMapWrapper(value);
    setDirty(12);
  }
  
  /**
   * Checks the dirty status of the 'mapDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMapDoubleDirty() {
    return isDirty(12);
  }

  /** Creates a new CassandraRecord RecordBuilder */
  public static org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder newBuilder() {
    return new org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder();
  }
  
  /** Creates a new CassandraRecord RecordBuilder by copying an existing Builder */
  public static org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder newBuilder(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder other) {
    return new org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder(other);
  }
  
  /** Creates a new CassandraRecord RecordBuilder by copying an existing CassandraRecord instance */
  public static org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder newBuilder(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord other) {
    return new org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder(other);
  }
  
  @Override
  public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord clone() {
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
   * RecordBuilder for CassandraRecord instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<CassandraRecord>
    implements org.apache.avro.data.RecordBuilder<CassandraRecord> {

    private java.lang.CharSequence dataString;
    private int dataInt;
    private java.lang.Long dataLong;
    private java.lang.Double dataDouble;
    private java.nio.ByteBuffer dataBytes;
    private java.util.List<java.lang.Integer> arrayInt;
    private java.util.List<java.lang.CharSequence> arrayString;
    private java.util.List<java.lang.Long> arrayLong;
    private java.util.List<java.lang.Double> arrayDouble;
    private java.util.Map<java.lang.CharSequence,java.lang.Integer> mapInt;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> mapString;
    private java.util.Map<java.lang.CharSequence,java.lang.Long> mapLong;
    private java.util.Map<java.lang.CharSequence,java.lang.Double> mapDouble;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing CassandraRecord instance */
    private Builder(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord other) {
            super(org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.SCHEMA$);
      if (isValidValue(fields()[0], other.dataString)) {
        this.dataString = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.dataString);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.dataInt)) {
        this.dataInt = (java.lang.Integer) data().deepCopy(fields()[1].schema(), other.dataInt);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.dataLong)) {
        this.dataLong = (java.lang.Long) data().deepCopy(fields()[2].schema(), other.dataLong);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.dataDouble)) {
        this.dataDouble = (java.lang.Double) data().deepCopy(fields()[3].schema(), other.dataDouble);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.dataBytes)) {
        this.dataBytes = (java.nio.ByteBuffer) data().deepCopy(fields()[4].schema(), other.dataBytes);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.arrayInt)) {
        this.arrayInt = (java.util.List<java.lang.Integer>) data().deepCopy(fields()[5].schema(), other.arrayInt);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.arrayString)) {
        this.arrayString = (java.util.List<java.lang.CharSequence>) data().deepCopy(fields()[6].schema(), other.arrayString);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.arrayLong)) {
        this.arrayLong = (java.util.List<java.lang.Long>) data().deepCopy(fields()[7].schema(), other.arrayLong);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.arrayDouble)) {
        this.arrayDouble = (java.util.List<java.lang.Double>) data().deepCopy(fields()[8].schema(), other.arrayDouble);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.mapInt)) {
        this.mapInt = (java.util.Map<java.lang.CharSequence,java.lang.Integer>) data().deepCopy(fields()[9].schema(), other.mapInt);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.mapString)) {
        this.mapString = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) data().deepCopy(fields()[10].schema(), other.mapString);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.mapLong)) {
        this.mapLong = (java.util.Map<java.lang.CharSequence,java.lang.Long>) data().deepCopy(fields()[11].schema(), other.mapLong);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.mapDouble)) {
        this.mapDouble = (java.util.Map<java.lang.CharSequence,java.lang.Double>) data().deepCopy(fields()[12].schema(), other.mapDouble);
        fieldSetFlags()[12] = true;
      }
    }

    /** Gets the value of the 'dataString' field */
    public java.lang.CharSequence getDataString() {
      return dataString;
    }
    
    /** Sets the value of the 'dataString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setDataString(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.dataString = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'dataString' field has been set */
    public boolean hasDataString() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'dataString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearDataString() {
      dataString = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'dataInt' field */
    public java.lang.Integer getDataInt() {
      return dataInt;
    }
    
    /** Sets the value of the 'dataInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setDataInt(int value) {
      validate(fields()[1], value);
      this.dataInt = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'dataInt' field has been set */
    public boolean hasDataInt() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'dataInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearDataInt() {
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'dataLong' field */
    public java.lang.Long getDataLong() {
      return dataLong;
    }
    
    /** Sets the value of the 'dataLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setDataLong(java.lang.Long value) {
      validate(fields()[2], value);
      this.dataLong = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'dataLong' field has been set */
    public boolean hasDataLong() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'dataLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearDataLong() {
      dataLong = null;
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'dataDouble' field */
    public java.lang.Double getDataDouble() {
      return dataDouble;
    }
    
    /** Sets the value of the 'dataDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setDataDouble(java.lang.Double value) {
      validate(fields()[3], value);
      this.dataDouble = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'dataDouble' field has been set */
    public boolean hasDataDouble() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'dataDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearDataDouble() {
      dataDouble = null;
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'dataBytes' field */
    public java.nio.ByteBuffer getDataBytes() {
      return dataBytes;
    }
    
    /** Sets the value of the 'dataBytes' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setDataBytes(java.nio.ByteBuffer value) {
      validate(fields()[4], value);
      this.dataBytes = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'dataBytes' field has been set */
    public boolean hasDataBytes() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'dataBytes' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearDataBytes() {
      dataBytes = null;
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'arrayInt' field */
    public java.util.List<java.lang.Integer> getArrayInt() {
      return arrayInt;
    }
    
    /** Sets the value of the 'arrayInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setArrayInt(java.util.List<java.lang.Integer> value) {
      validate(fields()[5], value);
      this.arrayInt = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'arrayInt' field has been set */
    public boolean hasArrayInt() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'arrayInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearArrayInt() {
      arrayInt = null;
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'arrayString' field */
    public java.util.List<java.lang.CharSequence> getArrayString() {
      return arrayString;
    }
    
    /** Sets the value of the 'arrayString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setArrayString(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[6], value);
      this.arrayString = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'arrayString' field has been set */
    public boolean hasArrayString() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'arrayString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearArrayString() {
      arrayString = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    /** Gets the value of the 'arrayLong' field */
    public java.util.List<java.lang.Long> getArrayLong() {
      return arrayLong;
    }
    
    /** Sets the value of the 'arrayLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setArrayLong(java.util.List<java.lang.Long> value) {
      validate(fields()[7], value);
      this.arrayLong = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'arrayLong' field has been set */
    public boolean hasArrayLong() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'arrayLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearArrayLong() {
      arrayLong = null;
      fieldSetFlags()[7] = false;
      return this;
    }
    
    /** Gets the value of the 'arrayDouble' field */
    public java.util.List<java.lang.Double> getArrayDouble() {
      return arrayDouble;
    }
    
    /** Sets the value of the 'arrayDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setArrayDouble(java.util.List<java.lang.Double> value) {
      validate(fields()[8], value);
      this.arrayDouble = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'arrayDouble' field has been set */
    public boolean hasArrayDouble() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'arrayDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearArrayDouble() {
      arrayDouble = null;
      fieldSetFlags()[8] = false;
      return this;
    }
    
    /** Gets the value of the 'mapInt' field */
    public java.util.Map<java.lang.CharSequence,java.lang.Integer> getMapInt() {
      return mapInt;
    }
    
    /** Sets the value of the 'mapInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setMapInt(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
      validate(fields()[9], value);
      this.mapInt = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'mapInt' field has been set */
    public boolean hasMapInt() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'mapInt' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearMapInt() {
      mapInt = null;
      fieldSetFlags()[9] = false;
      return this;
    }
    
    /** Gets the value of the 'mapString' field */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMapString() {
      return mapString;
    }
    
    /** Sets the value of the 'mapString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setMapString(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[10], value);
      this.mapString = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'mapString' field has been set */
    public boolean hasMapString() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'mapString' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearMapString() {
      mapString = null;
      fieldSetFlags()[10] = false;
      return this;
    }
    
    /** Gets the value of the 'mapLong' field */
    public java.util.Map<java.lang.CharSequence,java.lang.Long> getMapLong() {
      return mapLong;
    }
    
    /** Sets the value of the 'mapLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setMapLong(java.util.Map<java.lang.CharSequence,java.lang.Long> value) {
      validate(fields()[11], value);
      this.mapLong = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'mapLong' field has been set */
    public boolean hasMapLong() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'mapLong' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearMapLong() {
      mapLong = null;
      fieldSetFlags()[11] = false;
      return this;
    }
    
    /** Gets the value of the 'mapDouble' field */
    public java.util.Map<java.lang.CharSequence,java.lang.Double> getMapDouble() {
      return mapDouble;
    }
    
    /** Sets the value of the 'mapDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder setMapDouble(java.util.Map<java.lang.CharSequence,java.lang.Double> value) {
      validate(fields()[12], value);
      this.mapDouble = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'mapDouble' field has been set */
    public boolean hasMapDouble() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'mapDouble' field */
    public org.apache.gora.cassandra.example.generated.AvroSerialization.CassandraRecord.Builder clearMapDouble() {
      mapDouble = null;
      fieldSetFlags()[12] = false;
      return this;
    }
    
    @Override
    public CassandraRecord build() {
      try {
        CassandraRecord record = new CassandraRecord();
        record.dataString = fieldSetFlags()[0] ? this.dataString : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.dataInt = fieldSetFlags()[1] ? this.dataInt : (java.lang.Integer) defaultValue(fields()[1]);
        record.dataLong = fieldSetFlags()[2] ? this.dataLong : (java.lang.Long) defaultValue(fields()[2]);
        record.dataDouble = fieldSetFlags()[3] ? this.dataDouble : (java.lang.Double) defaultValue(fields()[3]);
        record.dataBytes = fieldSetFlags()[4] ? this.dataBytes : (java.nio.ByteBuffer) defaultValue(fields()[4]);
        record.arrayInt = fieldSetFlags()[5] ? this.arrayInt : (java.util.List<java.lang.Integer>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[5]));
        record.arrayString = fieldSetFlags()[6] ? this.arrayString : (java.util.List<java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[6]));
        record.arrayLong = fieldSetFlags()[7] ? this.arrayLong : (java.util.List<java.lang.Long>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[7]));
        record.arrayDouble = fieldSetFlags()[8] ? this.arrayDouble : (java.util.List<java.lang.Double>) new org.apache.gora.persistency.impl.DirtyListWrapper((java.util.List)defaultValue(fields()[8]));
        record.mapInt = fieldSetFlags()[9] ? this.mapInt : (java.util.Map<java.lang.CharSequence,java.lang.Integer>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[9]));
        record.mapString = fieldSetFlags()[10] ? this.mapString : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[10]));
        record.mapLong = fieldSetFlags()[11] ? this.mapLong : (java.util.Map<java.lang.CharSequence,java.lang.Long>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[11]));
        record.mapDouble = fieldSetFlags()[12] ? this.mapDouble : (java.util.Map<java.lang.CharSequence,java.lang.Double>) new org.apache.gora.persistency.impl.DirtyMapWrapper((java.util.Map)defaultValue(fields()[12]));
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public CassandraRecord.Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  public CassandraRecord newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends CassandraRecord implements org.apache.gora.persistency.Tombstone {
  
      private Tombstone() { }
  
	  		  /**
	   * Gets the value of the 'dataString' field.
		   */
	  public java.lang.CharSequence getDataString() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'dataString' field.
		   * @param value the value to set.
	   */
	  public void setDataString(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'dataString' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDataStringDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'dataInt' field.
		   */
	  public java.lang.Integer getDataInt() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'dataInt' field.
		   * @param value the value to set.
	   */
	  public void setDataInt(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'dataInt' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDataIntDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'dataLong' field.
		   */
	  public java.lang.Long getDataLong() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'dataLong' field.
		   * @param value the value to set.
	   */
	  public void setDataLong(java.lang.Long value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'dataLong' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDataLongDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'dataDouble' field.
		   */
	  public java.lang.Double getDataDouble() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'dataDouble' field.
		   * @param value the value to set.
	   */
	  public void setDataDouble(java.lang.Double value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'dataDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDataDoubleDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'dataBytes' field.
		   */
	  public java.nio.ByteBuffer getDataBytes() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'dataBytes' field.
		   * @param value the value to set.
	   */
	  public void setDataBytes(java.nio.ByteBuffer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'dataBytes' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isDataBytesDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'arrayInt' field.
		   */
	  public java.util.List<java.lang.Integer> getArrayInt() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'arrayInt' field.
		   * @param value the value to set.
	   */
	  public void setArrayInt(java.util.List<java.lang.Integer> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'arrayInt' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isArrayIntDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'arrayString' field.
		   */
	  public java.util.List<java.lang.CharSequence> getArrayString() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'arrayString' field.
		   * @param value the value to set.
	   */
	  public void setArrayString(java.util.List<java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'arrayString' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isArrayStringDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'arrayLong' field.
		   */
	  public java.util.List<java.lang.Long> getArrayLong() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'arrayLong' field.
		   * @param value the value to set.
	   */
	  public void setArrayLong(java.util.List<java.lang.Long> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'arrayLong' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isArrayLongDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'arrayDouble' field.
		   */
	  public java.util.List<java.lang.Double> getArrayDouble() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'arrayDouble' field.
		   * @param value the value to set.
	   */
	  public void setArrayDouble(java.util.List<java.lang.Double> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'arrayDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isArrayDoubleDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'mapInt' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.Integer> getMapInt() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'mapInt' field.
		   * @param value the value to set.
	   */
	  public void setMapInt(java.util.Map<java.lang.CharSequence,java.lang.Integer> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'mapInt' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMapIntDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'mapString' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getMapString() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'mapString' field.
		   * @param value the value to set.
	   */
	  public void setMapString(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'mapString' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMapStringDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'mapLong' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.Long> getMapLong() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'mapLong' field.
		   * @param value the value to set.
	   */
	  public void setMapLong(java.util.Map<java.lang.CharSequence,java.lang.Long> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'mapLong' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMapLongDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'mapDouble' field.
		   */
	  public java.util.Map<java.lang.CharSequence,java.lang.Double> getMapDouble() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'mapDouble' field.
		   * @param value the value to set.
	   */
	  public void setMapDouble(java.util.Map<java.lang.CharSequence,java.lang.Double> value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'mapDouble' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isMapDoubleDirty() {
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

