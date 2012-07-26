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

import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Base class for Gora based {@link Reducer}s.
 */
public class GoraReducer<K1, V1, K2, V2 extends Persistent>
  extends Reducer<K1, V1, K2, V2> {
 
  /**
   * Initializes the Reducer, and sets output parameters for the job. 
   * @param job the job to set the properties for
   * @param dataStoreClass the datastore class
   * @param keyClass output key class
   * @param persistentClass output value class
   * @param reducerClass the reducer class extending GoraReducer
   * @param reuseObjects whether to reuse objects in serialization
   */
  public static <K1, V1, K2, V2 extends Persistent>
  void initReducerJob(
      Job job, 
      Class<? extends DataStore<K2,V2>> dataStoreClass,
      Class<K2> keyClass, 
      Class<V2> persistentClass,
      Class<? extends GoraReducer<K1, V1, K2, V2>> reducerClass, 
      boolean reuseObjects) {
    
    GoraOutputFormat.setOutput(job, dataStoreClass, keyClass, persistentClass, reuseObjects);
    
    job.setReducerClass(reducerClass);
  }
  
  /**
   * Initializes the Reducer, and sets output parameters for the job. 
   * @param job the job to set the properties for
   * @param dataStore the datastore as the output
   * @param reducerClass the reducer class extending GoraReducer
   */
  public static <K1, V1, K2, V2 extends Persistent>
  void initReducerJob(
      Job job, 
      DataStore<K2,V2> dataStore,
      Class<? extends GoraReducer<K1, V1, K2, V2>> reducerClass) {

    initReducerJob(job, dataStore, reducerClass, true);
  }

  /**
   * Initializes the Reducer, and sets output parameters for the job. 
   * @param job the job to set the properties for
   * @param dataStore the datastore as the output
   * @param reducerClass the reducer class extending GoraReducer
   * @param reuseObjects whether to reuse objects in serialization
   */
  public static <K1, V1, K2, V2 extends Persistent>
  void initReducerJob(
      Job job, 
      DataStore<K2,V2> dataStore,
      Class<? extends GoraReducer<K1, V1, K2, V2>> reducerClass, 
      boolean reuseObjects) {

    GoraOutputFormat.setOutput(job, dataStore, reuseObjects);
    job.setReducerClass(reducerClass);
  }
}
