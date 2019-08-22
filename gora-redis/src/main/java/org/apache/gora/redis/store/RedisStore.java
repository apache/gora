/**
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
package org.apache.gora.redis.store;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.commons.io.IOUtils;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.redis.query.RedisQuery;
import org.apache.gora.redis.query.RedisResult;
import org.apache.gora.redis.util.DatumHandler;
import static org.apache.gora.redis.util.RedisStoreConstants.PREFIX;
import static org.apache.gora.redis.util.RedisStoreConstants.END_TAG;
import static org.apache.gora.redis.util.RedisStoreConstants.FIELD_SEPARATOR;
import static org.apache.gora.redis.util.RedisStoreConstants.GORA_REDIS_ADDRESS;
import static org.apache.gora.redis.util.RedisStoreConstants.GORA_REDIS_MASTERNAME;
import static org.apache.gora.redis.util.RedisStoreConstants.GORA_REDIS_MODE;
import static org.apache.gora.redis.util.RedisStoreConstants.GORA_REDIS_READMODE;
import static org.apache.gora.redis.util.RedisStoreConstants.GORA_REDIS_STORAGE;
import static org.apache.gora.redis.util.RedisStoreConstants.INDEX;
import static org.apache.gora.redis.util.RedisStoreConstants.START_TAG;
import static org.apache.gora.redis.util.RedisStoreConstants.WILDCARD;
import org.apache.gora.redis.util.ServerMode;
import org.apache.gora.redis.util.StorageMode;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.redisson.Redisson;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RFuture;
import org.redisson.api.RLexSortedSet;
import org.redisson.api.RLexSortedSetAsync;
import org.redisson.api.RList;
import org.redisson.api.RListAsync;
import org.redisson.api.RMap;
import org.redisson.api.RMapAsync;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Redis data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class RedisStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {
  
  protected static final String PARSE_MAPPING_FILE_KEY = "gora.redis.mapping.file";
  protected static final String DEFAULT_MAPPING_FILE = "gora-redis-mapping.xml";
  protected static final String XML_MAPPING_DEFINITION = "gora.mapping";
  private RedissonClient redisInstance;
  private RedisMapping mapping;
  public static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private static final DatumHandler handler = new DatumHandler();
  private StorageMode mode;

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file. Initialize is called when then
   * the call to {@link org.apache.gora.store.DataStoreFactory#createDataStore}
   * is made.
   *
   * @param keyClass Gora's key class
   * @param persistentClass Persistent class
   * @param properties Configurations for the data store
   * @throws org.apache.gora.util.GoraException Unexpected exception during initialization
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);
      
      InputStream mappingStream;
      if (properties.containsKey(XML_MAPPING_DEFINITION)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("{} = {}", XML_MAPPING_DEFINITION, properties.getProperty(XML_MAPPING_DEFINITION));
        }
        mappingStream = IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
      } else {
        mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      }
      RedisMappingBuilder mappingBuilder = new RedisMappingBuilder(this);
      mapping = mappingBuilder.readMapping(mappingStream);
      Config config = new Config();
      String storage = getConf().get(GORA_REDIS_STORAGE, properties.getProperty(GORA_REDIS_STORAGE));
      mode = StorageMode.valueOf(storage);
      String modeString = getConf().get(GORA_REDIS_MODE, properties.getProperty(GORA_REDIS_MODE));
      ServerMode connectionMode = ServerMode.valueOf(modeString);
      String name = getConf().get(GORA_REDIS_MASTERNAME, properties.getProperty(GORA_REDIS_MASTERNAME));
      String readm = getConf().get(GORA_REDIS_READMODE, properties.getProperty(GORA_REDIS_READMODE));
      //Override address in tests
      String[] hosts = getConf().get(GORA_REDIS_ADDRESS, properties.getProperty(GORA_REDIS_ADDRESS)).split(",");
      for (int indexHosts = 0; indexHosts < hosts.length; indexHosts++) {
        hosts[indexHosts] = PREFIX + hosts[indexHosts];
      }
      switch (connectionMode) {
        case SINGLE:
          config.useSingleServer()
                  .setAddress(hosts[0])
                  .setDatabase(mapping.getDatabase());
          break;
        case CLUSTER:
          config.useClusterServers()
                  .addNodeAddress(hosts);
          break;
        case REPLICATED:
          config.useReplicatedServers()
                  .addNodeAddress(hosts)
                  .setDatabase(mapping.getDatabase());
          break;
        case SENTINEL:
          config.useSentinelServers()
                  .setMasterName(name)
                  .setReadMode(ReadMode.valueOf(readm))
                  .addSentinelAddress(hosts);
          break;
        default:
          throw new AssertionError(connectionMode.name());
      }
      redisInstance = Redisson.create(config);
      if (autoCreateSchema && !schemaExists()) {
        createSchema();
      }
    } catch (IOException ex) {
      throw new GoraException(ex);
    }
  }

  /**
   * Redis, being a schemaless database does not support explicit schema
   * creation. When the records are added to the database, the schema is created
   * on the fly. Thus, schema operations are unavailable in gora-redis module.
   *
   * @return null
   */
  @Override
  public String getSchemaName() {
    return null;
  }

  /**
   * Redis, being a schemaless database does not support explicit schema
   * creation. When the records are added to the database, the schema is created
   * on the fly. Thus, schema operations are unavailable in gora-redis module.
   *
   * @throws org.apache.gora.util.GoraException Unexpected exception.
   */
  @Override
  public void createSchema() throws GoraException {
  }
  
  @Override
  public void deleteSchema() throws GoraException {
    redisInstance.getKeys().deleteByPattern(mapping.getPrefix() + FIELD_SEPARATOR + WILDCARD);
  }

  /**
   * Redis, being a schemaless database does not support explicit schema
   * creation. When the records are added to the database, the schema is created
   * on the fly. Thus, schema operations are unavailable in gora-redis module.
   *
   * @return true
   * @throws org.apache.gora.util.GoraException Unexpected exception.
   */
  @Override
  public boolean schemaExists() throws GoraException {
    return true;
  }
  
  private String generateKeyHash(K baseKey) {
    return mapping.getPrefix() + FIELD_SEPARATOR + START_TAG + baseKey + END_TAG;
  }
  
  private String generateKeyString(String field, K baseKey) {
    return generateKeyStringBase(baseKey) + field;
  }
  
  private String generateKeyStringBase(K baseKey) {
    return mapping.getPrefix() + FIELD_SEPARATOR + START_TAG + baseKey + END_TAG + FIELD_SEPARATOR;
  }
  
  private String generateIndexKey() {
    return mapping.getPrefix() + FIELD_SEPARATOR + INDEX;
  }
  
  public T newInstanceFromString(K key, String[] fields) throws GoraException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    int countRetrieved = 0;
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      String redisField = mapping.getFields().get(field.name());
      RedisType redisType = mapping.getTypes().get(field.name());
      Object redisVal = null;
      switch (redisType) {
        case STRING:
          RBucket<Object> bucket = redisInstance.getBucket(generateKeyString(redisField, key));
          redisVal = bucket.isExists() ? handler.deserializeFieldValue(field, field.schema(), bucket.get(), persistent) : null;
          break;
        case LIST:
          RList<Object> list = redisInstance.getList(generateKeyString(redisField, key));
          redisVal = list.isExists() ? handler.deserializeFieldList(field, field.schema(), list, persistent) : null;
          break;
        case HASH:
          RMap<Object, Object> map = redisInstance.getMap(generateKeyString(redisField, key));
          redisVal = map.isExists() ? handler.deserializeFieldMap(field, field.schema(), map, persistent) : null;
          break;
        default:
          throw new AssertionError(redisType.name());
      }
      if (redisVal == null) {
        continue;
      }
      countRetrieved++;
      persistent.put(field.pos(), redisVal);
      persistent.setDirty(field.pos());
    }
    return countRetrieved > 0 ? persistent : null;
  }
  
  public T newInstanceFromHash(RMap<String, Object> map, String[] fields) throws GoraException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    for (String fieldName : fields) {
      Schema.Field field = fieldMap.get(fieldName);
      Object fValue = map.get(mapping.getFields().get(field.name()));
      if (fValue == null) {
        continue;
      }
      Object fieldValue = handler.deserializeFieldValue(field, field.schema(), fValue, persistent);
      persistent.put(field.pos(), fieldValue);
      persistent.setDirty(field.pos());
    }
    return persistent;
  }
  
  @Override
  public T get(K key, String[] fields) throws GoraException {
    try {
      if (mode == StorageMode.SINGLEKEY) {
        RMap<String, Object> map = redisInstance.getMap(generateKeyHash(key));
        if (!map.isEmpty()) {
          return newInstanceFromHash(map, fields);
        } else {
          return null;
        }
      } else {
        return newInstanceFromString(key, fields);
      }
    } catch (IOException ex) {
      throw new GoraException(ex);
    }
  }
  
  @Override
  public void put(K key, T obj) throws GoraException {
    try {
      if (obj.isDirty()) {
        Schema objectSchema = obj.getSchema();
        List<Schema.Field> fields = objectSchema.getFields();
        RBatch batchInstance = redisInstance.createBatch();
        //update secundary index
        if (isNumericKey()) {
          RScoredSortedSetAsync<Object> secundaryIndex = batchInstance.getScoredSortedSet(generateIndexKey());
          secundaryIndex.addAsync(obtainDoubleValue(key), key);
        } else {
          RLexSortedSetAsync secundaryIndex = batchInstance.getLexSortedSet(generateIndexKey());
          secundaryIndex.addAsync(key.toString());
        }
        if (mode == StorageMode.SINGLEKEY) {
          RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyHash(key));
          fields.forEach((field) -> {
            Object fieldValue = handler.serializeFieldValue(field.schema(), obj.get(field.pos()));
            if (fieldValue != null) {
              map.fastPutAsync(mapping.getFields().get(field.name()), fieldValue);
            } else {
              map.fastRemoveAsync(mapping.getFields().get(field.name()));
            }
          });
        } else {
          for (Schema.Field field : fields) {
            Object fieldValue = obj.get(field.pos());
            String redisField = mapping.getFields().get(field.name());
            RedisType redisType = mapping.getTypes().get(field.name());
            switch (redisType) {
              case STRING:
                RBucketAsync<Object> bucket = batchInstance.getBucket(generateKeyString(redisField, key));
                bucket.deleteAsync();
                if (fieldValue != null) {
                  fieldValue = handler.serializeFieldValue(field.schema(), fieldValue);
                  bucket.setAsync(fieldValue);
                }
                break;
              case LIST:
                RListAsync<Object> rlist = batchInstance.getList(generateKeyString(redisField, key));
                rlist.deleteAsync();
                if (fieldValue != null) {
                  List<Object> list = handler.serializeFieldList(field.schema(), fieldValue);
                  rlist.addAllAsync(list);
                }
                break;
              case HASH:
                RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyString(redisField, key));
                map.deleteAsync();
                if (fieldValue != null) {
                  Map<Object, Object> mp = handler.serializeFieldMap(field.schema(), fieldValue);
                  map.putAllAsync(mp);
                }
                break;
              default:
                throw new AssertionError(redisType.name());
            }
          }
        }
        batchInstance.execute();
      } else {
        LOG.info("Ignored putting object {} in the store as it is neither "
                + "new, neither dirty.", new Object[]{obj});
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }
  
  @Override
  public boolean delete(K key) throws GoraException {
    try {
      RBatch batchInstance = redisInstance.createBatch();
      //update secundary index
      if (isNumericKey()) {
        RScoredSortedSetAsync<Object> secundaryIndex = batchInstance.getScoredSortedSet(generateIndexKey());
        secundaryIndex.removeAsync(key);
      } else {
        RLexSortedSetAsync secundaryIndex = batchInstance.getLexSortedSet(generateIndexKey());
        secundaryIndex.removeAsync(key.toString());
      }
      if (mode == StorageMode.SINGLEKEY) {
        RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyHash(key));
        RFuture<Boolean> deleteAsync = map.deleteAsync();
        batchInstance.execute();
        return deleteAsync.get();
      } else {
        batchInstance.execute();
        return redisInstance.getKeys().deleteByPattern(generateKeyStringBase(key) + WILDCARD) > 0;
      }
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
  }
  
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    Collection<K> range = runQuery(query);
    RBatch batchInstance = redisInstance.createBatch();
    RLexSortedSetAsync secundaryIndex = batchInstance.getLexSortedSet(generateIndexKey());
    if (query.getFields() != null && query.getFields().length < mapping.getFields().size()) {
      List<String> dbFields = new ArrayList<>();
      List<RedisType> dbTypes = new ArrayList<>();
      for (String af : query.getFields()) {
        dbFields.add(mapping.getFields().get(af));
        dbTypes.add(mapping.getTypes().get(af));
      }
      for (K key : range) {
        if (mode == StorageMode.SINGLEKEY) {
          RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyHash(key));
          dbFields.forEach((field) -> {
            map.removeAsync(field);
          });
        } else {
          for (int indexField = 0; indexField < dbFields.size(); indexField++) {
            String field = dbFields.get(indexField);
            RedisType type = dbTypes.get(indexField);
            switch (type) {
              case STRING:
                RBucketAsync<Object> bucket = batchInstance.getBucket(generateKeyString(field, key));
                bucket.deleteAsync();
                break;
              case LIST:
                RListAsync<Object> rlist = batchInstance.getList(generateKeyString(field, key));
                rlist.deleteAsync();
                break;
              case HASH:
                RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyString(field, key));
                map.deleteAsync();
                break;
              default:
                throw new AssertionError(type.name());
            }
          }
        }
      }
    } else {
      range.stream().map((key) -> {
        secundaryIndex.removeAsync(key);
        return key;
      }).forEachOrdered((key) -> {
        if (mode == StorageMode.SINGLEKEY) {
          RMapAsync<Object, Object> map = batchInstance.getMap(generateKeyHash(key));
          map.deleteAsync();
        } else {
          redisInstance.getKeys().deleteByPattern(generateKeyStringBase(key) + WILDCARD);
        }
      });
    }
    batchInstance.execute();
    return range.size();
  }

  /**
   * Execute the query and return the result.
   *
   * @param query Query sent to Redis
   * @return Query result
   * @throws org.apache.gora.util.GoraException  Unexpected exception in querying process.
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    return new RedisResult<>(this, query, runQuery(query));
  }
  
  @Override
  public Query<K, T> newQuery() {
    RedisQuery<K, T> query = new RedisQuery<>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }
  
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws GoraException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query);
    partitionQuery.setConf(getConf());
    partitions.add(partitionQuery);
    return partitions;
  }
  
  @Override
  public void flush() throws GoraException {
  }
  
  @Override
  public void close() {
    redisInstance.shutdown();
  }
  
  @Override
  public boolean exists(K key) throws GoraException {
    if (mode == StorageMode.SINGLEKEY) {
      return redisInstance.getKeys().countExists(generateKeyHash(key)) != 0;
    } else {
      Iterator<String> respKeys = redisInstance.getKeys().getKeysByPattern(generateKeyStringBase(key) + WILDCARD, 1).iterator();
      return respKeys.hasNext();
    }
  }
  
  private Collection<K> runQuery(Query<K, T> query) {
    Collection<K> range;
    if (isNumericKey()) {
      RScoredSortedSet<Object> index = redisInstance.getScoredSortedSet(generateIndexKey());
      Collection<ScoredEntry<Object>> rangeResponse;
      int limit = query.getLimit() > -1 ? (int) query.getLimit() : Integer.MAX_VALUE;
      if (query.getStartKey() != null && query.getEndKey() != null) {
        rangeResponse = index.entryRange(obtainDoubleValue(query.getStartKey()), true, obtainDoubleValue(query.getEndKey()), true, 0, limit);
      } else if (query.getStartKey() != null && query.getEndKey() == null) {
        rangeResponse = index.entryRange(obtainDoubleValue(query.getStartKey()), true, Double.MAX_VALUE, true, 0, limit);
      } else if (query.getStartKey() == null && query.getEndKey() != null) {
        rangeResponse = index.entryRange(Double.MIN_VALUE, true, obtainDoubleValue(query.getEndKey()), true, 0, limit);
      } else {
        rangeResponse = index.entryRange(Double.MIN_VALUE, true, Double.MAX_VALUE, true, 0, limit);
      }
      range = new ArrayList<>();
      for (ScoredEntry<Object> indexVal : rangeResponse) {
        range.add((K) indexVal.getValue());
      }
    } else {
      RLexSortedSet index = redisInstance.getLexSortedSet(generateIndexKey());
      Collection<String> rangeResponse;
      int limit = query.getLimit() > -1 ? (int) query.getLimit() : Integer.MAX_VALUE;
      if (query.getStartKey() != null && query.getEndKey() != null) {
        rangeResponse = index.range(query.getStartKey().toString(), true, query.getEndKey().toString(), true, 0, limit);
      } else if (query.getStartKey() != null && query.getEndKey() == null) {
        rangeResponse = index.rangeTail(query.getStartKey().toString(), true, 0, limit);
      } else if (query.getStartKey() == null && query.getEndKey() != null) {
        rangeResponse = index.rangeHead(query.getEndKey().toString(), true, 0, limit);
      } else {
        rangeResponse = index.stream().limit(limit).collect(Collectors.toList());
      }
      range = new ArrayList<>();
      for (String indexVal : rangeResponse) {
        range.add((K) indexVal);
      }
    }
    return range;
  }
  
  private boolean isNumericKey() {
    return Number.class.isAssignableFrom(keyClass);
  }
  
  private double obtainDoubleValue(K key) {
    return Double.parseDouble(key.toString());
  }
}
