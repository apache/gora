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
public class WebPage extends PersistentBase {
  
  /**
   * Variable holding the data bean schema.
   */
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"WebPage\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"url\",\"type\":\"string\"},{\"name\":\"content\",\"type\":[\"null\",\"bytes\"]},{\"name\":\"parsedContent\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"outlinks\",\"type\":{\"type\":\"map\",\"values\":\"string\"}},{\"name\":\"metadata\",\"type\":{\"type\":\"record\",\"name\":\"Metadata\",\"fields\":[{\"name\":\"version\",\"type\":\"int\"},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"}}]}}]}");
  
  /**
   * Enum containing all data bean's fields.
   */
  public static enum Field {
    URL(0,"url"),
    CONTENT(1,"content"),
    PARSED_CONTENT(2,"parsedContent"),
    OUTLINKS(3,"outlinks"),
    METADATA(4,"metadata"),
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
  public static final String[] _ALL_FIELDS = {"url","content","parsedContent","outlinks","metadata",};
  static {
    PersistentBase.registerFields(WebPage.class, _ALL_FIELDS);
  }
  private Utf8 url;
  private ByteBuffer content;
  private GenericArray<Utf8> parsedContent;
  private Map<Utf8,Utf8> outlinks;
  private Metadata metadata;
  
  /**
   * Default Constructor
   */
  public WebPage() {
    this(new StateManagerImpl());
  }
  
  /**
   * Constructor
   * @param stateManager for the data bean.
   */
  public WebPage(StateManager stateManager) {
    super(stateManager);
    parsedContent = new ListGenericArray<Utf8>(getSchema().getField("parsedContent").schema());
    outlinks = new StatefulHashMap<Utf8,Utf8>();
  }
  
  /**
   * Returns a new instance by using a state manager.
   * @param stateManager for the data bean.
   * @return WebPage created.
   */
  public WebPage newInstance(StateManager stateManager) {
    return new WebPage(stateManager);
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
    case 0: return url;
    case 1: return content;
    case 2: return parsedContent;
    case 3: return outlinks;
    case 4: return metadata;
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
    case 0:url = (Utf8)_value; break;
    case 1:content = (ByteBuffer)_value; break;
    case 2:parsedContent = (GenericArray<Utf8>)_value; break;
    case 3:outlinks = (Map<Utf8,Utf8>)_value; break;
    case 4:metadata = (Metadata)_value; break;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  
  /**
   * Gets the Url.
   * @return Utf8 representing WebPage Url.
   */
  public Utf8 getUrl() {
    return (Utf8) get(0);
  }
  
  /**
   * Sets the Url.
   * @param value containing WebPage Url.
   */
  public void setUrl(Utf8 value) {
    put(0, value);
  }
  
  /**
   * Gets Content.
   * @return the ByteBuffer value.
   */
  public ByteBuffer getContent() {
    return (ByteBuffer) get(1);
  }
  
  /**
   * Sets the Content.
   * @param the Content value to be set.
   */
  public void setContent(ByteBuffer value) {
    put(1, value);
  }
  
  /**
   * Gets the ParsedContent array.
   * @return GenericArray<Utf8> containing Utf8 elements.
   */
  public GenericArray<Utf8> getParsedContent() {
    return (GenericArray<Utf8>) get(2);
  }
  
  /**
   * Adds a Utf8 element into the array.
   * @param the Utf8 element to be added.
   */
  public void addToParsedContent(Utf8 element) {
    getStateManager().setDirty(this, 2);
    parsedContent.add(element);
  }
  
  /**
   * Gets Outlinks.
   * @return Map containing Outlinks value.
   */
  public Map<Utf8, Utf8> getOutlinks() {
    return (Map<Utf8, Utf8>) get(3);
  }
  
  /**
   * Gets the Outlinks's value using a key.
   * @param key gets a specific Outlinks using a WebPageID.
   * @return Utf8 containing Outlinks value.
   */
  public Utf8 getFromOutlinks(Utf8 key) {
    if (outlinks == null) { return null; }
    return outlinks.get(key);
  }
  
  /**
   * Adds a Outlinks into a WebPage.
   * @param Map containing Outlinks value.
   */
  public void putToOutlinks(Utf8 key, Utf8 value) {
    getStateManager().setDirty(this, 3);
    outlinks.put(key, value);
  }
  
  /**
   * Removes Outlinks from a WebPage.
   * @return key WebPage ID to be removed.
   */
  public Utf8 removeFromOutlinks(Utf8 key) {
    if (outlinks == null) { return null; }
    getStateManager().setDirty(this, 3);
    return outlinks.remove(key);
  }
  
  /**
   * Gets the Metadata.
   * @return Metadata representing WebPage Metadata.
   */
  public Metadata getMetadata() {
    return (Metadata) get(4);
  }
  
  /**
   * Sets the Metadata.
   * @param value containing WebPage Metadata.
   */
  public void setMetadata(Metadata value) {
    put(4, value);
  }
}
