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

package org.apache.gora.query;

import org.apache.gora.persistency.Persistent;

/**
 * PartitionQuery divides the results of the Query to multi partitions, so that 
 * queries can be run locally on the nodes that hold the data. PartitionQuery's are 
 * used for generating Hadoop InputSplits.
 */
public interface PartitionQuery<K, T extends Persistent> extends Query<K, T> {

  /* PartitionQuery interface relaxes the dependency of DataStores to Hadoop*/
  
  /**
   * Returns the locations on which this partial query will run locally.
   * @return the addresses of machines
   */
  public String[] getLocations();
  
}
