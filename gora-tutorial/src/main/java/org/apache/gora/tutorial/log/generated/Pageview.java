/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.tutorial.log.generated;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.persistency.impl.StateManagerImpl;

@SuppressWarnings("all")
public class Pageview extends PersistentBase {
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"Pageview\",\"namespace\":\"org.apache.gora.tutorial.log.generated\",\"fields\":[{\"name\":\"url\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"ip\",\"type\":\"string\"},{\"name\":\"httpMethod\",\"type\":\"string\"},{\"name\":\"httpStatusCode\",\"type\":\"int\"},{\"name\":\"responseSize\",\"type\":\"int\"},{\"name\":\"referrer\",\"type\":\"string\"},{\"name\":\"userAgent\",\"type\":\"string\"}]}");
  public static enum Field {
    URL(0,"url"),
    TIMESTAMP(1,"timestamp"),
    IP(2,"ip"),
    HTTP_METHOD(3,"httpMethod"),
    HTTP_STATUS_CODE(4,"httpStatusCode"),
    RESPONSE_SIZE(5,"responseSize"),
    REFERRER(6,"referrer"),
    USER_AGENT(7,"userAgent"),
    ;
    private int index;
    private String name;
    Field(int index, String name) {this.index=index;this.name=name;}
    public int getIndex() {return index;}
    public String getName() {return name;}
    public String toString() {return name;}
  };
  public static final String[] _ALL_FIELDS = {"url","timestamp","ip","httpMethod","httpStatusCode","responseSize","referrer","userAgent",};
  static {
    PersistentBase.registerFields(Pageview.class, _ALL_FIELDS);
  }
  private Utf8 url;
  private long timestamp;
  private Utf8 ip;
  private Utf8 httpMethod;
  private int httpStatusCode;
  private int responseSize;
  private Utf8 referrer;
  private Utf8 userAgent;
  public Pageview() {
    this(new StateManagerImpl());
  }
  public Pageview(StateManager stateManager) {
    super(stateManager);
  }
  public Pageview newInstance(StateManager stateManager) {
    return new Pageview(stateManager);
  }
  public Schema getSchema() { return _SCHEMA; }
  public Object get(int _field) {
    switch (_field) {
    case 0: return url;
    case 1: return timestamp;
    case 2: return ip;
    case 3: return httpMethod;
    case 4: return httpStatusCode;
    case 5: return responseSize;
    case 6: return referrer;
    case 7: return userAgent;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  @SuppressWarnings(value="unchecked")
  public void put(int _field, Object _value) {
    if(isFieldEqual(_field, _value)) return;
    getStateManager().setDirty(this, _field);
    switch (_field) {
    case 0:url = (Utf8)_value; break;
    case 1:timestamp = (Long)_value; break;
    case 2:ip = (Utf8)_value; break;
    case 3:httpMethod = (Utf8)_value; break;
    case 4:httpStatusCode = (Integer)_value; break;
    case 5:responseSize = (Integer)_value; break;
    case 6:referrer = (Utf8)_value; break;
    case 7:userAgent = (Utf8)_value; break;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  public Utf8 getUrl() {
    return (Utf8) get(0);
  }
  public void setUrl(Utf8 value) {
    put(0, value);
  }
  public long getTimestamp() {
    return (Long) get(1);
  }
  public void setTimestamp(long value) {
    put(1, value);
  }
  public Utf8 getIp() {
    return (Utf8) get(2);
  }
  public void setIp(Utf8 value) {
    put(2, value);
  }
  public Utf8 getHttpMethod() {
    return (Utf8) get(3);
  }
  public void setHttpMethod(Utf8 value) {
    put(3, value);
  }
  public int getHttpStatusCode() {
    return (Integer) get(4);
  }
  public void setHttpStatusCode(int value) {
    put(4, value);
  }
  public int getResponseSize() {
    return (Integer) get(5);
  }
  public void setResponseSize(int value) {
    put(5, value);
  }
  public Utf8 getReferrer() {
    return (Utf8) get(6);
  }
  public void setReferrer(Utf8 value) {
    put(6, value);
  }
  public Utf8 getUserAgent() {
    return (Utf8) get(7);
  }
  public void setUserAgent(Utf8 value) {
    put(7, value);
  }
}
