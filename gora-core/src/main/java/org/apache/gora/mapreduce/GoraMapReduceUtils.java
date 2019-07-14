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
import java.util.List;

import org.apache.gora.util.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.task.JobContextImpl;

/**
 * MapReduce related utilities for Gora
 */
public class GoraMapReduceUtils {

  public static class HelperInputFormat<K,V> extends FileInputFormat<K, V> {
    @Override
    public RecordReader<K, V> createRecordReader(InputSplit arg0,
        TaskAttemptContext arg1) throws IOException, InterruptedException {
      return null;
    }
  }
  
  /**
   * Add our own serializer (obtained via the {@link PersistentSerialization} 
   * wrapper) to any other <code>io.serializations</code> which may be specified 
   * within existing Hadoop configuration.
   * 
   * @param conf the Hadoop configuration object
   * @param reuseObjects boolean parameter to reuse objects
   */
  public static void setIOSerializations(Configuration conf, boolean reuseObjects) {
    String serializationClass =
      PersistentSerialization.class.getCanonicalName();
    String[] serializations = StringUtils.joinStringArrays(
        conf.getStrings("io.serializations"),
        "org.apache.hadoop.io.serializer.WritableSerialization",
        StringSerialization.class.getCanonicalName(),
        serializationClass); 
    conf.setStrings("io.serializations", serializations);
  }  
  
  public static List<InputSplit> getSplits(Configuration conf, String inputPath) 
    throws IOException {
    JobContext context = createJobContext(conf, inputPath);
    
    HelperInputFormat<?,?> inputFormat = new HelperInputFormat<Object,Object>();
    return inputFormat.getSplits(context);
  }
  
  public static JobContext createJobContext(Configuration conf, String inputPath) 
    throws IOException {
    
    if(inputPath != null) {
      Job job = Job.getInstance(conf);
      FileInputFormat.addInputPath(job, new Path(inputPath));
      return new JobContextImpl(job.getConfiguration(), null);
    } 
    
    return new JobContextImpl(conf, null);
  }
}
