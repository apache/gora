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
package org.apache.gora.tutorial.log.generated;  

public class MetricDatum extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"MetricDatum\",\"namespace\":\"org.apache.gora.tutorial.log.generated\",\"fields\":[{\"name\":\"metricDimension\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"timestamp\",\"type\":\"long\",\"default\":0},{\"name\":\"metric\",\"type\":\"long\",\"default\":0}],\"default\":null}");
  private static final long serialVersionUID = 8278557845311856507L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    METRIC_DIMENSION(0, "metricDimension"),
    TIMESTAMP(1, "timestamp"),
    METRIC(2, "metric"),
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
  "metricDimension",
  "timestamp",
  "metric",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return MetricDatum._ALL_FIELDS.length;
  }

  private java.lang.CharSequence metricDimension;
  private long timestamp;
  private long metric;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return this.metricDimension;
    case 1: return this.timestamp;
    case 2: return this.metric;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: this.metricDimension = (java.lang.CharSequence)(value); break;
    case 1: this.timestamp = (java.lang.Long)(value); break;
    case 2: this.metric = (java.lang.Long)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'metricDimension' field.
   */
  public java.lang.CharSequence getMetricDimension() {
    return metricDimension;
  }

  /**
   * Sets the value of the 'metricDimension' field.
   * @param value the value to set.
   */
  public void setMetricDimension(java.lang.CharSequence value) {
    this.metricDimension = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'metricDimension' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMetricDimensionDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'timestamp' field.
   */
  public java.lang.Long getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the value of the 'timestamp' field.
   * @param value the value to set.
   */
  public void setTimestamp(java.lang.Long value) {
    this.timestamp = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'timestamp' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isTimestampDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'metric' field.
   */
  public java.lang.Long getMetric() {
    return metric;
  }

  /**
   * Sets the value of the 'metric' field.
   * @param value the value to set.
   */
  public void setMetric(java.lang.Long value) {
    this.metric = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'metric' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isMetricDirty() {
    return isDirty(2);
  }

  /** Creates a new MetricDatum RecordBuilder */
  public static org.apache.gora.tutorial.log.generated.MetricDatum.Builder newBuilder() {
    return new org.apache.gora.tutorial.log.generated.MetricDatum.Builder();
  }
  
  /** Creates a new MetricDatum RecordBuilder by copying an existing Builder */
  public static org.apache.gora.tutorial.log.generated.MetricDatum.Builder newBuilder(org.apache.gora.tutorial.log.generated.MetricDatum.Builder other) {
    return new org.apache.gora.tutorial.log.generated.MetricDatum.Builder(other);
  }
  
  /** Creates a new MetricDatum RecordBuilder by copying an existing MetricDatum instance */
  public static org.apache.gora.tutorial.log.generated.MetricDatum.Builder newBuilder(org.apache.gora.tutorial.log.generated.MetricDatum other) {
    return new org.apache.gora.tutorial.log.generated.MetricDatum.Builder(other);
  }
  
  @Override
  public org.apache.gora.tutorial.log.generated.MetricDatum clone() {
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
   * RecordBuilder for MetricDatum instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<MetricDatum>
    implements org.apache.avro.data.RecordBuilder<MetricDatum> {

    private java.lang.CharSequence metricDimension;
    private long timestamp;
    private long metric;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.tutorial.log.generated.MetricDatum.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.tutorial.log.generated.MetricDatum.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing MetricDatum instance */
    private Builder(org.apache.gora.tutorial.log.generated.MetricDatum other) {
            super(org.apache.gora.tutorial.log.generated.MetricDatum.SCHEMA$);
      if (isValidValue(fields()[0], other.metricDimension)) {
        this.metricDimension = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.metricDimension);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = (java.lang.Long) data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.metric)) {
        this.metric = (java.lang.Long) data().deepCopy(fields()[2].schema(), other.metric);
        fieldSetFlags()[2] = true;
      }
    }

    /** Gets the value of the 'metricDimension' field */
    public java.lang.CharSequence getMetricDimension() {
      return metricDimension;
    }
    
    /** Sets the value of the 'metricDimension' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder setMetricDimension(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.metricDimension = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'metricDimension' field has been set */
    public boolean hasMetricDimension() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'metricDimension' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder clearMetricDimension() {
      metricDimension = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'timestamp' field */
    public java.lang.Long getTimestamp() {
      return timestamp;
    }
    
    /** Sets the value of the 'timestamp' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder setTimestamp(long value) {
      validate(fields()[1], value);
      this.timestamp = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'timestamp' field has been set */
    public boolean hasTimestamp() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'timestamp' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder clearTimestamp() {
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'metric' field */
    public java.lang.Long getMetric() {
      return metric;
    }
    
    /** Sets the value of the 'metric' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder setMetric(long value) {
      validate(fields()[2], value);
      this.metric = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'metric' field has been set */
    public boolean hasMetric() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'metric' field */
    public org.apache.gora.tutorial.log.generated.MetricDatum.Builder clearMetric() {
      fieldSetFlags()[2] = false;
      return this;
    }
    
    @Override
    public MetricDatum build() {
      try {
        MetricDatum record = new MetricDatum();
        record.metricDimension = fieldSetFlags()[0] ? this.metricDimension : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.timestamp = fieldSetFlags()[1] ? this.timestamp : (java.lang.Long) defaultValue(fields()[1]);
        record.metric = fieldSetFlags()[2] ? this.metric : (java.lang.Long) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public MetricDatum.Tombstone getTombstone(){
    return TOMBSTONE;
  }

  public MetricDatum newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends MetricDatum implements org.apache.gora.persistency.Tombstone {
  
    private Tombstone() { }
  
      /**
     * Gets the value of the 'metricDimension' field.
         */
    public java.lang.CharSequence getMetricDimension() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'metricDimension' field.
         * @param value the value to set.
     */
    public void setMetricDimension(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'metricDimension' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isMetricDimensionDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'timestamp' field.
         */
    public java.lang.Long getTimestamp() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'timestamp' field.
         * @param value the value to set.
     */
    public void setTimestamp(java.lang.Long value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'timestamp' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isTimestampDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'metric' field.
         */
    public java.lang.Long getMetric() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'metric' field.
         * @param value the value to set.
     */
    public void setMetric(java.lang.Long value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'metric' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isMetricDirty() {
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

