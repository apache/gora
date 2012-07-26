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

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.ListGenericArray;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.StatefulHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that infers the concrete Serializer needed to turn a value into
 * its binary representation
 */
public class TypeUtils {

  public static final Logger LOG = LoggerFactory.getLogger(TypeUtils.class);

  // @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Class getClass(Object value) {
    return value.getClass();
  }

  public static Schema getSchema(Object value) {
    if (value instanceof GenericArray) {
      return Schema.createArray( getElementSchema((GenericArray)value) );
    } else {
      return getSchema( getClass(value) );
    }
  }

  public static Type getType(Object value) {
    return getType( getClass(value) );
  }

  public static Type getType(Class<?> clazz) {
    if (clazz.equals(Utf8.class)) {
      return Type.STRING;
    } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
      return Type.BOOLEAN;
    } else if (clazz.equals(ByteBuffer.class)) {
      return Type.BYTES;
    } else if (clazz.equals(Double.class) || clazz.equals(double.class)) {
      return Type.DOUBLE;
    } else if (clazz.equals(Float.class) || clazz.equals(float.class)) {
      return Type.FLOAT;
    } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
      return Type.INT;
    } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
      return Type.LONG;
    } else if (clazz.equals(ListGenericArray.class)) {
      return Type.ARRAY;
    } else if (clazz.equals(StatefulHashMap.class)) {
      return Type.MAP;
    } else if (clazz.equals(Persistent.class)) {
      return Type.RECORD;
    } else if (clazz.getSuperclass().equals(SpecificFixed.class)) {
      return Type.FIXED;
    } else {
      return null;
    }
  }

  public static Class getClass(Type type) {
    if (type == Type.STRING) {
      return Utf8.class;
    } else if (type == Type.BOOLEAN) {
      return Boolean.class;
    } else if (type == Type.BYTES) {
      return ByteBuffer.class;
    } else if (type == Type.DOUBLE) {
      return Double.class;
    } else if (type == Type.FLOAT) {
      return Float.class;
    } else if (type == Type.INT) {
      return Integer.class;
    } else if (type == Type.LONG) {
      return Long.class;
    } else if (type == Type.ARRAY) {
      return ListGenericArray.class;
    } else if (type == Type.MAP) {
      return StatefulHashMap.class;
    } else if (type == Type.RECORD) {
      return Persistent.class;
    } else if (type == Type.FIXED) {
      // return SpecificFixed.class;
      return null;
    } else {
      return null;
    }
  }

  public static Schema getSchema(Class clazz) {
    Type type = getType(clazz);
    if (type == null) {
      return null;
    } else if (type == Type.FIXED) {
      int size = getFixedSize(clazz);
      String name = clazz.getName();
      String space = null;
      int n = name.lastIndexOf(".");
      if (n < 0) {
        space = name.substring(0,n);
        name = name.substring(n+1);
      } else {
        space = null;
      }
      String doc = null; // ?
      // LOG.info(Schema.createFixed(name, doc, space, size).toString());
      return Schema.createFixed(name, doc, space, size);
    } else if (type == Type.ARRAY) {
      Object obj = null;
      try {
        obj = clazz.newInstance();
      } catch (InstantiationException e) {
        LOG.warn(e.toString());
        return null;
      } catch (IllegalAccessException e) {
        LOG.warn(e.toString());
        return null;
      }
      return getSchema(obj);
    } else if (type == Type.MAP) {
      // TODO
      // return Schema.createMap(...);
      return null;
    } else if (type == Type.RECORD) {
      // TODO
      // return Schema.createRecord(...);
      return null;
    } else {
      return Schema.create(type);
    }
  }

  public static Class getClass(Schema schema) {
    Type type = schema.getType();
    if (type == null) {
      return null;
    } else if (type == Type.FIXED) {
      try {
        return Class.forName( schema.getFullName() );
      } catch (ClassNotFoundException e) {
        LOG.warn(e.toString() + " : " + schema);
        return null;
      }
    } else {
      return getClass(type);
    }
  }

  public static int getFixedSize(Type type) {
    if (type == Type.BOOLEAN) {
      return 1;
    } else if (type == Type.DOUBLE) {
      return 8;
    } else if (type == Type.FLOAT) {
      return 4;
    } else if (type == Type.INT) {
      return 4;
    } else if (type == Type.LONG) {
      return 8;
    } else {
      return -1;
    }
  }

  public static int getFixedSize(Schema schema) {
    Type type = schema.getType();
    if (type == Type.FIXED) {
      return schema.getFixedSize();
    } else {
      return getFixedSize(type);
    }
  }

  public static int getFixedSize(Class clazz) {
    Type type = getType(clazz);
    if (type == Type.FIXED) {
      try {
        return ((SpecificFixed)clazz.newInstance()).bytes().length;
      } catch (InstantiationException e) {
        LOG.warn(e.toString());
        return -1;
      } catch (IllegalAccessException e) {
        LOG.warn(e.toString());
        return -1;
      }
    } else {
      return getFixedSize(type);
    }
  }

  public static Schema getElementSchema(GenericArray array) {
    Schema schema = array.getSchema();
    return (schema.getType() == Type.ARRAY) ? schema.getElementType() : schema;
  }

  public static Type getElementType(ListGenericArray array) {
    return getElementSchema(array).getType();
  }

  /*
  public static Schema getValueSchema(StatefulHashMap map) {
    return map.getSchema().getValueType();
  }

  public static Type getValueType(StatefulHashMap map) {
    return getValueSchema(map).getType();
  }
  */

}
