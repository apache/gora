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

import java.nio.ByteBuffer;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.FloatSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.hector.api.Serializer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.util.Utf8;

import org.apache.gora.persistency.StatefulHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that infers the concrete Serializer needed to turn a value into
 * its binary representation
 */
public class GoraSerializerTypeInferer {

  public static final Logger LOG = LoggerFactory.getLogger(GoraSerializerTypeInferer.class);

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Object value) {
    Serializer serializer = null;
    if (value == null) {
      serializer = ByteBufferSerializer.get();
    } else if (value instanceof Utf8) {
      serializer = Utf8Serializer.get();
    } else if (value instanceof Boolean) {
      serializer = BooleanSerializer.get();
    } else if (value instanceof ByteBuffer) {
      serializer = ByteBufferSerializer.get();
    } else if (value instanceof byte[]) {
      serializer = BytesArraySerializer.get();
    } else if (value instanceof Double) {
      serializer = DoubleSerializer.get();
    } else if (value instanceof Float) {
      serializer = FloatSerializer.get();
    } else if (value instanceof Integer) {
      serializer = IntegerSerializer.get();
    } else if (value instanceof Long) {
      serializer = LongSerializer.get();
    } else if (value instanceof String) {
      serializer = StringSerializer.get();
    } else if (value instanceof SpecificFixed) {
      serializer = SpecificFixedSerializer.get(value.getClass());
    } else if (value instanceof GenericArray) {
      Schema schema = ((GenericArray)value).getSchema();
      if (schema.getType() == Type.ARRAY) {
        schema = schema.getElementType();
      }
      serializer = GenericArraySerializer.get(schema);
    } else if (value instanceof StatefulHashMap) {
      StatefulHashMap map = (StatefulHashMap)value;
      if (map.size() == 0) {
        serializer = ByteBufferSerializer.get();
      }
      else {
        Object value0 = map.values().iterator().next();
        Schema schema = TypeUtils.getSchema(value0);
        serializer = StatefulHashMapSerializer.get(schema);
      }
    } else {
      serializer = SerializerTypeInferer.getSerializer(value);
    }
    return serializer;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Class<?> valueClass) {
    Serializer serializer = null;
    if (valueClass.equals(Utf8.class)) {
      serializer = Utf8Serializer.get();
    } else if (valueClass.equals(Boolean.class) || valueClass.equals(boolean.class)) {
      serializer = BooleanSerializer.get();
    } else if (valueClass.equals(ByteBuffer.class)) {
      serializer = ByteBufferSerializer.get();
    } else if (valueClass.equals(Double.class) || valueClass.equals(double.class)) {
      serializer = DoubleSerializer.get();
    } else if (valueClass.equals(Float.class) || valueClass.equals(float.class)) {
      serializer = FloatSerializer.get();
    } else if (valueClass.equals(Integer.class) || valueClass.equals(int.class)) {
      serializer = IntegerSerializer.get();
    } else if (valueClass.equals(Long.class) || valueClass.equals(long.class)) {
      serializer = LongSerializer.get();
    } else if (valueClass.equals(String.class)) {
      serializer = StringSerializer.get();
    } else {
      serializer = SerializerTypeInferer.getSerializer(valueClass);
    }
    return serializer;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Schema schema) {
    Serializer serializer = null;
    Type type = schema.getType();
    if (type == Type.STRING) {
      serializer = Utf8Serializer.get();
    } else if (type == Type.BOOLEAN) {
      serializer = BooleanSerializer.get();
    } else if (type == Type.BYTES) {
      serializer = ByteBufferSerializer.get();
    } else if (type == Type.DOUBLE) {
      serializer = DoubleSerializer.get();
    } else if (type == Type.FLOAT) {
      serializer = FloatSerializer.get();
    } else if (type == Type.INT) {
      serializer = IntegerSerializer.get();
    } else if (type == Type.LONG) {
      serializer = LongSerializer.get();
    } else if (type == Type.FIXED) {
      Class clazz = TypeUtils.getClass(schema);
      serializer = SpecificFixedSerializer.get(clazz);
      // serializer = SpecificFixedSerializer.get(schema);
    } else if (type == Type.ARRAY) {
      serializer = GenericArraySerializer.get(schema.getElementType());
    } else if (type == Type.MAP) {
      serializer = StatefulHashMapSerializer.get(schema.getValueType());
    } else if (type == Type.UNION){
      serializer = ByteBufferSerializer.get();
    } else {
      serializer = null;
    }
    return serializer;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Type type) {
    Serializer serializer = null;
    if (type == Type.STRING) {
      serializer = Utf8Serializer.get();
    } else if (type == Type.BOOLEAN) {
      serializer = BooleanSerializer.get();
    } else if (type == Type.BYTES) {
      serializer = ByteBufferSerializer.get();
    } else if (type == Type.DOUBLE) {
      serializer = DoubleSerializer.get();
    } else if (type == Type.FLOAT) {
      serializer = FloatSerializer.get();
    } else if (type == Type.INT) {
      serializer = IntegerSerializer.get();
    } else if (type == Type.LONG) {
      serializer = LongSerializer.get();
    } else if (type == Type.FIXED) {
      serializer = SpecificFixedSerializer.get();
    } else {
      serializer = null;
    }
    return serializer;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Type type, Type elementType) {
    Serializer serializer = null;
    if (type == null) {
      if (elementType == null) {
        serializer = null;
      } else {
        serializer = getSerializer(elementType);
      }
    } else {
      if (elementType == null) {
        serializer = getSerializer(type);
      }
    }

    if (type == Type.ARRAY) {
      serializer = GenericArraySerializer.get(elementType);
    } else if (type == Type.MAP) {
      serializer = StatefulHashMapSerializer.get(elementType);
    } else {
      serializer = null;
    }
    return serializer;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T> Serializer<T> getSerializer(Type type, Class<T> clazz) {
    Serializer serializer = null;
    if (type != Type.FIXED) {
      serializer = null;
    }
    if (clazz == null) {
      serializer = null;
    } else {
      serializer = SpecificFixedSerializer.get(clazz);
    }
    return serializer;
  }

}
