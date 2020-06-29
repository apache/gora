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

package org.apache.gora.rethinkdb.store;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import org.apache.avro.Schema;
import org.apache.avro.util.Utf8;
import org.apache.gora.rethinkdb.query.RethinkDBQuery;
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
 * {@link RethinkDBStore} is the primary class
 * responsible for facilitating GORA CRUD operations on RethinkDB documents.
 */
public class RethinkDBStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final String DEFAULT_MAPPING_FILE = "/gora-rethinkdb-mapping.xml";
  private RethinkDBStoreParameters rethinkDBStoreParameters;
  private RethinkDBMapping rethinkDBMapping;
  public static final RethinkDB r = RethinkDB.r;
  public Connection connection;

  /**
   * {@inheritDoc}
   * Initialize the RethinkDB dataStore by {@link Properties} parameters.
   *
   * @param keyClass        key class type for dataStore.
   * @param persistentClass persistent class type for dataStore.
   * @param properties      RethinkDB dataStore properties EG:- RethinkDB client credentials.
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);
    try {
      rethinkDBStoreParameters = RethinkDBStoreParameters.load(properties);
      connection = r.connection()
              .hostname(rethinkDBStoreParameters.getServerHost())
              .port(Integer.valueOf(rethinkDBStoreParameters.getServerPort()))
              .user(rethinkDBStoreParameters.getUserName(), rethinkDBStoreParameters.getUserPassword())
              .connect();
      String databaseIdentifier = rethinkDBStoreParameters.getDatabaseName();
      if (!r.dbList()
              .run(connection, ArrayList.class)
              .first()
              .stream()
              .anyMatch(db -> db.equals(databaseIdentifier))) {
        r.dbCreate(rethinkDBStoreParameters.getDatabaseName()).run(connection);
      }

      RethinkDBMappingBuilder<K, T> builder = new RethinkDBMappingBuilder<>(this);
      rethinkDBMapping = builder.fromFile(rethinkDBStoreParameters.getMappingFile()).build();
      if (!schemaExists()) {
        createSchema();
      }
    } catch (Exception e) {
      LOG.error("Error while initializing RethinkDB dataStore: {}",
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
    return rethinkDBMapping.getDocumentClass();
  }

  /**
   * {@inheritDoc}
   * Create a new class of RethinkDB documents if necessary. Enforce specified schema over the document class.
   */
  @Override
  public void createSchema() throws GoraException {
    if (schemaExists()) {
      return;
    }
    try {
      r.db(rethinkDBStoreParameters.getDatabaseName())
              .tableCreate(rethinkDBMapping.getDocumentClass())
              .run(connection);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   * Deletes enforced schema over RethinkDB Document class.
   */
  @Override
  public void deleteSchema() throws GoraException {
    try {
      r.db(rethinkDBStoreParameters.getDatabaseName())
              .tableDrop(rethinkDBMapping.getDocumentClass())
              .run(connection);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   * Check whether there exist a schema enforced over RethinkDB document class.
   */
  @Override
  public boolean schemaExists() throws GoraException {
    try {
      String collectionIdentifier = rethinkDBMapping.getDocumentClass();
      return r.db(rethinkDBStoreParameters.getDatabaseName())
              .tableList()
              .run(connection, ArrayList.class).first()
              .stream()
              .anyMatch(db -> db.equals(collectionIdentifier));
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
      boolean isExists = r.db(rethinkDBStoreParameters.getDatabaseName())
              .table(rethinkDBMapping.getDocumentClass())
              .get(key)
              .count()
              .run(connection, Boolean.class)
              .first();
      if (isExists) {
        String[] dbFields = getFieldsToQuery(fields);
        MapObject<String, Object> document = r.db(rethinkDBStoreParameters.getDatabaseName())
                .table(rethinkDBMapping.getDocumentClass())
                .get(key)
                .run(connection, MapObject.class)
                .first();
        return convertRethinkDBDocToAvroBean(document, dbFields);
      } else {
        return null;
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(K key, T val) throws GoraException {
    if (val.isDirty()) {
      try {
        boolean isExists = r.db(rethinkDBStoreParameters.getDatabaseName())
                .table(rethinkDBMapping.getDocumentClass())
                .getAll(key)
                .count()
                .run(connection, Boolean.class)
                .first();
        if (!isExists) {
          MapObject<String, Object> document = convertAvroBeanToRethinkDBDocument(key, val);
          r.db(rethinkDBStoreParameters.getDatabaseName())
                  .table(rethinkDBMapping.getDocumentClass())
                  .insert(document)
                  .run(connection);
        } else {
          MapObject<String, Object> document = convertAvroBeanToRethinkDBDocument(key, val);
          r.db(rethinkDBStoreParameters.getDatabaseName())
                  .table(rethinkDBMapping.getDocumentClass())
                  .get(key)
                  .replace(document)
                  .run(connection);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean delete(K key) throws GoraException {
    try {
      r.db(rethinkDBStoreParameters.getDatabaseName())
              .table(rethinkDBMapping.getDocumentClass())
              .get(key)
              .delete()
              .run(connection);
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
    return 0L;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    try {
      return null;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query<K, T> newQuery() {
    return new RethinkDBQuery<>(this);
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
   * Flushes locally cached to content in memory to remote RethinkDB server.
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
      LOG.error("Error occurred while flushing data to RethinkDB : ", ex);
    }
  }

  @Override
  public boolean exists(K key) throws GoraException {
    boolean isExists = r.db(rethinkDBStoreParameters.getDatabaseName())
            .table(rethinkDBMapping.getDocumentClass())
            .getAll(key)
            .count()
            .run(connection, Boolean.class)
            .first();
    return isExists;
  }

  private MapObject<String, Object> convertAvroBeanToRethinkDBDocument(final K key, final T persistent) {
    MapObject<String, Object> result = new MapObject();
    for (Schema.Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
        String docf = rethinkDBMapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        RethinkDBMapping.DocumentFieldType storeType = rethinkDBMapping.getDocumentFieldType(docf);
        LOG.debug("Transform value to ODocument, docField:{}, schemaType:{}, storeType:{}",
                new Object[]{docf, f.schema().getType(), storeType});
        Object o = convertAvroFieldToRethinkDBField(docf, f.schema(), f.schema().getType(),
                storeType, value);
        result.put(docf, o);
      }
    }
    result.put("id", key.toString());
    return result;
  }

  private Object convertAvroFieldToRethinkDBField(final String docf, final Schema fieldSchema,
                                                  final Schema.Type fieldType,
                                                  final RethinkDBMapping.DocumentFieldType storeType,
                                                  final Object value) {
    Object result = null;
    switch (fieldType) {
      case MAP:
        if (storeType != null && !(storeType == RethinkDBMapping.DocumentFieldType.MAP ||
                storeType == RethinkDBMapping.DocumentFieldType.DOCUMENT)) {
          throw new IllegalStateException(
                  "Field " + fieldSchema.getName()
                          + ": to store a AVRO 'map', target RethinkDB mapping have to be of type 'Map'" +
                          "| 'Document'");
        }
        Schema valueSchema = fieldSchema.getValueType();
        result = convertAvroMapToDocField(docf, (Map<CharSequence, ?>) value, valueSchema,
                valueSchema.getType(), storeType);
        break;
      case ARRAY:
        if (storeType != null && !(storeType == RethinkDBMapping.DocumentFieldType.LIST)) {
          throw new IllegalStateException("Field " + fieldSchema.getName()
                  + ": To store a AVRO 'array', target RethinkDB mapping have to be of type 'List'");
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
        result = convertAvroBeanToRethinkDBDocField(docf, fieldSchema, value);
        break;
      case UNION:
        result = convertAvroUnionToRethinkDBField(docf, fieldSchema, storeType, value);
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
                                          final RethinkDBMapping.DocumentFieldType storeType) {
    if (storeType == RethinkDBMapping.DocumentFieldType.MAP) {
      HashMap map = new HashMap<String, Object>();
      if (value == null)
        return map;

      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = e.getKey().toString();
        Object mapValue = e.getValue();

        RethinkDBMapping.DocumentFieldType fieldStoreType = rethinkDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToRethinkDBField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        map.put(mapKey, result);
      }
      return map;
    } else {
      MapObject<String, Object> doc = new MapObject<String, Object>();
      if (value == null)
        return doc;
      for (Map.Entry<CharSequence, ?> e : value.entrySet()) {
        String mapKey = e.getKey().toString();
        Object mapValue = e.getValue();

        RethinkDBMapping.DocumentFieldType fieldStoreType = rethinkDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToRethinkDBField(docf, fieldSchema, fieldType, fieldStoreType,
                mapValue);
        doc.put(mapKey, result);
      }
      return doc;
    }
  }

  private Object convertAvroListToDocField(final String docf, final Collection<?> array,
                                           final Schema fieldSchema, final Schema.Type fieldType,
                                           final RethinkDBMapping.DocumentFieldType storeType) {
    if (storeType == RethinkDBMapping.DocumentFieldType.LIST) {
      ArrayList list;
      list = new ArrayList<Object>();
      if (array == null)
        return list;
      for (Object item : array) {
        RethinkDBMapping.DocumentFieldType fieldStoreType = rethinkDBMapping.getDocumentFieldType(docf);
        Object result = convertAvroFieldToRethinkDBField(docf, fieldSchema, fieldType, fieldStoreType, item);
        list.add(result);
      }
      return list;
    }
    return null;
  }

  private MapObject<String, Object> convertAvroBeanToRethinkDBDocField(final String docf,
                                                                       final Schema fieldSchema,
                                                                       final Object value) {
    MapObject<String, Object> record = new MapObject();
    for (Schema.Field member : fieldSchema.getFields()) {
      Object innerValue = ((PersistentBase) value).get(member.pos());
      String innerDoc = rethinkDBMapping.getDocumentField(member.name());
      Schema.Type innerType = member.schema().getType();
      RethinkDBMapping.DocumentFieldType innerStoreType = rethinkDBMapping.getDocumentFieldType(innerDoc);
      LOG.debug("Transform value to BaseDocument , docField:{}, schemaType:{}, storeType:{}",
              new Object[]{member.name(), member.schema().getType(),
                      innerStoreType});
      Object fieldValue = convertAvroFieldToRethinkDBField(docf, member.schema()
              , innerType, innerStoreType, innerValue);
      record.put(member.name(), fieldValue);
    }
    return record;
  }

  private Object convertAvroUnionToRethinkDBField(final String docf, final Schema fieldSchema,
                                                  final RethinkDBMapping.DocumentFieldType storeType,
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

      result = convertAvroFieldToRethinkDBField(docf, innerSchema, innerSchema.getType(), storeType, value);
    } else {
      throw new IllegalStateException("RethinkDBStore only supports Union of two types field.");
    }
    return result;
  }

  public T convertRethinkDBDocToAvroBean(final MapObject<String, Object> obj, final String[] fields) throws GoraException {
    T persistent = newPersistent();
    String[] dbFields = getFieldsToQuery(fields);
    for (String f : dbFields) {
      String docf = rethinkDBMapping.getDocumentField(f);
      if (docf == null || !obj.containsKey(docf))
        continue;

      RethinkDBMapping.DocumentFieldType storeType = rethinkDBMapping.getDocumentFieldType(docf);
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
                                            final RethinkDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final MapObject<String, Object> obj) throws GoraException {
    Object result = null;
    switch (fieldSchema.getType()) {
      case MAP:
        result = convertDocFieldToAvroMap(docf, fieldSchema, obj, field, storeType);
        break;
      case ARRAY:
        result = convertDocFieldToAvroList(docf, fieldSchema, obj, field, storeType);
        break;
      case RECORD:
        MapObject<String, Object> record = (MapObject<String, Object>) obj.get(docf);
        if (record == null) {
          result = null;
          break;
        }
        result = convertAvroBeanToRethinkDBDoc(fieldSchema, record);
        break;
      case BOOLEAN:
        result = Boolean.valueOf(obj.get(docf).toString());
        break;
      case DOUBLE:
        result = Double.valueOf(obj.get(docf).toString());
        break;
      case FLOAT:
        result = Float.valueOf(obj.get(docf).toString());
        break;
      case INT:
        result = Integer.valueOf(obj.get(docf).toString());
        break;
      case LONG:
        result = Long.valueOf(obj.get(docf).toString());
        break;
      case STRING:
        result = new Utf8(obj.get(docf).toString());
        ;
        break;
      case ENUM:
        result = AvroUtils.getEnumValue(fieldSchema, obj.get(docf).toString());
        break;
      case BYTES:
      case FIXED:
        if (!obj.containsKey(docf)) {
          result = null;
          break;
        }
        result = ByteBuffer.wrap(Base64
                .getDecoder()
                .decode(obj.get(docf).toString()));
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
                                            final RethinkDBMapping.DocumentFieldType storeType,
                                            final Schema.Field field,
                                            final String docf,
                                            final MapObject<String, Object> doc) throws GoraException {
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
      throw new GoraException("RethinkDBStore only supports Union of two types field.");
    }
    return result;
  }

  private Object convertAvroBeanToRethinkDBDoc(final Schema fieldSchema,
                                               final MapObject<String, Object> doc) throws GoraException {
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
      RethinkDBMapping.DocumentFieldType innerStoreType = rethinkDBMapping
              .getDocumentFieldType(recField.name());
      String innerDocField = rethinkDBMapping.getDocumentField(recField.name()) != null ? rethinkDBMapping
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
                                           final MapObject<String, Object> doc,
                                           final Schema.Field f,
                                           final RethinkDBMapping.DocumentFieldType storeType) throws GoraException {

    if (storeType == RethinkDBMapping.DocumentFieldType.LIST) {
      List<Object> list = (List<Object>) doc.get(docf);
      List<Object> rlist = new ArrayList<>();
      if (list == null) {
        return new DirtyListWrapper(rlist);
      }

      for (Object item : list) {
        MapObject<String, Object> innerDoc = new MapObject();
        innerDoc.put("item", item);
        Object o = convertDocFieldToAvroField(fieldSchema.getElementType(), storeType, f,
                "item", innerDoc);
        rlist.add(o);
      }
      return new DirtyListWrapper<>(rlist);
    }
    return null;
  }

  private Object convertDocFieldToAvroMap(final String docf, final Schema fieldSchema,
                                          final MapObject<String, Object> doc, final Schema.Field f,
                                          final RethinkDBMapping.DocumentFieldType storeType) throws GoraException {
    if (storeType == RethinkDBMapping.DocumentFieldType.MAP) {
      Map<String, Object> map = (Map<String, Object>) doc.get(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (map == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (Map.Entry entry : map.entrySet()) {
        String mapKey = entry.getKey().toString();
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                decorateMapToODoc(map));
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    } else {
      MapObject<String, Object> innerDoc = (MapObject<String, Object>) doc.get(docf);
      Map<Utf8, Object> rmap = new HashMap<>();
      if (innerDoc == null) {
        return new DirtyMapWrapper(rmap);
      }

      for (String fieldName : innerDoc.keySet()) {
        String mapKey = fieldName;
        Object o = convertDocFieldToAvroField(fieldSchema.getValueType(), storeType, f, mapKey,
                innerDoc);
        rmap.put(new Utf8(mapKey), o);
      }
      return new DirtyMapWrapper<>(rmap);
    }
  }

  private MapObject<String, Object> decorateMapToODoc(Map<String, Object> map) {
    MapObject<String, Object> doc = new MapObject();
    for (Map.Entry entry : map.entrySet()) {
      doc.put(entry.getKey().toString(), entry.getValue());
    }
    return doc;
  }

}
