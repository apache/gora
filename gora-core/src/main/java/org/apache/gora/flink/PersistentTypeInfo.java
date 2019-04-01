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

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.gora.persistency.impl.PersistentBase;

/**
 * PersistentTypeInfo contains details such as how the persistent data beans
 * are serialized and deserialized. Here GenericTypeInfo is extended otherwise
 * Flink engine consider Gora data beans as a Generic Type and serialize using KryoSerializer.
 */
public class PersistentTypeInfo<T extends PersistentBase> extends GenericTypeInfo<T> {

  public PersistentTypeInfo(Class<T> typeClass) {
    super(typeClass);
  }

  public TypeSerializer<T> createSerializer(ExecutionConfig config) {
    if (config.hasGenericTypesDisabled()) {
      throw new UnsupportedOperationException("Generic types have been disabled in the ExecutionConfig and type "
              + getTypeClass().getName() + " is treated as a generic type.");
    } else {
      return new PersistentTypeSerializer<T>(getTypeClass());
    }
  }

}
