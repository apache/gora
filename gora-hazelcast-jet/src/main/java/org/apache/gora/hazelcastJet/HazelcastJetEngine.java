package org.apache.gora.hazelcastJet;

import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.SourceBuilder;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.GoraException;

import java.io.Serializable;


public class HazelcastJetEngine<KeyIn, ValueIn extends PersistentBase> {

    private Class<KeyIn> classKeyIn;
    private Class<ValueIn> classValueIn;
    private Query<KeyIn, ValueIn> query = null;

    private DataStore<KeyIn, ValueIn> dataStore;
    private Result<KeyIn, ValueIn> results;

    private static HazelcastJetEngine hazelcastJetEngine;

    public HazelcastJetEngine(Class<KeyIn> classKeyIn,
                           Class<ValueIn> classValueIn) {
        this.classKeyIn = classKeyIn;
        this.classValueIn = classValueIn;
        this.hazelcastJetEngine = this;
    }

    public BatchSource<ValueIn> createDataSource(DataStore<KeyIn, ValueIn> dataStore,
                                                 Query<KeyIn, ValueIn> query) {
        this.dataStore = dataStore;
//        this.query = query;

//        final Result<KeyIn, ValueIn> results;
        BatchSource<ValueIn> fileSource = null;
        try {
            results = query.execute();
            fileSource = SourceBuilder
                    .batch("source", x ->
                            new RecordProvider<KeyIn, ValueIn>())
                    .fillBufferFn(RecordProvider<KeyIn, ValueIn>::fillBuffer)
                    .destroyFn(RecordProvider::close)
                    .build();
        } catch (GoraException e) {
            e.printStackTrace();
        }
        return fileSource;
    }

    private static class RecordProvider<KeyIn, ValueIn extends PersistentBase> {

//        private DataStore<KeyIn, ValueIn> dataStore;
//        private Result<KeyIn, ValueIn> results;

//        public RecordProvider(DataStore<KeyIn, ValueIn> dataStore, Result<KeyIn, ValueIn> results) {
//            this.dataStore = dataStore;
//            this.results = results;
//        }

        void fillBuffer(SourceBuilder.SourceBuffer<ValueIn> buf) {
            System.out.println("buf");

            try {
                if (hazelcastJetEngine.results.next()) {
                    ValueIn result = (ValueIn) hazelcastJetEngine.results.get();
                    buf.add(result);
                } else {
                    buf.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void close() {
            hazelcastJetEngine.dataStore.close();
        }
    }
}


