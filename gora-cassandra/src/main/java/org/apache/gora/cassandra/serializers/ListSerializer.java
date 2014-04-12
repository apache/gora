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
 * A GenericArraySerializer translates the byte[] to and from GenericArray of Avro.
 */
public class ListSerializer<T> extends AbstractSerializer<List<T>> {

  public static final Logger LOG = LoggerFactory.getLogger(ListSerializer.class);

  private static Map<Type, ListSerializer> elementTypeToSerializerMap = new HashMap<Type, ListSerializer>();
  private static Map<Class, ListSerializer> fixedClassToSerializerMap = new HashMap<Class, ListSerializer>();

  public static ListSerializer get(Type elementType) {
    ListSerializer serializer = elementTypeToSerializerMap.get(elementType);
    if (serializer == null) {
      serializer = new ListSerializer(elementType);
      elementTypeToSerializerMap.put(elementType, serializer);
    }
    return serializer;
  }

  public static ListSerializer get(Type elementType, Class clazz) {
    if (elementType != Type.FIXED) {
      return null;
    }
    ListSerializer serializer = elementTypeToSerializerMap.get(clazz);
    if (serializer == null) {
      serializer = new ListSerializer(clazz);
      fixedClassToSerializerMap.put(clazz, serializer);
    }
    return serializer;
  }

  public static ListSerializer get(Schema elementSchema) {
    Type type = elementSchema.getType();
    if (type == Type.FIXED) {
      return get(Type.FIXED, TypeUtils.getClass(elementSchema));
    } else {
      return get(type);
    }
  }

  private Schema elementSchema = null;
  private Type elementType = null;
  private int size = -1;
  private Class<T> clazz = null;
  private Serializer<T> elementSerializer = null;

  public ListSerializer(Serializer<T> elementSerializer) {
    this.elementSerializer = elementSerializer;
  }

  public ListSerializer(Schema elementSchema) {
    this.elementSchema = elementSchema;
    elementType = elementSchema.getType();
    size = TypeUtils.getFixedSize(elementSchema);
    elementSerializer = GoraSerializerTypeInferer.getSerializer(elementSchema);
  }

  @SuppressWarnings("unchecked")
  public ListSerializer(Type elementType) {
    this.elementType = elementType;
    if (elementType != Type.FIXED) {
      elementSchema = Schema.create(elementType);
    }
    clazz = (Class<T>) TypeUtils.getClass(elementType);
    size = TypeUtils.getFixedSize(elementType);
    elementSerializer = GoraSerializerTypeInferer.getSerializer(elementType);
  }

  public ListSerializer(Class<T> clazz) {
    this.clazz = clazz;
    elementType = TypeUtils.getType(clazz);
    size = TypeUtils.getFixedSize(clazz);
    if (elementType == null || elementType == Type.FIXED) {
      elementType = Type.FIXED;
      elementSchema = TypeUtils.getSchema(clazz);
      elementSerializer = GoraSerializerTypeInferer.getSerializer(elementType, clazz);
    } else {
      elementSerializer = GoraSerializerTypeInferer.getSerializer(elementType);
    }
  }

  @Override
  public ByteBuffer toByteBuffer(List<T> array) {
    if (array == null) {
      return null;
    }
    if (size > 0) {
      return toByteBufferWithFixedLengthElements(array);
    } else {
      return toByteBufferWithVariableLengthElements(array);
    }
  }

  private ByteBuffer toByteBufferWithFixedLengthElements(List<T> array) {
    ByteBuffer byteBuffer = ByteBuffer.allocate((int) array.size() * size);
    for (T element : array) {
      byteBuffer.put(elementSerializer.toByteBuffer(element));
    }
    byteBuffer.rewind();
    return byteBuffer;
  }

  private ByteBuffer toByteBufferWithVariableLengthElements(List<T> array) {
    int n = (int) array.size();
    List<byte[]> list = new ArrayList<byte[]>(n);
    n *= 4;
    for (T element : array) {
      byte[] bytes = BytesArraySerializer.get().fromByteBuffer(elementSerializer.toByteBuffer(element));
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
  public List<T> fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    ArrayList<T> array = new ArrayList<T>();
    while (true) {
      T element = null;
      try {
        if (size > 0) {
          element = elementSerializer.fromByteBuffer(byteBuffer);
        }
        else {
          int n = IntegerSerializer.get().fromByteBuffer(byteBuffer);
          byte[] bytes = new byte[n];
          byteBuffer.get(bytes, 0, n);
          element = elementSerializer.fromByteBuffer( BytesArraySerializer.get().toByteBuffer(bytes) );
        }
      } catch (BufferUnderflowException e) {
        break;
      }
      if (element == null) {
        break;
      }
      array.add(element);
    }
    return array;
  }

  @Override
  public ComparatorType getComparatorType() {
    return elementSerializer.getComparatorType();
  }

}
