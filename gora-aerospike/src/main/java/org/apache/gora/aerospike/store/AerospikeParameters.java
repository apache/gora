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

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Properties;

public class AerospikeParameters {
  private String host;

  private int port;

  private String user;

  private String password;

  private AerospikeMapping aerospikeMapping;

  private boolean isSingleBinEnabled;

  // Property names
  private static final String AS_SERVER_IP = "gora.aerospikestore.server.ip";

  private static final String AS_SERVER_PORT = "gora.aerospikestore.server.port";

  // Default property values
  private static final String DEFAULT_SERVER_IP = "localhost";

  private static final String DEFAULT_SERVER_PORT = "3000";

  private static final Logger LOG = LoggerFactory.getLogger(AerospikeParameters.class);

  /**
   * Constructor to create AerospikeParameters object with the given mapping and properties
   *
   * @param aerospikeMapping aerospike mapping initialized from the mapping file
   * @param properties       property details
   */
  public AerospikeParameters(AerospikeMapping aerospikeMapping, Properties properties) {
    this.aerospikeMapping = aerospikeMapping;
    this.host = properties.getProperty(AS_SERVER_IP, DEFAULT_SERVER_IP);
    this.port = Integer.parseInt(properties.getProperty(AS_SERVER_PORT, DEFAULT_SERVER_PORT));
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public AerospikeMapping getAerospikeMapping() {
    return aerospikeMapping;
  }

  public void setAerospikeMapping(AerospikeMapping aerospikeMapping) {
    this.aerospikeMapping = aerospikeMapping;
  }

  public boolean isSingleBinEnabled() {
    return isSingleBinEnabled;
  }

  public void setSingleBinEnabled(boolean singleBinEnabled) {
    this.isSingleBinEnabled = singleBinEnabled;
  }

  /**
   * Retrieves and sets the server specific parameters
   * Validates the existence of user provided namespace and validates for single binned
   * namespaces
   *
   * @param client aerospike client used to connect with the server
   */
  public void setServerSpecificParameters(AerospikeClient client) {

    String namespaceTokens = null;
    for (Node node : client.getNodes()) {
      String namespaceFilter = "namespace/" + aerospikeMapping.getNamespace();
      namespaceTokens = Info.request(null, node, namespaceFilter);

      if (namespaceTokens != null) {
        isSingleBinEnabled = parseBoolean(namespaceTokens, "single-bin");
        break;
      }
    }
    if (namespaceTokens == null) {
      LOG.error("Failed to get namespace info from Aerospike");
      throw new RuntimeException("Failed to get namespace info from Aerospike");
    }
  }

  /**
   * Parse the namespace tokens and retrieve the corresponding boolean value for the
   * provided parameter name
   *
   * @param namespaceTokens namespace tokens
   * @param name            name of the parameter
   * @return boolean value in the namespace corresponding to the provided parameter
   */
  private boolean parseBoolean(String namespaceTokens, String name) {
    String search = name + '=';
    int begin = namespaceTokens.indexOf(search);

    if (begin < 0) {
      return false;
    }

    begin += search.length();
    int end = namespaceTokens.indexOf(';', begin);

    if (end < 0) {
      end = namespaceTokens.length();
    }
    String value = namespaceTokens.substring(begin, end);
    return Boolean.parseBoolean(value);
  }

  /**
   * Method is used to validate server bin configuration. In Aerospike, it is possible to enable
   * single bin (column) for the namespace which will disallow multiple bins (columns) for a
   * record. Thus if the namespace is single bin enabled and the data bean contains more than one
   * field, that namespace cannot be used to mutiple bin operations.
   *
   * @param fields fields of the persistent class
   */
  public void validateServerBinConfiguration(Field[] fields) {
    if (isSingleBinEnabled && fields.length != 1) {
      LOG.error("Aerospike server is single bin enabled and cannot allow multiple bin operations");
      throw new RuntimeException("Aerospike server is single bin enabled and cannot allow "
              + "multiple bin operations");
    }
  }
}