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

public class Pageview extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Pageview\",\"namespace\":\"org.apache.gora.tutorial.log.generated\",\"fields\":[{\"name\":\"url\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"timestamp\",\"type\":\"long\",\"default\":0},{\"name\":\"ip\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"httpMethod\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"httpStatusCode\",\"type\":\"int\",\"default\":0},{\"name\":\"responseSize\",\"type\":\"int\",\"default\":0},{\"name\":\"referrer\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"userAgent\",\"type\":[\"null\",\"string\"],\"default\":null}],\"default\":null}");
  private static final long serialVersionUID = -6136058768384995982L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    URL(0, "url"),
    TIMESTAMP(1, "timestamp"),
    IP(2, "ip"),
    HTTP_METHOD(3, "httpMethod"),
    HTTP_STATUS_CODE(4, "httpStatusCode"),
    RESPONSE_SIZE(5, "responseSize"),
    REFERRER(6, "referrer"),
    USER_AGENT(7, "userAgent"),
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
  "url",
  "timestamp",
  "ip",
  "httpMethod",
  "httpStatusCode",
  "responseSize",
  "referrer",
  "userAgent",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return Pageview._ALL_FIELDS.length;
  }

  private java.lang.CharSequence url;
  private long timestamp;
  private java.lang.CharSequence ip;
  private java.lang.CharSequence httpMethod;
  private int httpStatusCode;
  private int responseSize;
  private java.lang.CharSequence referrer;
  private java.lang.CharSequence userAgent;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return this.url;
    case 1: return this.timestamp;
    case 2: return this.ip;
    case 3: return this.httpMethod;
    case 4: return this.httpStatusCode;
    case 5: return this.responseSize;
    case 6: return this.referrer;
    case 7: return this.userAgent;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: this.url = (java.lang.CharSequence)(value); break;
    case 1: this.timestamp = (java.lang.Long)(value); break;
    case 2: this.ip = (java.lang.CharSequence)(value); break;
    case 3: this.httpMethod = (java.lang.CharSequence)(value); break;
    case 4: this.httpStatusCode = (java.lang.Integer)(value); break;
    case 5: this.responseSize = (java.lang.Integer)(value); break;
    case 6: this.referrer = (java.lang.CharSequence)(value); break;
    case 7: this.userAgent = (java.lang.CharSequence)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'url' field.
   */
  public java.lang.CharSequence getUrl() {
    return url;
  }

  /**
   * Sets the value of the 'url' field.
   * @param value the value to set.
   */
  public void setUrl(java.lang.CharSequence value) {
    this.url = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'url' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isUrlDirty() {
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
   * Gets the value of the 'ip' field.
   */
  public java.lang.CharSequence getIp() {
    return ip;
  }

  /**
   * Sets the value of the 'ip' field.
   * @param value the value to set.
   */
  public void setIp(java.lang.CharSequence value) {
    this.ip = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'ip' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isIpDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'httpMethod' field.
   */
  public java.lang.CharSequence getHttpMethod() {
    return httpMethod;
  }

  /**
   * Sets the value of the 'httpMethod' field.
   * @param value the value to set.
   */
  public void setHttpMethod(java.lang.CharSequence value) {
    this.httpMethod = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'httpMethod' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isHttpMethodDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'httpStatusCode' field.
   */
  public java.lang.Integer getHttpStatusCode() {
    return httpStatusCode;
  }

  /**
   * Sets the value of the 'httpStatusCode' field.
   * @param value the value to set.
   */
  public void setHttpStatusCode(java.lang.Integer value) {
    this.httpStatusCode = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'httpStatusCode' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isHttpStatusCodeDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'responseSize' field.
   */
  public java.lang.Integer getResponseSize() {
    return responseSize;
  }

  /**
   * Sets the value of the 'responseSize' field.
   * @param value the value to set.
   */
  public void setResponseSize(java.lang.Integer value) {
    this.responseSize = value;
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'responseSize' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isResponseSizeDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'referrer' field.
   */
  public java.lang.CharSequence getReferrer() {
    return referrer;
  }

  /**
   * Sets the value of the 'referrer' field.
   * @param value the value to set.
   */
  public void setReferrer(java.lang.CharSequence value) {
    this.referrer = value;
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isReferrerDirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'userAgent' field.
   */
  public java.lang.CharSequence getUserAgent() {
    return userAgent;
  }

  /**
   * Sets the value of the 'userAgent' field.
   * @param value the value to set.
   */
  public void setUserAgent(java.lang.CharSequence value) {
    this.userAgent = value;
    setDirty(7);
  }
  
  /**
   * Checks the dirty status of the 'userAgent' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isUserAgentDirty() {
    return isDirty(7);
  }

  /** Creates a new Pageview RecordBuilder */
  public static org.apache.gora.tutorial.log.generated.Pageview.Builder newBuilder() {
    return new org.apache.gora.tutorial.log.generated.Pageview.Builder();
  }
  
  /** Creates a new Pageview RecordBuilder by copying an existing Builder */
  public static org.apache.gora.tutorial.log.generated.Pageview.Builder newBuilder(org.apache.gora.tutorial.log.generated.Pageview.Builder other) {
    return new org.apache.gora.tutorial.log.generated.Pageview.Builder(other);
  }
  
  /** Creates a new Pageview RecordBuilder by copying an existing Pageview instance */
  public static org.apache.gora.tutorial.log.generated.Pageview.Builder newBuilder(org.apache.gora.tutorial.log.generated.Pageview other) {
    return new org.apache.gora.tutorial.log.generated.Pageview.Builder(other);
  }
  
  @Override
  public org.apache.gora.tutorial.log.generated.Pageview clone() {
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
   * RecordBuilder for Pageview instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Pageview>
    implements org.apache.avro.data.RecordBuilder<Pageview> {

    private java.lang.CharSequence url;
    private long timestamp;
    private java.lang.CharSequence ip;
    private java.lang.CharSequence httpMethod;
    private int httpStatusCode;
    private int responseSize;
    private java.lang.CharSequence referrer;
    private java.lang.CharSequence userAgent;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.tutorial.log.generated.Pageview.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.tutorial.log.generated.Pageview.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing Pageview instance */
    private Builder(org.apache.gora.tutorial.log.generated.Pageview other) {
            super(org.apache.gora.tutorial.log.generated.Pageview.SCHEMA$);
      if (isValidValue(fields()[0], other.url)) {
        this.url = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.url);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = (java.lang.Long) data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.ip)) {
        this.ip = (java.lang.CharSequence) data().deepCopy(fields()[2].schema(), other.ip);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.httpMethod)) {
        this.httpMethod = (java.lang.CharSequence) data().deepCopy(fields()[3].schema(), other.httpMethod);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.httpStatusCode)) {
        this.httpStatusCode = (java.lang.Integer) data().deepCopy(fields()[4].schema(), other.httpStatusCode);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.responseSize)) {
        this.responseSize = (java.lang.Integer) data().deepCopy(fields()[5].schema(), other.responseSize);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.referrer)) {
        this.referrer = (java.lang.CharSequence) data().deepCopy(fields()[6].schema(), other.referrer);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.userAgent)) {
        this.userAgent = (java.lang.CharSequence) data().deepCopy(fields()[7].schema(), other.userAgent);
        fieldSetFlags()[7] = true;
      }
    }

    /** Gets the value of the 'url' field */
    public java.lang.CharSequence getUrl() {
      return url;
    }
    
    /** Sets the value of the 'url' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setUrl(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.url = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'url' field has been set */
    public boolean hasUrl() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'url' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearUrl() {
      url = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'timestamp' field */
    public java.lang.Long getTimestamp() {
      return timestamp;
    }
    
    /** Sets the value of the 'timestamp' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setTimestamp(long value) {
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
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearTimestamp() {
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'ip' field */
    public java.lang.CharSequence getIp() {
      return ip;
    }
    
    /** Sets the value of the 'ip' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setIp(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.ip = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'ip' field has been set */
    public boolean hasIp() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'ip' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearIp() {
      ip = null;
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'httpMethod' field */
    public java.lang.CharSequence getHttpMethod() {
      return httpMethod;
    }
    
    /** Sets the value of the 'httpMethod' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setHttpMethod(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.httpMethod = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'httpMethod' field has been set */
    public boolean hasHttpMethod() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'httpMethod' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearHttpMethod() {
      httpMethod = null;
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'httpStatusCode' field */
    public java.lang.Integer getHttpStatusCode() {
      return httpStatusCode;
    }
    
    /** Sets the value of the 'httpStatusCode' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setHttpStatusCode(int value) {
      validate(fields()[4], value);
      this.httpStatusCode = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'httpStatusCode' field has been set */
    public boolean hasHttpStatusCode() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'httpStatusCode' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearHttpStatusCode() {
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'responseSize' field */
    public java.lang.Integer getResponseSize() {
      return responseSize;
    }
    
    /** Sets the value of the 'responseSize' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setResponseSize(int value) {
      validate(fields()[5], value);
      this.responseSize = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'responseSize' field has been set */
    public boolean hasResponseSize() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'responseSize' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearResponseSize() {
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'referrer' field */
    public java.lang.CharSequence getReferrer() {
      return referrer;
    }
    
    /** Sets the value of the 'referrer' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setReferrer(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.referrer = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'referrer' field has been set */
    public boolean hasReferrer() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'referrer' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearReferrer() {
      referrer = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    /** Gets the value of the 'userAgent' field */
    public java.lang.CharSequence getUserAgent() {
      return userAgent;
    }
    
    /** Sets the value of the 'userAgent' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder setUserAgent(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.userAgent = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'userAgent' field has been set */
    public boolean hasUserAgent() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'userAgent' field */
    public org.apache.gora.tutorial.log.generated.Pageview.Builder clearUserAgent() {
      userAgent = null;
      fieldSetFlags()[7] = false;
      return this;
    }
    
    @Override
    public Pageview build() {
      try {
        Pageview record = new Pageview();
        record.url = fieldSetFlags()[0] ? this.url : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.timestamp = fieldSetFlags()[1] ? this.timestamp : (java.lang.Long) defaultValue(fields()[1]);
        record.ip = fieldSetFlags()[2] ? this.ip : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.httpMethod = fieldSetFlags()[3] ? this.httpMethod : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.httpStatusCode = fieldSetFlags()[4] ? this.httpStatusCode : (java.lang.Integer) defaultValue(fields()[4]);
        record.responseSize = fieldSetFlags()[5] ? this.responseSize : (java.lang.Integer) defaultValue(fields()[5]);
        record.referrer = fieldSetFlags()[6] ? this.referrer : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.userAgent = fieldSetFlags()[7] ? this.userAgent : (java.lang.CharSequence) defaultValue(fields()[7]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public Pageview.Tombstone getTombstone(){
    return TOMBSTONE;
  }

  public Pageview newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends Pageview implements org.apache.gora.persistency.Tombstone {
  
    private Tombstone() { }
  
      /**
     * Gets the value of the 'url' field.
         */
    public java.lang.CharSequence getUrl() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'url' field.
         * @param value the value to set.
     */
    public void setUrl(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'url' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isUrlDirty() {
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
     * Gets the value of the 'ip' field.
         */
    public java.lang.CharSequence getIp() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'ip' field.
         * @param value the value to set.
     */
    public void setIp(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'ip' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isIpDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'httpMethod' field.
         */
    public java.lang.CharSequence getHttpMethod() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'httpMethod' field.
         * @param value the value to set.
     */
    public void setHttpMethod(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'httpMethod' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isHttpMethodDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'httpStatusCode' field.
         */
    public java.lang.Integer getHttpStatusCode() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'httpStatusCode' field.
         * @param value the value to set.
     */
    public void setHttpStatusCode(java.lang.Integer value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'httpStatusCode' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isHttpStatusCodeDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'responseSize' field.
         */
    public java.lang.Integer getResponseSize() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'responseSize' field.
         * @param value the value to set.
     */
    public void setResponseSize(java.lang.Integer value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'responseSize' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isResponseSizeDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'referrer' field.
         */
    public java.lang.CharSequence getReferrer() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'referrer' field.
         * @param value the value to set.
     */
    public void setReferrer(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'referrer' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isReferrerDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'userAgent' field.
         */
    public java.lang.CharSequence getUserAgent() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'userAgent' field.
         * @param value the value to set.
     */
    public void setUserAgent(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'userAgent' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isUserAgentDirty() {
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

