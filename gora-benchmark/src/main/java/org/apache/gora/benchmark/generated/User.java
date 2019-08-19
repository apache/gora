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
package org.apache.gora.benchmark.generated;  

public class User extends org.apache.gora.persistency.impl.PersistentBase implements org.apache.avro.specific.SpecificRecord, org.apache.gora.persistency.Persistent {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"org.apache.gora.benchmark.generated\",\"fields\":[{\"name\":\"userId\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field0\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field1\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field2\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field3\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field4\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field5\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field6\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field7\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field8\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field9\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field10\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field11\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field12\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field13\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field14\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field15\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field16\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field17\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field18\",\"type\":\"string\",\"default\":\"null\"},{\"name\":\"field19\",\"type\":\"string\",\"default\":\"null\"}]}");
  private static final long serialVersionUID = -7912162892761759126L;
  /** Enum containing all data bean's fields. */
  public static enum Field {
    USER_ID(0, "userId"),
    FIELD0(1, "field0"),
    FIELD1(2, "field1"),
    FIELD2(3, "field2"),
    FIELD3(4, "field3"),
    FIELD4(5, "field4"),
    FIELD5(6, "field5"),
    FIELD6(7, "field6"),
    FIELD7(8, "field7"),
    FIELD8(9, "field8"),
    FIELD9(10, "field9"),
    FIELD10(11, "field10"),
    FIELD11(12, "field11"),
    FIELD12(13, "field12"),
    FIELD13(14, "field13"),
    FIELD14(15, "field14"),
    FIELD15(16, "field15"),
    FIELD16(17, "field16"),
    FIELD17(18, "field17"),
    FIELD18(19, "field18"),
    FIELD19(20, "field19"),
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
  "field0",
  "field1",
  "field2",
  "field3",
  "field4",
  "field5",
  "field6",
  "field7",
  "field8",
  "field9",
  "field10",
  "field11",
  "field12",
  "field13",
  "field14",
  "field15",
  "field16",
  "field17",
  "field18",
  "field19",
  };

  /**
   * Gets the total field count.
   * @return int field count
   */
  public int getFieldsCount() {
    return User._ALL_FIELDS.length;
  }

  private java.lang.CharSequence userId;
  private java.lang.CharSequence field0;
  private java.lang.CharSequence field1;
  private java.lang.CharSequence field2;
  private java.lang.CharSequence field3;
  private java.lang.CharSequence field4;
  private java.lang.CharSequence field5;
  private java.lang.CharSequence field6;
  private java.lang.CharSequence field7;
  private java.lang.CharSequence field8;
  private java.lang.CharSequence field9;
  private java.lang.CharSequence field10;
  private java.lang.CharSequence field11;
  private java.lang.CharSequence field12;
  private java.lang.CharSequence field13;
  private java.lang.CharSequence field14;
  private java.lang.CharSequence field15;
  private java.lang.CharSequence field16;
  private java.lang.CharSequence field17;
  private java.lang.CharSequence field18;
  private java.lang.CharSequence field19;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return userId;
    case 1: return field0;
    case 2: return field1;
    case 3: return field2;
    case 4: return field3;
    case 5: return field4;
    case 6: return field5;
    case 7: return field6;
    case 8: return field7;
    case 9: return field8;
    case 10: return field9;
    case 11: return field10;
    case 12: return field11;
    case 13: return field12;
    case 14: return field13;
    case 15: return field14;
    case 16: return field15;
    case 17: return field16;
    case 18: return field17;
    case 19: return field18;
    case 20: return field19;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value) {
    switch (field$) {
    case 0: userId = (java.lang.CharSequence)(value); break;
    case 1: field0 = (java.lang.CharSequence)(value); break;
    case 2: field1 = (java.lang.CharSequence)(value); break;
    case 3: field2 = (java.lang.CharSequence)(value); break;
    case 4: field3 = (java.lang.CharSequence)(value); break;
    case 5: field4 = (java.lang.CharSequence)(value); break;
    case 6: field5 = (java.lang.CharSequence)(value); break;
    case 7: field6 = (java.lang.CharSequence)(value); break;
    case 8: field7 = (java.lang.CharSequence)(value); break;
    case 9: field8 = (java.lang.CharSequence)(value); break;
    case 10: field9 = (java.lang.CharSequence)(value); break;
    case 11: field10 = (java.lang.CharSequence)(value); break;
    case 12: field11 = (java.lang.CharSequence)(value); break;
    case 13: field12 = (java.lang.CharSequence)(value); break;
    case 14: field13 = (java.lang.CharSequence)(value); break;
    case 15: field14 = (java.lang.CharSequence)(value); break;
    case 16: field15 = (java.lang.CharSequence)(value); break;
    case 17: field16 = (java.lang.CharSequence)(value); break;
    case 18: field17 = (java.lang.CharSequence)(value); break;
    case 19: field18 = (java.lang.CharSequence)(value); break;
    case 20: field19 = (java.lang.CharSequence)(value); break;
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
   * Gets the value of the 'field0' field.
   */
  public java.lang.CharSequence getField0() {
    return field0;
  }

  /**
   * Sets the value of the 'field0' field.
   * @param value the value to set.
   */
  public void setField0(java.lang.CharSequence value) {
    this.field0 = value;
    setDirty(1);
  }
  
  /**
   * Checks the dirty status of the 'field0' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField0Dirty() {
    return isDirty(1);
  }

  /**
   * Gets the value of the 'field1' field.
   */
  public java.lang.CharSequence getField1() {
    return field1;
  }

  /**
   * Sets the value of the 'field1' field.
   * @param value the value to set.
   */
  public void setField1(java.lang.CharSequence value) {
    this.field1 = value;
    setDirty(2);
  }
  
  /**
   * Checks the dirty status of the 'field1' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField1Dirty() {
    return isDirty(2);
  }

  /**
   * Gets the value of the 'field2' field.
   */
  public java.lang.CharSequence getField2() {
    return field2;
  }

  /**
   * Sets the value of the 'field2' field.
   * @param value the value to set.
   */
  public void setField2(java.lang.CharSequence value) {
    this.field2 = value;
    setDirty(3);
  }
  
  /**
   * Checks the dirty status of the 'field2' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField2Dirty() {
    return isDirty(3);
  }

  /**
   * Gets the value of the 'field3' field.
   */
  public java.lang.CharSequence getField3() {
    return field3;
  }

  /**
   * Sets the value of the 'field3' field.
   * @param value the value to set.
   */
  public void setField3(java.lang.CharSequence value) {
    this.field3 = value;
    setDirty(4);
  }
  
  /**
   * Checks the dirty status of the 'field3' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField3Dirty() {
    return isDirty(4);
  }

  /**
   * Gets the value of the 'field4' field.
   */
  public java.lang.CharSequence getField4() {
    return field4;
  }

  /**
   * Sets the value of the 'field4' field.
   * @param value the value to set.
   */
  public void setField4(java.lang.CharSequence value) {
    this.field4 = value;
    setDirty(5);
  }
  
  /**
   * Checks the dirty status of the 'field4' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField4Dirty() {
    return isDirty(5);
  }

  /**
   * Gets the value of the 'field5' field.
   */
  public java.lang.CharSequence getField5() {
    return field5;
  }

  /**
   * Sets the value of the 'field5' field.
   * @param value the value to set.
   */
  public void setField5(java.lang.CharSequence value) {
    this.field5 = value;
    setDirty(6);
  }
  
  /**
   * Checks the dirty status of the 'field5' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField5Dirty() {
    return isDirty(6);
  }

  /**
   * Gets the value of the 'field6' field.
   */
  public java.lang.CharSequence getField6() {
    return field6;
  }

  /**
   * Sets the value of the 'field6' field.
   * @param value the value to set.
   */
  public void setField6(java.lang.CharSequence value) {
    this.field6 = value;
    setDirty(7);
  }
  
  /**
   * Checks the dirty status of the 'field6' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField6Dirty() {
    return isDirty(7);
  }

  /**
   * Gets the value of the 'field7' field.
   */
  public java.lang.CharSequence getField7() {
    return field7;
  }

  /**
   * Sets the value of the 'field7' field.
   * @param value the value to set.
   */
  public void setField7(java.lang.CharSequence value) {
    this.field7 = value;
    setDirty(8);
  }
  
  /**
   * Checks the dirty status of the 'field7' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField7Dirty() {
    return isDirty(8);
  }

  /**
   * Gets the value of the 'field8' field.
   */
  public java.lang.CharSequence getField8() {
    return field8;
  }

  /**
   * Sets the value of the 'field8' field.
   * @param value the value to set.
   */
  public void setField8(java.lang.CharSequence value) {
    this.field8 = value;
    setDirty(9);
  }
  
  /**
   * Checks the dirty status of the 'field8' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField8Dirty() {
    return isDirty(9);
  }

  /**
   * Gets the value of the 'field9' field.
   */
  public java.lang.CharSequence getField9() {
    return field9;
  }

  /**
   * Sets the value of the 'field9' field.
   * @param value the value to set.
   */
  public void setField9(java.lang.CharSequence value) {
    this.field9 = value;
    setDirty(10);
  }
  
  /**
   * Checks the dirty status of the 'field9' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField9Dirty() {
    return isDirty(10);
  }

  /**
   * Gets the value of the 'field10' field.
   */
  public java.lang.CharSequence getField10() {
    return field10;
  }

  /**
   * Sets the value of the 'field10' field.
   * @param value the value to set.
   */
  public void setField10(java.lang.CharSequence value) {
    this.field10 = value;
    setDirty(11);
  }
  
  /**
   * Checks the dirty status of the 'field10' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField10Dirty() {
    return isDirty(11);
  }

  /**
   * Gets the value of the 'field11' field.
   */
  public java.lang.CharSequence getField11() {
    return field11;
  }

  /**
   * Sets the value of the 'field11' field.
   * @param value the value to set.
   */
  public void setField11(java.lang.CharSequence value) {
    this.field11 = value;
    setDirty(12);
  }
  
  /**
   * Checks the dirty status of the 'field11' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField11Dirty() {
    return isDirty(12);
  }

  /**
   * Gets the value of the 'field12' field.
   */
  public java.lang.CharSequence getField12() {
    return field12;
  }

  /**
   * Sets the value of the 'field12' field.
   * @param value the value to set.
   */
  public void setField12(java.lang.CharSequence value) {
    this.field12 = value;
    setDirty(13);
  }
  
  /**
   * Checks the dirty status of the 'field12' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField12Dirty() {
    return isDirty(13);
  }

  /**
   * Gets the value of the 'field13' field.
   */
  public java.lang.CharSequence getField13() {
    return field13;
  }

  /**
   * Sets the value of the 'field13' field.
   * @param value the value to set.
   */
  public void setField13(java.lang.CharSequence value) {
    this.field13 = value;
    setDirty(14);
  }
  
  /**
   * Checks the dirty status of the 'field13' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField13Dirty() {
    return isDirty(14);
  }

  /**
   * Gets the value of the 'field14' field.
   */
  public java.lang.CharSequence getField14() {
    return field14;
  }

  /**
   * Sets the value of the 'field14' field.
   * @param value the value to set.
   */
  public void setField14(java.lang.CharSequence value) {
    this.field14 = value;
    setDirty(15);
  }
  
  /**
   * Checks the dirty status of the 'field14' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField14Dirty() {
    return isDirty(15);
  }

  /**
   * Gets the value of the 'field15' field.
   */
  public java.lang.CharSequence getField15() {
    return field15;
  }

  /**
   * Sets the value of the 'field15' field.
   * @param value the value to set.
   */
  public void setField15(java.lang.CharSequence value) {
    this.field15 = value;
    setDirty(16);
  }
  
  /**
   * Checks the dirty status of the 'field15' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField15Dirty() {
    return isDirty(16);
  }

  /**
   * Gets the value of the 'field16' field.
   */
  public java.lang.CharSequence getField16() {
    return field16;
  }

  /**
   * Sets the value of the 'field16' field.
   * @param value the value to set.
   */
  public void setField16(java.lang.CharSequence value) {
    this.field16 = value;
    setDirty(17);
  }
  
  /**
   * Checks the dirty status of the 'field16' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField16Dirty() {
    return isDirty(17);
  }

  /**
   * Gets the value of the 'field17' field.
   */
  public java.lang.CharSequence getField17() {
    return field17;
  }

  /**
   * Sets the value of the 'field17' field.
   * @param value the value to set.
   */
  public void setField17(java.lang.CharSequence value) {
    this.field17 = value;
    setDirty(18);
  }
  
  /**
   * Checks the dirty status of the 'field17' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField17Dirty() {
    return isDirty(18);
  }

  /**
   * Gets the value of the 'field18' field.
   */
  public java.lang.CharSequence getField18() {
    return field18;
  }

  /**
   * Sets the value of the 'field18' field.
   * @param value the value to set.
   */
  public void setField18(java.lang.CharSequence value) {
    this.field18 = value;
    setDirty(19);
  }
  
  /**
   * Checks the dirty status of the 'field18' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField18Dirty() {
    return isDirty(19);
  }

  /**
   * Gets the value of the 'field19' field.
   */
  public java.lang.CharSequence getField19() {
    return field19;
  }

  /**
   * Sets the value of the 'field19' field.
   * @param value the value to set.
   */
  public void setField19(java.lang.CharSequence value) {
    this.field19 = value;
    setDirty(20);
  }
  
  /**
   * Checks the dirty status of the 'field19' field. A field is dirty if it represents a change that has not yet been written to the database.
   * @param value the value to set.
   */
  public boolean isField19Dirty() {
    return isDirty(20);
  }

  /** Creates a new User RecordBuilder */
  public static org.apache.gora.benchmark.generated.User.Builder newBuilder() {
    return new org.apache.gora.benchmark.generated.User.Builder();
  }
  
  /** Creates a new User RecordBuilder by copying an existing Builder */
  public static org.apache.gora.benchmark.generated.User.Builder newBuilder(org.apache.gora.benchmark.generated.User.Builder other) {
    return new org.apache.gora.benchmark.generated.User.Builder(other);
  }
  
  /** Creates a new User RecordBuilder by copying an existing User instance */
  public static org.apache.gora.benchmark.generated.User.Builder newBuilder(org.apache.gora.benchmark.generated.User other) {
    return new org.apache.gora.benchmark.generated.User.Builder(other);
  }
  
  @Override
  public org.apache.gora.benchmark.generated.User clone() {
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
   * RecordBuilder for User instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<User>
    implements org.apache.avro.data.RecordBuilder<User> {

    private java.lang.CharSequence userId;
    private java.lang.CharSequence field0;
    private java.lang.CharSequence field1;
    private java.lang.CharSequence field2;
    private java.lang.CharSequence field3;
    private java.lang.CharSequence field4;
    private java.lang.CharSequence field5;
    private java.lang.CharSequence field6;
    private java.lang.CharSequence field7;
    private java.lang.CharSequence field8;
    private java.lang.CharSequence field9;
    private java.lang.CharSequence field10;
    private java.lang.CharSequence field11;
    private java.lang.CharSequence field12;
    private java.lang.CharSequence field13;
    private java.lang.CharSequence field14;
    private java.lang.CharSequence field15;
    private java.lang.CharSequence field16;
    private java.lang.CharSequence field17;
    private java.lang.CharSequence field18;
    private java.lang.CharSequence field19;

    /** Creates a new Builder */
    private Builder() {
      super(org.apache.gora.benchmark.generated.User.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.apache.gora.benchmark.generated.User.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing User instance */
    private Builder(org.apache.gora.benchmark.generated.User other) {
            super(org.apache.gora.benchmark.generated.User.SCHEMA$);
      if (isValidValue(fields()[0], other.userId)) {
        this.userId = (java.lang.CharSequence) data().deepCopy(fields()[0].schema(), other.userId);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.field0)) {
        this.field0 = (java.lang.CharSequence) data().deepCopy(fields()[1].schema(), other.field0);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.field1)) {
        this.field1 = (java.lang.CharSequence) data().deepCopy(fields()[2].schema(), other.field1);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.field2)) {
        this.field2 = (java.lang.CharSequence) data().deepCopy(fields()[3].schema(), other.field2);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.field3)) {
        this.field3 = (java.lang.CharSequence) data().deepCopy(fields()[4].schema(), other.field3);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.field4)) {
        this.field4 = (java.lang.CharSequence) data().deepCopy(fields()[5].schema(), other.field4);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.field5)) {
        this.field5 = (java.lang.CharSequence) data().deepCopy(fields()[6].schema(), other.field5);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.field6)) {
        this.field6 = (java.lang.CharSequence) data().deepCopy(fields()[7].schema(), other.field6);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.field7)) {
        this.field7 = (java.lang.CharSequence) data().deepCopy(fields()[8].schema(), other.field7);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.field8)) {
        this.field8 = (java.lang.CharSequence) data().deepCopy(fields()[9].schema(), other.field8);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.field9)) {
        this.field9 = (java.lang.CharSequence) data().deepCopy(fields()[10].schema(), other.field9);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.field10)) {
        this.field10 = (java.lang.CharSequence) data().deepCopy(fields()[11].schema(), other.field10);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.field11)) {
        this.field11 = (java.lang.CharSequence) data().deepCopy(fields()[12].schema(), other.field11);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.field12)) {
        this.field12 = (java.lang.CharSequence) data().deepCopy(fields()[13].schema(), other.field12);
        fieldSetFlags()[13] = true;
      }
      if (isValidValue(fields()[14], other.field13)) {
        this.field13 = (java.lang.CharSequence) data().deepCopy(fields()[14].schema(), other.field13);
        fieldSetFlags()[14] = true;
      }
      if (isValidValue(fields()[15], other.field14)) {
        this.field14 = (java.lang.CharSequence) data().deepCopy(fields()[15].schema(), other.field14);
        fieldSetFlags()[15] = true;
      }
      if (isValidValue(fields()[16], other.field15)) {
        this.field15 = (java.lang.CharSequence) data().deepCopy(fields()[16].schema(), other.field15);
        fieldSetFlags()[16] = true;
      }
      if (isValidValue(fields()[17], other.field16)) {
        this.field16 = (java.lang.CharSequence) data().deepCopy(fields()[17].schema(), other.field16);
        fieldSetFlags()[17] = true;
      }
      if (isValidValue(fields()[18], other.field17)) {
        this.field17 = (java.lang.CharSequence) data().deepCopy(fields()[18].schema(), other.field17);
        fieldSetFlags()[18] = true;
      }
      if (isValidValue(fields()[19], other.field18)) {
        this.field18 = (java.lang.CharSequence) data().deepCopy(fields()[19].schema(), other.field18);
        fieldSetFlags()[19] = true;
      }
      if (isValidValue(fields()[20], other.field19)) {
        this.field19 = (java.lang.CharSequence) data().deepCopy(fields()[20].schema(), other.field19);
        fieldSetFlags()[20] = true;
      }
    }

    /** Gets the value of the 'userId' field */
    public java.lang.CharSequence getUserId() {
      return userId;
    }
    
    /** Sets the value of the 'userId' field */
    public org.apache.gora.benchmark.generated.User.Builder setUserId(java.lang.CharSequence value) {
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
    public org.apache.gora.benchmark.generated.User.Builder clearUserId() {
      userId = null;
      fieldSetFlags()[0] = false;
      return this;
    }
    
    /** Gets the value of the 'field0' field */
    public java.lang.CharSequence getField0() {
      return field0;
    }
    
    /** Sets the value of the 'field0' field */
    public org.apache.gora.benchmark.generated.User.Builder setField0(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.field0 = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'field0' field has been set */
    public boolean hasField0() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'field0' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField0() {
      field0 = null;
      fieldSetFlags()[1] = false;
      return this;
    }
    
    /** Gets the value of the 'field1' field */
    public java.lang.CharSequence getField1() {
      return field1;
    }
    
    /** Sets the value of the 'field1' field */
    public org.apache.gora.benchmark.generated.User.Builder setField1(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.field1 = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'field1' field has been set */
    public boolean hasField1() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'field1' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField1() {
      field1 = null;
      fieldSetFlags()[2] = false;
      return this;
    }
    
    /** Gets the value of the 'field2' field */
    public java.lang.CharSequence getField2() {
      return field2;
    }
    
    /** Sets the value of the 'field2' field */
    public org.apache.gora.benchmark.generated.User.Builder setField2(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.field2 = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'field2' field has been set */
    public boolean hasField2() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'field2' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField2() {
      field2 = null;
      fieldSetFlags()[3] = false;
      return this;
    }
    
    /** Gets the value of the 'field3' field */
    public java.lang.CharSequence getField3() {
      return field3;
    }
    
    /** Sets the value of the 'field3' field */
    public org.apache.gora.benchmark.generated.User.Builder setField3(java.lang.CharSequence value) {
      validate(fields()[4], value);
      this.field3 = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'field3' field has been set */
    public boolean hasField3() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'field3' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField3() {
      field3 = null;
      fieldSetFlags()[4] = false;
      return this;
    }
    
    /** Gets the value of the 'field4' field */
    public java.lang.CharSequence getField4() {
      return field4;
    }
    
    /** Sets the value of the 'field4' field */
    public org.apache.gora.benchmark.generated.User.Builder setField4(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.field4 = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'field4' field has been set */
    public boolean hasField4() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'field4' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField4() {
      field4 = null;
      fieldSetFlags()[5] = false;
      return this;
    }
    
    /** Gets the value of the 'field5' field */
    public java.lang.CharSequence getField5() {
      return field5;
    }
    
    /** Sets the value of the 'field5' field */
    public org.apache.gora.benchmark.generated.User.Builder setField5(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.field5 = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'field5' field has been set */
    public boolean hasField5() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'field5' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField5() {
      field5 = null;
      fieldSetFlags()[6] = false;
      return this;
    }
    
    /** Gets the value of the 'field6' field */
    public java.lang.CharSequence getField6() {
      return field6;
    }
    
    /** Sets the value of the 'field6' field */
    public org.apache.gora.benchmark.generated.User.Builder setField6(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.field6 = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'field6' field has been set */
    public boolean hasField6() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'field6' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField6() {
      field6 = null;
      fieldSetFlags()[7] = false;
      return this;
    }
    
    /** Gets the value of the 'field7' field */
    public java.lang.CharSequence getField7() {
      return field7;
    }
    
    /** Sets the value of the 'field7' field */
    public org.apache.gora.benchmark.generated.User.Builder setField7(java.lang.CharSequence value) {
      validate(fields()[8], value);
      this.field7 = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'field7' field has been set */
    public boolean hasField7() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'field7' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField7() {
      field7 = null;
      fieldSetFlags()[8] = false;
      return this;
    }
    
    /** Gets the value of the 'field8' field */
    public java.lang.CharSequence getField8() {
      return field8;
    }
    
    /** Sets the value of the 'field8' field */
    public org.apache.gora.benchmark.generated.User.Builder setField8(java.lang.CharSequence value) {
      validate(fields()[9], value);
      this.field8 = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'field8' field has been set */
    public boolean hasField8() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'field8' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField8() {
      field8 = null;
      fieldSetFlags()[9] = false;
      return this;
    }
    
    /** Gets the value of the 'field9' field */
    public java.lang.CharSequence getField9() {
      return field9;
    }
    
    /** Sets the value of the 'field9' field */
    public org.apache.gora.benchmark.generated.User.Builder setField9(java.lang.CharSequence value) {
      validate(fields()[10], value);
      this.field9 = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'field9' field has been set */
    public boolean hasField9() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'field9' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField9() {
      field9 = null;
      fieldSetFlags()[10] = false;
      return this;
    }
    
    /** Gets the value of the 'field10' field */
    public java.lang.CharSequence getField10() {
      return field10;
    }
    
    /** Sets the value of the 'field10' field */
    public org.apache.gora.benchmark.generated.User.Builder setField10(java.lang.CharSequence value) {
      validate(fields()[11], value);
      this.field10 = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'field10' field has been set */
    public boolean hasField10() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'field10' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField10() {
      field10 = null;
      fieldSetFlags()[11] = false;
      return this;
    }
    
    /** Gets the value of the 'field11' field */
    public java.lang.CharSequence getField11() {
      return field11;
    }
    
    /** Sets the value of the 'field11' field */
    public org.apache.gora.benchmark.generated.User.Builder setField11(java.lang.CharSequence value) {
      validate(fields()[12], value);
      this.field11 = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'field11' field has been set */
    public boolean hasField11() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'field11' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField11() {
      field11 = null;
      fieldSetFlags()[12] = false;
      return this;
    }
    
    /** Gets the value of the 'field12' field */
    public java.lang.CharSequence getField12() {
      return field12;
    }
    
    /** Sets the value of the 'field12' field */
    public org.apache.gora.benchmark.generated.User.Builder setField12(java.lang.CharSequence value) {
      validate(fields()[13], value);
      this.field12 = value;
      fieldSetFlags()[13] = true;
      return this; 
    }
    
    /** Checks whether the 'field12' field has been set */
    public boolean hasField12() {
      return fieldSetFlags()[13];
    }
    
    /** Clears the value of the 'field12' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField12() {
      field12 = null;
      fieldSetFlags()[13] = false;
      return this;
    }
    
    /** Gets the value of the 'field13' field */
    public java.lang.CharSequence getField13() {
      return field13;
    }
    
    /** Sets the value of the 'field13' field */
    public org.apache.gora.benchmark.generated.User.Builder setField13(java.lang.CharSequence value) {
      validate(fields()[14], value);
      this.field13 = value;
      fieldSetFlags()[14] = true;
      return this; 
    }
    
    /** Checks whether the 'field13' field has been set */
    public boolean hasField13() {
      return fieldSetFlags()[14];
    }
    
    /** Clears the value of the 'field13' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField13() {
      field13 = null;
      fieldSetFlags()[14] = false;
      return this;
    }
    
    /** Gets the value of the 'field14' field */
    public java.lang.CharSequence getField14() {
      return field14;
    }
    
    /** Sets the value of the 'field14' field */
    public org.apache.gora.benchmark.generated.User.Builder setField14(java.lang.CharSequence value) {
      validate(fields()[15], value);
      this.field14 = value;
      fieldSetFlags()[15] = true;
      return this; 
    }
    
    /** Checks whether the 'field14' field has been set */
    public boolean hasField14() {
      return fieldSetFlags()[15];
    }
    
    /** Clears the value of the 'field14' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField14() {
      field14 = null;
      fieldSetFlags()[15] = false;
      return this;
    }
    
    /** Gets the value of the 'field15' field */
    public java.lang.CharSequence getField15() {
      return field15;
    }
    
    /** Sets the value of the 'field15' field */
    public org.apache.gora.benchmark.generated.User.Builder setField15(java.lang.CharSequence value) {
      validate(fields()[16], value);
      this.field15 = value;
      fieldSetFlags()[16] = true;
      return this; 
    }
    
    /** Checks whether the 'field15' field has been set */
    public boolean hasField15() {
      return fieldSetFlags()[16];
    }
    
    /** Clears the value of the 'field15' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField15() {
      field15 = null;
      fieldSetFlags()[16] = false;
      return this;
    }
    
    /** Gets the value of the 'field16' field */
    public java.lang.CharSequence getField16() {
      return field16;
    }
    
    /** Sets the value of the 'field16' field */
    public org.apache.gora.benchmark.generated.User.Builder setField16(java.lang.CharSequence value) {
      validate(fields()[17], value);
      this.field16 = value;
      fieldSetFlags()[17] = true;
      return this; 
    }
    
    /** Checks whether the 'field16' field has been set */
    public boolean hasField16() {
      return fieldSetFlags()[17];
    }
    
    /** Clears the value of the 'field16' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField16() {
      field16 = null;
      fieldSetFlags()[17] = false;
      return this;
    }
    
    /** Gets the value of the 'field17' field */
    public java.lang.CharSequence getField17() {
      return field17;
    }
    
    /** Sets the value of the 'field17' field */
    public org.apache.gora.benchmark.generated.User.Builder setField17(java.lang.CharSequence value) {
      validate(fields()[18], value);
      this.field17 = value;
      fieldSetFlags()[18] = true;
      return this; 
    }
    
    /** Checks whether the 'field17' field has been set */
    public boolean hasField17() {
      return fieldSetFlags()[18];
    }
    
    /** Clears the value of the 'field17' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField17() {
      field17 = null;
      fieldSetFlags()[18] = false;
      return this;
    }
    
    /** Gets the value of the 'field18' field */
    public java.lang.CharSequence getField18() {
      return field18;
    }
    
    /** Sets the value of the 'field18' field */
    public org.apache.gora.benchmark.generated.User.Builder setField18(java.lang.CharSequence value) {
      validate(fields()[19], value);
      this.field18 = value;
      fieldSetFlags()[19] = true;
      return this; 
    }
    
    /** Checks whether the 'field18' field has been set */
    public boolean hasField18() {
      return fieldSetFlags()[19];
    }
    
    /** Clears the value of the 'field18' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField18() {
      field18 = null;
      fieldSetFlags()[19] = false;
      return this;
    }
    
    /** Gets the value of the 'field19' field */
    public java.lang.CharSequence getField19() {
      return field19;
    }
    
    /** Sets the value of the 'field19' field */
    public org.apache.gora.benchmark.generated.User.Builder setField19(java.lang.CharSequence value) {
      validate(fields()[20], value);
      this.field19 = value;
      fieldSetFlags()[20] = true;
      return this; 
    }
    
    /** Checks whether the 'field19' field has been set */
    public boolean hasField19() {
      return fieldSetFlags()[20];
    }
    
    /** Clears the value of the 'field19' field */
    public org.apache.gora.benchmark.generated.User.Builder clearField19() {
      field19 = null;
      fieldSetFlags()[20] = false;
      return this;
    }
    
    @Override
    public User build() {
      try {
        User record = new User();
        record.userId = fieldSetFlags()[0] ? this.userId : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.field0 = fieldSetFlags()[1] ? this.field0 : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.field1 = fieldSetFlags()[2] ? this.field1 : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.field2 = fieldSetFlags()[3] ? this.field2 : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.field3 = fieldSetFlags()[4] ? this.field3 : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.field4 = fieldSetFlags()[5] ? this.field4 : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.field5 = fieldSetFlags()[6] ? this.field5 : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.field6 = fieldSetFlags()[7] ? this.field6 : (java.lang.CharSequence) defaultValue(fields()[7]);
        record.field7 = fieldSetFlags()[8] ? this.field7 : (java.lang.CharSequence) defaultValue(fields()[8]);
        record.field8 = fieldSetFlags()[9] ? this.field8 : (java.lang.CharSequence) defaultValue(fields()[9]);
        record.field9 = fieldSetFlags()[10] ? this.field9 : (java.lang.CharSequence) defaultValue(fields()[10]);
        record.field10 = fieldSetFlags()[11] ? this.field10 : (java.lang.CharSequence) defaultValue(fields()[11]);
        record.field11 = fieldSetFlags()[12] ? this.field11 : (java.lang.CharSequence) defaultValue(fields()[12]);
        record.field12 = fieldSetFlags()[13] ? this.field12 : (java.lang.CharSequence) defaultValue(fields()[13]);
        record.field13 = fieldSetFlags()[14] ? this.field13 : (java.lang.CharSequence) defaultValue(fields()[14]);
        record.field14 = fieldSetFlags()[15] ? this.field14 : (java.lang.CharSequence) defaultValue(fields()[15]);
        record.field15 = fieldSetFlags()[16] ? this.field15 : (java.lang.CharSequence) defaultValue(fields()[16]);
        record.field16 = fieldSetFlags()[17] ? this.field16 : (java.lang.CharSequence) defaultValue(fields()[17]);
        record.field17 = fieldSetFlags()[18] ? this.field17 : (java.lang.CharSequence) defaultValue(fields()[18]);
        record.field18 = fieldSetFlags()[19] ? this.field18 : (java.lang.CharSequence) defaultValue(fields()[19]);
        record.field19 = fieldSetFlags()[20] ? this.field19 : (java.lang.CharSequence) defaultValue(fields()[20]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
  
  public User.Tombstone getTombstone(){
    return TOMBSTONE;
  }

  public User newInstance(){
    return newBuilder().build();
  }

  private static final Tombstone TOMBSTONE = new Tombstone();
  
  public static final class Tombstone extends User implements org.apache.gora.persistency.Tombstone {
  
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
     * Gets the value of the 'field0' field.
         */
    public java.lang.CharSequence getField0() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field0' field.
         * @param value the value to set.
     */
    public void setField0(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field0' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField0Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field1' field.
         */
    public java.lang.CharSequence getField1() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field1' field.
         * @param value the value to set.
     */
    public void setField1(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field1' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField1Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field2' field.
         */
    public java.lang.CharSequence getField2() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field2' field.
         * @param value the value to set.
     */
    public void setField2(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field2' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField2Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field3' field.
         */
    public java.lang.CharSequence getField3() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field3' field.
         * @param value the value to set.
     */
    public void setField3(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field3' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField3Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field4' field.
         */
    public java.lang.CharSequence getField4() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field4' field.
         * @param value the value to set.
     */
    public void setField4(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field4' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField4Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field5' field.
         */
    public java.lang.CharSequence getField5() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field5' field.
         * @param value the value to set.
     */
    public void setField5(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field5' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField5Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field6' field.
         */
    public java.lang.CharSequence getField6() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field6' field.
         * @param value the value to set.
     */
    public void setField6(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field6' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField6Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field7' field.
         */
    public java.lang.CharSequence getField7() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field7' field.
         * @param value the value to set.
     */
    public void setField7(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field7' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField7Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field8' field.
         */
    public java.lang.CharSequence getField8() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field8' field.
         * @param value the value to set.
     */
    public void setField8(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field8' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField8Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field9' field.
         */
    public java.lang.CharSequence getField9() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field9' field.
         * @param value the value to set.
     */
    public void setField9(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field9' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField9Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field10' field.
         */
    public java.lang.CharSequence getField10() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field10' field.
         * @param value the value to set.
     */
    public void setField10(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field10' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField10Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field11' field.
         */
    public java.lang.CharSequence getField11() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field11' field.
         * @param value the value to set.
     */
    public void setField11(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field11' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField11Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field12' field.
         */
    public java.lang.CharSequence getField12() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field12' field.
         * @param value the value to set.
     */
    public void setField12(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field12' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField12Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field13' field.
         */
    public java.lang.CharSequence getField13() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field13' field.
         * @param value the value to set.
     */
    public void setField13(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field13' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField13Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field14' field.
         */
    public java.lang.CharSequence getField14() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field14' field.
         * @param value the value to set.
     */
    public void setField14(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field14' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField14Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field15' field.
         */
    public java.lang.CharSequence getField15() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field15' field.
         * @param value the value to set.
     */
    public void setField15(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field15' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField15Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field16' field.
         */
    public java.lang.CharSequence getField16() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field16' field.
         * @param value the value to set.
     */
    public void setField16(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field16' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField16Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field17' field.
         */
    public java.lang.CharSequence getField17() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field17' field.
         * @param value the value to set.
     */
    public void setField17(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field17' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField17Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field18' field.
         */
    public java.lang.CharSequence getField18() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field18' field.
         * @param value the value to set.
     */
    public void setField18(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field18' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField18Dirty() {
      throw new java.lang.UnsupportedOperationException("IsDirty is not supported on tombstones");
    }

        /**
     * Gets the value of the 'field19' field.
         */
    public java.lang.CharSequence getField19() {
      throw new java.lang.UnsupportedOperationException("Get is not supported on tombstones");
    }

    /**
     * Sets the value of the 'field19' field.
         * @param value the value to set.
     */
    public void setField19(java.lang.CharSequence value) {
      throw new java.lang.UnsupportedOperationException("Set is not supported on tombstones");
    }
  
    /**
     * Checks the dirty status of the 'field19' field. A field is dirty if it represents a change that has not yet been written to the database.
         * @param value the value to set.
     */
    public boolean isField19Dirty() {
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

