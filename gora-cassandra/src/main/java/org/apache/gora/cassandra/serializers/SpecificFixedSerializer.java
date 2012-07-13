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
import java.util.HashMap;
import java.util.Map;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.ddl.ComparatorType;
import static me.prettyprint.hector.api.ddl.ComparatorType.BYTESTYPE;

import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.util.Utf8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SpecificFixedSerializer translates the byte[] to and from SpecificFixed of Avro.
 */
public class SpecificFixedSerializer extends AbstractSerializer<SpecificFixed> {

  public static final Logger LOG = LoggerFactory.getLogger(SpecificFixedSerializer.class);

  // for toByteBuffer
  private static SpecificFixedSerializer serializer = new SpecificFixedSerializer(SpecificFixed.class);

  // for fromByteBuffer, requiring Class info
  public static SpecificFixedSerializer get() {
    return serializer;
  }

  private static Map<Class, SpecificFixedSerializer> classToSerializerMap = new HashMap<Class, SpecificFixedSerializer>();

  public static SpecificFixedSerializer get(Class clazz) {
    SpecificFixedSerializer serializer = classToSerializerMap.get(clazz);
    if (serializer == null) {
      serializer = new SpecificFixedSerializer(clazz);
      classToSerializerMap.put(clazz, serializer);
    }
    return serializer;
  }

  private Class<? extends SpecificFixed> clazz;

  public SpecificFixedSerializer(Class<? extends SpecificFixed> clazz) {
    this.clazz = clazz;
  }

  @Override
  public ByteBuffer toByteBuffer(SpecificFixed fixed) {
    if (fixed == null) {
      return null;
    }
    byte[] bytes = fixed.bytes();
    if (bytes.length < 1) {
      return null;
    }
    return BytesArraySerializer.get().toByteBuffer(bytes);
  }

  @Override
  public SpecificFixed fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }

    Object value = null;
    try {
      value = clazz.newInstance();
    } catch (InstantiationException ie) {
      LOG.warn("Instantiation error for class=" + clazz, ie);
      return null;
    } catch (IllegalAccessException iae) {
      LOG.warn("Illegal access error for class=" + clazz, iae);
      return null;
    }

    if (! (value instanceof SpecificFixed)) {
      LOG.warn("Not an instance of SpecificFixed");
      return null;
    }

    SpecificFixed fixed = (SpecificFixed) value;
    byte[] bytes = fixed.bytes();
    try {
      byteBuffer.get(bytes, 0, bytes.length);
    }
    catch (BufferUnderflowException e) {
      // LOG.info(e.toString() + " : class=" + clazz.getName() + " length=" + bytes.length);
      throw e;
    }
    fixed.bytes(bytes);
    return fixed;
  }

  @Override
  public ComparatorType getComparatorType() {
    return BYTESTYPE;
  }

}
