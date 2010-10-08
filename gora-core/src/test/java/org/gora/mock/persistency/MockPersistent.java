
package org.gora.mock.persistency;

import org.apache.avro.Schema;
import org.gora.persistency.Persistent;
import org.gora.persistency.StateManager;
import org.gora.persistency.impl.PersistentBase;

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
    return Schema.parse("{\"type\":\"record\",\"name\":\"MockPersistent\",\"namespace\":\"org.gora.mock.persistency\",\"fields\":[{\"name\":\"foo\",\"type\":\"int\"},{\"name\":\"baz\",\"type\":\"int\"}]}");
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
