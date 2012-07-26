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

package org.apache.gora.mock.persistency;

import org.apache.avro.Schema;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.impl.PersistentBase;

public class MockPersistent extends PersistentBase {

  public static final String FOO = "foo";
  public static final String BAZ = "baz";
  
  public static final String[] _ALL_FIELDS = {FOO, BAZ};
  
  private int foo;
  private int baz;
  
  public MockPersistent() {
  }
  
  public MockPersistent(StateManager stateManager) {
    super(stateManager);
  }
  
  @Override
  public Object get(int field) {
    switch(field) {
      case 0: return foo;
      case 1: return baz;
    }
    return null;
  }

  @Override
  public void put(int field, Object value) {
    switch(field) {
      case 0:  foo = (Integer)value;
      case 1:  baz = (Integer)value;
    }
  }

  @Override
  public Schema getSchema() {
    return Schema.parse("{\"type\":\"record\",\"name\":\"MockPersistent\",\"namespace\":\"org.apache.gora.mock.persistency\",\"fields\":[{\"name\":\"foo\",\"type\":\"int\"},{\"name\":\"baz\",\"type\":\"int\"}]}");
  }
  
  public void setFoo(int foo) {
    this.foo = foo;
  }
  
  public void setBaz(int baz) {
    this.baz = baz;
  }
  
  public int getFoo() {
    return foo;
  }
  
  public int getBaz() {
    return baz;
  }

  @Override
  public String getField(int index) {
    return null;
  }

  @Override
  public int getFieldIndex(String field) {
    return 0;
  }

  @Override
  public String[] getFields() {
    return null;
  }

  @Override
  public Persistent newInstance(StateManager stateManager) {
    return new MockPersistent(stateManager);
  }
}
