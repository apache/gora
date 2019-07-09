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
    protected boolean tryProcess(int ordinal, Object item) throws Exception {
        JetEngine.dataOutStore.put(((JetOutputFormat<KeyOut, ValueOut>) item).getKey(),
                ((JetOutputFormat<KeyOut, ValueOut>) item).getValue());
        return true;
    }

    @Override
    public void close() {
        try {
            JetEngine.dataOutStore.flush();
        } catch (GoraException e) {
            e.printStackTrace();
        }
    }
}
