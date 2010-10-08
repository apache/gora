package org.gora.mapreduce;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.store.DataStore;

/**
 * Optional base class for gora based {@link Mapper}s.
 */
public class GoraMapper<K1, V1 extends Persistent, K2, V2>
extends Mapper<K1, V1, K2, V2> {

  @SuppressWarnings("rawtypes")
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(Job job, Query<K1,V1> query,
      DataStore<K1,V1> dataStore,
      Class<K2> outKeyClass, Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass,
      Class<? extends Partitioner> partitionerClass, boolean reuseObjects)
  throws IOException {
    //set the input via GoraInputFormat
    GoraInputFormat.setInput(job, query, dataStore, reuseObjects);

    job.setMapperClass(mapperClass);
    job.setMapOutputKeyClass(outKeyClass);
    job.setMapOutputValueClass(outValueClass);

    if (partitionerClass != null) {
      job.setPartitionerClass(partitionerClass);
    }
  }

  @SuppressWarnings({ "rawtypes" })
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(Job job, Query<K1,V1> query, DataStore<K1,V1> dataStore,
      Class<K2> outKeyClass, Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass, boolean reuseObjects)
  throws IOException {

    initMapperJob(job, query, dataStore, outKeyClass, outValueClass,
        mapperClass, null, reuseObjects);
  }


}
