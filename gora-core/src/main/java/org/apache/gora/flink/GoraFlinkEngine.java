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

package org.apache.gora.flink;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.hadoop.mapreduce.HadoopInputFormat;
import org.apache.flink.api.java.hadoop.mapreduce.HadoopOutputFormat;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Preconditions;
import org.apache.gora.io.serializer.GoraInputFormat;
import org.apache.gora.io.serializer.GoraOutputFormat;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Core class which handles Gora - Flink Engine integration.
 */
public class GoraFlinkEngine<KeyIn, ValueIn
        extends PersistentBase, KeyOut, ValueOut extends PersistentBase> {

  private Class<KeyIn> classKeyIn;
  private Class<ValueIn> classValueIn;
  private Class<KeyOut> classKeyOut;
  private Class<ValueOut> classValueOut;

  public GoraFlinkEngine(Class<KeyIn> classKeyIn,
                         Class<ValueIn> classValueIn) {
    this.classKeyIn = classKeyIn;
    this.classValueIn = classValueIn;
  }

  public GoraFlinkEngine(Class<KeyIn> classKeyIn,
                         Class<ValueIn> classValueIn,
                         Class<KeyOut> classKeyOut,
                         Class<ValueOut> classValueOut) {
    this.classKeyIn = classKeyIn;
    this.classValueIn = classValueIn;
    this.classKeyOut = classKeyOut;
    this.classValueOut = classValueOut;
  }

  public DataSource<Tuple2<KeyIn, ValueIn>> createDataSource(ExecutionEnvironment env,
                                                             Configuration conf,
                                                             Class<? extends DataStore<KeyIn, ValueIn>> dataStoreClass)
          throws IOException {
    Preconditions.checkNotNull(classKeyIn);
    Preconditions.checkNotNull(classValueIn);
    Job job = Job.getInstance(conf);
    DataStore<KeyIn, ValueIn> dataStore = DataStoreFactory.getDataStore(dataStoreClass
            , classKeyIn, classValueIn, job.getConfiguration());
    GoraInputFormat.setInput(job, dataStore.newQuery(), true);
    HadoopInputFormat<KeyIn, ValueIn> wrappedGoraInput =
            new HadoopInputFormat<>(new GoraInputFormat<>(),
                    classKeyIn, classValueIn, job);
    return env.createInput(wrappedGoraInput);
  }

  public DataSource<Tuple2<KeyIn, ValueIn>> createDataSource(ExecutionEnvironment env,
                                                             Configuration conf,
                                                             DataStore<KeyIn, ValueIn> dataStore)
          throws IOException {
    Preconditions.checkNotNull(classKeyIn);
    Preconditions.checkNotNull(classValueIn);
    Job job = Job.getInstance(conf);
    GoraInputFormat.setInput(job, dataStore.newQuery(), true);
    HadoopInputFormat<KeyIn, ValueIn> wrappedGoraInput =
            new HadoopInputFormat<>(new GoraInputFormat<>(),
                    classKeyIn, classValueIn, job);
    return env.createInput(wrappedGoraInput);
  }

  public OutputFormat<Tuple2<KeyOut, ValueOut>> createDataSink(Configuration conf,
                                                               DataStore<KeyOut, ValueOut> dataStore)
          throws IOException {
    Preconditions.checkNotNull(classKeyOut);
    Preconditions.checkNotNull(classValueOut);
    Job job = Job.getInstance(conf);
    GoraOutputFormat.setOutput(job, dataStore, true);
    HadoopOutputFormat<KeyOut, ValueOut> wrappedGoraOutput =
            new HadoopOutputFormat<>(
                    new GoraOutputFormat<>(), job);
    // Temp fix to prevent NullPointerException from Flink side.
    Path tempPath = Files.createTempDirectory("temp");
    job.getConfiguration().set("mapred.output.dir", tempPath.toAbsolutePath().toString());
    return wrappedGoraOutput;

  }

  public OutputFormat<Tuple2<KeyOut, ValueOut>> createDataSink(Configuration conf,
                                                               Class<? extends DataStore<KeyOut, ValueOut>> dataStoreClass)
          throws IOException {
    Preconditions.checkNotNull(classKeyOut);
    Preconditions.checkNotNull(classValueOut);
    Job job = Job.getInstance(conf);
    DataStore<KeyOut, ValueOut> dataStore = DataStoreFactory.getDataStore(dataStoreClass
            , classKeyOut, classValueOut, job.getConfiguration());
    GoraOutputFormat.setOutput(job, dataStore, true);
    HadoopOutputFormat<KeyOut, ValueOut> wrappedGoraOutput =
            new HadoopOutputFormat<>(
                    new GoraOutputFormat<>(), job);
    // Temp fix to prevent NullPointerException from Flink side.
    Path tempPath = Files.createTempDirectory("temp");
    job.getConfiguration().set("mapred.output.dir", tempPath.toAbsolutePath().toString());
    return wrappedGoraOutput;
  }

}
