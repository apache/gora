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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.util.Utf8;
import org.apache.avro.ipc.AvroRemoteException;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.FixedSize;
import org.apache.avro.specific.SpecificExceptionBase;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificFixed;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.persistency.impl.StateManagerImpl;
import org.apache.gora.persistency.StatefulHashMap;
import org.apache.gora.persistency.ListGenericArray;
@SuppressWarnings("all")
public class Employee extends PersistentBase {
  
  /**
   * Variable holding the data bean schema.
   */
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"Employee\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"dateOfBirth\",\"type\":\"long\"},{\"name\":\"ssn\",\"type\":\"string\"},{\"name\":\"salary\",\"type\":\"int\"},{\"name\":\"boss\",\"type\":[\"null\",\"Employee\",\"string\"]},{\"name\":\"webpage\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"WebPage\",\"fields\":[{\"name\":\"url\",\"type\":\"string\"},{\"name\":\"content\",\"type\":[\"null\",\"bytes\"]},{\"name\":\"parsedContent\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"outlinks\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"metadata\",\"type\":{\"type\":\"record\",\"name\":\"Metadata\",\"fields\":[{\"name\":\"version\",\"type\":\"int\"},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"}}]}}]}]}]}");
  
  /**
   * Enum containing all data bean's fields.
   */
  public static enum Field {
    NAME(0,"name"),
    DATE_OF_BIRTH(1,"dateOfBirth"),
    SSN(2,"ssn"),
    SALARY(3,"salary"),
    BOSS(4,"boss"),
    WEBPAGE(5,"webpage"),
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
    
    /**
     * Contains all field's names.
     */
  public static final String[] _ALL_FIELDS = {"name","dateOfBirth","ssn","salary","boss","webpage",};
  static {
    PersistentBase.registerFields(Employee.class, _ALL_FIELDS);
  }
  private Utf8 name;
  private long dateOfBirth;
  private Utf8 ssn;
  private int salary;
  private Object boss;
  private WebPage webpage;
  
  /**
   * Default Constructor
   */
  public Employee() {
    this(new StateManagerImpl());
  }
  
  /**
   * Constructor
   * @param stateManager for the data bean.
   */
  public Employee(StateManager stateManager) {
    super(stateManager);
  }
  
  /**
   * Returns a new instance by using a state manager.
   * @param stateManager for the data bean.
   * @return Employee created.
   */
  public Employee newInstance(StateManager stateManager) {
    return new Employee(stateManager);
  }
  
  /**
   * Returns the schema of the data bean.
   * @return Schema for the data bean.
   */
  public Schema getSchema() { return _SCHEMA; }
  
  /**
   * Gets a specific field.
   * @param field index of a field for the data bean.
   * @return Object representing a data bean's field.
   */
  public Object get(int _field) {
    switch (_field) {
    case 0: return name;
    case 1: return dateOfBirth;
    case 2: return ssn;
    case 3: return salary;
    case 4: return boss;
    case 5: return webpage;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  
  /**
   * Puts a value for a specific field.
   * @param field index of a field for the data bean.
   * @param value value of a field for the data bean.
   */
  @SuppressWarnings(value="unchecked")
  public void put(int _field, Object _value) {
    if(isFieldEqual(_field, _value)) return;
    getStateManager().setDirty(this, _field);
    switch (_field) {
    case 0:name = (Utf8)_value; break;
    case 1:dateOfBirth = (Long)_value; break;
    case 2:ssn = (Utf8)_value; break;
    case 3:salary = (Integer)_value; break;
    case 4:boss = (Object)_value; break;
    case 5:webpage = (WebPage)_value; break;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  
  /**
   * Gets the Name.
   * @return Utf8 representing Employee Name.
   */
  public Utf8 getName() {
    return (Utf8) get(0);
  }
  
  /**
   * Sets the Name.
   * @param value containing Employee Name.
   */
  public void setName(Utf8 value) {
    put(0, value);
  }
  
  /**
   * Gets the DateOfBirth.
   * @return long representing Employee DateOfBirth.
   */
  public long getDateOfBirth() {
    return (Long) get(1);
  }
  
  /**
   * Sets the DateOfBirth.
   * @param value containing Employee DateOfBirth.
   */
  public void setDateOfBirth(long value) {
    put(1, value);
  }
  
  /**
   * Gets the Ssn.
   * @return Utf8 representing Employee Ssn.
   */
  public Utf8 getSsn() {
    return (Utf8) get(2);
  }
  
  /**
   * Sets the Ssn.
   * @param value containing Employee Ssn.
   */
  public void setSsn(Utf8 value) {
    put(2, value);
  }
  
  /**
   * Gets the Salary.
   * @return int representing Employee Salary.
   */
  public int getSalary() {
    return (Integer) get(3);
  }
  
  /**
   * Sets the Salary.
   * @param value containing Employee Salary.
   */
  public void setSalary(int value) {
    put(3, value);
  }
  
  /**
   * Gets Boss.
   * @return the Object value.
   */
  public Object getBoss() {
    return (Object) get(4);
  }
  
  /**
   * Sets the Boss.
   * @param the Boss value to be set.
   */
  public void setBoss(Employee value) {
    put(4, value);
  }
  
  /**
   * Sets the Boss.
   * @param the Boss value to be set.
   */
  public void setBoss(Utf8 value) {
    put(4, value);
  }
  
  /**
   * Gets Webpage.
   * @return the WebPage value.
   */
  public WebPage getWebpage() {
    return (WebPage) get(5);
  }
  
  /**
   * Sets the Webpage.
   * @param the Webpage value to be set.
   */
  public void setWebpage(WebPage value) {
    put(5, value);
  }
}
