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
package org.apache.gora.elasticsearch.query;

import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.Query;
import org.apache.gora.query.impl.ResultBase;
import org.apache.gora.store.DataStore;

import java.util.List;

/**
 * ElasticsearchResult specific implementation of the
 * {@link org.apache.gora.query.Result} interface.
 */
public class ElasticsearchResult<K, T extends PersistentBase> extends ResultBase<K, T> {

    /**
     * List of resulting persistent objects.
     */
    private List<T> persistentObjects;

    /**
     * List of resulting objects keys.
     */
    private List<K> persistentKeys;

    public ElasticsearchResult(DataStore<K, T> dataStore, Query<K, T> query, List<K> persistentKeys, List<T> persistentObjects) {
        super(dataStore, query);
        this.persistentKeys = persistentKeys;
        this.persistentObjects = persistentObjects;
    }

    @Override
    public float getProgress() {
        if (persistentObjects.size() == 0) {
            return 1;
        }

        return offset / (float) persistentObjects.size();
    }

    @Override
    public int size() {
        return persistentObjects.size();
    }

    @Override
    protected boolean nextInner() {
        if ((int) offset == persistentObjects.size()) {
            return false;
        }

        persistent = persistentObjects.get((int) offset);
        key = persistentKeys.get((int) offset);
        return persistent != null;
    }
}
