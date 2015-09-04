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
package org.apache.gora.hbase.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.gora.filter.Filter;
import org.apache.gora.hbase.store.HBaseStore;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.ReflectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Scan;

import java.util.LinkedHashMap;
import java.util.Map;

public class HBaseFilterUtil<K, T extends PersistentBase> {
  private static final Log LOG = LogFactory.getLog(HBaseFilterUtil.class);

  private Map<String, FilterFactory<K, T>> factories = new LinkedHashMap<String, FilterFactory<K, T>>();

  public HBaseFilterUtil(Configuration conf) throws GoraException {
    String[] factoryClassNames = conf.getStrings("gora.hbase.filter.factories", "org.apache.gora.hbase.util.DefaultFactory");

    for (String factoryClass : factoryClassNames) {
      try {
        @SuppressWarnings("unchecked")
        FilterFactory<K, T> factory = (FilterFactory<K, T>) ReflectionUtils.newInstance(factoryClass);
        for (String filterClass : factory.getSupportedFilters()) {
          factories.put(filterClass, factory);
        }
        factory.setHBaseFitlerUtil(this);
      } catch (Exception e) {
        throw new GoraException(e);
      }
    }
  }

  public FilterFactory<K, T> getFactory(Filter<K, T> fitler) {
    return factories.get(fitler.getClass().getCanonicalName());
  }

  /**
   * Set a filter on the Scan. It translates a Gora filter to a HBase filter.
   * 
   * @param scan
   * @param filter
   *          The Gora filter.
   * @param store
   *          The HBaseStore.
   * @return if remote filter is succesfully applied.
   */
  public boolean setFilter(Scan scan, Filter<K, T> filter, HBaseStore<K, T> store) {

    FilterFactory<K, T> factory = getFactory(filter);
    if (factory != null) {
      org.apache.hadoop.hbase.filter.Filter hbaseFilter = factory.createFilter(filter, store);
      if (hbaseFilter != null) {
        scan.setFilter(hbaseFilter);
        return true;
      } else {
        LOG.warn("HBase remote filter not yet implemented for " + filter.getClass().getCanonicalName());
        return false;
      }
    } else {
      LOG.warn("HBase remote filter factory not yet implemented for " + filter.getClass().getCanonicalName());
      return false;
    }
  }
  
}
