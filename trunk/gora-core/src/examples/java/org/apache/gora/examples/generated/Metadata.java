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
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"Metadata\",\"namespace\":\"org.apache.gora.examples.generated\",\"fields\":[{\"name\":\"version\",\"type\":\"int\"},{\"name\":\"data\",\"type\":{\"type\":\"map\",\"values\":\"string\"}}]}");
  public static enum Field {
    VERSION(0,"version"),
    DATA(1,"data"),
    ;
    private int index;
    private String name;
    Field(int index, String name) {this.index=index;this.name=name;}
    public int getIndex() {return index;}
    public String getName() {return name;}
    public String toString() {return name;}
  };
  public static final String[] _ALL_FIELDS = {"version","data",};
  static {
    PersistentBase.registerFields(Metadata.class, _ALL_FIELDS);
  }
  private int version;
  private Map<Utf8,Utf8> data;
  public Metadata() {
    this(new StateManagerImpl());
  }
  public Metadata(StateManager stateManager) {
    super(stateManager);
    data = new StatefulHashMap<Utf8,Utf8>();
  }
  public Metadata newInstance(StateManager stateManager) {
    return new Metadata(stateManager);
  }
  public Schema getSchema() { return _SCHEMA; }
  public Object get(int _field) {
    switch (_field) {
    case 0: return version;
    case 1: return data;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
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
  public int getVersion() {
    return (Integer) get(0);
  }
  public void setVersion(int value) {
    put(0, value);
  }
  public Map<Utf8, Utf8> getData() {
    return (Map<Utf8, Utf8>) get(1);
  }
  public Utf8 getFromData(Utf8 key) {
    if (data == null) { return null; }
    return data.get(key);
  }
  public void putToData(Utf8 key, Utf8 value) {
    getStateManager().setDirty(this, 1);
    data.put(key, value);
  }
  public Utf8 removeFromData(Utf8 key) {
    if (data == null) { return null; }
    getStateManager().setDirty(this, 1);
    return data.remove(key);
  }
}
