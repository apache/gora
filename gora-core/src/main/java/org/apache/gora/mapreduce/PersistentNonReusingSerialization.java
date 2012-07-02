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
package org.apache.gora.mapreduce;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

public class PersistentNonReusingSerialization
implements Serialization<PersistentBase> {

  @Override
  public boolean accept(Class<?> c) {
    return Persistent.class.isAssignableFrom(c);
  }

  @Override
  public Deserializer<PersistentBase> getDeserializer(Class<PersistentBase> c) {
    return new PersistentDeserializer(c, false);
  }

  @Override
  public Serializer<PersistentBase> getSerializer(Class<PersistentBase> c) {
    return new PersistentSerializer();
  }
}
