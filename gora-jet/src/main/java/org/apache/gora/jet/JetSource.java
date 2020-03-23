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

import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.nio.Address;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Result;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

/**
 * jet-source implementation.
 */
public class JetSource<KeyIn, ValueIn extends PersistentBase> implements ProcessorMetaSupplier {

  private int totalParallelism;
  private transient int localParallelism;

  @Override
  public void init(@Nonnull Context context) {
    totalParallelism = context.totalParallelism();
    localParallelism = context.localParallelism();
  }

  @Nonnull
  @Override
  public Function<? super Address, ? extends ProcessorSupplier> get(@Nonnull List<Address> addresses) {
    Map<Address, ProcessorSupplier> map = new HashMap<>();
    for (int i = 0; i < addresses.size(); i++) {
      // We'll calculate the global index of each processor in the cluster:
      //globalIndexBase is the first processor index in a certain Jet-Cluster member
      int globalIndexBase = localParallelism * i;

      // processorCount will be equal to localParallelism:
      ProcessorSupplier supplier = processorCount ->
          range(globalIndexBase, globalIndexBase + processorCount)
              .mapToObj(globalIndex ->
                  new GoraJetProcessor<KeyIn, ValueIn>(getPartitionedData(globalIndex))
              ).collect(toList());
      map.put(addresses.get(i), supplier);
    }
    return map::get;
  }

  @SuppressWarnings("unchecked")
  private List<JetInputOutputFormat<KeyIn, ValueIn>> getPartitionedData(int globalIndex) {
    try {
      List<PartitionQuery<KeyIn, ValueIn>> partitionQueries = JetEngine.dataInStore.getPartitions(JetEngine.query);
      List<JetInputOutputFormat<KeyIn, ValueIn>> resultsList = new ArrayList<>();
      int i = 1;
      int partitionNo = globalIndex;
      while (partitionNo < partitionQueries.size()) {
        Result<KeyIn, ValueIn> result = null;
        result = partitionQueries.get(partitionNo).execute();
        while (result.next()) {
          resultsList.add(new JetInputOutputFormat<>(result.getKey(), result.get()));
        }
        partitionNo = (i * totalParallelism) + globalIndex;
        i++;
      }
      return resultsList;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

class GoraJetProcessor<KeyIn, ValueIn extends PersistentBase> extends AbstractProcessor {

  private final Traverser<JetInputOutputFormat<KeyIn, ValueIn>> traverser;

  GoraJetProcessor(List<JetInputOutputFormat<KeyIn, ValueIn>> list) {
    this.traverser = traverseIterable(list);
  }

  @Override
  public boolean complete() {
    return emitFromTraverser(traverser);
  }
}
