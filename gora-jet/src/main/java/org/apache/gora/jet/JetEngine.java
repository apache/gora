package org.apache.gora.jet;

import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;

public class JetEngine<KeyIn, ValueIn extends PersistentBase, KeyOut, ValueOut extends PersistentBase> {
  public static DataStore dataOutStore;
  public static DataStore dataInStore;
  public static Query query;

  public BatchSource<JetInputOutputFormat<KeyIn, ValueIn>> createDataSource(DataStore<KeyIn, ValueIn> dataOutStore) {
    return createDataSource(dataOutStore, dataOutStore.newQuery());
  }

  public BatchSource<JetInputOutputFormat<KeyIn, ValueIn>> createDataSource(DataStore<KeyIn, ValueIn> dataOutStore,
                                                                            Query<KeyIn, ValueIn> query) {
    JetEngine.dataInStore = dataOutStore;
    JetEngine.query = query;
    BatchSource<JetInputOutputFormat<KeyIn, ValueIn>> source = Sources.batchFromProcessor("gora-jet-source",
        new JetSource<KeyIn, ValueIn>());
    return source;
  }

  public Sink<JetInputOutputFormat<KeyOut, ValueOut>> createDataSink(DataStore<KeyOut, ValueOut> dataOutStore) {
    JetEngine.dataOutStore = dataOutStore;
    Sink<JetInputOutputFormat<KeyOut, ValueOut>> sink = Sinks.fromProcessor("gora-jet-sink",
        new JetSink<KeyOut, ValueOut>());
    return sink;
  }
}
