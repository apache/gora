package org.apache.gora.hazelcastJet;

import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.BatchSource;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import org.apache.gora.hazelcastJet.generated.Pageview;
import org.apache.gora.hazelcastJet.generated.ResultPageView;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.util.GoraException;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.Functions.wholeItem;

public class HazelcastJetTest {

    @Test
    public void testNewJetSource() {

        Configuration conf =  new Configuration();

        try {
            dataStore = DataStoreFactory.getDataStore(Long.class, Pageview.class, conf);
        } catch (GoraException e) {
            e.printStackTrace();
        }

        try {
            dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, conf);
        } catch (GoraException e) {
            e.printStackTrace();
        }

        query = dataStore.newQuery();
        query.setStartKey(0L);
        query.setEndKey(55L);

        HazelcastJetEngine2<Long, Pageview, Long, ResultPageView> hazelcastJetEngine2 = new HazelcastJetEngine2<>();
        BatchSource<Pageview> fileSource = hazelcastJetEngine2.createDataSource(query);
        Pipeline p = Pipeline.create();
        p.drawFrom(fileSource)
                .filter(item -> item.getIp().toString().equals("88.240.129.183"))
                .map(e -> {
                    ResultPageView resultPageView = new ResultPageView();
                    resultPageView.setIp(e.getIp());
                    resultPageView.setTimestamp(e.getTimestamp());
                    resultPageView.setUrl(e.getUrl());
                    return new JetOutputFormat<Long, ResultPageView>(e.getTimestamp(), resultPageView);
                })
                .drainTo(hazelcastJetEngine2.createDataSink(dataStoreOut));


        JetInstance jet = Jet.newJetInstance();
        Jet.newJetInstance();
        try
        {
            jet.newJob(p).join();
//            IList<Pageview> results = jet.getList("results");
//
//            for (Pageview pageview: results) {
//                System.out.println(pageview);
//            }
        } finally

        {
            Jet.shutdownAll();
        }


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
    public void insertData() {
        try {
            dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, new Configuration());
        } catch (GoraException e) {
            e.printStackTrace();
        }

        ResultPageView resultPageView = new ResultPageView();
        resultPageView.setIp("123");
        resultPageView.setTimestamp(123L);
        resultPageView.setUrl("I am the the one");

        ResultPageView resultPageView1 = new ResultPageView();
        resultPageView1.setIp("123");
        resultPageView1.setTimestamp(123L);
        resultPageView1.setUrl("How are you");

        try {
            dataStoreOut.put(1L,resultPageView);
            dataStoreOut.put(2L,resultPageView1);
            dataStoreOut.flush();
        } catch (GoraException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void jetWordCount() {
        try {
            dataStoreOut = DataStoreFactory.getDataStore(Long.class, ResultPageView.class, new Configuration());
        } catch (GoraException e) {
            e.printStackTrace();
        }
        Query<Long, ResultPageView> query = dataStoreOut.newQuery();
        HazelcastJetEngine2<Long, ResultPageView, Long, ResultPageView> hazelcastJetEngine2 = new HazelcastJetEngine2<>();

        Pattern delimiter = Pattern.compile("\\W+");
        Pipeline p = Pipeline.create();
        p.drawFrom(hazelcastJetEngine2.createDataSource(query))
                .flatMap(e -> traverseArray(delimiter.split(e.getUrl().toString().toLowerCase())))
//                .filter(word -> !word.isEmpty())
                .groupingKey(wholeItem())
                .aggregate(counting())
                .drainTo(Sinks.map("COUNTS"));
        JetInstance jet =  Jet.newJetInstance();;
        try {
            System.out.print("\nCounting words... ");
            long start = System.nanoTime();
            jet.newJob(p).join();
//            System.out.print("done in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " milliseconds.");
            IMap<String, Long> counts = jet.getMap("COUNTS");
            if (counts.get("the") != 2) {
                throw new AssertionError("Wrong count of 'the'");
            }
            System.out.println("Count of 'the' is valid");
        } finally {
            Jet.shutdownAll();
        }

    }

    @Test
    public void testJet2() {
//        JetInstance jet = Jet.newJetInstance();
//        Jet.newJetInstance();
//
//        int upperBound = 10;
//        DAG dag = new DAG();
//        Vertex generateNumbers = dag.newVertex("generate-numbers",
//                new GoraJetMetaSupplier(upperBound));
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
    private static DataStore<Long, ResultPageView> dataStoreOut;
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