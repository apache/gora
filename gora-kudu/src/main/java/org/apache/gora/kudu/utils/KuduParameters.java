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

  public KuduParameters(String masterAddresses) {
    this.masterAddresses = masterAddresses;
  }

  public String getMasterAddresses() {
    return masterAddresses;
  }

  public void setMasterAddresses(String masterAddresses) {
    this.masterAddresses = masterAddresses;
  }

  public Integer getBossCount() {
    return bossCount;
  }

  public void setBossCount(Integer bossCount) {
    this.bossCount = bossCount;
  }

  public Long getDefaultAdminOperationTimeoutMs() {
    return defaultAdminOperationTimeoutMs;
  }

  public void setDefaultAdminOperationTimeoutMs(Long defaultAdminOperationTimeoutMs) {
    this.defaultAdminOperationTimeoutMs = defaultAdminOperationTimeoutMs;
  }

  public Long getDefaultOperationTimeoutMs() {
    return defaultOperationTimeoutMs;
  }

  public void setDefaultOperationTimeoutMs(Long defaultOperationTimeoutMs) {
    this.defaultOperationTimeoutMs = defaultOperationTimeoutMs;
  }

  public Long getDefaultSocketReadTimeoutMs() {
    return defaultSocketReadTimeoutMs;
  }

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

  public void setWorkerCount(Integer workerCount) {
    this.workerCount = workerCount;
  }

  /**
   * Reads Kudu parameters from a properties list
   *
   * @param properties Properties list
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
    return kuduParameters;
  }

}
