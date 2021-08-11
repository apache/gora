/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.elasticsearch.utils;

import org.apache.hadoop.conf.Configuration;

import java.util.Properties;

/**
 * Parameters definitions for Elasticsearch.
 */
public class ElasticsearchParameters {

    /**
     * Elasticsearch server host.
     */
    private String host;

    /**
     * Elasticsearch server port.
     */
    private int port;

    /**
     * Elasticsearch server scheme.
     * Optional. If not provided, defaults to http.
     */
    private String scheme;

    /**
     * Authentication type to connect to the server.
     * Can be BASIC, TOKEN or APIKEY.
     */
    private AuthenticationType authenticationType;

    /**
     * Username to use for server authentication.
     * Required for BASIC authentication to connect to the server.
     */
    private String username;

    /**
     * Password to use for server authentication.
     * Required for BASIC authentication to connect to the server.
     */
    private String password;

    /**
     * Authorization token to use for server authentication.
     * Required for TOKEN authentication to connect to the server.
     */
    private String authorizationToken;

    /**
     * API Key ID to use for server authentication.
     * Required for APIKEY authentication to connect to the server.
     */
    private String apiKeyId;

    /**
     * API Key Secret to use for server authentication.
     * Required for APIKEY authentication to connect to the server.
     */
    private String apiKeySecret;

    /**
     * Timeout in milliseconds used for establishing the connection to the server.
     * Optional. If not provided, defaults to 5000s.
     *
     */
    private int connectTimeout;

    /**
     * Timeout in milliseconds used for waiting for data – after establishing the connection to the server.
     * Optional. If not provided, defaults to 60000s.
     */
    private int socketTimeout;

    /**
     * Number of worker threads used by the connection manager.
     * Optional. If not provided, defaults to 1.
     */
    private int ioThreadCount;

    public ElasticsearchParameters(String host, int port) {
        this.host = host;
        this.port = port;
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

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getApiKeySecret() {
        return apiKeySecret;
    }

    public void setApiKeySecret(String apiKeySecret) {
        this.apiKeySecret = apiKeySecret;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getIoThreadCount() {
        return ioThreadCount;
    }

    public void setIoThreadCount(int ioThreadCount) {
        this.ioThreadCount = ioThreadCount;
    }

    /**
     * Reads Elasticsearch parameters from a properties list.
     *
     * @param properties Properties list
     * @return Elasticsearch parameters instance
     */
    public static ElasticsearchParameters load(Properties properties, Configuration conf) {
        ElasticsearchParameters elasticsearchParameters;

        if (!Boolean.parseBoolean(properties.getProperty(ElasticsearchConstants.PROP_OVERRIDING))) {
            elasticsearchParameters = new ElasticsearchParameters(
                    conf.get(ElasticsearchConstants.PROP_HOST, ElasticsearchConstants.DEFAULT_HOST),
                    conf.getInt(ElasticsearchConstants.PROP_PORT, ElasticsearchConstants.DEFAULT_PORT));
        } else {
            elasticsearchParameters = new ElasticsearchParameters(
                    properties.getProperty(ElasticsearchConstants.PROP_HOST, ElasticsearchConstants.DEFAULT_HOST),
                    Integer.parseInt(properties.getProperty(ElasticsearchConstants.PROP_PORT,
                            String.valueOf(ElasticsearchConstants.DEFAULT_PORT))));
        }

        String schemeProperty = properties.getProperty(ElasticsearchConstants.PROP_SCHEME);
        if (schemeProperty != null) {
            elasticsearchParameters.setScheme(schemeProperty);
        }

        AuthenticationType authenticationTypeProperty =
                AuthenticationType.valueOf(properties.getProperty(ElasticsearchConstants.PROP_AUTHENTICATIONTYPE));
        if (authenticationTypeProperty != null) {
            elasticsearchParameters.setAuthenticationType(authenticationTypeProperty);
        }

        String usernameProperty = properties.getProperty(ElasticsearchConstants.PROP_USERNAME);
        if (usernameProperty != null) {
            elasticsearchParameters.setUsername(usernameProperty);
        }

        String passwordProperty = properties.getProperty(ElasticsearchConstants.PROP_PASSWORD);
        if (passwordProperty != null) {
            elasticsearchParameters.setPassword(passwordProperty);
        }

        String authorizationTokenProperty = properties.getProperty(ElasticsearchConstants.PROP_AUTHORIZATIONTOKEN);
        if (authorizationTokenProperty != null) {
            elasticsearchParameters.setAuthorizationToken(authorizationTokenProperty);
        }

        String apiKeyIdProperty = properties.getProperty(ElasticsearchConstants.PROP_APIKEYID);
        if (apiKeyIdProperty != null) {
            elasticsearchParameters.setApiKeyId(apiKeyIdProperty);
        }

        String apiKeySecretProperty = properties.getProperty(ElasticsearchConstants.PROP_APIKEYSECRET);
        if (apiKeySecretProperty != null) {
            elasticsearchParameters.setApiKeySecret(apiKeySecretProperty);
        }

        String connectTimeoutProperty = properties.getProperty(ElasticsearchConstants.PROP_CONNECTTIMEOUT);
        if (connectTimeoutProperty != null) {
            elasticsearchParameters.setConnectTimeout(Integer.parseInt(connectTimeoutProperty));
        }

        String socketTimeoutProperty = properties.getProperty(ElasticsearchConstants.PROP_SOCKETTIMEOUT);
        if (socketTimeoutProperty != null) {
            elasticsearchParameters.setSocketTimeout(Integer.parseInt(socketTimeoutProperty));
        }

        String ioThreadCountProperty = properties.getProperty(ElasticsearchConstants.PROP_IOTHREADCOUNT);
        if (ioThreadCountProperty != null) {
            elasticsearchParameters.setIoThreadCount(Integer.parseInt(ioThreadCountProperty));
        }

        return elasticsearchParameters;
    }
}
