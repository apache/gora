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

package org.apache.gora.cassandra.serializers;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.ddl.ComparatorType;
import static me.prettyprint.hector.api.ddl.ComparatorType.UTF8TYPE;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.State;
import org.apache.gora.persistency.StatefulHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A StatefulHashMapSerializer translates the byte[] to and from StatefulHashMap of Avro.
 */
public class StatefulHashMapSerializer<T> extends AbstractSerializer<StatefulHashMap<Utf8, T>> {

  public static final Logger LOG = LoggerFactory.getLogger(StatefulHashMapSerializer.class);

  private static Map<Type, StatefulHashMapSerializer> valueTypeToSerializerMap = new HashMap<Type, StatefulHashMapSerializer>();
  private static Map<Class, StatefulHashMapSerializer> fixedClassToSerializerMap = new HashMap<Class, StatefulHashMapSerializer>();

  public static StatefulHashMapSerializer get(Type valueType) {
    StatefulHashMapSerializer serializer = valueTypeToSerializerMap.get(valueType);
    if (serializer == null) {
      serializer = new StatefulHashMapSerializer(valueType);
      valueTypeToSerializerMap.put(valueType, serializer);
    }
    return serializer;
  }

  public static StatefulHashMapSerializer get(Type valueType, Class clazz) {
    if (valueType != Type.FIXED) {
      return null;
    }
    StatefulHashMapSerializer serializer = valueTypeToSerializerMap.get(clazz);
    if (serializer == null) {
      serializer = new StatefulHashMapSerializer(clazz);
      fixedClassToSerializerMap.put(clazz, serializer);
    }
    return serializer;
  }

  public static StatefulHashMapSerializer get(Schema valueSchema) {
    Type type = valueSchema.getType();
    if (type == Type.FIXED) {
      return get(Type.FIXED, TypeUtils.getClass(valueSchema));
    } else {
      return get(type);
    }
  }

  private Schema valueSchema = null;
  private Type valueType = null;
  private int size = -1;
  private Class<T> clazz = null;
  private Serializer<T> valueSerializer = null;

  public StatefulHashMapSerializer(Serializer<T> valueSerializer) {
    this.valueSerializer = valueSerializer;
  }

  public StatefulHashMapSerializer(Schema valueSchema) {
    this.valueSchema = valueSchema;
    valueType = valueSchema.getType();
    size = TypeUtils.getFixedSize(valueSchema);
    valueSerializer = GoraSerializerTypeInferer.getSerializer(valueSchema);
  }

  public StatefulHashMapSerializer(Type valueType) {
    this.valueType = valueType;
    if (valueType != Type.FIXED) {
      valueSchema = Schema.create(valueType);
    }
    clazz = TypeUtils.getClass(valueType);
    size = TypeUtils.getFixedSize(valueType);
    valueSerializer = GoraSerializerTypeInferer.getSerializer(valueType);
  }

  public StatefulHashMapSerializer(Class<T> clazz) {
    this.clazz = clazz;
    valueType = TypeUtils.getType(clazz);
    size = TypeUtils.getFixedSize(clazz);
    if (valueType == null || valueType == Type.FIXED) {
      valueType = Type.FIXED;
      valueSchema = TypeUtils.getSchema(clazz);
      valueSerializer = GoraSerializerTypeInferer.getSerializer(valueType, clazz);
    } else {
      valueSerializer = GoraSerializerTypeInferer.getSerializer(valueType);
    }
  }

  @Override
  public ByteBuffer toByteBuffer(StatefulHashMap<Utf8, T> map) {
    if (map == null) {
      return null;
    }
    if (size > 0) {
      return toByteBufferWithFixedLengthElements(map);
    } else {
      return toByteBufferWithVariableLengthElements(map);
    }
  }

  private ByteBuffer toByteBufferWithFixedLengthElements(StatefulHashMap<Utf8, T> map) {
    List<byte[]> list = new ArrayList<byte[]>(map.size());
    int n = 0;
    for (Utf8 key : map.keySet()) {
      if (map.getState(key) == State.DELETED) {
        continue;
      }
      T value = map.get(key);
      byte[] bytes = BytesArraySerializer.get().fromByteBuffer(Utf8Serializer.get().toByteBuffer(key));
      list.add(bytes);
      n += 4;
      n += bytes.length;
      bytes = BytesArraySerializer.get().fromByteBuffer(valueSerializer.toByteBuffer(value));
      list.add(bytes);
      n += bytes.length;
    }
    ByteBuffer byteBuffer = ByteBuffer.allocate(n);
    int i = 0;
    for (byte[] bytes : list) {
      if (i % 2 == 0) {
        byteBuffer.put(IntegerSerializer.get().toByteBuffer(bytes.length));
      }
      byteBuffer.put(BytesArraySerializer.get().toByteBuffer(bytes));
      i += 1;
    }
    byteBuffer.rewind();
    return byteBuffer;
  }

  private ByteBuffer toByteBufferWithVariableLengthElements(StatefulHashMap<Utf8, T> map) {
    List<byte[]> list = new ArrayList<byte[]>(map.size());
    int n = 0;
    for (Utf8 key : map.keySet()) {
      if (map.getState(key) == State.DELETED) {
        continue;
      }
      T value = map.get(key);
      byte[] bytes = BytesArraySerializer.get().fromByteBuffer(Utf8Serializer.get().toByteBuffer(key));
      list.add(bytes);
      n += 4;
      n += bytes.length;
      bytes = BytesArraySerializer.get().fromByteBuffer(valueSerializer.toByteBuffer(value));
      list.add(bytes);
      n += 4;
      n += bytes.length;
    }
    ByteBuffer byteBuffer = ByteBuffer.allocate(n);
    for (byte[] bytes : list) {
      byteBuffer.put(IntegerSerializer.get().toByteBuffer(bytes.length));
      byteBuffer.put(BytesArraySerializer.get().toByteBuffer(bytes));
    }
    byteBuffer.rewind();
    return byteBuffer;
  }

  @Override
  public StatefulHashMap<Utf8, T> fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    StatefulHashMap<Utf8, T> map = new StatefulHashMap<Utf8, T>();
int i = 0;
    while (true) {
      Utf8 key = null;
      T value = null;
      try {
        int n = IntegerSerializer.get().fromByteBuffer(byteBuffer);
        byte[] bytes = new byte[n];
        byteBuffer.get(bytes, 0, n);
        key = Utf8Serializer.get().fromByteBuffer( BytesArraySerializer.get().toByteBuffer(bytes) );

        if (size > 0) {
          value = valueSerializer.fromByteBuffer(byteBuffer);
        }
        else {
          n = IntegerSerializer.get().fromByteBuffer(byteBuffer);
          bytes = new byte[n];
          byteBuffer.get(bytes, 0, n);
          value = valueSerializer.fromByteBuffer( BytesArraySerializer.get().toByteBuffer(bytes) );
        }
      } catch (BufferUnderflowException e) {
        break;
      }
      if (key == null) {
        break;
      }
      if (value == null) {
        break;
      }
      map.put(key, value);
    }
    return map;
  }

  @Override
  public ComparatorType getComparatorType() {
    return valueSerializer.getComparatorType();
  }

}
