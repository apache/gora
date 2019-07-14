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

package org.apache.gora.dynamodb.query;

/**
 * Class abstracting a composed DynamoDB key.
 * @param <H>
 * @param <R>
 */
public class DynamoDBKey<H, R> {  
  /**
   * Hash key used for a specific table 
   */
  private H hashKey;

  /**
   * Range key used for a specific table
   */
  private R rangeKey;

  /**
   * Gets hash key
   * @return
   */
  public H getHashKey() {
    return hashKey;
  }

  /**
   * Sets hash key
   * @param hashKey
   */
  public void setHashKey(H hashKey) {
    this.hashKey = hashKey;
  }

  /**
   * Gets range key
   * @return
   */
  public R getRangeKey() {
    return rangeKey;
  }

  /**
   * Sets range key
   * @param rangeKey
   */
  public void setRangeKey(R rangeKey) {
    this.rangeKey = rangeKey;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[').append(hashKey != null? hashKey.toString():":");
    sb.append(rangeKey != null? ":" + rangeKey.toString():"");
    sb.append(']');
    return sb.toString();
  }
}
