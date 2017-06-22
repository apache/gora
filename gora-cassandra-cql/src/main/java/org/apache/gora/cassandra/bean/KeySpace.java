/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the Cassandra Keyspace.
 */
public class KeySpace {
  public String getName() {
    return name;
  }

  public boolean isDurableWritesEnabled() {
    return durableWritesEnabled;
  }

  public PlacementStrategy getPlacementStrategy() {
    return placementStrategy;
  }

  public int getReplicationFactor() {
    return replicationFactor;
  }

  public Map<String, Integer> getDataCenters() {
    return dataCenters;
  }

  public void addDataCenter(String key, Integer value) {
    this.dataCenters.put(key, value);
  }

  private String name;

  private Map<String, String> properties;

  private boolean durableWritesEnabled;

  public KeySpace() {
    this.properties = new HashMap<>();
  }

  public enum PlacementStrategy {
    SimpleStrategy,
    NetworkTopologyStrategy,
  }

  public void setPlacementStrategy(PlacementStrategy placementStrategy) {
    this.placementStrategy = placementStrategy;
    if(placementStrategy.equals(PlacementStrategy.NetworkTopologyStrategy) && this.dataCenters == null) {
      this.dataCenters = new HashMap<>();
    }
  }

  private PlacementStrategy placementStrategy;

  public void setReplicationFactor(int replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  private int replicationFactor;

  private Map<String, Integer> dataCenters;

  private List<String> tables;

  public void addProperty(String key, String value) {
    this.properties.put(key, value);
  }

  public String getProperty(String key) {
    return this.properties.get(key);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDurableWritesEnabled(boolean durableWritesEnabled) {
    this.durableWritesEnabled = durableWritesEnabled;
  }
}
