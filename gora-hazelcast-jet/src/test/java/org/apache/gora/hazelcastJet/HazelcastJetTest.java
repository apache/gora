package org.apache.gora.hazelcastJet;

import com.hazelcast.core.IList;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.core.processor.DiagnosticProcessors;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import org.apache.gora.hazelcastJet.generated.Pageview;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

public class HazelcastJetTest {

    @Test
    public void testJet() {
//        Pipeline p = Pipeline.create();
//        p.drawFrom(Sources.batchFromProcessor("gora", new GenerateNumbersPMetaSupplier(10)))
//                .drainTo(Sinks.list("results"));
//
//        JetInstance jet = Jet.newJetInstance();
//        try
//        {
//            jet.newJob(p).join();
//            IList<Object> results = jet.getList("results");
//
//            for (Object object: results) {
//                System.out.println(object);
//            }
//        } finally
//
//        {
//            Jet.shutdownAll();
//        }


//        JetInstance jet = Jet.newJetInstance();
//
//        int upperBound = 10;
//        DAG dag = new DAG();
//        Vertex generateNumbers = dag.newVertex("generate-numbers",
//                () -> new HazelcastJetEngine(upperBound));
//        Vertex logInput = dag.newVertex("log-input",
//                DiagnosticProcessors.writeLoggerP(i -> "Received number: " + i));
//        dag.edge(Edge.between(generateNumbers,logInput));
//
//        try
//        {
//            jet.newJob(dag).join();
//        } finally
//
//        {
//            Jet.shutdownAll();
//        }
    }

    @Test
    public void testJet2() {
//        JetInstance jet = Jet.newJetInstance();
//        Jet.newJetInstance();
//
//        int upperBound = 10;
//        DAG dag = new DAG();
//        Vertex generateNumbers = dag.newVertex("generate-numbers",
//                new GenerateNumbersPMetaSupplier(upperBound));
//        Vertex logInput = dag.newVertex("log-input",
//                DiagnosticProcessors.writeLoggerP(i -> "Received number: " + i));
//        dag.edge(Edge.between(generateNumbers, logInput));
//        try
//        {
//            jet.newJob(dag).join();
//        } finally
//
//        {
//            Jet.shutdownAll();
//        }
    }

    private static DataStore<Long, Pageview> dataStore;
    private static final String LIST_NAME = "page-views";
    static Query<Long, Pageview> query = null;


    @Test
    public void testJetSource() {

        System.out.println("ok");
        try {
            dataStore = DataStoreFactory.getDataStore(Long.class, Pageview.class,
                    new Configuration());
        } catch (GoraException e) {
            e.printStackTrace();
        }
        query = dataStore.newQuery();
        query.setStartKey(0L);
        query.setEndKey(55L);

        IList<Pageview> list;
        try {
            JetInstance jet = startJet();
            list = jet.getList(LIST_NAME);
            runPipeline(jet);

            System.out.println("printing " + list.size());

            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i));
            }
        } finally {
            System.out.println("shutting down");
            Jet.shutdownAll();
        }
    }

    private static JetInstance startJet() {
        System.out.println("Creating Jet instance");
        return Jet.newJetInstance();
    }

    private static void runPipeline(JetInstance jet) {
        System.out.println("\nRunning the pipeline ");
        Pipeline p = buildPipeline();
        jet.newJob(p).join();
    }

    private static Pipeline buildPipeline() {
        HazelcastJetEngine<Long, Pageview> hazelcastJetEngine = new HazelcastJetEngine<>(Long.class, Pageview.class);

        BatchSource<Pageview> fileSource = hazelcastJetEngine.createDataSource(dataStore, query);
        Pipeline p = Pipeline.create();
        p.drawFrom(fileSource)
                .filter(item -> item.getIp().toString().equals("88.240.129.183"))
                .drainTo(Sinks.list(LIST_NAME));
        return p;
    }
}