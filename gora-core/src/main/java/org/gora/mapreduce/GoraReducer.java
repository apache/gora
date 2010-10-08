package org.gora.mapreduce;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.gora.persistency.Persistent;
import org.gora.store.DataStore;

/**
 * Optional base class for gora based {@link Reducer}s.
 */
public class GoraReducer<K1, V1, K2, V2 extends Persistent>
extends Reducer<K1, V1, K2, V2> {

  public static <K1, V1, K2, V2 extends Persistent>
  void initReducerJob(Job job, DataStore<K2,V2> dataStore,
      Class<? extends GoraReducer<K1, V1, K2, V2>> reducerClass) {
    initReducerJob(job, dataStore, reducerClass, true);
  }

  public static <K1, V1, K2, V2 extends Persistent>
  void initReducerJob(Job job, DataStore<K2,V2> dataStore,
      Class<? extends GoraReducer<K1, V1, K2, V2>> reducerClass,
          boolean reuseObjects) {
    
    GoraOutputFormat.setOutput(job, dataStore, reuseObjects);
    
    job.setReducerClass(reducerClass);
  }
}
