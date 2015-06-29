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

import org.apache.gora.mapreduce.GoraInputFormat;
import org.apache.gora.mapreduce.GoraMapReduceUtils;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.store.DataStore;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * Base class for Spark integration
 */
public class GoraSpark<K, V extends Persistent> {
  Class<K> clazzK;
  Class<V> clazzV;

  public GoraSpark(Class<K> clazzK, Class<V> clazzV) {
    this.clazzK = clazzK;
    this.clazzV = clazzV;
  }

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

  public JavaPairRDD<K, V> initialize(JavaSparkContext sparkContext,
      DataStore<K, V> dataStore) {
    Configuration hadoopConf;

      if ((dataStore instanceof Configurable) && ((Configurable) dataStore).getConf() != null) {
          hadoopConf = ((Configurable) dataStore).getConf();
      } else {
          hadoopConf = new Configuration();
      }

      GoraMapReduceUtils.setIOSerializations(hadoopConf, true);

    try {
      IOUtils.storeToConf(dataStore.newQuery(), hadoopConf,
              GoraInputFormat.QUERY_KEY);
    } catch (IOException ioex) {
      throw new RuntimeException(ioex.getMessage());
    }

    return sparkContext.newAPIHadoopRDD(hadoopConf, GoraInputFormat.class,
        clazzK, clazzV);
  }
}
