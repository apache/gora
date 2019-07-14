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

  public BatchSource<JetOutputFormat<KeyIn, ValueIn>> createDataSource(DataStore<KeyIn, ValueIn> dataStore) {
    return createDataSource(dataStore.newQuery());
  }

  public BatchSource<JetOutputFormat<KeyIn, ValueIn>> createDataSource(Query<KeyIn, ValueIn> query) {
    BatchSource<JetOutputFormat<KeyIn, ValueIn>> source = Sources.batchFromProcessor("gora-jet-source",
        new JetSource<KeyIn, ValueIn>(query));
    return source;
  }

  public Sink<JetOutputFormat<KeyOut, ValueOut>> createDataSink(DataStore<KeyOut, ValueOut> dataOutStore) {
    JetEngine.dataOutStore = dataOutStore;
    Sink<JetOutputFormat<KeyOut, ValueOut>> sink = Sinks.fromProcessor("gora-jet-sink",
        new JetSink<KeyOut, ValueOut>());
    return sink;
  }
}
