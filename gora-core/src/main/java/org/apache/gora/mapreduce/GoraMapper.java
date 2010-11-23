/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mapreduce;

import java.io.IOException;

import org.apache.gora.persistency.Persistent;
import org.apache.gora.query.Query;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;

/**
 * Base class for Gora based {@link Mapper}s.
 */
public class GoraMapper<K1, V1 extends Persistent, K2, V2>
  extends Mapper<K1, V1, K2, V2> {

  /**
   * Initializes the Mapper, and sets input parameters for the job. All of 
   * the records in the dataStore are used as the input. If you want to 
   * include a specific subset, use one of the overloaded methods which takes
   * query parameter.
   * @param job the job to set the properties for
   * @param dataStoreClass the datastore class
   * @param inKeyClass Map input key class
   * @param inValueClass Map input value class
   * @param outKeyClass Map output key class
   * @param outValueClass Map output value class
   * @param mapperClass the mapper class extending GoraMapper
   * @param partitionerClass optional partitioner class
   * @param reuseObjects whether to reuse objects in serialization
   */
  @SuppressWarnings("rawtypes")
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(
      Job job,
      Class<? extends DataStore<K1,V1>> dataStoreClass,
      Class<K1> inKeyClass, 
      Class<V1> inValueClass,
      Class<K2> outKeyClass, 
      Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass,
      Class<? extends Partitioner> partitionerClass, 
      boolean reuseObjects)
  throws IOException {
    
    //set the input via GoraInputFormat
    GoraInputFormat.setInput(job, dataStoreClass, inKeyClass, inValueClass, reuseObjects);

    job.setMapperClass(mapperClass);
    job.setMapOutputKeyClass(outKeyClass);
    job.setMapOutputValueClass(outValueClass);

    if (partitionerClass != null) {
      job.setPartitionerClass(partitionerClass);
    }
  }
  
  /**
   * Initializes the Mapper, and sets input parameters for the job. All of 
   * the records in the dataStore are used as the input. If you want to 
   * include a specific subset, use one of the overloaded methods which takes
   * query parameter.
   * @param job the job to set the properties for
   * @param dataStoreClass the datastore class
   * @param inKeyClass Map input key class
   * @param inValueClass Map input value class
   * @param outKeyClass Map output key class
   * @param outValueClass Map output value class
   * @param mapperClass the mapper class extending GoraMapper
   * @param reuseObjects whether to reuse objects in serialization
   */  
  @SuppressWarnings("rawtypes")
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(
      Job job,
      Class<? extends DataStore<K1,V1>> dataStoreClass,
      Class<K1> inKeyClass, 
      Class<V1> inValueClass,
      Class<K2> outKeyClass, 
      Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass,
      boolean reuseObjects)
  throws IOException {
    initMapperJob(job, dataStoreClass, inKeyClass, inValueClass, outKeyClass
        , outValueClass, mapperClass, null, reuseObjects);
  }
  
  /**
   * Initializes the Mapper, and sets input parameters for the job
   * @param job the job to set the properties for
   * @param query the query to get the inputs from
   * @param dataStore the datastore as the input
   * @param outKeyClass Map output key class
   * @param outValueClass Map output value class
   * @param mapperClass the mapper class extending GoraMapper
   * @param partitionerClass optional partitioner class
   * @param reuseObjects whether to reuse objects in serialization
   */
  @SuppressWarnings("rawtypes")
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(
      Job job, 
      Query<K1,V1> query,
      DataStore<K1,V1> dataStore,
      Class<K2> outKeyClass, 
      Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass,
      Class<? extends Partitioner> partitionerClass, 
      boolean reuseObjects)
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

  /**
   * Initializes the Mapper, and sets input parameters for the job
   * @param job the job to set the properties for
   * @param dataStore the datastore as the input
   * @param outKeyClass Map output key class
   * @param outValueClass Map output value class
   * @param mapperClass the mapper class extending GoraMapper
   * @param reuseObjects whether to reuse objects in serialization
   */
  @SuppressWarnings({ "rawtypes" })
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(
      Job job, 
      DataStore<K1,V1> dataStore,
      Class<K2> outKeyClass, 
      Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass, 
      boolean reuseObjects)
  throws IOException {
    initMapperJob(job, dataStore.newQuery(), dataStore, 
        outKeyClass, outValueClass, mapperClass, reuseObjects);
  }
  
  /**
   * Initializes the Mapper, and sets input parameters for the job
   * @param job the job to set the properties for
   * @param query the query to get the inputs from
   * @param dataStore the datastore as the input
   * @param outKeyClass Map output key class
   * @param outValueClass Map output value class
   * @param mapperClass the mapper class extending GoraMapper
   * @param reuseObjects whether to reuse objects in serialization
   */
  @SuppressWarnings({ "rawtypes" })
  public static <K1, V1 extends Persistent, K2, V2>
  void initMapperJob(
      Job job, 
      Query<K1,V1> query, 
      DataStore<K1,V1> dataStore,
      Class<K2> outKeyClass, 
      Class<V2> outValueClass,
      Class<? extends GoraMapper> mapperClass, 
      boolean reuseObjects)
  throws IOException {

    initMapperJob(job, query, dataStore, outKeyClass, outValueClass,
        mapperClass, null, reuseObjects);
  }
}
