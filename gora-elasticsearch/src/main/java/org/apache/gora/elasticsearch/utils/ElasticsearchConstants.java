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
package org.apache.gora.elasticsearch.utils;

/**
 * Constants file for Elasticsearch.
 */
public class ElasticsearchConstants {
    /**
     * Property indicating if the hadoop configuration has priority or not.
     */
    public static final String PROP_OVERRIDING = "gora.elasticsearch.override.hadoop.configuration";

    /**
     * Default configurations for Elasticsearch.
     */
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 9200;

    /**
     * List of keys used in the configuration file of Elasticsearch.
     */
    public static final String PROP_HOST = "gora.datastore.elasticsearch.host";
    public static final String PROP_PORT = "gora.datastore.elasticsearch.port";
    public static final String PROP_SCHEME = "gora.datastore.elasticsearch.scheme";
    public static final String PROP_USERNAME = "gora.datastore.elasticsearch.username";
    public static final String PROP_PASSWORD = "gora.datastore.elasticsearch.password";
    public static final String PROP_AUTHORIZATIONTOKEN = "gora.datastore.elasticsearch.authorizationToken";
    public static final String PROP_APIKEYID = "gora.datastore.elasticsearch.apiKeyId";
    public static final String PROP_APIKEYSECRET = "gora.datastore.elasticsearch.apiKeySecret";
    public static final String PROP_CONNECTTIMEOUT = "gora.datastore.elasticsearch.connectTimeout";
    public static final String PROP_SOCKETTIMEOUT = "gora.datastore.elasticsearch.socketTimeout";
    public static final String PROP_IOTHREADCOUNT = "gora.datastore.elasticsearch.ioThreadCount";
}
