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

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MapSerializer translates the byte[] to and from Map of Avro.
 */
public class MapSerializer<T> extends AbstractSerializer<Map<CharSequence, T>> {

  public static final Logger LOG = LoggerFactory.getLogger(MapSerializer.class);

  private static Map<Type, MapSerializer> valueTypeToSerializerMap = new HashMap<Type, MapSerializer>();
  private static Map<Class, MapSerializer> fixedClassToSerializerMap = new HashMap<Class, MapSerializer>();

  public static MapSerializer get(Type valueType) {
    MapSerializer serializer = valueTypeToSerializerMap.get(valueType);
    if (serializer == null) {
      serializer = new MapSerializer(valueType);
      valueTypeToSerializerMap.put(valueType, serializer);
    }
    return serializer;
  }

  public static MapSerializer get(Type valueType, Class clazz) {
    if (valueType != Type.FIXED) {
      return null;
    }
    MapSerializer serializer = valueTypeToSerializerMap.get(clazz);
    if (serializer == null) {
      serializer = new MapSerializer(clazz);
      fixedClassToSerializerMap.put(clazz, serializer);
    }
    return serializer;
  }

  public static MapSerializer get(Schema valueSchema) {
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

  public MapSerializer(Serializer<T> valueSerializer) {
    this.valueSerializer = valueSerializer;
  }

  public MapSerializer(Schema valueSchema) {
    this.valueSchema = valueSchema;
    valueType = valueSchema.getType();
    size = TypeUtils.getFixedSize(valueSchema);
    valueSerializer = GoraSerializerTypeInferer.getSerializer(valueSchema);
  }

  @SuppressWarnings("unchecked")
  public MapSerializer(Type valueType) {
    this.valueType = valueType;
    if (valueType != Type.FIXED) {
      valueSchema = Schema.create(valueType);
    }
    clazz = (Class<T>) TypeUtils.getClass(valueType);
    size = TypeUtils.getFixedSize(valueType);
    valueSerializer = GoraSerializerTypeInferer.getSerializer(valueType);
  }

  public MapSerializer(Class<T> clazz) {
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
  public ByteBuffer toByteBuffer(Map<CharSequence, T> map) {
    if (map == null) {
      return null;
    }
    if (size > 0) {
      return toByteBufferWithFixedLengthElements(map);
    } else {
      return toByteBufferWithVariableLengthElements(map);
    }
  }

  private ByteBuffer toByteBufferWithFixedLengthElements(Map<CharSequence, T> map) {
    int n = (int) map.size();
    List<byte[]> list = new ArrayList<byte[]>(n);
    n *= 4;
    for (CharSequence key : map.keySet()) {
      T value = map.get(key);
      byte[] bytes = BytesArraySerializer.get().fromByteBuffer(CharSequenceSerializer.get().toByteBuffer(key));
      list.add(bytes);
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

  private ByteBuffer toByteBufferWithVariableLengthElements(Map<CharSequence, T> map) {
    int n = (int) map.size();
    List<byte[]> list = new ArrayList<byte[]>(n);
    n *= 8;
    for (CharSequence key : map.keySet()) {
      T value = map.get(key);
      byte[] bytes = BytesArraySerializer.get().fromByteBuffer(CharSequenceSerializer.get().toByteBuffer(key));
      list.add(bytes);
      n += bytes.length;
      bytes = BytesArraySerializer.get().fromByteBuffer(valueSerializer.toByteBuffer(value));
      list.add(bytes);
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
  public Map<CharSequence, T> fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    Map<CharSequence, T> map = new HashMap<CharSequence, T>();
    while (true) {
      CharSequence key = null;
      T value = null;
      try {
        int n = IntegerSerializer.get().fromByteBuffer(byteBuffer);
        byte[] bytes = new byte[n];
        byteBuffer.get(bytes, 0, n);
        key = CharSequenceSerializer.get().fromByteBuffer( BytesArraySerializer.get().toByteBuffer(bytes) );

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