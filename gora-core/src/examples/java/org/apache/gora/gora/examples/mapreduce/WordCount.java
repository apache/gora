
package org.gora.examples.mapreduce;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.gora.examples.generated.TokenDatum;
import org.gora.examples.generated.WebPage;
import org.gora.mapreduce.GoraMapper;
import org.gora.mapreduce.GoraReducer;
import org.gora.query.Query;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;

/**
 * Classic word count example in Gora.
 */
public class WordCount extends Configured implements Tool {

  public WordCount() {
    
  }
  
  public WordCount(Configuration conf) {
    setConf(conf);
  }
  
  /**
   * TokenizerMapper takes &lt;String, WebPage&gt; pairs as obtained 
   * from the input DataStore, and tokenizes the content via 
   * {@link WebPage#getContent()}. The tokens are emitted as 
   * &lt;String, WebPage&gt; pairs.
   */
  public static class TokenizerMapper 
    extends GoraMapper<String, WebPage, Text, IntWritable> {
    
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
    
    @Override
    protected void map(String key, WebPage page, Context context) 
      throws IOException ,InterruptedException {
      
      //Get the content from a WebPage as obtained from the DataStore
      String content =  new String(page.getContent().array());
      
      StringTokenizer itr = new StringTokenizer(content);
      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      }
    };
  }
  
  public static class WordCountReducer extends GoraReducer<Text, IntWritable, 
  String, TokenDatum> {
    
    TokenDatum result = new TokenDatum();
    
    @Override
    protected void reduce(Text key, Iterable<IntWritable> values, Context context) 
      throws IOException ,InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.setCount(sum);
      context.write(key.toString(), result);
    };
    
  }
  
  /**
   * Creates and returns the {@link Job} for submitting to Hadoop mapreduce.
   * @param inStore
   * @param query
   * @return
   * @throws IOException
   */
  public Job createJob(DataStore<String,WebPage> inStore, Query<String,WebPage> query
      , DataStore<String,TokenDatum> outStore) throws IOException {
    Job job = new Job(getConf());
   
    job.setJobName("WordCount");
    
    job.setNumReduceTasks(10);
    job.setJarByClass(getClass());
    
    /* Mappers are initialized with GoraMapper#initMapper().
     * Instead of the TokenizerMapper defined here, if the input is not 
     * obtained via Gora, any other mapper can be used, such as 
     * Hadoop-MapReduce's WordCount.TokenizerMapper.
     */
    GoraMapper.initMapperJob(job, query, inStore, Text.class
        , IntWritable.class, TokenizerMapper.class, true);
    
    /* Reducers are initialized with GoraReducer#initReducer().
     * If the output is not to be persisted via Gora, any reducer 
     * can be used instead.
     */
    GoraReducer.initReducerJob(job, outStore, WordCountReducer.class);
    
    //TODO: set combiner
    
    return job;
  }
  
  public int wordCount(DataStore<String,WebPage> inStore, 
      DataStore<String, TokenDatum> outStore) throws IOException, InterruptedException, ClassNotFoundException {
    Query<String,WebPage> query = inStore.newQuery();
    
    Job job = createJob(inStore, query, outStore);
    return job.waitForCompletion(true) ? 0 : 1;
  }
  
  @Override
  public int run(String[] args) throws Exception {
    
    DataStore<String,WebPage> inStore;
    DataStore<String, TokenDatum> outStore;
    
    if(args.length > 0) {
      String dataStoreClass = args[0];
      inStore = DataStoreFactory.getDataStore(dataStoreClass, 
          String.class, WebPage.class);
      if(args.length > 1) {
        dataStoreClass = args[1];
      }
      outStore = DataStoreFactory.getDataStore(dataStoreClass, 
          String.class, TokenDatum.class);
    } else {
      inStore = DataStoreFactory.getDataStore(String.class, WebPage.class);
      outStore = DataStoreFactory.getDataStore(String.class, TokenDatum.class);
    }
    
    return wordCount(inStore, outStore);
  }
  
  // Usage WordCount [<input datastore class> [output datastore class]]
  public static void main(String[] args) throws Exception {
    int ret = ToolRunner.run(new WordCount(), args);
    System.exit(ret);
  }
  
}
