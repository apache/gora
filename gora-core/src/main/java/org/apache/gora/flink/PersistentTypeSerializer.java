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

package org.apache.gora.flink;

import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.ByteValueSerializer;
import org.apache.flink.api.common.typeutils.base.TypeSerializerSingleton;
import org.apache.flink.api.java.typeutils.runtime.DataInputViewStream;
import org.apache.flink.api.java.typeutils.runtime.DataOutputViewStream;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.gora.mapreduce.PersistentDeserializer;
import org.apache.gora.mapreduce.PersistentSerializer;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.AvroUtils;

import java.io.IOException;

/**
 * Custom Serializer extends TypeSerializer written to serialize and deserialize Gora data beans.
 *
 * @param <T> Persistent record type.
 */
public class PersistentTypeSerializer<T extends PersistentBase> extends TypeSerializerSingleton<T> {

  private static final long serialVersionUID = 1L;
  private Class<T> persistentClass;

  public PersistentTypeSerializer(Class<T> persistentClass) {
    this.persistentClass = persistentClass;
  }

  public boolean isImmutableType() {
    return false;
  }

  public T createInstance() {
    return null;
  }

  public T copy(T from) {
    return AvroUtils.deepClonePersistent(from);
  }

  public T copy(T from, T reuse) {
    return this.copy(from);
  }

  public int getLength() {
    return -1;
  }

  public void serialize(T record, DataOutputView target) throws IOException {
    PersistentSerializer serializer = new PersistentSerializer();
    DataOutputViewStream outViewWrapper = new DataOutputViewStream(target);
    try {
      serializer.open(outViewWrapper);
      serializer.serialize(record);
    } finally {
      serializer.close();
      outViewWrapper.close();
    }
  }

  @SuppressWarnings("unchecked")
  public T deserialize(DataInputView source) throws IOException {
    PersistentDeserializer deserializer = new PersistentDeserializer(persistentClass, false);
    DataInputViewStream inViewWrapper = new DataInputViewStream(source);
    try {
      deserializer.open(inViewWrapper);
      return (T) deserializer.deserialize(null);
    } finally {
      deserializer.close();
      inViewWrapper.close();
    }
  }

  @SuppressWarnings("unchecked")
  public T deserialize(T reuse, DataInputView source) throws IOException {
    PersistentDeserializer deserializer = new PersistentDeserializer(persistentClass, true);
    DataInputViewStream inViewWrapper = new DataInputViewStream(source);
    try {
      deserializer.open(inViewWrapper);
      return (T) deserializer.deserialize(reuse);
    } finally {
      deserializer.close();
      inViewWrapper.close();
    }
  }

  public void copy(DataInputView source, DataOutputView target) throws IOException {
    T record = this.deserialize(source);
    this.serialize(record, target);
  }

  public boolean canEqual(Object obj) {
    return obj instanceof PersistentTypeSerializer;
  }

  protected boolean isCompatibleSerializationFormatIdentifier(String identifier) {
    return identifier.equals(ByteValueSerializer.class.getCanonicalName());
  }

  @Override
  public TypeSerializerSnapshot<T> snapshotConfiguration() {
    // TODO Auto-generated method stub
    return null;
  }
}