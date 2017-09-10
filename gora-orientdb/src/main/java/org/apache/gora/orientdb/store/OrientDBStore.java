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

package org.apache.gora.orientdb.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.util.TimeZone;
import java.util.Locale;

import com.github.raymanrt.orientqb.query.Parameter;
import com.gitub.raymanrt.orientqb.delete.Delete;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.db.record.OTrackedSet;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OConcurrentResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.orientdb.query.OrientDBQuery;
import org.apache.gora.orientdb.query.OrientDBResult;
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

import javax.xml.bind.DatatypeConverter;

import static com.github.raymanrt.orientqb.query.Projection.projection;

/**
 * {@link org.apache.gora.orientdb.store.OrientDBStore} is the primary class
 * responsible for facilitating GORA CRUD operations on OrientDB documents.
 */
public class OrientDBStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final String DEFAULT_MAPPING_FILE = "/gora-orientdb-mapping.xml";
  private String ROOT_URL;
  private String ROOT_DATABASE_URL;
  private OrientDBStoreParameters orientDbStoreParams;
  private OrientDBMapping orientDBMapping;
  private OServerAdmin remoteServerAdmin;
  private OPartitionedDatabasePool connectionPool;
  private List<ODocument> docBatch = Collections.synchronizedList(new ArrayList<>());
  private ReentrantLock flushLock = new ReentrantLock();

  /**
   * Initialize the OrientDB dataStore by {@link Properties} parameters.
   *
   * @param keyClass key class type for dataStore.
   * @param persistentClass persistent class type for dataStore.
   * @param properties OrientDB dataStore properties EG:- OrientDB client credentials.
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
    try {
      orientDbStoreParams = OrientDBStoreParameters.load(properties);
      ROOT_URL = "remote:".concat(orientDbStoreParams.getServerHost()).concat(":")
              .concat(orientDbStoreParams.getServerPort());
      ROOT_DATABASE_URL = ROOT_URL.concat("/").concat(orientDbStoreParams.getDatabaseName());
      remoteServerAdmin = new OServerAdmin(ROOT_URL).connect(orientDbStoreParams.getUserName(),
              orientDbStoreParams.getUserPassword());
      if (!remoteServerAdmin.existsDatabase(orientDbStoreParams.getDatabaseName(), "memory")) {
        remoteServerAdmin.createDatabase(orientDbStoreParams.getDatabaseName(), "document", "memory");
      }

      if (orientDbStoreParams.getConnectionPoolSize() != null) {
        int connPoolSize = Integer.valueOf(orientDbStoreParams.getConnectionPoolSize());
        connectionPool = new OPartitionedDatabasePoolFactory(connPoolSize)
                .get(ROOT_DATABASE_URL, orientDbStoreParams.getUserName(),
                        orientDbStoreParams.getUserPassword());
      } else {
        connectionPool = new OPartitionedDatabasePoolFactory().get(ROOT_DATABASE_URL,
                orientDbStoreParams.getUserName(), orientDbStoreParams.getUserPassword());
      }

      OrientDBMappingBuilder<K, T> builder = new OrientDBMappingBuilder<>(this);
      orientDBMapping = builder.fromFile(orientDbStoreParams.getMappingFile()).build();

      if (!schemaExists()) {
        createSchema();
      }
    } catch (Exception e) {
      LOG.error("Error while initializing OrientDB dataStore: {}",
              new Object[]{e.getMessage()});
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getSchemaName(final String mappingSchemaName,
                              final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  @Override
  public String getSchemaName() {
    return orientDBMapping.getDocumentClass();
  }

  /**
   * Create a new class of OrientDB documents if necessary. Enforce specified schema over the document class.
   *
   */
  @Override
  public void createSchema() {
    if (schemaExists()) {
      return;
    }

    ODatabaseDocumentTx schemaTx = connectionPool.acquire();
    schemaTx.activateOnCurrentThread();
    try {

      OClass documentClass = schemaTx.getMetadata().getSchema().createClass(orientDBMapping.getDocumentClass());
      documentClass.createProperty("_id",
              OType.getTypeByClass(super.getKeyClass())).createIndex(OClass.INDEX_TYPE.UNIQUE);
      for (String docField : orientDBMapping.getDocumentFields()) {
        documentClass.createProperty(docField,
                OType.valueOf(orientDBMapping.getDocumentFieldType(docField).name()));
      }
      schemaTx.getMetadata().getSchema().reload();
    } finally {
      schemaTx.close();
    }
  }

  /**
   * Deletes enforced schema over OrientDB Document class.
   *
   */
  @Override
  public void deleteSchema() {
    ODatabaseDocumentTx schemaTx = connectionPool.acquire();
    schemaTx.activateOnCurrentThread();
    try {
      schemaTx.getMetadata().getSchema().dropClass(orientDBMapping.getDocumentClass());
    } finally {
      schemaTx.close();
    }
  }

  /**
   * Check whether there exist a schema enforced over OrientDB document class.
   *
   */
  @Override
  public boolean schemaExists() {
    ODatabaseDocumentTx schemaTx = connectionPool.acquire();
    schemaTx.activateOnCurrentThread();
    try {
      return schemaTx.getMetadata().getSchema()
              .existsClass(orientDBMapping.getDocumentClass());
    } finally {
      schemaTx.close();
    }
  }

  @Override
  public T get(K key, String[] fields) {
    String[] dbFields = getFieldsToQuery(fields);
    com.github.raymanrt.orientqb.query.Query selectQuery = new com.github.raymanrt.orientqb.query.Query();
    for (String k : dbFields) {
      String dbFieldName = orientDBMapping.getDocumentField(k);
      if (dbFieldName != null && dbFieldName.length() > 0) {
        selectQuery.select(dbFieldName);
      }
    }
    selectQuery.from(orientDBMapping.getDocumentClass())
            .where(projection("_id").eq(Parameter.parameter("key")));
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", key);
    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(selectQuery.toString());
    ODatabaseDocumentTx selectTx = connectionPool.acquire();
    selectTx.activateOnCurrentThread();
    try {
      List<ODocument> result = selectTx.command(query).execute(params);
      if (result.size() == 1) {
        return convertOrientDocToAvroBean(result.get(0), dbFields);
      } else {
        return null;
      }
    } finally {
      selectTx.close();
    }
  }

  @Override
  public void put(K key, T val) {
    if (val.isDirty()) {
      OrientDBQuery<K, T> dataStoreQuery = new OrientDBQuery<>(this);
      dataStoreQuery.setStartKey(key);
      dataStoreQuery.setEndKey(key);
      dataStoreQuery.populateOrientDBQuery(orientDBMapping, getFieldsToQuery(null), getFields());

      ODatabaseDocumentTx selectTx = connectionPool.acquire();
      selectTx.activateOnCurrentThread();
      try {
        List<ODocument> result = selectTx.command(dataStoreQuery.getOrientDBQuery())
                .execute(dataStoreQuery.getParams());
        if (result.size() == 1) {
          ODocument document = updateOrientDocFromAvroBean(key, val, result.get(0));
          docBatch.add(document);
        } else {
          ODocument document = convertAvroBeanToOrientDoc(key, val);
          docBatch.add(document);
        }
      } finally {
        selectTx.close();
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.info("Ignored putting persistent bean {} in the store as it is neither "
                + "new, neither dirty.", new Object[]{val});
      }
    }
  }

  @Override
  public boolean delete(K key) {
    Delete delete = new Delete();
    delete.from(orientDBMapping.getDocumentClass())
            .where(projection("_id").eq(Parameter.parameter("key")));
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", key);
    OCommandSQL query = new OCommandSQL(delete.toString().replace("DELETE", "DELETE FROM"));
    ODatabaseDocumentTx deleteTx = connectionPool.acquire();
    deleteTx.activateOnCurrentThread();
    try {
      int deleteCount = deleteTx.command(query).execute(params);
      if (deleteCount == 1) {
        return true;
      } else {
        return false;
      }
    } finally {
      deleteTx.close();
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    Delete delete = new Delete();
    delete.from(orientDBMapping.getDocumentClass());
    Map<String, Object> params = new HashMap<String, Object>();
    if (query.getFields() == null || (query.getFields().length == getFields().length)) {
      if (query.getStartKey() != null) {
        delete.where(projection("_id").ge(Parameter.parameter("start")));
        params.put("start", query.getStartKey());
      }
      if (query.getEndKey() != null) {
        delete.where(projection("_id").le(Parameter.parameter("end")));
        params.put("end", query.getEndKey());
      }

      OCommandSQL dbQuery = new OCommandSQL(delete.toString().replace("DELETE", "DELETE FROM"));
      ODatabaseDocumentTx deleteTx = connectionPool.acquire();
      deleteTx.activateOnCurrentThread();
      try {
        int deleteCount;
        if (params.isEmpty()) {
          deleteCount = deleteTx.command(dbQuery).execute();
        } else {
          deleteCount = deleteTx.command(dbQuery).execute(params);
        }
        if (deleteCount > 0) {
          return deleteCount;
        } else {
          return 0;
        }
      } finally {
        deleteTx.close();
      }
    } else {

      OrientDBQuery<K, T> dataStoreQuery = new OrientDBQuery<>(this);
      dataStoreQuery.setStartKey(query.getStartKey());
      dataStoreQuery.setEndKey(query.getEndKey());
      dataStoreQuery.populateOrientDBQuery(orientDBMapping, getFieldsToQuery(null), getFields());

      ODatabaseDocumentTx selectTx = connectionPool.acquire();
      selectTx.activateOnCurrentThread();
      try {
        List<ODocument> result = selectTx.command(dataStoreQuery.getOrientDBQuery())
                .execute(dataStoreQuery.getParams());
        if (result != null && result.isEmpty()) {
          return 0;
        } else {
          for (ODocument doc : result) {
            for (String docField : query.getFields()) {
              if (doc.containsField(orientDBMapping.getDocumentField(docField))) {
                doc.removeField(orientDBMapping.getDocumentField(docField));
              }
            }
            doc.save();
          }
          return result.size();
        }
      } finally {
        selectTx.close();
      }
    }
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    String[] fields = getFieldsToQuery(query.getFields());
    OrientDBQuery dataStoreQuery;
    if (query instanceof OrientDBQuery) {
      dataStoreQuery = ((OrientDBQuery) query);
    } else {
      dataStoreQuery = (OrientDBQuery) ((PartitionQueryImpl<K, T>) query).getBaseQuery();
    }
    dataStoreQuery.populateOrientDBQuery(orientDBMapping, fields, getFields());
    ODatabaseDocumentTx selectTx = connectionPool.acquire();
    selectTx.activateOnCurrentThread();
    try {
      OConcurrentResultSet<ODocument> result = selectTx.command(dataStoreQuery.getOrientDBQuery())
              .execute(dataStoreQuery.getParams());
      result.setLimit((int) query.getLimit());
      return new OrientDBResult<K, T>(this, query, result);
    } finally {
      selectTx.close();
    }
  }

  @Override
  public Query<K, T> newQuery() {
    OrientDBQuery<K, T> query = new OrientDBQuery<K, T>(this);
    query.setFields(getFieldsToQuery(null));
    return new OrientDBQuery<K, T>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    // TODO : Improve code on OrientDB clusters
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query);
    partitionQuery.setConf(this.getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  /**
   * Flushes locally cached to content in memory to remote OrientDB server.
   *
   */
  @Override
  public void flush() {
    ODatabaseDocumentTx updateTx = connectionPool.acquire();
    updateTx.activateOnCurrentThread();
    try {
      flushLock.lock();
      for (ODocument document : docBatch) {
        updateTx.save(document);
      }
    } finally {
      updateTx.close();
      docBatch.clear();
      flushLock.unlock();
    }
  }

  /**
   * Releases resources which have been used dataStore. Eg:- OrientDB Client connection pool.
   *
   */
  @Override
  public void close() {
    docBatch.clear();
    remoteServerAdmin.close();
    connectionPool.close();
  }

  /**
   * Returns OrientDB client connection pool maintained at Gora dataStore.
   *
   * @return {@link OPartitionedDatabasePool} OrientDB client connection pool.
   */
  public OPartitionedDatabasePool getConnectionPool() {
    return connectionPool;
  }

  public T convertOrientDocToAvroBean(final ODocument obj, final String[] fields) {
    T persistent = newPersistent();
    String[] dbFields = getFieldsToQuery(fields);
    for (String f : dbFields) {
      String docf = orientDBMapping.getDocumentField(f);
      if (docf == null || !obj.containsField(docf))
        continue;

      OrientDBMapping.DocumentFieldType storeType = orientDBMapping.getDocumentFieldType(docf);
      Schema.Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();

      LOG.debug("Load from ODocument, field:{}, schemaType:{}, docField:{}, storeType:{}",
              new Object[]{field.name(), fieldSchema.getType(), docf, storeType});
      Object result = convertDocFieldToAvroField(fieldSchema, storeType, field, docf, obj);
      persistent.put(field.pos(), result);
    }
    persistent.clearDirty();
    return persistent;
  }

  private Object convertDocFieldToAvroField(final Schema fieldSchema,
                                            final OrientDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final ODocument obj) {
    Object result = null;
    switch (fieldSchema.getType()) {
      case MAP:
        result = convertDocFieldToAvroMap(docf, fieldSchema, obj, field, storeType);
        break;
      case ARRAY:
        result = convertDocFieldToAvroList(docf, fieldSchema, obj, field, storeType);
        break;
      case RECORD:
        ODocument record = obj.field(docf);
        if (record == null) {
          result = null;
          break;
        }
        result = convertAvroBeanToOrientDoc(fieldSchema, record);
        break;
      case BOOLEAN:
        result = OType.convert(obj.field(docf), Boolean.class);
        break;
      case DOUBLE:
        result = OType.convert(obj.field(docf), Double.class);
        break;
      case FLOAT:
        result = OType.convert(obj.field(docf), Float.class);
        break;
      case INT:
        result = OType.convert(obj.field(docf), Integer.class);
        break;
      case LONG:
        result = OType.convert(obj.field(docf), Long.class);
        break;
      case STRING:
        result = convertDocFieldToAvroString(storeType, docf, obj);
        break;
      case ENUM:
        result = AvroUtils.getEnumValue(fieldSchema, obj.field(docf));
        break;
      case BYTES:
      case FIXED:
        if (obj.field(docf) == null) {
          result = null;
          break;
        }
        result = ByteBuffer.wrap((byte[]) obj.field(docf));
        break;
      case NULL:
        result = null;
        break;
      case UNION:
        result = convertDocFieldToAvroUnion(fieldSchema, storeType, field, docf, obj);
        break;
      default:
        LOG.warn("Unable to read {}", docf);
        break;
    }
    return result;
  }

  private Object convertDocFieldToAvroList(final String docf,
                                           final Schema fieldSchema,
                                           final ODocument doc,
                                           final Schema.Field f,
                                           final OrientDBMapping.DocumentFieldType storeType) {
    if (storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDSET) {
      OTrackedSet<Object> set = doc.field(docf);
      List<Object> rlist = new ArrayList<>();
      if (set == null) {
        return new DirtyListWrapper(rlist);
      }

      for (Object item : set) {
        Object o = convertDocFieldToAvroField(fieldSchema.getElementType(), storeType, f,
                "item", new ODocument("item", item));
        rlist.add(o);
      }
      return new DirtyListWrapper<>(rlist);

    } else {
      OTrackedList<Object> list = doc.field(docf);
      List<Object> rlist = new ArrayList<>();
      if (list == null) {
        return new DirtyListWrapper(rlist);
      }

      for (Object item : list) {
        Object o = convertDocFieldToAvroField(fieldSchema.getElementType(), storeType, f,
                "item", new ODocument("item", item));
        rlist.add(o);
      }
      return new DirtyListWrapper<>(rlist);
    }
  }

  private Object convertAvroListToDocField(final String docf, final Collection<?> array,
                                           final Schema fieldSchema, final Schema.Type fieldType,
                                           final OrientDBMapping.DocumentFieldType storeType) {
    if (storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDLIST) {
      ArrayList list;
      list = new ArrayList<Object>();
      if (array == null)
        return list;
      for (Object item : array) {
        OrientDBMapping.DocumentFieldType fieldStoreType = orientDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToOrientField(docf, fieldSchema, fieldType, fieldStoreType, item);
        list.add(result);
      }
      return list;
    } else if (storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDSET) {
      HashSet set;
      set = new HashSet<Object>();
      if (array == null)
        return set;
      for (Object item : array) {
        OrientDBMapping.DocumentFieldType fieldStoreType = orientDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToOrientField(docf, fieldSchema, fieldType, fieldStoreType, item);
        set.add(result);
      }
      return set;
    }
    return null;
  }

  private Object convertDocFieldToAvroMap(final String docf, final Schema fieldSchema,
                                          final ODocument doc, final Schema.Field f,
                                          final OrientDBMapping.DocumentFieldType storeType) {
    if (storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDMAP) {
      OTrackedMap<Object> map = doc.field(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (map == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (Map.Entry entry : map.entrySet()) {
        String mapKey = decodeFieldKey((String) entry.getKey());
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                decorateOTrackedMapToODoc(map));
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    } else {
      ODocument innerDoc = doc.field(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (innerDoc == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (String fieldName : innerDoc.fieldNames()) {
        String mapKey = decodeFieldKey(fieldName);
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                innerDoc);
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    }
  }

  private ODocument decorateOTrackedMapToODoc(OTrackedMap<Object> map) {
    ODocument doc = new ODocument();
    for (Map.Entry entry : map.entrySet()) {
      doc.field((String) entry.getKey(), entry.getValue());
    }
    return doc;
  }

  private Object convertAvroMapToDocField(final String docf,
                                          final Map<CharSequence, ?> value, final Schema fieldSchema,
                                          final Schema.Type fieldType,
                                          final OrientDBMapping.DocumentFieldType storeType) {
    if (storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDMAP) {
      HashMap map = new HashMap<String, Object>();
      if (value == null)
        return map;

      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = encodeFieldKey(e.getKey().toString());
        Object mapValue = e.getValue();

        OrientDBMapping.DocumentFieldType fieldStoreType = orientDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToOrientField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        map.put(mapKey, result);
      }
      return map;
    } else {
      ODocument doc = new ODocument();
      if (value == null)
        return doc;
      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = encodeFieldKey(e.getKey().toString());
        Object mapValue = e.getValue();

        OrientDBMapping.DocumentFieldType fieldStoreType = orientDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToOrientField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        doc.field(mapKey, result);
      }
      return doc;
    }
  }

  private Object convertAvroBeanToOrientDoc(final Schema fieldSchema,
                                            final ODocument doc) {
    Object result;
    Class<?> clazz = null;
    try {
      clazz = ClassLoadingUtils.loadClass(fieldSchema.getFullName());
    } catch (ClassNotFoundException e) {
      //Ignore
    }
    PersistentBase record = (PersistentBase) new BeanFactoryImpl(keyClass, clazz).newPersistent();
    for (Schema.Field recField : fieldSchema.getFields()) {
      Schema innerSchema = recField.schema();
      OrientDBMapping.DocumentFieldType innerStoreType = orientDBMapping
              .getDocumentFieldType(recField.name());
      String innerDocField = orientDBMapping.getDocumentField(recField.name()) != null ? orientDBMapping
              .getDocumentField(recField.name()) : recField.name();
      LOG.debug("Load from ODocument (RECORD), field:{}, schemaType:{}, docField:{}, storeType:{}",
              new Object[]{recField.name(), innerSchema.getType(), innerDocField,
                      innerStoreType});
      record.put(recField.pos(),
              convertDocFieldToAvroField(innerSchema, innerStoreType, recField, innerDocField,
                      doc));
    }
    result = record;
    return result;
  }

  private Object convertDocFieldToAvroString(final OrientDBMapping.DocumentFieldType storeType,
                                             final String docf, final ODocument doc) {
    Object result;
    if (storeType == OrientDBMapping.DocumentFieldType.DATE ||
            storeType == OrientDBMapping.DocumentFieldType.DATETIME) {
      Date dateTime = doc.field(docf);
      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault());
      calendar.setTime(dateTime);
      result = new Utf8(DatatypeConverter.printDateTime(calendar));
    } else {
      result = new Utf8((String) doc.field(encodeFieldKey(docf)));
    }
    return result;
  }

  private Object convertDocFieldToAvroUnion(final Schema fieldSchema,
                                            final OrientDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final ODocument doc) {
    Object result;
    Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
    Schema.Type type1 = fieldSchema.getTypes().get(1).getType();

    if (!type0.equals(type1)
            && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {
      Schema innerSchema = null;
      if (type0.equals(Schema.Type.NULL)) {
        innerSchema = fieldSchema.getTypes().get(1);
      } else {
        innerSchema = fieldSchema.getTypes().get(0);
      }

      LOG.debug("Load from ODocument (UNION), schemaType:{}, docField:{}, storeType:{}",
              new Object[]{innerSchema.getType(), docf, storeType});

      result = convertDocFieldToAvroField(innerSchema, storeType, field, docf, doc);
    } else {
      throw new IllegalStateException("OrientDBStore only supports Union of two types field.");
    }
    return result;
  }

  private Object convertAvroUnionToOrientDBField(final String docf, final Schema fieldSchema,
                                                 final OrientDBMapping.DocumentFieldType storeType,
                                                 final Object value) {
    Object result;
    Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
    Schema.Type type1 = fieldSchema.getTypes().get(1).getType();

    if (!type0.equals(type1)
            && (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL))) {
      Schema innerSchema = null;
      if (type0.equals(Schema.Type.NULL)) {
        innerSchema = fieldSchema.getTypes().get(1);
      } else {
        innerSchema = fieldSchema.getTypes().get(0);
      }

      LOG.debug("Transform value to ODocument (UNION), type:{}, storeType:{}",
              new Object[]{innerSchema.getType(), type1, storeType});

      result = convertAvroFieldToOrientField(docf, innerSchema, innerSchema.getType(), storeType, value);
    } else {
      throw new IllegalStateException("OrientDBStore only supports Union of two types field.");
    }
    return result;
  }

  private ODocument convertAvroBeanToOrientDoc(final K key, final T persistent) {
    ODocument result = new ODocument(orientDBMapping.getDocumentClass());
    for (Schema.Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
        String docf = orientDBMapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        OrientDBMapping.DocumentFieldType storeType = orientDBMapping.getDocumentFieldType(docf);
        LOG.debug("Transform value to ODocument, docField:{}, schemaType:{}, storeType:{}",
                new Object[]{docf, f.schema().getType(), storeType});
        Object o = convertAvroFieldToOrientField(docf, f.schema(), f.schema().getType(),
                storeType, value);
        result.field(docf, o);
      }
    }
    result.field("_id", key);
    return result;
  }

  private ODocument updateOrientDocFromAvroBean(final K key, final T persistent, final ODocument result) {
    for (Schema.Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) /*&& (persistent.get(f.pos()) != null)*/) {
        String docf = orientDBMapping.getDocumentField(f.name());
        if (persistent.get(f.pos()) == null) {
          result.removeField(docf);
          continue;
        }
        Object value = persistent.get(f.pos());
        OrientDBMapping.DocumentFieldType storeType = orientDBMapping.getDocumentFieldType(docf);
        LOG.debug("Transform value to ODocument, docField:{}, schemaType:{}, storeType:{}",
                new Object[]{docf, f.schema().getType(), storeType});
        Object o = convertAvroFieldToOrientField(docf, f.schema(), f.schema().getType(),
                storeType, value);
        result.field(docf, o);
      }
    }
    return result;
  }

  private Object convertAvroFieldToOrientField(final String docf, final Schema fieldSchema,
                                               final Schema.Type fieldType,
                                               final OrientDBMapping.DocumentFieldType storeType,
                                               final Object value) {
    Object result = null;
    switch (fieldType) {
      case MAP:
        if (storeType != null && !(storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDMAP ||
                storeType == OrientDBMapping.DocumentFieldType.EMBEDDED)) {
          throw new IllegalStateException(
                  "Field " + fieldSchema.getName()
                          + ": to store a AVRO 'map', target OrientDB mapping have to be of type 'EmbeddedMap'" +
                          "| 'Embedded'");
        }
        Schema valueSchema = fieldSchema.getValueType();
        result = convertAvroMapToDocField(docf, (Map<CharSequence, ?>) value, valueSchema,
                valueSchema.getType(), storeType);
        break;
      case ARRAY:
        if (storeType != null && !(storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDLIST ||
                storeType == OrientDBMapping.DocumentFieldType.EMBEDDEDSET)) {
          throw new IllegalStateException("Field " + fieldSchema.getName()
                  + ": To store a AVRO 'array', target Mongo mapping have to be of type 'EmbeddedMap'" +
                  "|'EmbeddedList'");
        }
        Schema elementSchema = fieldSchema.getElementType();
        result = convertAvroListToDocField(docf, (List<?>) value, elementSchema,
                elementSchema.getType(), storeType);
        break;
      case BYTES:
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
        result = convertAvroStringToDocField(fieldSchema, storeType, value);
        break;
      case ENUM:
        if (value != null)
          result = value.toString();
        break;
      case RECORD:
        if (value == null)
          break;
        result = convertAvroBeanToOrientDoc(docf, fieldSchema, value);
        break;
      case UNION:
        result = convertAvroUnionToOrientDBField(docf, fieldSchema, storeType, value);
        break;
      case FIXED:
        result = value;
        break;
      default:
        LOG.error("Unknown field type: {}", fieldSchema.getType());
        break;
    }
    return result;
  }

  private Object convertAvroStringToDocField(final Schema fieldSchema,
                                             final OrientDBMapping.DocumentFieldType storeType,
                                             final Object value) {
    Object result = null;
    if (storeType == OrientDBMapping.DocumentFieldType.DATETIME) {
      if (value != null) {
        Calendar dateTime = null;
        try {
          dateTime = DatatypeConverter.parseDateTime(value.toString());
        } catch (IllegalArgumentException e) {
          throw new IllegalStateException("Field " + fieldSchema.getType()
                  + ": Invalid date and time format '" + value + "'", e);

        }
        result = dateTime.getTime();
      }
    } else if (storeType == OrientDBMapping.DocumentFieldType.DATE) {
      Calendar date = null;
      try {
        date = DatatypeConverter.parseDate(value.toString());
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Field " + fieldSchema.getType()
                + ": Invalid date format '" + value + "'", e);
      }
      result = date.getTime();
    } else {
      if (value != null) {
        result = value.toString();
      }
    }
    return result;
  }

  private ODocument convertAvroBeanToOrientDoc(final String docf,
                                               final Schema fieldSchema,
                                               final Object value) {
    ODocument record = new ODocument();
    for (Schema.Field member : fieldSchema.getFields()) {
      Object innerValue = ((PersistentBase) value).get(member.pos());
      String innerDoc = orientDBMapping.getDocumentField(member.name());
      Schema.Type innerType = member.schema().getType();
      OrientDBMapping.DocumentFieldType innerStoreType = orientDBMapping.getDocumentFieldType(innerDoc);
      LOG.debug("Transform value to ODocument , docField:{}, schemaType:{}, storeType:{}",
              new Object[]{member.name(), member.schema().getType(),
                      innerStoreType});
      Object fieldValue = convertAvroFieldToOrientField(docf, member.schema()
              , innerType, innerStoreType, innerValue);
      record.field(member.name(), fieldValue);
    }
    return record;
  }

  private String encodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace(".", "\u00B7")
            .replace(":", "\u00FF")
            .replace(";", "\u00FE")
            .replace(" ", "\u00FD")
            .replace("%", "\u00FC")
            .replace("=", "\u00FB");
  }

  private String decodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace("\u00B7", ".")
            .replace("\u00FF", ":")
            .replace("\u00FE", ";")
            .replace("\u00FD", " ")
            .replace("\u00FC", "%")
            .replace("\u00FB", "=");
  }

}