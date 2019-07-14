/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.kudu.utils;

import java.util.Properties;
import org.apache.hadoop.conf.Configuration;

/**
 * Parameters definitions for Kudu.
 */
public class KuduParameters {

  private String masterAddresses;
  private Integer bossCount;
  private Long defaultAdminOperationTimeoutMs;
  private Long defaultOperationTimeoutMs;
  private Long defaultSocketReadTimeoutMs;
  private Boolean clientStatistics;
  private Integer workerCount;
  private String flushMode;
  private Integer flushInterval;

  public KuduParameters(String masterAddresses) {
    this.masterAddresses = masterAddresses;
  }

  public String getMasterAddresses() {
    return masterAddresses;
  }

  /**
   * Set list of masters
   *
   * @param masterAddresses comma-separated list of "host:port" pairs of the
   * masters
   */
  public void setMasterAddresses(String masterAddresses) {
    this.masterAddresses = masterAddresses;
  }

  public Integer getBossCount() {
    return bossCount;
  }

  /**
   * Set the maximum number of boss threads. Optional. If not provided, 1 is
   * used.
   *
   * @param bossCount
   */
  public void setBossCount(Integer bossCount) {
    this.bossCount = bossCount;
  }

  public Long getDefaultAdminOperationTimeoutMs() {
    return defaultAdminOperationTimeoutMs;
  }

  /**
   * Sets the default timeout used for administrative operations (e.g.
   * createTable, deleteTable, etc). Optional. If not provided, defaults to 30s.
   * A value of 0 disables the timeout.
   *
   * @param defaultAdminOperationTimeoutMs a timeout in milliseconds
   */
  public void setDefaultAdminOperationTimeoutMs(Long defaultAdminOperationTimeoutMs) {
    this.defaultAdminOperationTimeoutMs = defaultAdminOperationTimeoutMs;
  }

  public Long getDefaultOperationTimeoutMs() {
    return defaultOperationTimeoutMs;
  }

  /**
   * Sets the default timeout used for user operations (using sessions and
   * scanners). Optional. If not provided, defaults to 30s. A value of 0
   * disables the timeout.
   *
   * @param defaultOperationTimeoutMs a timeout in milliseconds
   */
  public void setDefaultOperationTimeoutMs(Long defaultOperationTimeoutMs) {
    this.defaultOperationTimeoutMs = defaultOperationTimeoutMs;
  }

  public Long getDefaultSocketReadTimeoutMs() {
    return defaultSocketReadTimeoutMs;
  }

  /**
   * Sets the default timeout to use when waiting on data from a socket.
   * Optional. If not provided, defaults to 10s. A value of 0 disables the
   * timeout.
   *
   * @param defaultSocketReadTimeoutMs a timeout in milliseconds
   *
   */
  public void setDefaultSocketReadTimeoutMs(Long defaultSocketReadTimeoutMs) {
    this.defaultSocketReadTimeoutMs = defaultSocketReadTimeoutMs;
  }

  public Boolean isClientStatistics() {
    return clientStatistics;
  }

  public void setClientStatistics(Boolean clientStatistics) {
    this.clientStatistics = clientStatistics;
  }

  public Integer getWorkerCount() {
    return workerCount;
  }

  /**
   * Set the maximum number of worker threads. Optional. If not provided, (2 *
   * the number of available processors) is used.
   *
   * @param workerCount
   */
  public void setWorkerCount(Integer workerCount) {
    this.workerCount = workerCount;
  }

  public String getFlushMode() {
    return flushMode;
  }

  /**
   * Set the new flush mode for this session.
   *
   * @see
   * https://kudu.apache.org/apidocs/org/apache/kudu/client/SessionConfiguration.FlushMode.html
   *
   * @param flushMode flush mode
   */
  public void setFlushMode(String flushMode) {
    this.flushMode = flushMode;
  }

  public Integer getFlushInterval() {
    return flushInterval;
  }

  /**
   * Set the flush interval, which will be used for the next scheduling
   * decision.
   *
   * @param flushInterval interval in milliseconds.
   */
  public void setFlushInterval(Integer flushInterval) {
    this.flushInterval = flushInterval;
  }

  /**
   * Reads Kudu parameters from a properties list
   *
   * @param properties Properties list
   * @param conf Configuration object used for overriding parameters on runtime
   * (For setting testing environment)
   * @return Kudu parameters instance
   */
  public static KuduParameters load(Properties properties, Configuration conf) {
    KuduParameters kuduParameters;
    if (!Boolean.parseBoolean(properties.getProperty(KuduBackendConstants.AS_PROP_OVERRIDING))) {
      kuduParameters = new KuduParameters(conf.get(KuduBackendConstants.PROP_MASTERADDRESSES, KuduBackendConstants.DEFAULT_KUDU_MASTERS));
    } else {
      kuduParameters = new KuduParameters(properties.getProperty(KuduBackendConstants.PROP_MASTERADDRESSES, KuduBackendConstants.DEFAULT_KUDU_MASTERS));
    }
    String aProperty = properties.getProperty(KuduBackendConstants.PROP_BOSSCOUNT);
    if (aProperty != null) {
      kuduParameters.setBossCount(Integer.parseInt(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_CLIENTSTATISTICS);
    if (aProperty != null) {
      kuduParameters.setClientStatistics(Boolean.parseBoolean(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_DEFAULTADMINOPERATIONTIMEOUTMS);
    if (aProperty != null) {
      kuduParameters.setDefaultAdminOperationTimeoutMs(Long.parseLong(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_DEFAULTOPERATIONTIMEOUTMS);
    if (aProperty != null) {
      kuduParameters.setDefaultOperationTimeoutMs(Long.parseLong(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_DEFAULTSOCKETREADTIMEOUTMS);
    if (aProperty != null) {
      kuduParameters.setDefaultSocketReadTimeoutMs(Long.parseLong(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_WORKERCOUNT);
    if (aProperty != null) {
      kuduParameters.setWorkerCount(Integer.parseInt(aProperty));
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_FLUSHMODE);
    if (aProperty != null) {
      kuduParameters.setFlushMode(aProperty);
    }
    aProperty = properties.getProperty(KuduBackendConstants.PROP_FLUSHINTERVAL);
    if (aProperty != null) {
      kuduParameters.setFlushInterval(Integer.parseInt(aProperty));
    }
    return kuduParameters;
  }

}
