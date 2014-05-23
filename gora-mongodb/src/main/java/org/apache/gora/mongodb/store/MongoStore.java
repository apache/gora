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
package org.apache.gora.mongodb.store;

import static org.apache.gora.mongodb.store.MongoMapping.DocumentFieldType;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.DatatypeConverter;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.gora.mongodb.query.MongoDBQuery;
import org.apache.gora.mongodb.query.MongoDBResult;
import org.apache.gora.mongodb.utils.BSONDecorator;
import org.apache.gora.mongodb.utils.GoraDBEncoder;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.mongodb.*;

/**
 * Implementation of a MongoDB data store to be used by gora.
 * 
 * @param <K>
 *          class to be used for the key
 * @param <T>
 *          class to be persisted within the store
 * @author Fabien Poulard <fpoulard@dictanova.com>
 * @author Damien Raude-Morvan <draudemorvan@dictanova.com>
 */
public class MongoStore<K, T extends PersistentBase> extends
    DataStoreBase<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(MongoStore.class);

  // Configuration properties

  /**
   * Property indicating if the hadoop configuration has priority or not
   */
  public static final String PROP_OVERRIDING = "gora.mongodb.override_hadoop_configuration";

  /**
   * Property pointing to the file for the mapping
   */
  public static final String PROP_MAPPING_FILE = "gora.mongodb.mapping.file";

  /**
   * Property pointing to the host where the server is running
   */
  public static final String PROP_MONGO_SERVERS = "gora.mongodb.servers";

  /**
   * Property pointing to the username to connect to the server
   */
  public static final String PROP_MONGO_LOGIN = "gora.mongodb.login";

  /**
   * Property pointing to the secret to connect to the server
   */
  public static final String PROP_MONGO_SECRET = "gora.mongodb.secret";

  /**
   * Default value for mapping file
   */
  public static final String DEFAULT_MAPPING_FILE = "/gora-mongodb-mapping.xml";

  /**
   * Property to select the database
   */
  public static String PROP_MONGO_DB = "gora.mongodb.db";

  /**
   * MongoDB client
   */
  private static ConcurrentHashMap<String, MongoClient> mapsOfClients = new ConcurrentHashMap<String, MongoClient>();

  private DB mongoClientDB;

  private DBCollection mongoClientColl;

  /**
   * Mapping definition for MongoDB
   */
  private MongoMapping mapping;

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file.
   */
  public void initialize(Class<K> keyClass, Class<T> pPersistentClass,
      Properties properties) {
    try {
      LOG.debug("Initializing MongoDB store");

      // Prepare the configuration
      String vPropMappingFile = properties.getProperty(PROP_MAPPING_FILE,
          DEFAULT_MAPPING_FILE);
      String vPropMongoServers = properties.getProperty(PROP_MONGO_SERVERS);
      String vPropMongoLogin = properties.getProperty(PROP_MONGO_LOGIN);
      String vPropMongoSecret = properties.getProperty(PROP_MONGO_SECRET);
      String vPropMongoDb = properties.getProperty(PROP_MONGO_DB);
      String overrideHadoop = properties.getProperty(PROP_OVERRIDING);
      if (!Boolean.parseBoolean(overrideHadoop)) {
        LOG.debug("Hadoop configuration has priority.");
        vPropMappingFile = getConf().get(PROP_MAPPING_FILE, vPropMappingFile);
        vPropMongoServers = getConf()
            .get(PROP_MONGO_SERVERS, vPropMongoServers);
        vPropMongoLogin = getConf().get(PROP_MONGO_LOGIN, vPropMongoLogin);
        vPropMongoSecret = getConf().get(PROP_MONGO_SECRET, vPropMongoSecret);
        vPropMongoDb = getConf().get(PROP_MONGO_DB, vPropMongoDb);
      }
      super.initialize(keyClass, pPersistentClass, properties);

      // Load the mapping
      MongoMappingBuilder<K, T> builder = new MongoMappingBuilder<K, T>(this);
      LOG.debug("Initializing Mongo store with mapping {}.",
          new Object[] { vPropMappingFile });
      builder.fromFile(vPropMappingFile);
      mapping = builder.build();

      // Prepare MongoDB connection
      mongoClientDB = getDB(vPropMongoServers, vPropMongoDb, vPropMongoLogin,
          vPropMongoSecret);
      mongoClientColl = mongoClientDB
          .getCollection(mapping.getCollectionName());

      LOG.info("Initialized Mongo store for database {} of {}.", new Object[] {
          vPropMongoDb, vPropMongoServers });
    } catch (IOException e) {
      LOG.error("Error while initializing MongoDB store: {}",
          new Object[] { e.getMessage() });
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieve a client connected to the MongoDB server to be used.
   * 
   * @param servers
   *          This value should specify the host:port (at least one) for
   *          connecting to remote MongoDB. Multiple values must be separated by
   *          coma.
   * @return a {@link Mongo} instance connected to the server
   * @throws UnknownHostException
   */
  private MongoClient getClient(String servers) throws UnknownHostException {
    // Configure options
    MongoClientOptions opts = new MongoClientOptions.Builder()
        .dbEncoderFactory(GoraDBEncoder.FACTORY) // Utf8 serialization!
        .build();
    // Build server address
    List<ServerAddress> addrs = new ArrayList<ServerAddress>();
    Iterable<String> serversArray = Splitter.on(",").split(servers);
    if (serversArray != null) {
      for (String server : serversArray) {
        Iterable<String> params = Splitter.on(":").trimResults().split(server);
        Iterator<String> paramsIterator = params.iterator();
        if (!paramsIterator.hasNext()) {
          // No server, use default
          addrs.add(new ServerAddress());
        } else {
          String host = paramsIterator.next();
          if (paramsIterator.hasNext()) {
            String port = paramsIterator.next();
            addrs.add(new ServerAddress(host, Integer.parseInt(port)));
          } else {
            addrs.add(new ServerAddress(host));
          }
        }
      }
    }
    // Connect to the Mongo server
    return new MongoClient(addrs, opts);
  }

  /**
   * Get reference to Mongo DB, using credentials if not null.
   * 
   * @param servers
   * @param dbname
   *          Name of database to connect to.
   * @param login
   *          Optionnal login for remote database.
   * @param secret
   *          Optional secret for remote database.
   * @return a {@link DB} instance from <tt>mongoClient</tt> or null if
   *         authentication request failed.
   */
  private DB getDB(String servers, String dbname, String login, String secret)
      throws UnknownHostException {

    // Get reference to Mongo DB
    if (!mapsOfClients.containsKey(servers))
      mapsOfClients.put(servers, getClient(servers));
    DB db = mapsOfClients.get(servers).getDB(dbname);
    // By default, we are authenticated
    boolean auth = true;
    // If configuration contains a login + secret, try to authenticated with DB
    if (login != null && secret != null) {
      auth = db.authenticate(login, secret.toCharArray());
    }

    if (auth) {
      return db;
    } else {
      return null;
    }
  }

  /**
   * Accessor to the name of the collection used.
   */
  @Override
  public String getSchemaName() {
    return mapping.getCollectionName();
  }

  @Override
  public String getSchemaName(String mappingSchemaName, Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  /**
   * Create a new collection in MongoDB if necessary.
   */
  @Override
  public void createSchema() {
    if (mongoClientDB == null)
      throw new IllegalStateException(
          "Impossible to create the schema as no database has been selected.");
    if (schemaExists()) {
      return;
    }

    // If initialized create the collection
    mongoClientColl = mongoClientDB.createCollection(
        mapping.getCollectionName(), new BasicDBObject()); // send a DBObject to
    // force creation
    // otherwise creation is deferred
    mongoClientColl.setDBEncoderFactory(GoraDBEncoder.FACTORY);

    LOG.info("Collection {} has been created for Mongo instance {}.",
        new Object[] { mapping.getCollectionName(), mongoClientDB.getMongo() });
  }

  /**
   * Drop the collection.
   */
  @Override
  public void deleteSchema() {
    if (mongoClientColl == null)
      throw new IllegalStateException(
          "Impossible to delete the schema as no schema is selected.");
    // If initialized, simply drop the collection
    mongoClientColl.drop();

    LOG.info(
        "Collection {} has been dropped for Mongo instance {}.",
        new Object[] { mongoClientColl.getFullName(), mongoClientDB.getMongo() });
  }

  /**
   * Check if the collection already exists or should be created.
   */
  @Override
  public boolean schemaExists() {
    return mongoClientDB.collectionExists(mapping.getCollectionName());
  }

  /**
   * Ensure the data is synced to disk.
   */
  @Override
  public void flush() {
    for (MongoClient client : mapsOfClients.values()) {
      client.fsync(false);
      LOG.info("Forced synced of database for Mongo instance {}.",
          new Object[] { client });
    }
  }

  /**
   * Release the resources linked to this collection
   */
  @Override
  public void close() {
    mongoClientDB.cleanCursors(true);
    // mongoClient.close();
    // LOG.info("Closed database and connection for Mongo instance {}.",
    // new Object[]{mongoClient});
    LOG.debug("Not closed!!!");
  }

  /**
   * Retrieve an entry from the store with only selected fields.
   * 
   * @param key
   *          identifier of the document in the database
   * @param fields
   *          list of fields to be loaded from the database
   * @throws IOException
   */
  @Override
  public T get(K key, String[] fields) {
    fields = getFieldsToQuery(fields);
    // Prepare the MongoDB query
    BasicDBObject q = new BasicDBObject("_id", key);
    BasicDBObject proj = new BasicDBObject();
    for (String field : fields) {
      String docf = mapping.getDocumentField(field);
      if (docf != null) {
        proj.put(docf, true);
      }
    }
    // Execute the query
    DBObject res = mongoClientColl.findOne(q, proj);
    // Build the corresponding persistent and clears its states
    T persistent = newInstance(res, fields);
    if (persistent != null) {
      persistent.clearDirty();
    }
    // Done
    return persistent;
  }

  /**
   * Persist an object into the store.
   * 
   * @param key
   *          identifier of the object in the store
   * @param obj
   *          the object to be inserted
   */
  @Override
  public void put(K key, T obj) {
    // Save the object in the database
    if (obj.isDirty()) {
      performPut(key, obj);
    } else {
      LOG.info("Ignored putting object {} in the store as it is neither "
          + "new, neither dirty.", new Object[] { obj });
    }
  }

  /**
   * Update a object that already exists in the store. The object must exist
   * already or the update may fail.
   * 
   * @param key
   *          identifier of the object in the store
   * @param obj
   *          the object to be inserted
   */
  private void performPut(K key, T obj) {
    // Build the query to select the object to be updated
    DBObject qSel = new BasicDBObject("_id", key);

    // Build the update query
    BasicDBObject qUpdate = new BasicDBObject();

    BasicDBObject qUpdateSet = newUpdateSetInstance(obj);
    if (qUpdateSet.size() > 0) {
      qUpdate.put("$set", qUpdateSet);
    }

    BasicDBObject qUpdateUnset = newUpdateUnsetInstance(obj);
    if (qUpdateUnset.size() > 0) {
      qUpdate.put("$unset", qUpdateUnset);
    }

    // Execute the update (if there is at least one $set ot $unset
    if (!qUpdate.isEmpty()) {
      mongoClientColl.update(qSel, qUpdate, true, false);
      obj.clearDirty();
    } else {
      LOG.debug("No update to perform, skip {}", key);
    }
  }

  @Override
  public boolean delete(K key) {
    DBObject removeKey = new BasicDBObject("_id", key);
    WriteResult writeResult = mongoClientColl.remove(removeKey);
    return writeResult != null && writeResult.getN() > 0;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    // Build the actual MongoDB query
    DBObject q = MongoDBQuery.toDBQuery(query);
    WriteResult writeResult = mongoClientColl.remove(q);
    if (writeResult != null) {
      return writeResult.getN();
    }
    return 0;
  }

  /**
   * Execute the query and return the result.
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) {

    String[] fields = getFieldsToQuery(query.getFields());
    // Build the actual MongoDB query
    DBObject q = MongoDBQuery.toDBQuery(query);
    DBObject p = MongoDBQuery.toProjection(fields, mapping);

    // Execute the query on the collection
    DBCursor cursor = mongoClientColl.find(q, p);
    if (query.getLimit() > 0)
      cursor = cursor.limit((int) query.getLimit());
    cursor.batchSize(100);

    // Build the result
    MongoDBResult<K, T> mongoResult = new MongoDBResult<K, T>(this, query);
    mongoResult.setCursor(cursor);

    return mongoResult;
  }

  /**
   * Create a new {@link Query} to query the datastore.
   */
  @Override
  public Query<K, T> newQuery() {
    MongoDBQuery<K, T> query = new MongoDBQuery<K, T>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }

  /**
   * Partitions the given query and returns a list of PartitionQuerys, which
   * will execute on local data.
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
      throws IOException {
    // FIXME: for now, there is only one partition as we do not handle
    // MongoDB sharding configuration
    List<PartitionQuery<K, T>> partitions = new ArrayList<PartitionQuery<K, T>>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<K, T>(
        query);
    partitionQuery.setConf(getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  // //////////////////////////////////////////////////////// DESERIALIZATION

  /**
   * Build a new instance of the persisted class from the {@link DBObject}
   * retrieved from the database.
   * 
   * @param obj
   *          the {@link DBObject} that results from the query to the database
   * @param fields
   *          the list of fields to be mapped to the persistence class instance
   * @return a persistence class instance which content was deserialized from
   *         the {@link DBObject}
   * @throws IOException
   */
  public T newInstance(DBObject obj, String[] fields) {
    if (obj == null)
      return null;
    BSONDecorator easybson = new BSONDecorator(obj);
    // Create new empty persistent bean instance
    T persistent = newPersistent();
    fields = getFieldsToQuery(fields);

    // Populate each field
    for (String f : fields) {
      // Check the field exists in the mapping and in the db
      String docf = mapping.getDocumentField(f);
      if (docf == null || !easybson.containsField(docf))
        continue;

      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();

      LOG.debug(
          "Load from DBObject (MAIN), field:{}, schemaType:{}, docField:{}, storeType:{}",
          new Object[] { field.name(), fieldSchema.getType(), docf, storeType });
      Object result = fromDBObject(fieldSchema, storeType, field, docf,
          easybson);
      persistent.put(field.pos(), result);
    }
    persistent.clearDirty();
    return persistent;
  }

  private Object fromDBObject(final Schema fieldSchema,
      final DocumentFieldType storeType, final Field field, final String docf,
      final BSONDecorator easybson) {
    Object result = null;
    switch (fieldSchema.getType()) {
    case MAP:
      BasicDBObject map = easybson.getDBObject(docf);
      if (map != null) {
        result = fromMongoMap(fieldSchema, map);
      }
      break;
    case ARRAY:
      List<Object> list = easybson.getDBList(docf);
      if (list != null) {
        result = fromMongoList(fieldSchema, list);
      }
      break;
    case RECORD:
      DBObject rec = easybson.getDBObject(docf);
      if (rec == null) {
        return result;
      }
      BSONDecorator innerBson = new BSONDecorator(rec);
      Class<?> clazz = null;
      try {
        clazz = ClassLoadingUtils.loadClass(fieldSchema.getFullName());
      } catch (ClassNotFoundException e) {
      }
      Persistent record = new BeanFactoryImpl(keyClass, clazz).newPersistent();
      for (Field recField : fieldSchema.getFields()) {
        Schema innerSchema = recField.schema();
        DocumentFieldType innerStoreType = mapping
            .getDocumentFieldType(innerSchema.getName());
        String innerDocField = mapping.getDocumentField(recField.name()) != null ? mapping
            .getDocumentField(recField.name()) : recField.name();
        String fieldPath = docf + "." + innerDocField;
        LOG.debug(
            "Load from DBObject (RECORD), field:{}, schemaType:{}, docField:{}, storeType:{}",
            new Object[] { recField.name(), innerSchema.getType(), fieldPath,
                innerStoreType });
        record.put(
            recField.pos(),
            fromDBObject(innerSchema, innerStoreType, recField, innerDocField,
                innerBson));
      }
      result = record;
      break;
    case BOOLEAN:
      result = easybson.getBoolean(docf);
      break;
    case DOUBLE:
      result = easybson.getDouble(docf);
      break;
    case FLOAT:
      result = easybson.getDouble(docf).floatValue();
      break;
    case INT:
      result = easybson.getInt(docf);
      break;
    case LONG:
      result = easybson.getLong(docf);
      break;
    case STRING:
      result = fromMongoString(storeType, docf, easybson);
      break;
    case ENUM:
      result = AvroUtils.getEnumValue(fieldSchema, easybson.getUtf8String(docf)
          .toString());
      break;
    case BYTES:
    case FIXED:
      result = easybson.getBytes(docf);
      break;
    case NULL:
      result = null;
      break;
    case UNION:
      // schema [type0, type1]
      Type type0 = fieldSchema.getTypes().get(0).getType();
      Type type1 = fieldSchema.getTypes().get(1).getType();

      // Check if types are different and there's a "null", like ["null","type"]
      // or ["type","null"]
      if (!type0.equals(type1)
          && (type0.equals(Type.NULL) || type1.equals(Type.NULL))) {
        Schema innerSchema = fieldSchema.getTypes().get(1);
        LOG.debug(
            "Load from DBObject (UNION), schemaType:{}, docField:{}, storeType:{}",
            new Object[] { innerSchema.getType(), docf, storeType });
        // Deserialize as if schema was ["type"]
        result = fromDBObject(innerSchema, storeType, field, docf, easybson);
      } else {
        throw new IllegalStateException(
            "MongoStore doesn't support 3 types union field yet. Please update your mapping");
      }
      break;
    default:
      LOG.warn("Unable to read {}", docf);
      break;
    }
    return result;
  }

  private Object fromMongoList(Schema fieldSchema, List<Object> list) {
    Object result;
    switch (fieldSchema.getElementType().getType()) {
    case STRING:
      List<Utf8> arrS = new ArrayList<Utf8>();
      for (Object o : list)
        arrS.add(new Utf8((String) o));
      result = new DirtyListWrapper<Utf8>(arrS);
      break;
    case BYTES:
      List<ByteBuffer> arrB = new ArrayList<ByteBuffer>();
      for (Object o : list)
        arrB.add(ByteBuffer.wrap((byte[]) o));
      result = new DirtyListWrapper<ByteBuffer>(arrB);
      break;
    default:
      List<Object> arrT = new ArrayList<Object>();
      for (Object o : list)
        arrT.add(o);
      result = new DirtyListWrapper<Object>(arrT);
      break;
    }
    return result;
  }

  private Object fromMongoMap(Schema fieldSchema, BasicDBObject map) {
    Object result;
    Map<Utf8, Object> rmap = new HashMap<Utf8, Object>();
    for (Entry<String, Object> e : map.entrySet()) {
      String mKey = decodeFieldKey(e.getKey());
      Object mValue = e.getValue();
      switch (fieldSchema.getValueType().getType()) {
      case STRING:
        rmap.put(new Utf8(mKey), new Utf8((String) mValue));
        break;
      case BYTES:
        rmap.put(new Utf8(mKey), ByteBuffer.wrap((byte[]) mValue));
        break;
      default:
        rmap.put(new Utf8(mKey), mValue);
        break;
      }
    }
    result = new DirtyMapWrapper(rmap);
    return result;
  }

  private Object fromMongoString(final DocumentFieldType storeType,
      final String docf, final BSONDecorator easybson) {
    Object result;
    if (storeType == DocumentFieldType.OBJECTID) {
      // Try auto-conversion of BSON data to ObjectId
      // It will work if data is stored as String or as ObjectId
      Object bin = easybson.get(docf);
      ObjectId id = ObjectId.massageToObjectId(bin);
      result = new Utf8(id.toString());
    } else if (storeType == DocumentFieldType.DATE) {
      Object bin = easybson.get(docf);
      if (bin instanceof Date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime((Date) bin);
        result = new Utf8(DatatypeConverter.printDateTime(calendar));
      } else {
        result = new Utf8(bin.toString());
      }
    } else {
      result = easybson.getUtf8String(docf);
    }
    return result;
  }

  // ////////////////////////////////////////////////////////// SERIALIZATION

  /**
   * Build a new instance of {@link DBObject} from the persistence class
   * instance in parameter. Limit the {@link DBObject} to the fields that are
   * dirty and not null, that is the fields that will need to be updated in the
   * store.
   * 
   * @param persistent
   *          a persistence class instance which content is to be serialized as
   *          a {@link DBObject} for use as parameter of a $set operator
   * @return a {@link DBObject} which content corresponds to the fields that
   *         have to be updated... and formatted to be passed in parameter of a
   *         $set operator
   */
  private BasicDBObject newUpdateSetInstance(T persistent) {
    BasicDBObject result = new BasicDBObject();
    for (Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
        String docf = mapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
        LOG.debug(
            "Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}",
            new Object[] { docf, f.schema().getType(), storeType });
        result.put(docf,
            toDBObject(f.schema(), f.schema().getType(), storeType, value));
      }
    }
    return result;
  }

  /**
   * Build a new instance of {@link DBObject} from the persistence class
   * instance in parameter. Limit the {@link DBObject} to the fields that are
   * dirty and null, that is the fields that will need to be updated in the
   * store by being removed.
   * 
   * @param persistent
   *          a persistence class instance which content is to be serialized as
   *          a {@link DBObject} for use as parameter of a $set operator
   * @return a {@link DBObject} which content corresponds to the fields that
   *         have to be updated... and formated to be passed in parameter of a
   *         $unset operator
   */
  private BasicDBObject newUpdateUnsetInstance(T persistent) {
    BasicDBObject result = new BasicDBObject();
    for (Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) == null)) {
        String docf = mapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
        LOG.debug(
            "Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}",
            new Object[] { docf, f.schema().getType(), storeType });
        Object o = toDBObject(f.schema(), f.schema().getType(), storeType,
            value);
        result.put(docf, o);
      }
    }
    return result;
  }

  private Object toDBObject(Schema fieldSchema, Type fieldType,
      DocumentFieldType storeType, Object value) {
    Object result = null;
    switch (fieldType) {
    case MAP:
      if (storeType != null && storeType != DocumentFieldType.DOCUMENT) {
        throw new IllegalStateException(
            "Field "
                + fieldSchema.getType()
                + ": to store a Gora 'map', target Mongo mapping have to be of 'document' type");
      }
      Schema valueSchema = fieldSchema.getValueType();
      result = mapToMongo((Map<CharSequence, ?>) value, valueSchema.getType());
      break;
    case ARRAY:
      if (storeType != null && storeType != DocumentFieldType.LIST) {
        throw new IllegalStateException(
            "Field "
                + fieldSchema.getType()
                + ": To store a Gora 'array', target Mongo mapping have to be of 'list' type");
      }
      Schema elementSchema = fieldSchema.getElementType();
      result = listToMongo((List<?>) value, elementSchema.getType());
      break;
    case BYTES:
      // Beware of ByteBuffer not being safely serialized
      if (value != null) {
        result = ((ByteBuffer) value).array();
      }
      break;
    case INT:
    case LONG:
    case FLOAT:
    case DOUBLE:
    case BOOLEAN:
      result = value;
      break;
    case STRING:
      result = stringToMongo(fieldSchema, storeType, value);
      break;
    case ENUM:
      // Beware of Utf8 not being safely serialized
      if (value != null)
        result = value.toString();
      break;
    case RECORD:
      if (value == null)
        break;
      BasicDBObject record = new BasicDBObject();
      for (Field member : fieldSchema.getFields()) {
        Object innerValue = ((PersistentBase) value).get(member.pos());
        String innerDoc = mapping.getDocumentField(member.name());
        Type innerType = member.schema().getType();
        DocumentFieldType innerStoreType = mapping
            .getDocumentFieldType(innerDoc);
        LOG.debug(
            "Transform value to DBObject (RECORD), docField:{}, schemaType:{}, storeType:{}",
            new Object[] { member.name(), member.schema().getType(),
                innerStoreType });
        record.put(member.name(),
            toDBObject(member.schema(), innerType, innerStoreType, innerValue));
      }
      result = record;
      break;
    case UNION:
      // schema [type0, type1]
      Type type0 = fieldSchema.getTypes().get(0).getType();
      Type type1 = fieldSchema.getTypes().get(1).getType();

      // Check if types are different and there's a "null", like ["null","type"]
      // or ["type","null"]
      if (!type0.equals(type1)
          && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {
        Schema innerSchema = fieldSchema.getTypes().get(1);
        LOG.debug(
            "Transform value to DBObject (UNION), schemaType:{}, type1:{}, storeType:{}",
            new Object[] { innerSchema.getType(), type1, storeType });
        // Deserialize as if schema was ["type"]
        result = toDBObject(innerSchema, type1, storeType, value);
      } else {
        throw new IllegalStateException(
            "MongoStore doesn't support 3 types union field yet. Please update your mapping");
      }
      break;
    case FIXED:
      result = value;
      break;

    default:
      LOG.error("Unknown field type: " + fieldSchema.getType());
      break;
    }

    return result;
  }

  private Object stringToMongo(final Schema fieldSchema,
      final DocumentFieldType storeType, final Object value) {
    Object result = null;
    if (storeType == DocumentFieldType.OBJECTID) {
      if (value != null) {
        ObjectId id;
        try {
          id = new ObjectId(value.toString());
        } catch (IllegalArgumentException e1) {
          // Unable to parse anything from Utf8 value, throw error
          throw new IllegalStateException("Field " + fieldSchema.getType()
              + ": Invalid string: unable to convert to ObjectId");
        }
        result = id;
      }
    } else if (storeType == DocumentFieldType.DATE) {
      if (value != null) {
        // Try to parse date from Utf8 value
        Calendar calendar = null;
        try {
          // Parse as date + time
          calendar = DatatypeConverter.parseDateTime(value.toString());
        } catch (IllegalArgumentException e1) {
          try {
            // Parse as date only
            calendar = DatatypeConverter.parseDate(value.toString());
          } catch (IllegalArgumentException e2) {
            // No-op
          }
        }
        if (calendar == null) {
          // Unable to parse anything from Utf8 value, throw error
          throw new IllegalStateException("Field " + fieldSchema.getType()
              + ": Invalid date format '" + value + "'");
        }
        result = calendar.getTime();
      }
    } else {
      // Beware of Utf8 not being safely serialized
      if (value != null) {
        result = value.toString();
      }
    }
    return result;
  }

  /**
   * Convert a Java Map as used in Gora generated classes to a Map that can
   * safely be serialized into MongoDB.
   * 
   * @param jmap
   *          the Java Map that must be serialized into a MongoDB object
   * @param type
   *          type of the values within the map
   * @return a {@link BasicDBObject} version of the {@link Map} that can be
   *         safely serialized into MongoDB.
   */
  private BasicDBObject mapToMongo(Map<CharSequence, ?> jmap, Type type) {
    // Handle null case
    if (jmap == null)
      return null;
    // Handle regular cases
    BasicDBObject map = new BasicDBObject();
    for (Entry<CharSequence, ?> e : jmap.entrySet()) {
      String mKey = encodeFieldKey(e.getKey().toString());
      Object mValue = e.getValue();
      switch (type) {
      case STRING:
        // Beware of Utf8 not being safely serialized
        map.put(mKey, mValue.toString());
        break;
      case BYTES:
        // Beware of ByteBuffer not being safely serialized
        map.put(mKey, ((ByteBuffer) mValue).array());
        break;
      // FIXME Record ?
      default:
        map.put(mKey, e.getValue());
        break;
      }
    }
    return map;
  }

  /**
   * Convert a Java {@link GenericArray} as used in Gora generated classes to a
   * List that can safely be serialized into MongoDB.
   * 
   * @param array
   *          the {@link GenericArray} to be serialized
   * @param type
   *          type of the elements within the array
   * @return a {@link BasicDBList} version of the {@link GenericArray} that can
   *         be safely serialized into MongoDB.
   */
  private BasicDBList listToMongo(Collection<?> array, Type type) {
    // Handle null case
    if (array == null)
      return null;
    // Handle regular cases
    BasicDBList list = new BasicDBList();
    for (Object item : array) {
      switch (type) {
      case STRING:
        // Beware of Utf8 not being safely serialized
        list.add(item.toString());
        break;
      case BYTES:
        // Beware of ByteBuffer not being safely serialized
        list.add(((ByteBuffer) item).array());
        break;
      // FIXME Record ?
      default:
        list.add(item);
        break;
      }
    }
    return list;
  }

  // //////////////////////////////////////////////////////// CLEANUP

  /**
   * Ensure Key encoding -> dots replaced with middle dots
   * 
   * @param key
   *          char with only dots.
   * @return encoded string with "\u00B7" chars..
   */
  protected String encodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace(".", "\u00B7");
  }

  /**
   * Ensure Key decoding -> middle dots replaced with dots
   * 
   * @param key
   *          encoded string with "\u00B7" chars.
   * @return Cleanup up char with only dots.
   */
  protected String decodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace("\u00B7", ".");
  }
}
