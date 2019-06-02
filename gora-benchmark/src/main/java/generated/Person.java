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
package generated;  

public class Person extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Person\",\"namespace\":\"generated\",\"fields\":[{\"name\":\"userId\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"firstName\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"lastName\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"birthDate\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"company\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"city\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"address\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"streetname\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"streetsuffix\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"personalnumber\",\"type\":[\"null\",\"int\"],\"default\":null}],\"default\":null}");
  private static final long serialVersionUID = 6477119430345314284L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    USER_ID(0, "userId"),
    FIRST_NAME(1, "firstName"),
    LAST_NAME(2, "lastName"),
    BIRTH_DATE(3, "birthDate"),
    COMPANY(4, "company"),
    CITY(5, "city"),
    ADDRESS(6, "address"),
    EMAIL(7, "email"),
    STREETNAME(8, "streetname"),
    STREETSUFFIX(9, "streetsuffix"),
    PERSONALNUMBER(10, "personalnumber"),
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
  "userId",
  "firstName",
  "lastName",
  "birthDate",
  "company",
  "city",
  "address",
  "email",
  "streetname",
  "streetsuffix",
  "personalnumber",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return Person._ALL_FIELDS.length;
  }

  private java.lang.CharSequence userId;
  private java.lang.CharSequence firstName;
  private java.lang.CharSequence lastName;
  private java.lang.Integer birthDate;
  private java.lang.CharSequence company;
  private java.lang.CharSequence city;
  private java.lang.CharSequence address;
  private java.lang.CharSequence email;
  private java.lang.CharSequence streetname;
  private java.lang.CharSequence streetsuffix;
  private java.lang.Integer personalnumber;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return userId;
    case 1: return firstName;
    case 2: return lastName;
    case 3: return birthDate;
    case 4: return company;
    case 5: return city;
    case 6: return address;
    case 7: return email;
    case 8: return streetname;
    case 9: return streetsuffix;
    case 10: return personalnumber;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: userId = (java.lang.CharSequence)(value); break;
    case 1: firstName = (java.lang.CharSequence)(value); break;
    case 2: lastName = (java.lang.CharSequence)(value); break;
    case 3: birthDate = (java.lang.Integer)(value); break;
    case 4: company = (java.lang.CharSequence)(value); break;
    case 5: city = (java.lang.CharSequence)(value); break;
    case 6: address = (java.lang.CharSequence)(value); break;
    case 7: email = (java.lang.CharSequence)(value); break;
    case 8: streetname = (java.lang.CharSequence)(value); break;
    case 9: streetsuffix = (java.lang.CharSequence)(value); break;
    case 10: personalnumber = (java.lang.Integer)(value); break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'userId' field.
   */
  public java.lang.CharSequence getUserId() {
    return userId;
  }

  /**
   * Sets the value of the 'userId' field.
   * @param value the value to set.
   */
  public void setUserId(java.lang.CharSequence value) {
    this.userId = value;
    setDirty(0);
  }
  
  /**
   * Checks the dirty status of the 'userId' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isUserIdDirty() {
    return isDirty(0);
  }

  /**
   * Gets the value of the 'firstName' field.
   */
  public java.lang.CharSequence getFirstName() {
    return firstName;
  }

  /**
   * Sets the value of the 'firstName' field.
   * @param value the value to set.
   */
  public void setFirstName(java.lang.CharSequence value) {
    this.firstName = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'firstName' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isFirstNameDirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'lastName' field.
   */
  public java.lang.CharSequence getLastName() {
    return lastName;
  }

  /**
   * Sets the value of the 'lastName' field.
   * @param value the value to set.
   */
  public void setLastName(java.lang.CharSequence value) {
    this.lastName = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'lastName' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isLastNameDirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'birthDate' field.
   */
  public java.lang.Integer getBirthDate() {
    return birthDate;
  }

  /**
   * Sets the value of the 'birthDate' field.
   * @param value the value to set.
   */
  public void setBirthDate(java.lang.Integer value) {
    this.birthDate = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'birthDate' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isBirthDateDirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'company' field.
   */
  public java.lang.CharSequence getCompany() {
    return company;
  }

  /**
   * Sets the value of the 'company' field.
   * @param value the value to set.
   */
  public void setCompany(java.lang.CharSequence value) {
    this.company = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'company' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCompanyDirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'city' field.
   */
  public java.lang.CharSequence getCity() {
    return city;
  }

  /**
   * Sets the value of the 'city' field.
   * @param value the value to set.
   */
  public void setCity(java.lang.CharSequence value) {
    this.city = value;
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'city' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isCityDirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'address' field.
   */
  public java.lang.CharSequence getAddress() {
    return address;
  }

  /**
   * Sets the value of the 'address' field.
   * @param value the value to set.
   */
  public void setAddress(java.lang.CharSequence value) {
    this.address = value;
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'address' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isAddressDirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'email' field.
   */
  public java.lang.CharSequence getEmail() {
    return email;
  }

  /**
   * Sets the value of the 'email' field.
   * @param value the value to set.
   */
  public void setEmail(java.lang.CharSequence value) {
    this.email = value;
    setDirty(7);
  }
  
  /**
   * Checks the dirty status of the 'email' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isEmailDirty() {
    return isDirty(7);
  }

  /**
   * Gets the value of the 'streetname' field.
   */
  public java.lang.CharSequence getStreetname() {
    return streetname;
  }

  /**
   * Sets the value of the 'streetname' field.
   * @param value the value to set.
   */
  public void setStreetname(java.lang.CharSequence value) {
    this.streetname = value;
    setDirty(8);
  }
  
  /**
   * Checks the dirty status of the 'streetname' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isStreetnameDirty() {
    return isDirty(8);
  }

  /**
   * Gets the value of the 'streetsuffix' field.
   */
  public java.lang.CharSequence getStreetsuffix() {
    return streetsuffix;
  }

  /**
   * Sets the value of the 'streetsuffix' field.
   * @param value the value to set.
   */
  public void setStreetsuffix(java.lang.CharSequence value) {
    this.streetsuffix = value;
    setDirty(9);
  }
  
  /**
   * Checks the dirty status of the 'streetsuffix' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isStreetsuffixDirty() {
    return isDirty(9);
  }

  /**
   * Gets the value of the 'personalnumber' field.
   */
  public java.lang.Integer getPersonalnumber() {
    return personalnumber;
  }

  /**
   * Sets the value of the 'personalnumber' field.
   * @param value the value to set.
   */
  public void setPersonalnumber(java.lang.Integer value) {
    this.personalnumber = value;
    setDirty(10);
  }
  
  /**
   * Checks the dirty status of the 'personalnumber' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isPersonalnumberDirty() {
    return isDirty(10);
  }

  /** Creates a new Person RecordBuilder */
  public static generated.Person.Builder newBuilder() {
    return new generated.Person.Builder();
  }
  
  /** Creates a new Person RecordBuilder by copying an existing Builder */
  public static generated.Person.Builder newBuilder(generated.Person.Builder other) {
    return new generated.Person.Builder(other);
  }
  
  /** Creates a new Person RecordBuilder by copying an existing Person instance */
  public static generated.Person.Builder newBuilder(generated.Person other) {
    return new generated.Person.Builder(other);
  }
  
  @Override
  public generated.Person clone() {
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
   * RecordBuilder for Person instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Person>
    implements org.apache.avro.data.RecordBuilder<Person> {

    private java.lang.CharSequence userId;
    private java.lang.CharSequence firstName;
    private java.lang.CharSequence lastName;
    private java.lang.Integer birthDate;
    private java.lang.CharSequence company;
    private java.lang.CharSequence city;
    private java.lang.CharSequence address;
    private java.lang.CharSequence email;
    private java.lang.CharSequence streetname;
    private java.lang.CharSequence streetsuffix;
    private java.lang.Integer personalnumber;

    /** Creates a new Builder */
    private Builder() {
      super(generated.Person.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(generated.Person.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing Person instance */
    private Builder(generated.Person other) {
            super(generated.Person.SCHEMA$);
      if (isValidValue(fields()[0], other.userId)) {
        this.userId = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.userId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.firstName)) {
        this.firstName = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.firstName);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.lastName)) {
        this.lastName = (java.lang.CharSequence) data().deepCopy(fields()[2].schema(), other.lastName);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.birthDate)) {
        this.birthDate = (java.lang.Integer) data().deepCopy(fields()[3].schema(), other.birthDate);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.company)) {
        this.company = (java.lang.CharSequence) data().deepCopy(fields()[4].schema(), other.company);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.city)) {
        this.city = (java.lang.CharSequence) data().deepCopy(fields()[5].schema(), other.city);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.address)) {
        this.address = (java.lang.CharSequence) data().deepCopy(fields()[6].schema(), other.address);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.email)) {
        this.email = (java.lang.CharSequence) data().deepCopy(fields()[7].schema(), other.email);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.streetname)) {
        this.streetname = (java.lang.CharSequence) data().deepCopy(fields()[8].schema(), other.streetname);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.streetsuffix)) {
        this.streetsuffix = (java.lang.CharSequence) data().deepCopy(fields()[9].schema(), other.streetsuffix);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.personalnumber)) {
        this.personalnumber = (java.lang.Integer) data().deepCopy(fields()[10].schema(), other.personalnumber);
        fieldSetFlags()[10] = true;
      }
    }

    /** Gets the value of the 'userId' field */
    public java.lang.CharSequence getUserId() {
      return userId;
    }
    
    /** Sets the value of the 'userId' field */
    public generated.Person.Builder setUserId(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.userId = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'userId' field has been set */
    public boolean hasUserId() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'userId' field */
    public generated.Person.Builder clearUserId() {
      userId = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'firstName' field */
    public java.lang.CharSequence getFirstName() {
      return firstName;
    }
    
    /** Sets the value of the 'firstName' field */
    public generated.Person.Builder setFirstName(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.firstName = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'firstName' field has been set */
    public boolean hasFirstName() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'firstName' field */
    public generated.Person.Builder clearFirstName() {
      firstName = null;
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'lastName' field */
    public java.lang.CharSequence getLastName() {
      return lastName;
    }
    
    /** Sets the value of the 'lastName' field */
    public generated.Person.Builder setLastName(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.lastName = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'lastName' field has been set */
    public boolean hasLastName() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'lastName' field */
    public generated.Person.Builder clearLastName() {
      lastName = null;
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'birthDate' field */
    public java.lang.Integer getBirthDate() {
      return birthDate;
    }
    
    /** Sets the value of the 'birthDate' field */
    public generated.Person.Builder setBirthDate(java.lang.Integer value) {
      validate(fields()[3], value);
      this.birthDate = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'birthDate' field has been set */
    public boolean hasBirthDate() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'birthDate' field */
    public generated.Person.Builder clearBirthDate() {
      birthDate = null;
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'company' field */
    public java.lang.CharSequence getCompany() {
      return company;
    }
    
    /** Sets the value of the 'company' field */
    public generated.Person.Builder setCompany(java.lang.CharSequence value) {
      validate(fields()[4], value);
      this.company = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'company' field has been set */
    public boolean hasCompany() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'company' field */
    public generated.Person.Builder clearCompany() {
      company = null;
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'city' field */
    public java.lang.CharSequence getCity() {
      return city;
    }
    
    /** Sets the value of the 'city' field */
    public generated.Person.Builder setCity(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.city = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'city' field has been set */
    public boolean hasCity() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'city' field */
    public generated.Person.Builder clearCity() {
      city = null;
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'address' field */
    public java.lang.CharSequence getAddress() {
      return address;
    }
    
    /** Sets the value of the 'address' field */
    public generated.Person.Builder setAddress(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.address = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'address' field has been set */
    public boolean hasAddress() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'address' field */
    public generated.Person.Builder clearAddress() {
      address = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    /** Gets the value of the 'email' field */
    public java.lang.CharSequence getEmail() {
      return email;
    }
    
    /** Sets the value of the 'email' field */
    public generated.Person.Builder setEmail(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.email = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'email' field has been set */
    public boolean hasEmail() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'email' field */
    public generated.Person.Builder clearEmail() {
      email = null;
      fieldSetFlags()[7] = false;
      return this;
    }
    
    /** Gets the value of the 'streetname' field */
    public java.lang.CharSequence getStreetname() {
      return streetname;
    }
    
    /** Sets the value of the 'streetname' field */
    public generated.Person.Builder setStreetname(java.lang.CharSequence value) {
      validate(fields()[8], value);
      this.streetname = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'streetname' field has been set */
    public boolean hasStreetname() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'streetname' field */
    public generated.Person.Builder clearStreetname() {
      streetname = null;
      fieldSetFlags()[8] = false;
      return this;
    }
    
    /** Gets the value of the 'streetsuffix' field */
    public java.lang.CharSequence getStreetsuffix() {
      return streetsuffix;
    }
    
    /** Sets the value of the 'streetsuffix' field */
    public generated.Person.Builder setStreetsuffix(java.lang.CharSequence value) {
      validate(fields()[9], value);
      this.streetsuffix = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'streetsuffix' field has been set */
    public boolean hasStreetsuffix() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'streetsuffix' field */
    public generated.Person.Builder clearStreetsuffix() {
      streetsuffix = null;
      fieldSetFlags()[9] = false;
      return this;
    }
    
    /** Gets the value of the 'personalnumber' field */
    public java.lang.Integer getPersonalnumber() {
      return personalnumber;
    }
    
    /** Sets the value of the 'personalnumber' field */
    public generated.Person.Builder setPersonalnumber(java.lang.Integer value) {
      validate(fields()[10], value);
      this.personalnumber = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'personalnumber' field has been set */
    public boolean hasPersonalnumber() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'personalnumber' field */
    public generated.Person.Builder clearPersonalnumber() {
      personalnumber = null;
      fieldSetFlags()[10] = false;
      return this;
    }
    
    @Override
    public Person build() {
      try {
        Person record = new Person();
        record.userId = fieldSetFlags()[0] ? this.userId : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.firstName = fieldSetFlags()[1] ? this.firstName : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.lastName = fieldSetFlags()[2] ? this.lastName : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.birthDate = fieldSetFlags()[3] ? this.birthDate : (java.lang.Integer) defaultValue(fields()[3]);
        record.company = fieldSetFlags()[4] ? this.company : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.city = fieldSetFlags()[5] ? this.city : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.address = fieldSetFlags()[6] ? this.address : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.email = fieldSetFlags()[7] ? this.email : (java.lang.CharSequence) defaultValue(fields()[7]);
        record.streetname = fieldSetFlags()[8] ? this.streetname : (java.lang.CharSequence) defaultValue(fields()[8]);
        record.streetsuffix = fieldSetFlags()[9] ? this.streetsuffix : (java.lang.CharSequence) defaultValue(fields()[9]);
        record.personalnumber = fieldSetFlags()[10] ? this.personalnumber : (java.lang.Integer) defaultValue(fields()[10]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public Person.Tombstone getTombstone(){
  	return TOMBSTONE;
  }

  public Person newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends Person implements org.apache.gora.persistency.Tombstone {
  
      private Tombstone() { }
  
	  		  /**
	   * Gets the value of the 'userId' field.
		   */
	  public java.lang.CharSequence getUserId() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'userId' field.
		   * @param value the value to set.
	   */
	  public void setUserId(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'userId' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isUserIdDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'firstName' field.
		   */
	  public java.lang.CharSequence getFirstName() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'firstName' field.
		   * @param value the value to set.
	   */
	  public void setFirstName(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'firstName' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isFirstNameDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'lastName' field.
		   */
	  public java.lang.CharSequence getLastName() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'lastName' field.
		   * @param value the value to set.
	   */
	  public void setLastName(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'lastName' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isLastNameDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'birthDate' field.
		   */
	  public java.lang.Integer getBirthDate() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'birthDate' field.
		   * @param value the value to set.
	   */
	  public void setBirthDate(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'birthDate' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isBirthDateDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'company' field.
		   */
	  public java.lang.CharSequence getCompany() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'company' field.
		   * @param value the value to set.
	   */
	  public void setCompany(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'company' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCompanyDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'city' field.
		   */
	  public java.lang.CharSequence getCity() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'city' field.
		   * @param value the value to set.
	   */
	  public void setCity(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'city' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isCityDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'address' field.
		   */
	  public java.lang.CharSequence getAddress() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'address' field.
		   * @param value the value to set.
	   */
	  public void setAddress(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'address' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isAddressDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'email' field.
		   */
	  public java.lang.CharSequence getEmail() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'email' field.
		   * @param value the value to set.
	   */
	  public void setEmail(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'email' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isEmailDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'streetname' field.
		   */
	  public java.lang.CharSequence getStreetname() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'streetname' field.
		   * @param value the value to set.
	   */
	  public void setStreetname(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'streetname' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isStreetnameDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'streetsuffix' field.
		   */
	  public java.lang.CharSequence getStreetsuffix() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'streetsuffix' field.
		   * @param value the value to set.
	   */
	  public void setStreetsuffix(java.lang.CharSequence value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'streetsuffix' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isStreetsuffixDirty() {
	    throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
	  }
	
				  /**
	   * Gets the value of the 'personalnumber' field.
		   */
	  public java.lang.Integer getPersonalnumber() {
	    throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
	  }
	
	  /**
	   * Sets the value of the 'personalnumber' field.
		   * @param value the value to set.
	   */
	  public void setPersonalnumber(java.lang.Integer value) {
	    throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
	  }
	  
	  /**
	   * Checks the dirty status of the 'personalnumber' field. A field is dirty if it represents a change that has not yet been written to the database.
		   * @param value the value to set.
	   */
	  public boolean isPersonalnumberDirty() {
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

