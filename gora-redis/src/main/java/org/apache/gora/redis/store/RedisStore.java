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

import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.GoraException;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
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
  private RedissonClient redisInstance;
  private RedisMapping mapping;

  public static final Logger LOG = LoggerFactory.getLogger(RedisStore.class);

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
      String host = properties.getProperty("gora.datastore.redis.instance");
      mapping = readMapping(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));

      Config config = new Config();
      com.fasterxml.jackson.dataformat.avro.AvroMapper am = new AvroMapper();
      AvroSchema schemaFor = new AvroSchema(schema);
      JsonJacksonCodec jsoncodec = new JsonJacksonCodec();
      config.setCodec(jsoncodec);
      config.useSingleServer()
          .setAddress("redis://" + host)
          .setDatabase(mapping.getDatebase());
      redisInstance = Redisson.create(config);
      if (autoCreateSchema && !schemaExists()) {
        createSchema();
      }
    } catch (IOException ex) {
      throw new GoraException(ex);
    }
  }

  protected RedisMapping readMapping(String filename) throws IOException {
    try {
      RedisMapping mapping = new RedisMapping();
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document dom = db.parse(getClass().getClassLoader().getResourceAsStream(filename));
      Element root = dom.getDocumentElement();
      NodeList nl = root.getElementsByTagName("class");
      for (int i = 0; i < nl.getLength(); i++) {
        Element classElement = (Element) nl.item(i);
        if (classElement.getAttribute("keyClass").equals(keyClass.getCanonicalName())
            && classElement.getAttribute("name").equals(persistentClass.getCanonicalName())) {
          mapping.setDatebase(Integer.parseInt(classElement.getAttribute("database")));
          mapping.setPrefix(classElement.getAttribute("prefix"));
        }
      }
      return mapping;
    } catch (Exception ex) {
      throw new IOException("Unable to read " + filename, ex);
    }
  }

  @Override
  public String getSchemaName() {
    return mapping.getDatebase() + "";
  }

  @Override
  public void createSchema() throws GoraException {
  }

  @Override
  public void deleteSchema() throws GoraException {
  }

  @Override
  public boolean schemaExists() throws GoraException {
    return true;
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
    RBucket<T> bucket = redisInstance.getBucket(mapping.getPrefix() + "." + key);
    return bucket.get();
  }

  @Override
  public void put(K key, T val) throws GoraException {
    RBucket<T> bucket = redisInstance.getBucket(mapping.getPrefix() + "." + key);
    bucket.set(val);
  }

  @Override
  public boolean delete(K key) throws GoraException {
    RBucket<T> bucket = redisInstance.getBucket(mapping.getPrefix() + "." + key);
    return bucket.delete();
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

  public boolean exists(K key) throws GoraException {
    RBucket<T> bucket = redisInstance.getBucket(mapping.getPrefix() + "." + key);
    return bucket.isExists();
  }
}