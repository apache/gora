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
import java.util.Map.Entry;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.Encoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.StatefulMap;
import org.apache.gora.util.IOUtils;

/**
 * PersistentDatumWriter writes, fields' dirty and readable information.
 */
public class PersistentDatumWriter<T extends Persistent>
  extends SpecificDatumWriter<T> {

  private T persistent = null;

  private boolean writeDirtyBits = true;

  public PersistentDatumWriter() {
  }

  public PersistentDatumWriter(Schema schema, boolean writeDirtyBits) {
    setSchema(schema);
    this.writeDirtyBits = writeDirtyBits;
  }

  public void setPersistent(T persistent) {
    this.persistent = persistent;
  }

  @Override
  /**exposing this function so that fields can be written individually*/
  public void write(Schema schema, Object datum, Encoder out)
      throws IOException {
    super.write(schema, datum, out);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void writeRecord(Schema schema, Object datum, Encoder out)
      throws IOException {

    if(persistent == null) {
      persistent = (T) datum;
    }

    if (!writeDirtyBits) {
      super.writeRecord(schema, datum, out);
      return;
    }

    //check if top level schema
    if(schema.equals(persistent.getSchema())) {
      //write readable fields and dirty fields info
      boolean[] dirtyFields = new boolean[schema.getFields().size()];
      boolean[] readableFields = new boolean[schema.getFields().size()];
      StateManager manager = persistent.getStateManager();

      int i=0;
      for (@SuppressWarnings("unused") Field field : schema.getFields()) {
        dirtyFields[i] = manager.isDirty(persistent, i);
        readableFields[i] = manager.isReadable(persistent, i);
        i++;
      }

      IOUtils.writeBoolArray(out, dirtyFields);
      IOUtils.writeBoolArray(out, readableFields);

      for (Field field : schema.getFields()) {
        if(readableFields[field.pos()]) {
          write(field.schema(), getField(datum, field.name(), field.pos()), out);
        }
      }

    } else {
      super.writeRecord(schema, datum, out);
    }
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void writeMap(Schema schema, Object datum, Encoder out)
      throws IOException {

    if (writeDirtyBits) {
      // write extra state information for maps
      StatefulMap<Utf8, ?> map = (StatefulMap) datum;
      out.writeInt(map.states().size());
      for (Entry<Utf8, State> e2 : map.states().entrySet()) {
        out.writeString(e2.getKey());
        out.writeInt(e2.getValue().ordinal());
      }
    }
    super.writeMap(schema, datum, out);
  }

}
