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
package org.apache.gora.tutorial.log.generated;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.StateManager;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.persistency.impl.StateManagerImpl;

@SuppressWarnings("all")
public class MetricDatum extends PersistentBase {
  public static final Schema _SCHEMA = Schema.parse("{\"type\":\"record\",\"name\":\"MetricDatum\",\"namespace\":\"org.apache.gora.tutorial.log.generated\",\"fields\":[{\"name\":\"metricDimension\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"metric\",\"type\":\"long\"}]}");
  public static enum Field {
    METRIC_DIMENSION(0,"metricDimension"),
    TIMESTAMP(1,"timestamp"),
    METRIC(2,"metric"),
    ;
    private int index;
    private String name;
    Field(int index, String name) {this.index=index;this.name=name;}
    public int getIndex() {return index;}
    public String getName() {return name;}
    public String toString() {return name;}
  };
  public static final String[] _ALL_FIELDS = {"metricDimension","timestamp","metric",};
  static {
    PersistentBase.registerFields(MetricDatum.class, _ALL_FIELDS);
  }
  private Utf8 metricDimension;
  private long timestamp;
  private long metric;
  public MetricDatum() {
    this(new StateManagerImpl());
  }
  public MetricDatum(StateManager stateManager) {
    super(stateManager);
  }
  public MetricDatum newInstance(StateManager stateManager) {
    return new MetricDatum(stateManager);
  }
  public Schema getSchema() { return _SCHEMA; }
  public Object get(int _field) {
    switch (_field) {
    case 0: return metricDimension;
    case 1: return timestamp;
    case 2: return metric;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  @SuppressWarnings(value="unchecked")
  public void put(int _field, Object _value) {
    if(isFieldEqual(_field, _value)) return;
    getStateManager().setDirty(this, _field);
    switch (_field) {
    case 0:metricDimension = (Utf8)_value; break;
    case 1:timestamp = (Long)_value; break;
    case 2:metric = (Long)_value; break;
    default: throw new AvroRuntimeException("Bad index");
    }
  }
  public Utf8 getMetricDimension() {
    return (Utf8) get(0);
  }
  public void setMetricDimension(Utf8 value) {
    put(0, value);
  }
  public long getTimestamp() {
    return (Long) get(1);
  }
  public void setTimestamp(long value) {
    put(1, value);
  }
  public long getMetric() {
    return (Long) get(2);
  }
  public void setMetric(long value) {
    put(2, value);
  }
}
