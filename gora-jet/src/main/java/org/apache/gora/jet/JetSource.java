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
                  new GoraJetProcessor<KeyIn, ValueIn>(getPartionedData(globalIndex))
              ).collect(toList());
      map.put(addresses.get(i), supplier);
    }
    return map::get;
  }

  List<JetInputOutputFormat<KeyIn, ValueIn>> getPartionedData(int globalIndex) {
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
      e.printStackTrace();
    }
    return null;
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
