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
import org.apache.gora.persistency.Tombstone;
import org.apache.gora.persistency.impl.PersistentBase;

public class MockPersistent extends PersistentBase {

  private static final long serialVersionUID = -7468893532296148608L;
  public static final org.apache.avro.Schema SCHEMA$ =
          new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"MockPersistent\",\"namespace\":\"org.apache.gora.mock.persistency\",\"fields\":[{\"name\":\"foo\",\"type\":\"int\"},{\"name\":\"baz\",\"type\":\"int\"}]}");
  public static final String FOO = "foo";
  public static final String BAZ = "baz";
  
  public static final String[] _ALL_FIELDS = {FOO, BAZ};

  /**
   * Gets the total field count.
   */
  public int getFieldsCount() {
    return MockPersistent._ALL_FIELDS.length;
  }

  private int foo;
  private int baz;
  
  public MockPersistent() {
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
    Schema.Parser parser = new Schema.Parser();
    return parser.parse("{\"type\":\"record\",\"name\":\"MockPersistent\",\"namespace\":\"org.apache.gora.mock.persistency\",\"fields\":[{\"name\":\"foo\",\"type\":\"int\"},{\"name\":\"baz\",\"type\":\"int\"}]}");
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
  public Tombstone getTombstone() {
    return new Tombstone(){};
  }

  @Override
  public Persistent newInstance() {
    return new MockPersistent();
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
