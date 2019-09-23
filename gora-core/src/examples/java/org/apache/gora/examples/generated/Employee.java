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

public class Employee extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Employee\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"dateOfBirth\",\"type\":\"long\",\"default\":0},{\"name\":\"ssn\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"salary\",\"type\":\"int\",\"default\":0},{\"name\":\"boss\",\"type\":[\"null\",\"Employee\",\"string\"],\"default\":null},{\"name\":\"value\",\"type\":\"string\",\"default\":\"\"},{\"name\":\"webpage\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"WebPage\",\"fields\":[{\"name\":\"url\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"content\",\"type\":[\"null\",\"bytes\"],\"default\":null},{\"name\":\"parsedContent\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[]},{\"name\":\"outlinks\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"string\"]},\"default\":{}},{\"name\":\"headers\",\"type\":[\"null\",{\"type\":\"map\",\"values\":[\"null\",\"string\"]}],\"default\":null},{\"name\":\"metadata\",\"type\":{\"type\":\"record\",\"name\":\"Metadata\",\"fields\":[{\"name\":\"version\",\"type\":\"int\",\"default\":0},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"default\":{}}]},\"default\":{}},{\"name\":\"byteData\",\"type\":{\"type\":\"map\",\"values\":\"bytes\"},\"default\":{}},{\"name\":\"stringData\",\"type\":{\"type\":\"map\",\"values\":\"string\"},\"default\":{}}]}],\"default\":null}],\"default\":null}");
  private static final long serialVersionUID = -3912735665682783747L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    NAME(0, "name"),
    DATE_OF_BIRTH(1, "dateOfBirth"),
    SSN(2, "ssn"),
    SALARY(3, "salary"),
    BOSS(4, "boss"),
    VALUE(5, "value"),
    WEBPAGE(6, "webpage"),
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
  "name",
  "dateOfBirth",
  "ssn",
  "salary",
  "boss",
  "value",
  "webpage",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return Employee._ALL_FIELDS.length;
  }

  private java.lang.CharSequence name;
  private long dateOfBirth;
  private java.lang.CharSequence ssn;
  private int salary;
  private java.lang.Object boss;
  private java.lang.CharSequence value;
  private org.apache.gora.examples.generated.WebPage webpage;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return this.name;
    case 1: return this.dateOfBirth;
    case 2: return this.ssn;
    case 3: return this.salary;
    case 4: return this.boss;
    case 5: return this.value;
    case 6: return this.webpage;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: this.name = (java.lang.CharSequence)(value); break;
    case 1: this.dateOfBirth = (java.lang.Long)(value); break;
    case 2: this.ssn = (java.lang.CharSequence)(value); break;
    case 3: this.salary = (java.lang.Integer)(value); break;
    case 4: this.boss = (java.lang.Object)(value); break;
    case 5: this.value = (java.lang.CharSequence)(value); break;
    case 6: this.webpage = (org.apache.gora.examples.generated.WebPage)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'name' field.
   */
  public java.lang.CharSequence getName() {
    return name;
  }

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  public void setName(java.lang.CharSequence value) {
    this.name = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'name' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isNameDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'dateOfBirth' field.
   */
  public java.lang.Long getDateOfBirth() {
    return dateOfBirth;
  }

  /**
   * Sets the value of the 'dateOfBirth' field.
   * @param value the value to set.
   */
  public void setDateOfBirth(java.lang.Long value) {
    this.dateOfBirth = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'dateOfBirth' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isDateOfBirthDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'ssn' field.
   */
  public java.lang.CharSequence getSsn() {
    return ssn;
  }

  /**
   * Sets the value of the 'ssn' field.
   * @param value the value to set.
   */
  public void setSsn(java.lang.CharSequence value) {
    this.ssn = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'ssn' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isSsnDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'salary' field.
   */
  public java.lang.Integer getSalary() {
    return salary;
  }

  /**
   * Sets the value of the 'salary' field.
   * @param value the value to set.
   */
  public void setSalary(java.lang.Integer value) {
    this.salary = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'salary' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isSalaryDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'boss' field.
   */
  public java.lang.Object getBoss() {
    return boss;
  }

  /**
   * Sets the value of the 'boss' field.
   * @param value the value to set.
   */
  public void setBoss(java.lang.Object value) {
    this.boss = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'boss' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isBossDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'value' field.
   */
  public java.lang.CharSequence getValue() {
    return value;
  }

  /**
   * Sets the value of the 'value' field.
   * @param value the value to set.
   */
  public void setValue(java.lang.CharSequence value) {
    this.value = value;
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'value' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isValueDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'webpage' field.
   */
  public org.apache.gora.examples.generated.WebPage getWebpage() {
    return webpage;
  }

  /**
   * Sets the value of the 'webpage' field.
   * @param value the value to set.
   */
  public void setWebpage(org.apache.gora.examples.generated.WebPage value) {
    this.webpage = value;
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'webpage' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isWebpageDirty() {
    return isDirty(6);
  }

  /** Creates a new Employee RecordBuilder */
  public static org.apache.gora.examples.generated.Employee.Builder newBuilder() {
    return new org.apache.gora.examples.generated.Employee.Builder();
  }
  
  /** Creates a new Employee RecordBuilder by copying an existing Builder */
  public static org.apache.gora.examples.generated.Employee.Builder newBuilder(org.apache.gora.examples.generated.Employee.Builder other) {
    return new org.apache.gora.examples.generated.Employee.Builder(other);
  }
  
  /** Creates a new Employee RecordBuilder by copying an existing Employee instance */
  public static org.apache.gora.examples.generated.Employee.Builder newBuilder(org.apache.gora.examples.generated.Employee other) {
    return new org.apache.gora.examples.generated.Employee.Builder(other);
  }
  
  @Override
  public org.apache.gora.examples.generated.Employee clone() {
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
   * RecordBuilder for Employee instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Employee>
    implements org.apache.avro.data.RecordBuilder<Employee> {

    private java.lang.CharSequence name;
    private long dateOfBirth;
    private java.lang.CharSequence ssn;
    private int salary;
    private java.lang.Object boss;
    private java.lang.CharSequence value;
    private org.apache.gora.examples.generated.WebPage webpage;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.examples.generated.Employee.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.examples.generated.Employee.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing Employee instance */
    private Builder(org.apache.gora.examples.generated.Employee other) {
            super(org.apache.gora.examples.generated.Employee.SCHEMA$);
      if (isValidValue(fields()[0], other.name)) {
        this.name = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.dateOfBirth)) {
        this.dateOfBirth = (java.lang.Long) data().deepCopy(fields()[1].schema(), other.dateOfBirth);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.ssn)) {
        this.ssn = (java.lang.CharSequence) data().deepCopy(fields()[2].schema(), other.ssn);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.salary)) {
        this.salary = (java.lang.Integer) data().deepCopy(fields()[3].schema(), other.salary);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.boss)) {
        this.boss = (java.lang.Object) data().deepCopy(fields()[4].schema(), other.boss);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.value)) {
        this.value = (java.lang.CharSequence) data().deepCopy(fields()[5].schema(), other.value);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.webpage)) {
        this.webpage = (org.apache.gora.examples.generated.WebPage) data().deepCopy(fields()[6].schema(), other.webpage);
        fieldSetFlags()[6] = true;
      }
    }

    /** Gets the value of the 'name' field */
    public java.lang.CharSequence getName() {
      return name;
    }
    
    /** Sets the value of the 'name' field */
    public org.apache.gora.examples.generated.Employee.Builder setName(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.name = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'name' field has been set */
    public boolean hasName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'name' field */
    public org.apache.gora.examples.generated.Employee.Builder clearName() {
      name = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'dateOfBirth' field */
    public java.lang.Long getDateOfBirth() {
      return dateOfBirth;
    }
    
    /** Sets the value of the 'dateOfBirth' field */
    public org.apache.gora.examples.generated.Employee.Builder setDateOfBirth(long value) {
      validate(fields()[1], value);
      this.dateOfBirth = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'dateOfBirth' field has been set */
    public boolean hasDateOfBirth() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'dateOfBirth' field */
    public org.apache.gora.examples.generated.Employee.Builder clearDateOfBirth() {
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'ssn' field */
    public java.lang.CharSequence getSsn() {
      return ssn;
    }
    
    /** Sets the value of the 'ssn' field */
    public org.apache.gora.examples.generated.Employee.Builder setSsn(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.ssn = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'ssn' field has been set */
    public boolean hasSsn() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'ssn' field */
    public org.apache.gora.examples.generated.Employee.Builder clearSsn() {
      ssn = null;
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'salary' field */
    public java.lang.Integer getSalary() {
      return salary;
    }
    
    /** Sets the value of the 'salary' field */
    public org.apache.gora.examples.generated.Employee.Builder setSalary(int value) {
      validate(fields()[3], value);
      this.salary = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'salary' field has been set */
    public boolean hasSalary() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'salary' field */
    public org.apache.gora.examples.generated.Employee.Builder clearSalary() {
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'boss' field */
    public java.lang.Object getBoss() {
      return boss;
    }
    
    /** Sets the value of the 'boss' field */
    public org.apache.gora.examples.generated.Employee.Builder setBoss(java.lang.Object value) {
      validate(fields()[4], value);
      this.boss = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'boss' field has been set */
    public boolean hasBoss() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'boss' field */
    public org.apache.gora.examples.generated.Employee.Builder clearBoss() {
      boss = null;
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'value' field */
    public java.lang.CharSequence getValue() {
      return value;
    }
    
    /** Sets the value of the 'value' field */
    public org.apache.gora.examples.generated.Employee.Builder setValue(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.value = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'value' field has been set */
    public boolean hasValue() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'value' field */
    public org.apache.gora.examples.generated.Employee.Builder clearValue() {
      value = null;
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'webpage' field */
    public org.apache.gora.examples.generated.WebPage getWebpage() {
      return webpage;
    }
    
    /** Sets the value of the 'webpage' field */
    public org.apache.gora.examples.generated.Employee.Builder setWebpage(org.apache.gora.examples.generated.WebPage value) {
      validate(fields()[6], value);
      this.webpage = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'webpage' field has been set */
    public boolean hasWebpage() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'webpage' field */
    public org.apache.gora.examples.generated.Employee.Builder clearWebpage() {
      webpage = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    @Override
    public Employee build() {
      try {
        Employee record = new Employee();
        record.name = fieldSetFlags()[0] ? this.name : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.dateOfBirth = fieldSetFlags()[1] ? this.dateOfBirth : (java.lang.Long) defaultValue(fields()[1]);
        record.ssn = fieldSetFlags()[2] ? this.ssn : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.salary = fieldSetFlags()[3] ? this.salary : (java.lang.Integer) defaultValue(fields()[3]);
        record.boss = fieldSetFlags()[4] ? this.boss : (java.lang.Object) defaultValue(fields()[4]);
        record.value = fieldSetFlags()[5] ? this.value : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.webpage = fieldSetFlags()[6] ? this.webpage : (org.apache.gora.examples.generated.WebPage) defaultValue(fields()[6]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public Employee.Tombstone getTombstone(){
    return TOMBSTONE;
  }

  public Employee newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends Employee implements org.apache.gora.persistency.Tombstone {
  
    private Tombstone() { }
  
      /**
     * Gets the value of the 'name' field.
         */
    public java.lang.CharSequence getName() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'name' field.
         * @param value the value to set.
     */
    public void setName(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'name' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isNameDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'dateOfBirth' field.
         */
    public java.lang.Long getDateOfBirth() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'dateOfBirth' field.
         * @param value the value to set.
     */
    public void setDateOfBirth(java.lang.Long value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'dateOfBirth' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isDateOfBirthDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'ssn' field.
         */
    public java.lang.CharSequence getSsn() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'ssn' field.
         * @param value the value to set.
     */
    public void setSsn(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'ssn' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isSsnDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'salary' field.
         */
    public java.lang.Integer getSalary() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'salary' field.
         * @param value the value to set.
     */
    public void setSalary(java.lang.Integer value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'salary' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isSalaryDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'boss' field.
         */
    public java.lang.Object getBoss() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'boss' field.
         * @param value the value to set.
     */
    public void setBoss(java.lang.Object value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'boss' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isBossDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'value' field.
         */
    public java.lang.CharSequence getValue() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'value' field.
         * @param value the value to set.
     */
    public void setValue(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'value' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isValueDirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'webpage' field.
         */
    public org.apache.gora.examples.generated.WebPage getWebpage() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'webpage' field.
         * @param value the value to set.
     */
    public void setWebpage(org.apache.gora.examples.generated.WebPage value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'webpage' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isWebpageDirty() {
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

