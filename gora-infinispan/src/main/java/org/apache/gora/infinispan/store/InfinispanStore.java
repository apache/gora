/*
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
package org.apache.gora.infinispan.store;

import org.apache.gora.infinispan.query.InfinispanQuery;
import org.apache.gora.infinispan.query.InfinispanResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.apache.gora.mapreduce.GoraRecordReader.BUFFER_LIMIT_READ_NAME;
import static org.apache.gora.mapreduce.GoraRecordReader.BUFFER_LIMIT_READ_VALUE;

/**
 * {@link org.apache.gora.infinispan.store.InfinispanStore} is the primary class
 * responsible for directing Gora CRUD operations to Infinispan.This class delegate
 * most operations, e.g., initialization, creation and deletion to (Infinispan caches),
 * via {@link org.apache.gora.infinispan.store.InfinispanClient}.
 *
 * To specify the Infinispan deployment, include parameter <i>infinispan.connectionstring</i>
 * in <i>gora.properties</i> with the list of servers, e.g., "127.0.0.1:11222,127.0.0.1:11223".
 *
 * @author Pierre Sutra, Valerio Schiavoni
 *
 */
public class InfinispanStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

   public static final Logger LOG = LoggerFactory.getLogger(InfinispanStore.class);

   private InfinispanClient<K, T> infinispanClient;
   private String primaryFieldName;
   private int primaryFieldPos;
   private int splitSize;

   public InfinispanStore() throws Exception {}

   @Override
   public synchronized void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {

      try {

         if (primaryFieldName!=null) {
            LOG.info("Client already initialized; ignoring.");
            return;
         }

         super.initialize(keyClass, persistentClass, properties);
         infinispanClient  = new InfinispanClient<>();
         infinispanClient.setConf(conf);

         LOG.info("key class: "
               + keyClass.getCanonicalName()
               + ", persistent class: "
               + persistentClass.getCanonicalName());
         schema = persistentClass.newInstance().getSchema();

         splitSize = Integer.valueOf(
               properties.getProperty( BUFFER_LIMIT_READ_NAME,
                     getConf().get(
                           BUFFER_LIMIT_READ_NAME,
                           Integer.toString(BUFFER_LIMIT_READ_VALUE))));
         LOG.info("split size: "+splitSize);

         primaryFieldPos = 0;
         primaryFieldName = schema.getFields().get(0).name();
         this.infinispanClient.initialize(keyClass, persistentClass, properties);

      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void close() {
      LOG.debug("close()");
      infinispanClient.close();
   }

   @Override
   public void createSchema() {
      LOG.debug("createSchema()");
      this.infinispanClient.createCache();
   }

   @Override
   public boolean delete(K key) {
      LOG.debug("delete(" + key+")");
      this.infinispanClient.deleteByKey(key);
      return true;
   }

   @Override
   public long deleteByQuery(Query<K, T> query) {
      ((InfinispanQuery<K, T>) query).build();
      LOG.debug("deleteByQuery("+query.toString()+")");
      InfinispanQuery<K, T> q = (InfinispanQuery) query;
      q.build();
      for( T t : q.list()){
         infinispanClient.deleteByKey((K) t.get(primaryFieldPos));
      }
      return q.getResultSize();
   }

   @Override
   public void deleteSchema() {
      LOG.debug("deleteSchema()");
      this.infinispanClient.dropCache();
   }

   @Override
   public Result<K, T> execute(Query<K, T> query) {
      LOG.debug("execute()");
      ((InfinispanQuery<K,T>)query).build();
      InfinispanResult<K,T> result = new InfinispanResult<>(this, (InfinispanQuery<K,T>)query);
      LOG.trace("query: " + query.toString());
      LOG.trace("result size: " + result.size());
      return result;
   }

   @Override
   public T get(K key){
      LOG.debug("get("+key+")");
      return infinispanClient.get(key);
   }

   @Override
   public T get(K key, String[] fields) {
      LOG.debug("get("+key+","+fields+")");
      if (fields==null)
         return infinispanClient.get(key);

      InfinispanQuery query = new InfinispanQuery(this);
      query.setKey(key);
      query.setFields(fields);
      query.build();


      Result<K,T> result = query.execute();
      try {
         result.next();
         return result.get();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    *
    * Split the query per infinispan node resulting in a list of queries.
    * For each Infinispan server, this function returns a set of qeuries
    * using pagination of the originial query. The size of each query
    * in this pagination equals <i>gora.buffer.read.limit</i>.
    *
    * @param query the base query to create the partitions for. If the query
    * is null, then the data store returns the partitions for the default query
    * (returning every object)
    * @return
    * @throws IOException
    */
   @Override
   public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
         throws IOException {
      LOG.debug("getPartitions()");

      // 1 - split the query per location
      List<PartitionQuery<K,T>> locations = ((InfinispanQuery<K,T>)query).split();

      // 2 -split each location
      List<PartitionQuery<K,T>> splitLocations = new ArrayList<>();
      for(PartitionQuery<K,T> location : locations) {

         LOG.trace("location: "+ ((InfinispanQuery)location).getLocation().toString());

         // 2.1 - compute the result size
         InfinispanQuery<K,T> sizeQuery = (InfinispanQuery<K, T>) ((InfinispanQuery<K, T>) location).clone();
         sizeQuery.setFields(primaryFieldName);
         sizeQuery.setLimit(1);
         sizeQuery.rebuild();

         // 2.2 - check if splitting is necessary
         int resultSize = sizeQuery.getResultSize();
         long queryLimit = query.getLimit();
         long splitLimit = queryLimit>0 ? Math.min((long)resultSize,queryLimit) : resultSize;
         LOG.trace("split limit: "+ splitLimit);
         LOG.trace("split size: "+ splitSize);
         if (splitLimit <= splitSize) {
            LOG.trace("location returned");
            splitLocations.add(location);
            continue;
         }

         // 2.3 - compute the splits
         for(int i=0; i<Math.ceil((double)splitLimit/(double)splitSize); i++) {
            InfinispanQuery<K, T> split = (InfinispanQuery<K, T>) ((InfinispanQuery<K, T>) location).clone();
            split.setOffset(i * splitSize);
            split.setLimit(splitSize);
            split.rebuild();
            splitLocations.add(split);
         }
      }

      return splitLocations;
   }

   @Override
   public void flush() {
      LOG.debug("flush()");
      infinispanClient.flush();
   }

   /**
    * In Infinispan, Schemas are referred to as caches.
    *
    * @return Cache
    */
   @Override
   public String getSchemaName() {
      LOG.debug("getSchemaName()");
      return this.infinispanClient.getCacheName();
   }

   @Override
   public Query<K, T> newQuery() {
      LOG.debug("newQuery()");
      Query<K, T> query = new InfinispanQuery<K, T>(this);
      query.setFields(getFieldsToQuery(null));
      return query;
   }

   @Override
   public void put(K key, T obj) {
      LOG.debug("put(" +key.toString()+")");
      LOG.trace(obj.toString());

      if (obj.get(primaryFieldPos)==null)
         obj.put(primaryFieldPos,key);

      if (!obj.get(primaryFieldPos).equals(key) )
         LOG.warn("Invalid or different primary field :"+key+"<->"+obj.get(primaryFieldPos));

      this.infinispanClient.put(key, obj);
   }

   @Override
   public boolean schemaExists() {
      LOG.debug("schemaExists()");
      return infinispanClient.cacheExists();
   }

   public InfinispanClient<K, T> getClient() {
      LOG.debug("getClient()");
      return infinispanClient;
   }

   public String getPrimaryFieldName() {
      LOG.debug("getPrimaryField()");
      return primaryFieldName;
   }

   public void setPrimaryFieldName(String name){
      LOG.debug("getPrimaryFieldName()");
      primaryFieldName = name;
   }

   public int getPrimaryFieldPos(){
      LOG.debug("getPrimaryFieldPos()");
      return primaryFieldPos;
   }

   public void setPrimaryFieldPos(int p){
      LOG.debug("setPrimaryFieldPos()");
      primaryFieldPos = p;
   }

}
