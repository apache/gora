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
package org.apache.gora.jet;

import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.nio.Address;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.GoraException;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * jet-sink implementation.
 */
public class JetSink<KeyOut, ValueOut extends PersistentBase> implements ProcessorMetaSupplier {

  private transient int localParallelism;

  @Override
  public void init(@Nonnull Context context) {
    localParallelism = context.localParallelism();
  }

  @Nonnull
  @Override
  public Function<? super Address, ? extends ProcessorSupplier> get(@Nonnull List<Address> addresses) {
    Map<Address, ProcessorSupplier> map = new HashMap<>();
    for (int i = 0; i < addresses.size(); i++) {
      //globalIndexBase is the first processor index in a certain Jet-Cluster member
      int globalIndexBase = localParallelism * i;

      // processorCount will be equal to localParallelism:
      ProcessorSupplier supplier = processorCount ->
          range(globalIndexBase, globalIndexBase + processorCount)
              .mapToObj(globalIndex ->
                  new SinkProcessor<KeyOut, ValueOut>()
              ).collect(toList());
      map.put(addresses.get(i), supplier);
    }
    return map::get;
  }
}

class SinkProcessor<KeyOut, ValueOut extends PersistentBase> extends AbstractProcessor {

  @Override
  public boolean isCooperative() {
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected boolean tryProcess(int ordinal, Object item) throws Exception {
    JetEngine.dataOutStore.put(((JetInputOutputFormat<KeyOut, ValueOut>) item).getKey(),
        ((JetInputOutputFormat<KeyOut, ValueOut>) item).getValue());
    return true;
  }

  @Override
  public void close() {
    try {
      JetEngine.dataOutStore.flush();
    } catch (GoraException e) {
      throw new RuntimeException(e);
    }
  }
}
