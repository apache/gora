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

package org.apache.gora.arangodb.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.arangodb.query.ArangoDBQuery;
import org.apache.gora.arangodb.query.ArangoDBResult;
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
import org.apache.gora.util.GoraException;

/**
 * {@inheritDoc}
 * {@link ArangoDBStore} is the primary class
 * responsible for facilitating GORA CRUD operations on ArangoDB documents.
 */
public class ArangoDBStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final String DEFAULT_MAPPING_FILE = "/gora-arangodb-mapping.xml";
  private ArangoDBStoreParameters arangoDbStoreParams;
  private ArangoDBMapping arangoDBMapping;
  private ArangoDB arangoDB;

  /**
   * {@inheritDoc}
   * Initialize the ArangoDB dataStore by {@link Properties} parameters.
   *
   * @param keyClass        key class type for dataStore.
   * @param persistentClass persistent class type for dataStore.
   * @param properties      ArangoDB dataStore properties EG:- ArangoDB client credentials.
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);
    try {
      arangoDbStoreParams = ArangoDBStoreParameters.load(properties, getConf());
      arangoDB = new ArangoDB.Builder()
              .host(arangoDbStoreParams.getServerHost(),
                      Integer.valueOf(arangoDbStoreParams.getServerPort()))
              .user(arangoDbStoreParams.getUserName())
              .password(arangoDbStoreParams.getUserPassword())
              .maxConnections(Integer.valueOf(arangoDbStoreParams.getConnectionPoolSize()))
              .build();
      if (!arangoDB.db(arangoDbStoreParams.getDatabaseName()).exists())
        arangoDB.createDatabase(arangoDbStoreParams.getDatabaseName());

      ArangoDBMappingBuilder<K, T> builder = new ArangoDBMappingBuilder<>(this);
      arangoDBMapping = builder.fromFile(arangoDbStoreParams.getMappingFile()).build();
      if (!schemaExists()) {
        createSchema();
      }
    } catch (Exception e) {
      LOG.error("Error while initializing ArangoDB dataStore: {}",
              new Object[]{e.getMessage()});
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSchemaName(final String mappingSchemaName,
                              final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSchemaName() {
    return arangoDBMapping.getDocumentClass();
  }

  /**
   * {@inheritDoc}
   * Create a new class of ArangoDB documents if necessary. Enforce specified schema over the document class.
   */
  @Override
  public void createSchema() throws GoraException {
    if (schemaExists()) {
      return;
    }
    try {
      arangoDB.db(arangoDbStoreParams.getDatabaseName())
              .createCollection(arangoDBMapping.getDocumentClass());

    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   * Deletes enforced schema over ArangoDB Document class.
   */
  @Override
  public void deleteSchema() throws GoraException {
    try {
      arangoDB.db(arangoDbStoreParams.getDatabaseName())
              .collection(arangoDBMapping.getDocumentClass()).drop();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   * Check whether there exist a schema enforced over ArangoDB document class.
   */
  @Override
  public boolean schemaExists() throws GoraException {
    try {
      return arangoDB
              .db(arangoDbStoreParams.getDatabaseName())
              .collection(arangoDBMapping.getDocumentClass()).exists();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T get(K key, String[] fields) throws GoraException {
    try {
      key = encodeArangoDBKey(key);
      boolean isExists = arangoDB.db(arangoDbStoreParams.getDatabaseName())
              .collection(arangoDBMapping.getDocumentClass()).documentExists(key.toString());
      if (isExists) {
        String[] dbFields = getFieldsToQuery(fields);
        BaseDocument document = arangoDB.db(arangoDbStoreParams.getDatabaseName())
                .collection(arangoDBMapping.getDocumentClass()).getDocument(key.toString(), BaseDocument.class);
        return convertArangoDBDocToAvroBean(document, dbFields);
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  public T convertArangoDBDocToAvroBean(final BaseDocument obj, final String[] fields) throws GoraException {
    T persistent = newPersistent();
    String[] dbFields = getFieldsToQuery(fields);
    for (String f : dbFields) {
      String docf = arangoDBMapping.getDocumentField(f);
      if (docf == null || !obj.getProperties().containsKey(docf))
        continue;

      ArangoDBMapping.DocumentFieldType storeType = arangoDBMapping.getDocumentFieldType(docf);
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
                                            final ArangoDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final BaseDocument obj) throws GoraException {
    Object result = null;
    switch (fieldSchema.getType()) {
      case MAP:
        result = convertDocFieldToAvroMap(docf, fieldSchema, obj, field, storeType);
        break;
      case ARRAY:
        result = convertDocFieldToAvroList(docf, fieldSchema, obj, field, storeType);
        break;
      case RECORD:
        LinkedHashMap record = (LinkedHashMap) obj.getAttribute(docf);
        if (record == null) {
          result = null;
          break;
        }
        result = convertAvroBeanToArangoDBDoc(fieldSchema, convertToArangoDBDoc(record));
        break;
      case BOOLEAN:
        result = Boolean.valueOf(obj.getAttribute(docf).toString());
        break;
      case DOUBLE:
        result = Double.valueOf(obj.getAttribute(docf).toString());
        break;
      case FLOAT:
        result = Float.valueOf(obj.getAttribute(docf).toString());
        break;
      case INT:
        result = Integer.valueOf(obj.getAttribute(docf).toString());
        break;
      case LONG:
        result = Long.valueOf(obj.getAttribute(docf).toString());
        break;
      case STRING:
        result = new Utf8(obj.getAttribute(docf).toString());
        ;
        break;
      case ENUM:
        result = AvroUtils.getEnumValue(fieldSchema, obj.getAttribute(docf).toString());
        break;
      case BYTES:
      case FIXED:
        if (!obj.getProperties().containsKey(docf)) {
          result = null;
          break;
        }
        result = ByteBuffer.wrap(Base64
                .getDecoder()
                .decode(obj.getAttribute(docf).toString()));
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

  private Object convertDocFieldToAvroUnion(final Schema fieldSchema,
                                            final ArangoDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final BaseDocument doc) throws GoraException {
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
      throw new GoraException("ArangoDBStore only supports Union of two types field.");
    }
    return result;
  }

  private BaseDocument convertToArangoDBDoc(final LinkedHashMap<String, Object> linkedHashMap) {
    BaseDocument document = new BaseDocument();
    for (String key : linkedHashMap.keySet()) {
      document.addAttribute(key, linkedHashMap.get(key));
    }
    return document;
  }

  private Object convertAvroBeanToArangoDBDoc(final Schema fieldSchema,
                                              final BaseDocument doc) throws GoraException {
    Object result;
    Class<?> clazz = null;
    try {
      clazz = ClassLoadingUtils.loadClass(fieldSchema.getFullName());
    } catch (Exception e) {
      throw new GoraException(e);
    }
    PersistentBase record = (PersistentBase) new BeanFactoryImpl(keyClass, clazz).newPersistent();
    for (Schema.Field recField : fieldSchema.getFields()) {
      Schema innerSchema = recField.schema();
      ArangoDBMapping.DocumentFieldType innerStoreType = arangoDBMapping
              .getDocumentFieldType(recField.name());
      String innerDocField = arangoDBMapping.getDocumentField(recField.name()) != null ? arangoDBMapping
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

  private Object convertDocFieldToAvroList(final String docf,
                                           final Schema fieldSchema,
                                           final BaseDocument doc,
                                           final Schema.Field f,
                                           final ArangoDBMapping.DocumentFieldType storeType) throws GoraException {

    if (storeType == ArangoDBMapping.DocumentFieldType.LIST || storeType == null) {
      List<Object> list = (List<Object>) doc.getAttribute(docf);
      List<Object> rlist = new ArrayList<>();
      if (list == null) {
        return new DirtyListWrapper(rlist);
      }

      for (Object item : list) {
        BaseDocument innerDoc = new BaseDocument();
        innerDoc.addAttribute("item", item);
        Object o = convertDocFieldToAvroField(fieldSchema.getElementType(), storeType, f,
                "item", innerDoc);
        rlist.add(o);
      }
      return new DirtyListWrapper<>(rlist);
    }
    return null;
  }

  private Object convertDocFieldToAvroMap(final String docf, final Schema fieldSchema,
                                          final BaseDocument doc, final Schema.Field f,
                                          final ArangoDBMapping.DocumentFieldType storeType) throws GoraException {
    if (storeType == ArangoDBMapping.DocumentFieldType.MAP) {
      Map<String, Object> map = (Map<String, Object>) doc.getAttribute(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (map == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (Map.Entry entry : map.entrySet()) {
        String mapKey = entry.getKey().toString();
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                decorateOTrackedMapToODoc(map));
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    } else {
      LinkedHashMap<String, Object> innerDoc = (LinkedHashMap) doc.getAttribute(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (innerDoc == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (String fieldName : innerDoc.keySet()) {
        String mapKey = fieldName;
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                convertToArangoDBDoc(innerDoc));
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    }
  }

  private BaseDocument decorateOTrackedMapToODoc(Map<String, Object> map) {
    BaseDocument doc = new BaseDocument();
    for (Map.Entry entry : map.entrySet()) {
      doc.addAttribute(entry.getKey().toString(), entry.getValue());
    }
    return doc;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void put(K key, T val) throws GoraException {
    if (val.isDirty()) {
      try {
        key = encodeArangoDBKey(key);
        boolean isExists = arangoDB.db(arangoDbStoreParams.getDatabaseName())
                .collection(arangoDBMapping.getDocumentClass()).documentExists(key.toString());
        if (!isExists) {
          BaseDocument document = convertAvroBeanToArangoDocument(key, val);
          arangoDB.db(arangoDbStoreParams.getDatabaseName())
                  .collection(arangoDBMapping.getDocumentClass())
                  .insertDocument(document);
        } else {
          BaseDocument document = convertAvroBeanToArangoDocument(key, val);
          arangoDB.db(arangoDbStoreParams.getDatabaseName())
                  .collection(arangoDBMapping.getDocumentClass())
                  .updateDocument(key.toString(), document);
        }
      } catch (Exception e) {
        throw new GoraException(e);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.info("Ignored putting persistent bean {} in the store as it is neither "
                + "new, neither dirty.", new Object[]{val});
      }
    }
  }

  private BaseDocument convertAvroBeanToArangoDocument(final K key, final T persistent) {
    BaseDocument result = new BaseDocument();
    for (Schema.Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
        String docf = arangoDBMapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        ArangoDBMapping.DocumentFieldType storeType = arangoDBMapping.getDocumentFieldType(docf);
        LOG.debug("Transform value to ODocument, docField:{}, schemaType:{}, storeType:{}",
                new Object[]{docf, f.schema().getType(), storeType});
        Object o = convertAvroFieldToArangoField(docf, f.schema(), f.schema().getType(),
                storeType, value);
        result.addAttribute(docf, o);
      }
    }
    result.setKey(key.toString());
    return result;
  }

  private Object convertAvroFieldToArangoField(final String docf, final Schema fieldSchema,
                                               final Schema.Type fieldType,
                                               final ArangoDBMapping.DocumentFieldType storeType,
                                               final Object value) {
    Object result = null;
    switch (fieldType) {
      case MAP:
        if (storeType != null && !(storeType == ArangoDBMapping.DocumentFieldType.MAP ||
                storeType == ArangoDBMapping.DocumentFieldType.DOCUMENT)) {
          throw new IllegalStateException(
                  "Field " + fieldSchema.getName()
                          + ": to store a AVRO 'map', target ArangoDB mapping have to be of type 'Map'" +
                          "| 'Document'");
        }
        Schema valueSchema = fieldSchema.getValueType();
        result = convertAvroMapToDocField(docf, (Map<CharSequence, ?>) value, valueSchema,
                valueSchema.getType(), storeType);
        break;
      case ARRAY:
        if (storeType != null && !(storeType == ArangoDBMapping.DocumentFieldType.LIST)) {
          throw new IllegalStateException("Field " + fieldSchema.getName()
                  + ": To store a AVRO 'array', target ArangoDB mapping have to be of type 'List'");
        }
        Schema elementSchema = fieldSchema.getElementType();
        result = convertAvroListToDocField(docf, (List<?>) value, elementSchema,
                elementSchema.getType(), storeType);
        break;
      case BYTES:
      case FIXED:
        if (value != null) {
          result = Base64.getEncoder()
                  .encodeToString(((ByteBuffer) value).array());
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
        if (value != null) {
          result = value.toString();
        }
        break;
      case ENUM:
        if (value != null)
          result = value.toString();
        break;
      case RECORD:
        if (value == null)
          break;
        result = convertAvroBeanToArangoDBDocField(docf, fieldSchema, value);
        break;
      case UNION:
        result = convertAvroUnionToArangoDBField(docf, fieldSchema, storeType, value);
        break;
      default:
        LOG.error("Unknown field type: {}", fieldSchema.getType());
        break;
    }
    return result;
  }

  private Object convertAvroMapToDocField(final String docf,
                                          final Map<CharSequence, ?> value, final Schema fieldSchema,
                                          final Schema.Type fieldType,
                                          final ArangoDBMapping.DocumentFieldType storeType) {
    if (storeType == ArangoDBMapping.DocumentFieldType.MAP) {
      HashMap map = new HashMap<String, Object>();
      if (value == null)
        return map;

      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = e.getKey().toString();
        Object mapValue = e.getValue();

        ArangoDBMapping.DocumentFieldType fieldStoreType = arangoDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToArangoField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        map.put(mapKey, result);
      }
      return map;
    } else {
      BaseDocument doc = new BaseDocument();
      if (value == null)
        return doc;
      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = e.getKey().toString();
        Object mapValue = e.getValue();

        ArangoDBMapping.DocumentFieldType fieldStoreType = arangoDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToArangoField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        doc.addAttribute(mapKey, result);
      }
      return doc;
    }
  }

  private Object convertAvroListToDocField(final String docf, final Collection<?> array,
                                           final Schema fieldSchema, final Schema.Type fieldType,
                                           final ArangoDBMapping.DocumentFieldType storeType) {
    if (storeType == ArangoDBMapping.DocumentFieldType.LIST) {
      ArrayList list;
      list = new ArrayList<Object>();
      if (array == null)
        return list;
      for (Object item : array) {
        ArangoDBMapping.DocumentFieldType fieldStoreType = arangoDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToArangoField(docf, fieldSchema, fieldType, fieldStoreType, item);
        list.add(result);
      }
      return list;
    }
    return null;
  }

  private BaseDocument convertAvroBeanToArangoDBDocField(final String docf,
                                                         final Schema fieldSchema,
                                                         final Object value) {
    BaseDocument record = new BaseDocument();
    for (Schema.Field member : fieldSchema.getFields()) {
      Object innerValue = ((PersistentBase) value).get(member.pos());
      String innerDoc = arangoDBMapping.getDocumentField(member.name());
      Schema.Type innerType = member.schema().getType();
      ArangoDBMapping.DocumentFieldType innerStoreType = arangoDBMapping.getDocumentFieldType(innerDoc);
      LOG.debug("Transform value to BaseDocument , docField:{}, schemaType:{}, storeType:{}",
              new Object[]{member.name(), member.schema().getType(),
                      innerStoreType});
      Object fieldValue = convertAvroFieldToArangoField(docf, member.schema()
              , innerType, innerStoreType, innerValue);
      record.addAttribute(member.name(), fieldValue);
    }
    return record;
  }

  private Object convertAvroUnionToArangoDBField(final String docf, final Schema fieldSchema,
                                                 final ArangoDBMapping.DocumentFieldType storeType,
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

      result = convertAvroFieldToArangoField(docf, innerSchema, innerSchema.getType(), storeType, value);
    } else {
      throw new IllegalStateException("ArangoDBStore only supports Union of two types field.");
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean delete(K key) throws GoraException {
    try {
      key = encodeArangoDBKey(key);
      arangoDB.db(arangoDbStoreParams.getDatabaseName())
              .collection(arangoDBMapping.getDocumentClass())
              .deleteDocument(key.toString());
      return true;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    if (query.getFields() == null || (query.getFields().length == getFields().length)) {
      ArangoDBQuery dataStoreQuery = (ArangoDBQuery) query;
      try {
        dataStoreQuery.populateArangoDBQuery(arangoDBMapping, true);
        AqlQueryOptions aqlQueryOptions = new AqlQueryOptions().fullCount(true);
        ArangoCursor<BaseDocument> cursor = arangoDB.db(arangoDbStoreParams.getDatabaseName())
                .query(dataStoreQuery.getArangoDBQuery(), dataStoreQuery.getParams(), aqlQueryOptions, BaseDocument.class);
        long size = new ArangoDBResult<K, T>(this, query, cursor).size();
        dataStoreQuery.populateArangoDBQuery(arangoDBMapping, false);
        arangoDB.db(arangoDbStoreParams.getDatabaseName())
                .query(dataStoreQuery.getArangoDBQuery(), dataStoreQuery.getParams(), aqlQueryOptions, BaseDocument.class);
        return size;
      } catch (Exception e) {
        throw new GoraException(e);
      }
    } else {
      // TODO: not supported for case where query.getFields().length != getFields().length
      // TODO: BaseDocument.removeAttribute or similar is not available at the moment
      return -1;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    ArangoDBQuery dataStoreQuery;
    if (query instanceof ArangoDBQuery) {
      dataStoreQuery = ((ArangoDBQuery) query);
    } else {
      dataStoreQuery = (ArangoDBQuery) ((PartitionQueryImpl<K, T>) query).getBaseQuery();
    }
    dataStoreQuery.populateArangoDBQuery(arangoDBMapping, true);
    try {
      AqlQueryOptions aqlQueryOptions = new AqlQueryOptions().fullCount(true);
      ArangoCursor<BaseDocument> cursor = arangoDB.db(arangoDbStoreParams.getDatabaseName())
              .query(dataStoreQuery.getArangoDBQuery(), dataStoreQuery.getParams(), aqlQueryOptions, BaseDocument.class);
      return new ArangoDBResult<K, T>(this, query, cursor);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query<K, T> newQuery() {
    return new ArangoDBQuery<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
            query);
    partitionQuery.setConf(this.getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  /**
   * {@inheritDoc}
   * Flushes locally cached to content in memory to remote ArangoDB server.
   */
  @Override
  public void flush() throws GoraException {
  }

  /**
   * {@inheritDoc}
   * Releases resources which have been used dataStore.
   */
  @Override
  public void close() {
    try {
      flush();
    } catch (Exception ex) {
      LOG.error("Error occurred while flushing data to ArangoDB : ", ex);
    }
  }

  @Override
  public boolean exists(K key) throws GoraException {
    boolean isExists = arangoDB.db(arangoDbStoreParams.getDatabaseName())
            .collection(arangoDBMapping.getDocumentClass()).documentExists(key.toString());
    return isExists;
  }

  public K encodeArangoDBKey(final K key) {
    if (key == null) {
      return null;
    } else if (!(key instanceof String)) {
      return key;
    }
    return (K) key.toString().replace("/", "%2F")
            .replace("&", "%26");
  }

}
