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
/**
 * This package contains all the Hazelcast store related classes which involve manipulating Hazelcast
 * caches. {@link org.apache.gora.hazelcast.store.HazelcastCacheWriter} and
 * {@link org.apache.gora.hazelcast.store.HazelcastCacheLoader} handles the read/write operations
 * from/to caches and persistent backend. {@link org.apache.gora.hazelcast.store.HazelcastCacheLoaderFactory} and
 * {@link org.apache.gora.hazelcast.store.HazelcastCacheWriterFactory} provides factory implementation that handles
 * singleton instance creation of writer/loader. {@link org.apache.gora.hazelcast.store.HazelcastCacheEntryListener}
 * is the class which manages local cache entry set and
 * {@link org.apache.gora.hazelcast.store.HazelcastCacheEntryListenerFactory} takes care of singleton instance creation
 * for entry listener. {@link org.apache.gora.hazelcast.store.HazelcastCacheFactoryBuilder} is generic factory builder
 * for above mentioned factory classes.
 */
package org.apache.gora.hazelcast.store;