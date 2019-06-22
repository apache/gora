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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.avro.Schema;
import org.apache.commons.io.IOUtils;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.redis.util.DatumHandler;
import org.apache.gora.redis.util.StorageMode;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Implementation of a Redis data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class RedisStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  protected static final String MOCK_PROPERTY = "redis.mock";
  protected static final String INSTANCE_NAME_PROPERTY = "redis.instance";
  protected static final String ZOOKEEPERS_NAME_PROPERTY = "redis.zookeepers";
  protected static final String USERNAME_PROPERTY = "redis.user";
  protected static final String PASSWORD_PROPERTY = "redis.password";
  protected static final String PARSE_MAPPING_FILE_KEY = "gora.redis.mapping.file";
  protected static final String DEFAULT_MAPPING_FILE = "gora-redis-mapping.xml";
  protected static final String XML_MAPPING_DEFINITION = "gora.mapping";
  private RedissonClient redisInstance;
  private RedisMapping mapping;
  public static final Logger LOG = LoggerFactory.getLogger(RedisStore.class);

  private static final DatumHandler handler = new DatumHandler();
  private StorageMode mode;

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file. Initialize is called when then
   * the call to {@link org.apache.gora.store.DataStoreFactory#createDataStore}
   * is made.
   *
   * @param keyClass
   * @param persistentClass
   * @param properties
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);

      InputStream mappingStream;
      if (properties.containsKey(XML_MAPPING_DEFINITION)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace(XML_MAPPING_DEFINITION + " = " + properties.getProperty(XML_MAPPING_DEFINITION));
        }
        mappingStream = IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
      } else {
        mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      }
      mapping = readMapping(mappingStream);
      Config config = new Config();
      String storage = getConf().get("gora.datastore.redis.storage", properties.getProperty("gora.datastore.redis.storage"));
      mode = StorageMode.valueOf(storage);
      String mode = properties.getProperty("gora.datastore.redis.mode");
      String name = properties.getProperty("gora.datastore.redis.masterName");
      String readm = properties.getProperty("gora.datastore.redis.readMode");
      //Override address in tests
      String[] hosts = getConf().get("gora.datastore.redis.address", properties.getProperty("gora.datastore.redis.address")).split(",");
      switch (mode) {
        case "single":
          config.useSingleServer()
              .setAddress("redis://" + hosts[0])
              .setDatabase(mapping.getDatebase());
          break;
        case "cluster":
          config.useClusterServers()
              .addNodeAddress(hosts);
          break;
        case "replicated":
          config.useReplicatedServers()
              .addNodeAddress(hosts)
              .setDatabase(mapping.getDatebase());
          break;
        case "sentinel":
          config.useSentinelServers()
              .setMasterName(name)
              .setReadMode(ReadMode.valueOf(readm))
              .addSentinelAddress(hosts)
              .setDatabase(mapping.getDatebase());
          break;
      }
      redisInstance = Redisson.create(config);
      if (autoCreateSchema && !schemaExists()) {
        createSchema();
      }
    } catch (IOException ex) {
      throw new GoraException(ex);
    }
  }

  protected RedisMapping readMapping(InputStream inputStream) throws IOException {
    try {
      RedisMapping mapping = new RedisMapping();
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document dom = db.parse(inputStream);
      Element root = dom.getDocumentElement();
      NodeList nl = root.getElementsByTagName("class");
      for (int i = 0; i < nl.getLength(); i++) {
        Element classElement = (Element) nl.item(i);
        if (classElement.getAttribute("keyClass").equals(keyClass.getCanonicalName())
            && classElement.getAttribute("name").equals(persistentClass.getCanonicalName())) {
          mapping.setDatebase(Integer.parseInt(classElement.getAttribute("database")));
          mapping.setPrefix(classElement.getAttribute("prefix"));
          NodeList elementsByTagName = classElement.getElementsByTagName("field");
          Map<String, String> map = new HashMap<>();
          for (int j = 0; j < elementsByTagName.getLength(); j++) {
            Element item = (Element) elementsByTagName.item(j);
            String name = item.getAttribute("name");
            String column = item.getAttribute("column");
            map.put(name, column);
          }
          mapping.setFields(map);
        }
      }
      return mapping;
    } catch (Exception ex) {
      throw new IOException(ex);
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
   */
  @Override
  public void createSchema() throws GoraException {
  }

  /**
   * Redis, being a schemaless database does not support explicit schema
   * creation. When the records are added to the database, the schema is created
   * on the fly. Thus, schema operations are unavailable in gora-redis module.
   */
  @Override
  public void deleteSchema() throws GoraException {
  }

  /**
   * Redis, being a schemaless database does not support explicit schema
   * creation. When the records are added to the database, the schema is created
   * on the fly. Thus, schema operations are unavailable in gora-redis module.
   *
   * @return true
   */
  @Override
  public boolean schemaExists() throws GoraException {
    return true;
  }

  private String generateKeyHash(Object baseKey) {
    return mapping.getPrefix() + ".key." + baseKey;
  }

  private String generateKeyString(String field, Object baseKey) {
    return generateKeyStringBase(baseKey) + field;
  }

  private String generateKeyStringBase(Object baseKey) {
    return mapping.getPrefix() + ".key." + baseKey + ".field.";
  }

  private String generateIndexKey() {
    return mapping.getPrefix() + ".index";
  }

  public T newInstanceFromString(K key, String[] fields) throws GoraException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    int countRetrieved = 0;
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      RBucket<Object> bucket = redisInstance.getBucket(generateKeyString(mapping.getFields().get(field.name()), key));
      Object val = bucket.get();
      if (val == null) {
        continue;
      }
      Object fieldValue = handler.deserializeFieldValue(field, field.schema(), val, persistent);
      countRetrieved++;
      persistent.put(field.pos(), fieldValue);
      persistent.setDirty(field.pos());
    }
    return countRetrieved > 0 ? persistent : null;
  }

  public T newInstanceFromHash(RMap<String, Object> map, String[] fields) throws GoraException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      Object val = map.get(mapping.getFields().get(field.name()));
      if (val == null) {
        continue;
      }
      Object fieldValue = handler.deserializeFieldValue(field, field.schema(), val, persistent);
      persistent.put(field.pos(), fieldValue);
      persistent.setDirty(field.pos());
    }
    return persistent;
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    try {
      if (mode == StorageMode.HASH) {
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
        Schema schema = obj.getSchema();
        List<Schema.Field> fields = schema.getFields();
        //update secundary index
        RScoredSortedSet<K> secundaryIndex = redisInstance.getScoredSortedSet(generateIndexKey());
        secundaryIndex.add(0, key);
        if (mode == StorageMode.HASH) {
          RMap<String, Object> map = redisInstance.getMap(generateKeyHash(key));
          for (Schema.Field field : fields) {
            Object fieldValue = handler.serializeFieldValue(field.schema(), obj.get(field.pos()));
            if (fieldValue != null) {
              map.put(mapping.getFields().get(field.name()), fieldValue);
            }
          }
        } else {
          for (Schema.Field field : fields) {
            Object fieldValue = handler.serializeFieldValue(field.schema(), obj.get(field.pos()));
            if (fieldValue != null) {
              RBucket<Object> bucket = redisInstance.getBucket(generateKeyString(mapping.getFields().get(field.name()), key));
              bucket.set(fieldValue);
            }
          }
        }
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
    //update secundary index
    RScoredSortedSet<K> secundaryIndex = redisInstance.getScoredSortedSet(generateIndexKey());
    secundaryIndex.remove(key);
    if (mode == StorageMode.HASH) {
      RMap<String, Object> map = redisInstance.getMap(generateKeyHash(key));
      return map.delete();
    } else {
      return redisInstance.getKeys().deleteByPattern(generateKeyStringBase(key) + "*") > 0;
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    return 0;
  }

  /**
   * Execute the query and return the result.
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    return null;
  }

  @Override
  public Query<K, T> newQuery() {
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws GoraException {
    return null;
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
    if (mode == StorageMode.HASH) {
      return redisInstance.getKeys().countExists(generateKeyHash(key)) != 0;
    } else {
      Iterator<String> respKeys = redisInstance.getKeys().getKeysByPattern(generateKeyStringBase(key) + "*", 1).iterator();
      return respKeys.hasNext();
    }
  }
}
