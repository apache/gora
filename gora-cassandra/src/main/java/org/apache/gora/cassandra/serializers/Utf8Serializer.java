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

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.ddl.ComparatorType;
import static me.prettyprint.hector.api.ddl.ComparatorType.UTF8TYPE;

import org.apache.avro.util.Utf8;

/**
 * A Utf8Serializer translates the byte[] to and from Utf8 object of Avro.
 */
public final class Utf8Serializer extends AbstractSerializer<Utf8> {

  private static final Utf8Serializer instance = new Utf8Serializer();

  public static Utf8Serializer get() {
    return instance;
  }

  @Override
  public ByteBuffer toByteBuffer(Utf8 obj) {
    if (obj == null) {
      return null;
    }
    return StringSerializer.get().toByteBuffer(obj.toString());
  }

  @Override
  public Utf8 fromByteBuffer(ByteBuffer byteBuffer) {
    if (byteBuffer == null) {
      return null;
    }
    return new Utf8(StringSerializer.get().fromByteBuffer(byteBuffer));
  }

  @Override
  public ComparatorType getComparatorType() {
    return UTF8TYPE;
  }

}
