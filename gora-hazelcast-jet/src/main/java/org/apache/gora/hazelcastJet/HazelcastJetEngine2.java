package org.apache.gora.hazelcastJet;

import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.core.ProcessorSupplier;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.nio.Address;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class HazelcastJetEngine2<KeyIn, ValueIn extends PersistentBase, KeyOut, ValueOut extends PersistentBase> {
    public static DataStore dataOutStore;
    public BatchSource<ValueIn> createDataSource(Query<KeyIn, ValueIn> query) {
        BatchSource<ValueIn> source = Sources.batchFromProcessor("gora-jet-source",
                new GoraJetMetaSupplier<KeyIn, ValueIn>(query));
        return source;
    }

    public Sink<JetOutputFormat<KeyOut, ValueOut>> createDataSink(DataStore<KeyOut, ValueOut> dataOutStore) {
        HazelcastJetEngine2.dataOutStore = dataOutStore;
        Sink<JetOutputFormat<KeyOut, ValueOut>> sink = Sinks.fromProcessor("gora-jet-sink",
                new GoraJetSinkMetaSupplier<KeyOut, ValueOut>());
        return sink;
    }
}

class GoraJetProcessor<KeyIn, ValueIn extends PersistentBase> extends AbstractProcessor {

    private final Traverser<ValueIn> traverser;

    GoraJetProcessor(List<ValueIn> list) {
        System.out.println(list.size());
        this.traverser = traverseIterable(list);
    }

    @Override
    public boolean complete() {
        return emitFromTraverser(traverser);
    }
}


class GoraJetMetaSupplier<KeyIn, ValueIn extends PersistentBase> implements ProcessorMetaSupplier {

    private transient int totalParallelism;
    private transient int localParallelism;
    private List<ValueIn> allResultsList = new ArrayList<>();


    GoraJetMetaSupplier(Query<KeyIn, ValueIn> query) {
        try {
            Result<KeyIn, ValueIn> result = query.execute();
            while (result.next()) {
                allResultsList.add(result.get());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

            int partionSize = (allResultsList.size() / totalParallelism) + 1;
            // processorCount will be equal to localParallelism:
            ProcessorSupplier supplier = processorCount ->
                    range(globalIndexBase, globalIndexBase + processorCount)
                            .mapToObj(globalIndex ->
                                    new GoraJetProcessor<KeyIn, ValueIn>(getPartionedData(globalIndex * partionSize,
                                            (globalIndex + 1) * partionSize))
                            ).collect(toList());
            map.put(addresses.get(i), supplier);
        }
        return map::get;
    }

    List<ValueIn> getPartionedData(int start, int end) {
        if (end > allResultsList.size())
            end = allResultsList.size();
        List<ValueIn> resultsList = new ArrayList<>();
        for (int i = start; i < end; i++) {
            resultsList.add(allResultsList.get(i));
        }
        return resultsList;
    }
}

class SinkProcessor<KeyOut, ValueOut extends PersistentBase> extends AbstractProcessor{


    @Override
    public boolean isCooperative() {
        return false;
    }

    @Override
    protected boolean tryProcess(int ordinal, Object item) throws Exception {
        HazelcastJetEngine2.dataOutStore.put(((JetOutputFormat<KeyOut, ValueOut>)item).getKey(),
                ((JetOutputFormat<KeyOut, ValueOut>)item).getValue());
        return true;
    }

    @Override
    public void close() {
        try {
            HazelcastJetEngine2.dataOutStore.flush();
        } catch (GoraException e) {
            e.printStackTrace();
        }

    }
}

class GoraJetSinkMetaSupplier<KeyOut, ValueOut extends PersistentBase> implements ProcessorMetaSupplier {

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
            // We'll calculate the global index of each processor in the cluster:
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