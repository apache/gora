/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.spark;

import java.io.IOException;

import org.apache.gora.io.serializer.GoraInputFormat;
import org.apache.gora.io.serializer.GoraMapReduceUtils;
import org.apache.gora.io.serializer.GoraOutputFormat;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * Base class for Gora - Spark integration.
 */
public class GoraSparkEngine<K, V extends Persistent> {
  Class<K> clazzK;
  Class<V> clazzV;

  public GoraSparkEngine(Class<K> clazzK, Class<V> clazzV) {
    this.clazzK = clazzK;
    this.clazzV = clazzV;
  }

  /**
   * Initializes a {@link JavaPairRDD} from given Spark context, Hadoop
   * configuration and data store.
   * 
   * @param sparkContext
   *          Spark context
   * @param conf
   *          Hadoop configuration
   * @param dataStore
   *          Data store
   * @return initialized rdd
   */
  public JavaPairRDD<K, V> initialize(JavaSparkContext sparkContext,
      Configuration conf, DataStore<K, V> dataStore) {
    GoraMapReduceUtils.setIOSerializations(conf, true);

    try {
      IOUtils
          .storeToConf(dataStore.newQuery(), conf, GoraInputFormat.QUERY_KEY);
    } catch (IOException ioex) {
      throw new RuntimeException(ioex.getMessage());
    }

    return sparkContext.newAPIHadoopRDD(conf, GoraInputFormat.class, clazzK,
        clazzV);
  }

  /**
   * Initializes a {@link JavaPairRDD} from given Spark context and data store.
   * If given data store is {@link Configurable} and has not a configuration
   * than a Hadoop configuration is created otherwise existed configuration is
   * used.
   * 
   * @param sparkContext
   *          Spark context
   * @param dataStore
   *          Data store
   * @return initialized rdd
   */
  public JavaPairRDD<K, V> initialize(JavaSparkContext sparkContext,
      DataStore<K, V> dataStore) {
    Configuration hadoopConf;

    if ((dataStore instanceof Configurable)
        && ((Configurable) dataStore).getConf() != null) {
      hadoopConf = ((Configurable) dataStore).getConf();
    } else {
      hadoopConf = new Configuration();
    }

    return initialize(sparkContext, hadoopConf, dataStore);
  }

  /**
   * Creates a job and sets the output parameters for the conf that Spark will use
   *
   * @param dataStore the datastore as the output
   * @return a populated output {@link org.apache.hadoop.conf.Configuration} 
   * @throws IOException if there is an error creating the configuration
   */
    public <K, V extends Persistent> Configuration generateOutputConf(DataStore<K, V> dataStore)
       throws IOException {
      Configuration hadoopConf;

      if ((dataStore instanceof Configurable)
              && ((Configurable) dataStore).getConf() != null) {
        hadoopConf = ((Configurable) dataStore).getConf();
      } else {
        hadoopConf = new Configuration();
      }

      GoraMapReduceUtils.setIOSerializations(hadoopConf, true);
      Job job = Job.getInstance(hadoopConf);

      return generateOutputConf(job, dataStore.getClass(), dataStore.getKeyClass(),
           dataStore.getPersistentClass());
    }

  /**
   * Sets the output parameters for the conf that Spark will use
   *
   * @param job the job to set the properties for
   * @param dataStore the datastore as the output
   * @return a populated output {@link org.apache.hadoop.conf.Configuration} 
   */
    public <K, V extends Persistent> Configuration generateOutputConf(Job job,
        DataStore<K, V> dataStore) {
      return generateOutputConf(job, dataStore.getClass(), dataStore.getKeyClass(),
              dataStore.getPersistentClass());
    }

  /**
   * Sets the output parameters for the conf that Spark will use
   *
   * @param job             the job to set the properties for
   * @param dataStoreClass  the datastore class
   * @param keyClass        output key class
   * @param persistentClass output value class
   * @return a populated output {@link org.apache.hadoop.conf.Configuration} 
   */
    @SuppressWarnings("rawtypes")
    public <K, V extends Persistent> Configuration generateOutputConf(Job job,
        Class<? extends DataStore> dataStoreClass,
        Class<K> keyClass, Class<V> persistentClass) {

      job.setOutputFormatClass(GoraOutputFormat.class);
      job.setOutputKeyClass(keyClass);
      job.setOutputValueClass(persistentClass);

      job.getConfiguration().setClass(GoraOutputFormat.DATA_STORE_CLASS, dataStoreClass,
              DataStore.class);
      job.getConfiguration().setClass(GoraOutputFormat.OUTPUT_KEY_CLASS, keyClass, Object.class);
      job.getConfiguration().setClass(GoraOutputFormat.OUTPUT_VALUE_CLASS,
              persistentClass, Persistent.class);
      return job.getConfiguration();
    }
}
