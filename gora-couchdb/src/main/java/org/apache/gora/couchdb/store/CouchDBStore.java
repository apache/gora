/**
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

package org.apache.gora.couchdb.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.util.Utf8;
import org.apache.commons.lang.StringUtils;
import org.apache.gora.couchdb.query.CouchDBQuery;
import org.apache.gora.couchdb.query.CouchDBResult;
import org.apache.gora.couchdb.util.CouchDBObjectMapperFactory;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.apache.gora.util.GoraException;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

/**
 * Implementation of a CouchDB data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class CouchDBStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  /**
   * Logging implementation
   */
  protected static final Logger LOG = LoggerFactory.getLogger(CouchDBStore.class);

  /**
   * The default file name value to be used for obtaining the CouchDB object field mapping's
   */
  public static final String DEFAULT_MAPPING_FILE = "gora-couchdb-mapping.xml";

  /**
   * for bulk document operations
   */
  private final List<Object> bulkDocs = new ArrayList<>();

  /**
   * Mapping definition for CouchDB
   */
  private CouchDBMapping mapping;

  /**
   * The standard implementation of the CouchDbInstance interface. This interface provides methods for
   * managing databases on the connected CouchDb instance.
   * StdCouchDbInstance is thread-safe.
   */
  private CouchDbInstance dbInstance;

  /**
   * The standard implementation of the CouchDbConnector interface. This interface provides methods for
   * manipulating documents within a specific database.
   * StdCouchDbConnector is thread-safe.
   */
  private CouchDbConnector db;

  /**
   * Initialize the data store by reading the credentials, setting the client's properties up and
   * reading the mapping file. Initialize is called when then the call to
   * {@link org.apache.gora.store.DataStoreFactory#createDataStore} is made.
   *
   * @param keyClass
   * @param persistentClass
   * @param properties
   * @throws GoraException 
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    LOG.debug("Initializing CouchDB store");
    super.initialize(keyClass, persistentClass, properties);

    final CouchDBParameters params = CouchDBParameters.load(properties);

    try {
      final String mappingFile = DataStoreFactory.getMappingFile(properties, this, DEFAULT_MAPPING_FILE);
      final HttpClient httpClient = new StdHttpClient.Builder()
          .url("http://" + params.getServer() + ":" + params.getPort())
          .build();

      dbInstance = new StdCouchDbInstance(httpClient);

      final CouchDBMappingBuilder<K, T> builder = new CouchDBMappingBuilder<>(this);
      LOG.debug("Initializing CouchDB store with mapping {}.", new Object[] { mappingFile });
      builder.readMapping(mappingFile);
      mapping = builder.build();

      final ObjectMapperFactory myObjectMapperFactory = new CouchDBObjectMapperFactory();
      myObjectMapperFactory.createObjectMapper().addMixInAnnotations(persistentClass, CouchDbDocument.class);

      db = new StdCouchDbConnector(mapping.getDatabaseName(), dbInstance, myObjectMapperFactory);
      db.createDatabaseIfNotExists();
    } catch (Exception e) {
      throw new GoraException("Error while initializing CouchDB store", e);
    }
  }

  /**
   * In CouchDB, Schemas are referred to as database name.
   *
   * @return databasename
   */
  @Override
  public String getSchemaName() {
    return mapping.getDatabaseName();
  }

  /**
   * In CouchDB, Schemas are referred to as database name.
   *
   * @param mappingSchemaName the name of the schema as read from the mapping file
   * @param persistentClass   persistent class
   * @return database name
   */
  @Override
  public String getSchemaName(final String mappingSchemaName, final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  /**
   * Create a new database in CouchDB if necessary.
   */
  @Override
  public void createSchema() throws GoraException {
    try {
      if (schemaExists()) {
        return;
      }
      dbInstance.createDatabase(mapping.getDatabaseName());
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Drop the database.
   */
  @Override
  public void deleteSchema() throws GoraException {
    try {
      if (schemaExists()) {
        dbInstance.deleteDatabase(mapping.getDatabaseName());
      }
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }     
  }

  /**
   * Check if the database already exists or should be created.
   */
  @Override
  public boolean schemaExists() throws GoraException {
    try {
      return dbInstance.checkIfDbExists(mapping.getDatabaseName());
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Retrieve an entry from the store with only selected fields.
   *
   * @param key    identifier of the document in the database
   * @param fields list of fields to be loaded from the database
   */
  @Override
  public T get(final K key, final String[] fields) throws GoraException {

    final Map<String, Object>  result;
    try {
      result = db.get(Map.class, key.toString());
      return newInstance(result, getFieldsToQuery(fields));
    } catch (DocumentNotFoundException e) {
      return null ;
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public boolean exists(final K key) throws GoraException {
    try {
      return db.contains(key.toString());
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Persist an object into the store.
   *
   * @param key identifier of the object in the store
   * @param obj the object to be inserted
   */
  @Override
  public void put(K key, T obj) throws GoraException {
    final Map<String, Object> buffer = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
    buffer.put("_id", key);

    Schema schema = obj.getSchema();

    List<Field> fields = schema.getFields();
    for (int i = 0; i < fields.size(); i++) {
      if (!obj.isDirty(i)) {
        continue;
      }
      Field field = fields.get(i);
      Object fieldValue = obj.get(field.pos());

      Schema fieldSchema = field.schema();

      // check if field has a nested structure (array, map, record or union)
      fieldValue = toDBObject(fieldSchema, fieldValue);
      buffer.put(field.name(), fieldValue);
    }
    bulkDocs.add(buffer);

  }

  private Map<String, Object> mapToCouchDB(final Object fieldValue) {
    final Map<String, Object> newMap = new LinkedHashMap<>();
    final Map<?, ?> fieldMap = (Map<?, ?>) fieldValue;
    if (fieldValue == null) {
      return null;
    }
    for (Object key : fieldMap.keySet()) {
      newMap.put(key.toString(), fieldMap.get(key).toString());
    }
    return newMap;
  }

  private List<Object> listToCouchDB(final Schema fieldSchema, final Object fieldValue) {
    final List<Object> list = new LinkedList<>();
    for (Object obj : (List<Object>) fieldValue) {
      list.add(toDBObject(fieldSchema.getElementType(), obj));
    }
    return list;
  }

  private Map<String, Object> recordToCouchDB(final Schema fieldSchema, final Object fieldValue) {
    final PersistentBase persistent = (PersistentBase) fieldValue;
    final Map<String, Object> newMap = new LinkedHashMap<>();

    if (persistent != null) {
      for (Field member : fieldSchema.getFields()) {
        Schema memberSchema = member.schema();
        Object memberValue = persistent.get(member.pos());
        newMap.put(member.name(), toDBObject(memberSchema, memberValue));
      }
      return newMap;
    }
    return null;
  }

  private String bytesToCouchDB(final Object fieldValue) {
    return new String(((ByteBuffer) fieldValue).array(), StandardCharsets.UTF_8);
  }

  private Object unionToCouchDB(final Schema fieldSchema, final Object fieldValue) {
    Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
    Schema.Type type1 = fieldSchema.getTypes().get(1).getType();

    // Check if types are different and there's a "null", like ["null","type"]
    // or ["type","null"]
    if (!type0.equals(type1)
        && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {
      Schema innerSchema = fieldSchema.getTypes().get(1);
      LOG.debug("Transform value to DBObject (UNION), schemaType:{}, type1:{}",
          new Object[] { innerSchema.getType(), type1 });

      // Deserialize as if schema was ["type"]
      return toDBObject(innerSchema, fieldValue);
    } else {
      throw new IllegalStateException(
          "CouchDBStore doesn't support 3 types union field yet. Please update your mapping");
    }
  }

  private Object toDBObject(final Schema fieldSchema, final Object fieldValue) {

    final Object result;

    switch (fieldSchema.getType()) {
    case MAP:
      result = mapToCouchDB(fieldValue);
      break;
    case ARRAY:
      result = listToCouchDB(fieldSchema, fieldValue);
      break;
    case RECORD:
      result = recordToCouchDB(fieldSchema, fieldValue);
      break;
    case BYTES:
      result = bytesToCouchDB(fieldValue);
      break;
    case ENUM:
    case STRING:
      result = fieldValue.toString();
      break;
    case UNION:
      result = unionToCouchDB(fieldSchema, fieldValue);
      break;
    default:
      result = fieldValue;
      break;
    }
    return result;
  }

  /**
   * Deletes the object with the given key
   *
   * @param key the key of the object
   * @return whether the object was successfully deleted
   */
  @Override
  public boolean delete(K key) throws GoraException {
    if (key == null) {
      deleteSchema();
      createSchema();
      return true;
    }
    try {
      final String keyString = key.toString();
      final Map<String, Object> referenceData = db.get(Map.class, keyString);
      return StringUtils.isNotEmpty(db.delete(keyString, referenceData.get("_rev").toString()));
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * Deletes all the objects matching the query.
   * See also the note on <a href="#visibility">visibility</a>.
   *
   * @param query matching records to this query will be deleted
   * @return number of deleted records
   */
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {

    final K key = query.getKey();
    final K startKey = query.getStartKey();
    final K endKey = query.getEndKey();

    if (key == null && startKey == null && endKey == null) {
      deleteSchema();
      createSchema();
      return -1;
    } else {
      try {
        final ViewQuery viewQuery = new ViewQuery()
            .allDocs()
            .includeDocs(true)
            .key(key)
            .startKey(startKey)
            .endKey(endKey);
  
        final List<Map> result = db.queryView(viewQuery, Map.class);
        final Map<String, List<String>> revisionsToPurge = new HashMap<>();
  
        for (Map map : result) {
          final List<String> revisions = new ArrayList<>();
          String keyString = map.get("_id").toString();
          String rev = map.get("_rev").toString();
          revisions.add(rev);
          revisionsToPurge.put(keyString, revisions);
        }
        return db.purge(revisionsToPurge).getPurged().size();
      } catch (Exception e) {
        throw new GoraException(e);
      }
    }
  }

  /**
   * Create a new {@link Query} to query the datastore.
   */
  @Override
  public Query<K, T> newQuery() {
    CouchDBQuery<K, T> query = new CouchDBQuery<>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }

  /**
   * Execute the query and return the result.
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {

    try {

      query.setFields(getFieldsToQuery(query.getFields()));
      final ViewQuery viewQuery = new ViewQuery()
          .allDocs()
          .includeDocs(true)
          .startKey(query.getStartKey())
          .endKey(query.getEndKey())
          .limit(Ints.checkedCast(query.getLimit())); //FIXME GORA have long value but ektorp client use integer
      CouchDBResult<K, T> couchDBResult = new CouchDBResult<>(this, query, db.queryView(viewQuery, Map.class));
      return couchDBResult;

    } catch (Exception e) {
      throw new GoraException(e) ;
    }

  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {

    final List<PartitionQuery<K, T>> list = new ArrayList<>();
    final PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<>(query);

    pqi.setConf(getConf());
    list.add(pqi);
    return list;
  }

  /**
   * Creates a new Persistent instance with the values in 'result' for the fields listed.
   *
   * @param result result from the query to the database
   * @param fields the list of fields to be mapped to the persistence class instance
   * @return a persistence class instance which content was deserialized
   * @throws GoraException
   */
  public T newInstance(Map<String, Object> result, String[] fields) throws GoraException {
    if (result == null)
      return null;

    T persistent = newPersistent();

    // Populate each field
    for (String fieldName : fields) {
      if (result.get(fieldName) == null) {
        continue;
      }
      final Field field = fieldMap.get(fieldName);
      final Schema fieldSchema = field.schema();

      LOG.debug("Load from DBObject (MAIN), field:{}, schemaType:{}, docField:{}",
          new Object[] { field.name(), fieldSchema.getType(), fieldName });

      final Object resultObj = fromDBObject(fieldSchema, field, fieldName, result);
      persistent.put(field.pos(), resultObj);
      persistent.setDirty(field.pos());
    }

    persistent.clearDirty();
    return persistent;

  }

  private Object fromCouchDBRecord(final Schema fieldSchema, final String docf, final Object value) throws GoraException {

    final Object innerValue = ((Map) value).get(docf);
    if (innerValue == null) {
      return null;
    }

    Class<?> clazz = null;
    try {
      clazz = ClassLoadingUtils.loadClass(fieldSchema.getFullName());
    } catch (ClassNotFoundException e) {
      throw new GoraException(e) ;
    }

    final PersistentBase record = (PersistentBase) new BeanFactoryImpl(keyClass, clazz).newPersistent();

    for (Field recField : fieldSchema.getFields()) {
      Schema innerSchema = recField.schema();

      record.put(recField.pos(), fromDBObject(innerSchema, recField, recField.name(), innerValue));
    }
    return record;
  }

  private Object fromCouchDBMap(final Schema fieldSchema, final Field field, final String docf, final Object value) throws GoraException {

    final Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) value).get(docf);
    final Map<Utf8, Object> rmap = new HashMap<>();

    if (map == null) {
      return new DirtyMapWrapper(rmap);
    }

    for (Map.Entry<String, Object> e : map.entrySet()) {
      Schema innerSchema = fieldSchema.getValueType();
      ;
      Object o = fromDBObject(innerSchema, field, e.getKey(), e.getValue());
      rmap.put(new Utf8(e.getKey()), o);
    }
    return new DirtyMapWrapper<>(rmap);
  }

  private Object fromCouchDBUnion(final Schema fieldSchema, final Field field, final String docf, final Object value) throws GoraException {

    Object result;// schema [type0, type1]
    Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
    Schema.Type type1 = fieldSchema.getTypes().get(1).getType();

    // Check if types are different and there's a "null", like ["null","type"]
    // or ["type","null"]
    if (!type0.equals(type1)
        && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {
      Schema innerSchema = fieldSchema.getTypes().get(1);
      LOG.debug(
          "Load from DBObject (UNION), schemaType:{}, docField:{}, storeType:{}",
          new Object[] { innerSchema.getType(), docf });
      // Deserialize as if schema was ["type"]
      result = fromDBObject(innerSchema, field, docf, value);
    } else {
      throw new GoraException(
          "CouchDBStore doesn't support 3 types union field yet. Please update your mapping");
    }
    return result;
  }

  private Object fromCouchDBList(final Schema fieldSchema, final Field field, final String docf, final Object value) throws GoraException {
    final List<Object> list = (List<Object>) ((Map<String, Object>) value).get(docf);
    final List<Object> rlist = new ArrayList<>();

    if (list == null) {
      return new DirtyListWrapper(rlist);
    }

    for (Object item : list) {

      Object o = fromDBObject(fieldSchema.getElementType(), field, "item", item);
      rlist.add(o);
    }
    return new DirtyListWrapper<>(rlist);
  }

  private Object fromCouchDBEnum(final Schema fieldSchema, final String docf, final Object value) {
    final Object result;
    if (value instanceof Map) {
      result = AvroUtils.getEnumValue(fieldSchema, (String) ((Map) value).get(docf));
    } else {
      result = AvroUtils.getEnumValue(fieldSchema, (String) value);
    }
    return result;
  }

  private Object fromCouchDBBytes(final String docf, final Object value) {
    final byte[] array;
    if (value instanceof Map) {
      array = ((String) ((Map) value).get(docf)).getBytes(StandardCharsets.UTF_8);
    } else {
      array = ((String) value).getBytes(StandardCharsets.UTF_8);
    }
    return ByteBuffer.wrap(array);
  }

  private Object fromCouchDBString(final String docf, final Object value) {
    final Object result;

    if (value instanceof Map) {
      result = new Utf8((String) ((Map) value).get(docf));
    } else {
      result = new Utf8((String) value);
    }

    return result;
  }

  private Object fromDBObject(final Schema fieldSchema, final Field field, final String docf, final Object value) throws GoraException {
    if (value == null) {
      return null;
    }

    final Object result;

    switch (fieldSchema.getType()) {
    case MAP:
      result = fromCouchDBMap(fieldSchema, field, docf, value);
      break;
    case ARRAY:
      result = fromCouchDBList(fieldSchema, field, docf, value);
      break;
    case RECORD:
      result = fromCouchDBRecord(fieldSchema, docf, value);
      break;
    case UNION:
      result = fromCouchDBUnion(fieldSchema, field, docf, value);
      break;
    case ENUM:
      result = fromCouchDBEnum(fieldSchema, docf, value);
      break;
    case BYTES:
      result = fromCouchDBBytes(docf, value);
      break;
    case STRING:
      result = fromCouchDBString(docf, value);
      break;
    case LONG:
    case DOUBLE:
    case INT:
      result = ((Map) value).get(docf);
      break;
    default:
      result = value;
    }
    return result;
  }

  @Override
  public void flush() throws GoraException {
    try {
      db.executeBulk(bulkDocs);
      bulkDocs.clear();
      db.flushBulkBuffer();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void close() {
    try {
      flush();
    } catch (GoraException e) {
      //Log and ignore. We are closing... so is doest not matter if it just died
      LOG.warn("Error flushing when closing", e);
    }
  }
}