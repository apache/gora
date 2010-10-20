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

package org.apache.gora.avro;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.Utf8;
import org.apache.gora.mapreduce.FakeResolvingDecoder;
import org.apache.gora.persistency.ListGenericArray;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StatefulHashMap;
import org.apache.gora.persistency.StatefulMap;
import org.apache.gora.persistency.impl.StateManagerImpl;
import org.apache.gora.util.IOUtils;

/**
 * PersistentDatumReader reads, fields' dirty and readable information.
 */
public class PersistentDatumReader<T extends Persistent>
  extends SpecificDatumReader<T> {

  private Schema rootSchema;
  private T cachedPersistent; // for creating objects

  private WeakHashMap<Decoder, ResolvingDecoder> decoderCache
    = new WeakHashMap<Decoder, ResolvingDecoder>();

  private boolean readDirtyBits = true;

  public PersistentDatumReader() {
  }

  public PersistentDatumReader(Schema schema, boolean readDirtyBits) {
    this.readDirtyBits = readDirtyBits;
    setSchema(schema);
  }

  @Override
  public void setSchema(Schema actual) {
    this.rootSchema = actual;
    super.setSchema(actual);
  }

  @SuppressWarnings("unchecked")
  public T newPersistent() {
    if(cachedPersistent == null) {
      cachedPersistent = (T)super.newRecord(null, rootSchema);
      return cachedPersistent; //we can return the cached object
    }
    return (T)cachedPersistent.newInstance(new StateManagerImpl());
  }

  @Override
  protected Object newRecord(Object old, Schema schema) {
    if(old != null) {
      return old;
    }

    if(schema.equals(rootSchema)) {
      return newPersistent();
    } else {
      return super.newRecord(old, schema);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T read(T reuse, Decoder in) throws IOException {
    return (T) read(reuse, rootSchema, in);
  }

  public Object read(Object reuse, Schema schema, Decoder decoder)
    throws IOException {
    return super.read(reuse, schema, getResolvingDecoder(decoder));
  }

  protected ResolvingDecoder getResolvingDecoder(Decoder decoder)
  throws IOException {
    ResolvingDecoder resolvingDecoder = decoderCache.get(decoder);
    if(resolvingDecoder == null) {
      resolvingDecoder = new FakeResolvingDecoder(rootSchema, decoder);
      decoderCache.put(decoder, resolvingDecoder);
    }
    return resolvingDecoder;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object readRecord(Object old, Schema expected, ResolvingDecoder in)
      throws IOException {

    Object record = newRecord(old, expected);

    //check if top-level
    if(expected.equals(rootSchema) && readDirtyBits) {
      T persistent = (T)record;
      persistent.clear();

      boolean[] dirtyFields = IOUtils.readBoolArray(in);
      boolean[] readableFields = IOUtils.readBoolArray(in);

      //read fields
      int i = 0;

      for (Field f : expected.getFields()) {
        if(readableFields[f.pos()]) {
          int pos = f.pos();
          String name = f.name();
          Object oldDatum = (old != null) ? getField(record, name, pos) : null;
          setField(record, name, pos, read(oldDatum, f.schema(), in));
        }
      }

      // Now set changed bits
      for (i = 0; i < dirtyFields.length; i++) {
        if (dirtyFields[i]) {
          persistent.setDirty(i);
        }
      }
      return record;
    } else {
      //since ResolvingDecoder.readFieldOrder is final, we cannot override it
      //so this is a copy of super.readReacord, with the readFieldOrder change

      for (Field f : expected.getFields()) {
        int pos = f.pos();
        String name = f.name();
        Object oldDatum = (old != null) ? getField(record, name, pos) : null;
        setField(record, name, pos, read(oldDatum, f.schema(), in));
      }

      return record;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object readMap(Object old, Schema expected, ResolvingDecoder in)
      throws IOException {

    StatefulMap<Utf8, ?> map = (StatefulMap<Utf8, ?>) newMap(old, 0);
    map.clearStates();
    if (readDirtyBits) {
      int size = in.readInt();
      for (int j = 0; j < size; j++) {
        Utf8 key = in.readString(null);
        State state = State.values()[in.readInt()];
        map.putState(key, state);
      }
    }
    return super.readMap(map, expected, in);
  }

  @Override
  @SuppressWarnings({ "rawtypes" })
  protected Object newMap(Object old, int size) {
    if (old instanceof StatefulHashMap) {
      ((Map) old).clear();
      ((StatefulHashMap)old).clearStates();
      return old;
    }
    return new StatefulHashMap<Object, Object>();
  }

  /** Called to create new array instances.  Subclasses may override to use a
   * different array implementation.  By default, this returns a 
   * GenericData.Array instance.*/
  @Override
  @SuppressWarnings("rawtypes")
  protected Object newArray(Object old, int size, Schema schema) {
    if (old instanceof ListGenericArray) {
      ((GenericArray) old).clear();
      return old;
    } else return new ListGenericArray(size, schema);
  }
  
  public Persistent clone(Persistent persistent, Schema schema) {
    Persistent cloned = persistent.newInstance(new StateManagerImpl());
    List<Field> fields = schema.getFields();
    for(Field field: fields) {
      int pos = field.pos();
      switch(field.schema().getType()) {
        case MAP    :
        case ARRAY  :
        case RECORD : 
        case STRING : cloned.put(pos, cloneObject(
            field.schema(), persistent.get(pos), cloned.get(pos))); break;
        case NULL   : break;
        default     : cloned.put(pos, persistent.get(pos)); break;
      }
    }
    
    return cloned;
  }
  
  @SuppressWarnings("unchecked")
  protected Object cloneObject(Schema schema, Object toClone, Object cloned) {
    if(toClone == null) {
      return null;
    }
    
    switch(schema.getType()) {
      case MAP    :
        Map<Utf8, Object> map = (Map<Utf8, Object>)newMap(cloned, 0);
        for(Map.Entry<Utf8, Object> entry: ((Map<Utf8, Object>)toClone).entrySet()) {
          map.put((Utf8)createString(entry.getKey().toString())
              , cloneObject(schema.getValueType(), entry.getValue(), null));
        }
        return map;
      case ARRAY  :
        GenericArray<Object> array = (GenericArray<Object>) 
          newArray(cloned, (int)((GenericArray<?>)toClone).size(), schema);
        for(Object element: (GenericArray<Object>)toClone) {
          array.add(cloneObject(schema.getElementType(), element, null));
        }
        return array;
      case RECORD : return clone((Persistent)toClone, schema);
      case STRING : return createString(toClone.toString());
      default     : return toClone; //shallow copy is enough
    }
  }
}
