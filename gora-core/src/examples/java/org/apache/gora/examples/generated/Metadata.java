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
public class Metadata extends PersistentBase {
  
  /**
   * Variable holding the data bean schema.
   */
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"Metadata\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"version\",\"type\":\"int\"},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"}}]}");
  
  /**
   * Enum containing all data bean's fields.
   */
  public static enum Field {
    VERSION(0,"version"),
    DATA(1,"data"),
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
  public static final String[] _ALL_FIELDS = {"version","data",};
  static {
    PersistentBase.registerFields(Metadata.class, _ALL_FIELDS);
  }
  private int version;
  private Map<Utf8,Utf8> data;
  
  /**
   * Default Constructor
   */
  public Metadata() {
    this(new StateManagerImpl());
  }
  
  /**
   * Constructor
   * @param stateManager for the data bean.
   */
  public Metadata(StateManager stateManager) {
    super(stateManager);
    data = new StatefulHashMap<Utf8,Utf8>();
  }
  
  /**
   * Returns a new instance by using a state manager.
   * @param stateManager for the data bean.
   * @return Metadata created.
   */
  public Metadata newInstance(StateManager stateManager) {
    return new Metadata(stateManager);
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
    case 0: return version;
    case 1: return data;
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
    case 0:version = (Integer)_value; break;
    case 1:data = (Map<Utf8,Utf8>)_value; break;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  
  /**
   * Gets the Version.
   * @return int representing Metadata Version.
   */
  public int getVersion() {
    return (Integer) get(0);
  }
  
  /**
   * Sets the Version.
   * @param value containing Metadata Version.
   */
  public void setVersion(int value) {
    put(0, value);
  }
  
  /**
   * Gets Data.
   * @return Map containing Data value.
   */
  public Map<Utf8, Utf8> getData() {
    return (Map<Utf8, Utf8>) get(1);
  }
  
  /**
   * Gets the Data's value using a key.
   * @param key gets a specific Data using a MetadataID.
   * @return Utf8 containing Data value.
   */
  public Utf8 getFromData(Utf8 key) {
    if (data == null) { return null; }
    return data.get(key);
  }
  
  /**
   * Adds a Data into a Metadata.
   * @param Map containing Data value.
   */
  public void putToData(Utf8 key, Utf8 value) {
    getStateManager().setDirty(this, 1);
    data.put(key, value);
  }
  
  /**
   * Removes Data from a Metadata.
   * @return key Metadata ID to be removed.
   */
  public Utf8 removeFromData(Utf8 key) {
    if (data == null) { return null; }
    getStateManager().setDirty(this, 1);
    return data.remove(key);
  }
}
