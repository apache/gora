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
package org.apache.gora.shims.hadoop1;

import java.io.IOException;

import org.apache.gora.shims.hadoop.HadoopShim;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;

/**
 * Provides shim implementation for Hadoop 1.x.
 */
public class HadoopShim1 extends HadoopShim {

  public HadoopShim1() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  public Job createJob(Configuration configuration) throws IOException {
    return new Job(configuration);
  }

  /**
   * {@inheritDoc}
   */
  public JobContext createJobContext(Configuration configuration) {
    return new JobContext(configuration, null);
  }

}
