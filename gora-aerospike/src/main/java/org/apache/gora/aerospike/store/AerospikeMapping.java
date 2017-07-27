/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.aerospike.store;

import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to represent the Aerospike Mapping
 */
public class AerospikeMapping {
  private String namespace;

  private String set;

  private WritePolicy writePolicy;

  private Policy readPolicy;

  private Map<String, String> binMapping;

  public AerospikeMapping() {
    writePolicy = new WritePolicy();
    readPolicy = new Policy();
    binMapping = new HashMap<>();
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getSet() {
    return set;
  }

  public void setSet(String set) {
    this.set = set;
  }

  public WritePolicy getWritePolicy() {
    return writePolicy;
  }

  public void setWritePolicy(WritePolicy writePolicy) {
    this.writePolicy = writePolicy;
  }

  public Policy getReadPolicy() {
    return readPolicy;
  }

  public void setReadPolicy(Policy readPolicy) {
    this.readPolicy = readPolicy;
  }

  public Map<String, String> getBinMapping() {
    return binMapping;
  }

  public void setBinMapping(Map<String, String> binMapping) {
    this.binMapping = binMapping;
  }

  public String getBinName(String fieldName) {
    return binMapping.get(fieldName);
  }
}
