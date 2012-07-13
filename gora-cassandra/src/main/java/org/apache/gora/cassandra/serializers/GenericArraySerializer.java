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
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.ListGenericArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A GenericArraySerializer translates the byte[] to and from GenericArray of Avro.
 */
public class GenericArraySerializer<T> extends AbstractSerializer<GenericArray<T>> {

  public static final Logger LOG = LoggerFactory.getLogger(GenericArraySerializer.class);

  private static Map<Type, GenericArraySerializer> elementTypeToSerializerMap = new HashMap<Type, GenericArraySerializer>();
  private static Map<Class, GenericArraySerializer> fixedClassToSerializerMap = new HashMap<Class, GenericArraySerializer>();

  public static GenericArraySerializer get(Type elementType) {
    GenericArraySerializer serializer = elementTypeToSerializerMap.get(elementType);
    if (serializer == null) {
      serializer = new GenericArraySerializer(elementType);
      elementTypeToSerializerMap.put(elementType, serializer);
    }
    return serializer;
  }

  public static GenericArraySerializer get(Type elementType, Class clazz) {
    if (elementType != Type.FIXED) {
      return null;
    }
    GenericArraySerializer serializer = elementTypeToSerializerMap.get(clazz);
    if (serializer == null) {
      serializer = new GenericArraySerializer(clazz);
      fixedClassToSerializerMap.put(clazz, serializer);
    }
    return serializer;
  }

  public static GenericArraySerializer get(Schema elementSchema) {
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

  public GenericArraySerializer(Serializer<T> elementSerializer) {
    this.elementSerializer = elementSerializer;
  }

  public GenericArraySerializer(Schema elementSchema) {
    this.elementSchema = elementSchema;
    elementType = elementSchema.getType();
    size = TypeUtils.getFixedSize(elementSchema);
    elementSerializer = GoraSerializerTypeInferer.getSerializer(elementSchema);
  }

  public GenericArraySerializer(Type elementType) {
    this.elementType = elementType;
    if (elementType != Type.FIXED) {
      elementSchema = Schema.create(elementType);
    }
    clazz = TypeUtils.getClass(elementType);
    size = TypeUtils.getFixedSize(elementType);
    elementSerializer = GoraSerializerTypeInferer.getSerializer(elementType);
  }

  public GenericArraySerializer(Class<T> clazz) {
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
  public ByteBuffer toByteBuffer(GenericArray<T> array) {
    if (array == null) {
      return null;
    }
    if (size > 0) {
      return toByteBufferWithFixedLengthElements(array);
    } else {
      return toByteBufferWithVariableLengthElements(array);
    }
  }

  private ByteBuffer toByteBufferWithFixedLengthElements(GenericArray<T> array) {
    ByteBuffer byteBuffer = ByteBuffer.allocate((int) array.size() * size);
    for (T element : array) {
      byteBuffer.put(elementSerializer.toByteBuffer(element));
    }
    byteBuffer.rewind();
    return byteBuffer;
  }

  private ByteBuffer toByteBufferWithVariableLengthElements(GenericArray<T> array) {
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
  public GenericArray<T> fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    GenericArray<T> array = new ListGenericArray<T>(elementSchema);
int i = 0;
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
