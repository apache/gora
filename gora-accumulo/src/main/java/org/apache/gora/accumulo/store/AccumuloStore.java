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
package org.apache.gora.accumulo.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableDeletedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.mock.MockConnector;
import org.apache.accumulo.core.client.mock.MockInstance;
import org.apache.accumulo.core.client.mock.MockTabletLocator;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.KeyExtent;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyIterator;
import org.apache.accumulo.core.iterators.user.TimestampFilter;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.CredentialHelper;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.accumulo.core.util.UtilWaitThread;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.accumulo.encoders.BinaryEncoder;
import org.apache.gora.accumulo.encoders.Encoder;
import org.apache.gora.accumulo.query.AccumuloQuery;
import org.apache.gora.accumulo.query.AccumuloResult;
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
import org.apache.gora.util.GoraException;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Directs CRUD operations into Accumulo.
 */
public class AccumuloStore<K,T extends PersistentBase> extends DataStoreBase<K,T> {

  protected static final String MOCK_PROPERTY = "accumulo.mock";
  protected static final String INSTANCE_NAME_PROPERTY = "accumulo.instance";
  protected static final String ZOOKEEPERS_NAME_PROPERTY = "accumulo.zookeepers";
  protected static final String USERNAME_PROPERTY = "accumulo.user";
  protected static final String PASSWORD_PROPERTY = "accumulo.password";
  protected static final String DEFAULT_MAPPING_FILE = "gora-accumulo-mapping.xml";

  private Connector conn;
  private BatchWriter batchWriter;
  private AccumuloMapping mapping;
  private TCredentials credentials;
  private Encoder encoder;

  public static final Logger LOG = LoggerFactory.getLogger(AccumuloStore.class);

  public Object fromBytes(Schema schema, byte data[]) throws GoraException {
    Schema fromSchema = null;
    if (schema.getType() == Type.UNION) {
      try {
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        int unionIndex = decoder.readIndex();
        List<Schema> possibleTypes = schema.getTypes();
        fromSchema = possibleTypes.get(unionIndex);
        Schema effectiveSchema = possibleTypes.get(unionIndex);
        if (effectiveSchema.getType() == Type.NULL) {
          decoder.readNull();
          return null;
        } else {
          data = decoder.readBytes(null).array();
        }
      } catch (IOException e) {
        e.printStackTrace();
        throw new GoraException("Error decoding union type: ", e);
      }
    } else {
      fromSchema = schema;
    }
    return fromBytes(encoder, fromSchema, data);
  }

  public static Object fromBytes(Encoder encoder, Schema schema, byte data[]) {
    switch (schema.getType()) {
    case BOOLEAN:
      return encoder.decodeBoolean(data);
    case DOUBLE:
      return encoder.decodeDouble(data);
    case FLOAT:
      return encoder.decodeFloat(data);
    case INT:
      return encoder.decodeInt(data);
    case LONG:
      return encoder.decodeLong(data);
    case STRING:
      return new Utf8(data);
    case BYTES:
      return ByteBuffer.wrap(data);
    case ENUM:
      return AvroUtils.getEnumValue(schema, encoder.decodeInt(data));
    case ARRAY:
      break;
    case FIXED:
      break;
    case MAP:
      break;
    case NULL:
      break;
    case RECORD:
      break;
    case UNION:
      break;
    default:
      break;
    }
    throw new IllegalArgumentException("Unknown type " + schema.getType());

  }

  public K fromBytes(Class<K> clazz, byte[] val) {
    return fromBytes(encoder, clazz, val);
  }

  @SuppressWarnings("unchecked")
  public static <K> K fromBytes(Encoder encoder, Class<K> clazz, byte[] val) {
    try {
      if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
        return (K) Byte.valueOf(encoder.decodeByte(val));
      } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
        return (K) Boolean.valueOf(encoder.decodeBoolean(val));
      } else if (clazz.equals(Short.TYPE) || clazz.equals(Short.class)) {
        return (K) Short.valueOf(encoder.decodeShort(val));
      } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
        return (K) Integer.valueOf(encoder.decodeInt(val));
      } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
        return (K) Long.valueOf(encoder.decodeLong(val));
      } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
        return (K) Float.valueOf(encoder.decodeFloat(val));
      } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
        return (K) Double.valueOf(encoder.decodeDouble(val));
      } else if (clazz.equals(String.class)) {
        return (K) new String(val, "UTF-8");
      } else if (clazz.equals(Utf8.class)) {
        return (K) new Utf8(val);
      }

      throw new IllegalArgumentException("Unknown type " + clazz.getName());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static byte[] copyIfNeeded(byte b[], int offset, int len) {
    if (len != b.length || offset != 0) {
      byte copy[] = new byte[len];
      System.arraycopy(b, offset, copy, 0, copy.length);
      b = copy;
    }
    return b;
  }

  public byte[] toBytes(Schema toSchema, Object o) {
    if (toSchema != null && toSchema.getType() == Type.UNION) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      org.apache.avro.io.BinaryEncoder avroEncoder = EncoderFactory.get().binaryEncoder(baos, null);
      int unionIndex = 0;
      try {
        if (o == null) {
          unionIndex = firstNullSchemaTypeIndex(toSchema);
          avroEncoder.writeIndex(unionIndex);
          avroEncoder.writeNull();
        } else {
          unionIndex = firstNotNullSchemaTypeIndex(toSchema);
          avroEncoder.writeIndex(unionIndex);
          avroEncoder.writeBytes(toBytes(o));
        }
        avroEncoder.flush();
        return baos.toByteArray();
      } catch (IOException e) {
        e.printStackTrace();
        return toBytes(o);
      }
    } else {
      return toBytes(o);
    }
  }

  private int firstNullSchemaTypeIndex(Schema toSchema) {
    List<Schema> possibleTypes = toSchema.getTypes();
    int unionIndex = 0;
    for (int i = 0; i < possibleTypes.size(); i++ ) {
      Type pType = possibleTypes.get(i).getType();
      if (pType == Type.NULL) { // FIXME HUGE kludge to pass tests
        unionIndex = i; break;
      }
    }
    return unionIndex;
  }

  private int firstNotNullSchemaTypeIndex(Schema toSchema) {
    List<Schema> possibleTypes = toSchema.getTypes();
    int unionIndex = 0;
    for (int i = 0; i < possibleTypes.size(); i++ ) {
      Type pType = possibleTypes.get(i).getType();
      if (pType != Type.NULL) { // FIXME HUGE kludge to pass tests
        unionIndex = i; break;
      }
    }
    return unionIndex;
  }

  public byte[] toBytes(Object o) {
    return toBytes(encoder, o);
  }

  public static byte[] toBytes(Encoder encoder, Object o) {

    try {
      if (o instanceof String) {
        return ((String) o).getBytes("UTF-8");
      } else if (o instanceof Utf8) {
        return copyIfNeeded(((Utf8) o).getBytes(), 0, ((Utf8) o).getByteLength());
      } else if (o instanceof ByteBuffer) {
        return copyIfNeeded(((ByteBuffer) o).array(), ((ByteBuffer) o).arrayOffset() + ((ByteBuffer) o).position(), ((ByteBuffer) o).remaining());
      } else if (o instanceof Long) {
        return encoder.encodeLong((Long) o);
      } else if (o instanceof Integer) {
        return encoder.encodeInt((Integer) o);
      } else if (o instanceof Short) {
        return encoder.encodeShort((Short) o);
      } else if (o instanceof Byte) {
        return encoder.encodeByte((Byte) o);
      } else if (o instanceof Boolean) {
        return encoder.encodeBoolean((Boolean) o);
      } else if (o instanceof Float) {
        return encoder.encodeFloat((Float) o);
      } else if (o instanceof Double) {
        return encoder.encodeDouble((Double) o);
      } else if (o instanceof Enum) {
        return encoder.encodeInt(((Enum<?>) o).ordinal());
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    throw new IllegalArgumentException("Uknown type " + o.getClass().getName());
  }

  private BatchWriter getBatchWriter() throws IOException {
    if (batchWriter == null)
      try {
        BatchWriterConfig batchWriterConfig = new BatchWriterConfig();
        batchWriterConfig.setMaxMemory(10000000);
        batchWriterConfig.setMaxLatency(60000l, TimeUnit.MILLISECONDS);
        batchWriterConfig.setMaxWriteThreads(4);
        batchWriter = conn.createBatchWriter(mapping.tableName, batchWriterConfig);
      } catch (TableNotFoundException e) {
        throw new IOException(e);
      }
    return batchWriter;
  }

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    try{
      super.initialize(keyClass, persistentClass, properties);

      String mock = DataStoreFactory.findProperty(properties, this, MOCK_PROPERTY, null);
      String mappingFile = DataStoreFactory.getMappingFile(properties, this, DEFAULT_MAPPING_FILE);
      String user = DataStoreFactory.findProperty(properties, this, USERNAME_PROPERTY, null);
      String password = DataStoreFactory.findProperty(properties, this, PASSWORD_PROPERTY, null);

      mapping = readMapping(mappingFile);

      if (mapping.encoder == null || mapping.encoder.equals("")) {
        encoder = new BinaryEncoder();
      } else {
        try {
          encoder = (Encoder) getClass().getClassLoader().loadClass(mapping.encoder).newInstance();
        } catch (InstantiationException e) {
          throw new IOException(e);
        } catch (IllegalAccessException e) {
          throw new IOException(e);
        } catch (ClassNotFoundException e) {
          throw new IOException(e);
        }
      }

      try {
        AuthenticationToken token = new PasswordToken(password);
        if (mock == null || !mock.equals("true")) {
          String instance = DataStoreFactory.findProperty(properties, this, INSTANCE_NAME_PROPERTY, null);
          String zookeepers = DataStoreFactory.findProperty(properties, this, ZOOKEEPERS_NAME_PROPERTY, null);
          conn = new ZooKeeperInstance(instance, zookeepers).getConnector(user, token);
        } else {
          conn = new MockInstance().getConnector(user, token);
        }
        credentials = CredentialHelper.create(user, token, conn.getInstance().getInstanceID());

        if (autoCreateSchema && !schemaExists())
          createSchema();
      } catch (AccumuloException e) {
        throw new IOException(e);
      } catch (AccumuloSecurityException e) {
        throw new IOException(e);
      }
    } catch(IOException e){
      LOG.error(e.getMessage(), e);
    }
  }

  protected AccumuloMapping readMapping(String filename) throws IOException {
    try {

      AccumuloMapping mapping = new AccumuloMapping();

      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document dom = db.parse(getClass().getClassLoader().getResourceAsStream(filename));

      Element root = dom.getDocumentElement();

      NodeList nl = root.getElementsByTagName("class");
      for (int i = 0; i < nl.getLength(); i++) {

        Element classElement = (Element) nl.item(i);
        if (classElement.getAttribute("keyClass").equals(keyClass.getCanonicalName())
            && classElement.getAttribute("name").equals(persistentClass.getCanonicalName())) {

          mapping.tableName = getSchemaName(classElement.getAttribute("table"), persistentClass);
          mapping.encoder = classElement.getAttribute("encoder");

          NodeList fields = classElement.getElementsByTagName("field");
          for (int j = 0; j < fields.getLength(); j++) {
            Element fieldElement = (Element) fields.item(j);

            String name = fieldElement.getAttribute("name");
            String family = fieldElement.getAttribute("family");
            String qualifier = fieldElement.getAttribute("qualifier");
            if (qualifier.equals(""))
              qualifier = null;

            Pair<Text,Text> col = new Pair<Text,Text>(new Text(family), qualifier == null ? null : new Text(qualifier));
            mapping.fieldMap.put(name, col);
            mapping.columnMap.put(col, name);
          }
        }

      }

      if (mapping.tableName == null) {
        throw new GoraException("Please define the accumulo 'table' name mapping in " + filename + " for " + persistentClass.getCanonicalName());
      }

      nl = root.getElementsByTagName("table");
      for (int i = 0; i < nl.getLength(); i++) {
        Element tableElement = (Element) nl.item(i);
        if (tableElement.getAttribute("name").equals(mapping.tableName)) {
          NodeList configs = tableElement.getElementsByTagName("config");
          for (int j = 0; j < configs.getLength(); j++) {
            Element configElement = (Element) configs.item(j);
            String key = configElement.getAttribute("key");
            String val = configElement.getAttribute("value");
            mapping.tableConfig.put(key, val);
          }
        }
      }

      return mapping;
    } catch (Exception ex) {
      throw new IOException("Unable to read " + filename, ex);
    }

  }

  @Override
  public String getSchemaName() {
    return mapping.tableName;
  }

  @Override
  public void createSchema() {
    try {
      conn.tableOperations().create(mapping.tableName);
      Set<Entry<String,String>> es = mapping.tableConfig.entrySet();
      for (Entry<String,String> entry : es) {
        conn.tableOperations().setProperty(mapping.tableName, entry.getKey(), entry.getValue());
      }

    } catch (AccumuloException e) {
      LOG.error(e.getMessage(), e);
    } catch (AccumuloSecurityException e) {
      LOG.error(e.getMessage(), e);
    } catch (TableExistsException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void deleteSchema() {
    try {
      if (batchWriter != null)
        batchWriter.close();
      batchWriter = null;
      conn.tableOperations().delete(mapping.tableName);
    } catch (AccumuloException e) {
      LOG.error(e.getMessage(), e);
    } catch (AccumuloSecurityException e) {
      LOG.error(e.getMessage(), e);
    } catch (TableNotFoundException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean schemaExists() {
    return conn.tableOperations().exists(mapping.tableName);
  }

  public ByteSequence populate(Iterator<Entry<Key,Value>> iter, T persistent) throws IOException {
    ByteSequence row = null;

    Map<Utf8, Object> currentMap = null;
    List currentArray = null;
    Text currentFam = null;
    int currentPos = 0;
    Schema currentSchema = null;
    Field currentField = null;

    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new byte[0], null);

    while (iter.hasNext()) {
      Entry<Key,Value> entry = iter.next();

      if (row == null) {
        row = entry.getKey().getRowData();
      }
      byte[] val = entry.getValue().get();

      Field field = fieldMap.get(getFieldName(entry));

      if (currentMap != null) {
        if (currentFam.equals(entry.getKey().getColumnFamily())) {
          currentMap.put(new Utf8(entry.getKey().getColumnQualifierData().toArray()),
              fromBytes(currentSchema, entry.getValue().get()));
          continue;
        } else {
          persistent.put(currentPos, currentMap);
          currentMap = null;
        }
      } else if (currentArray != null) {
        if (currentFam.equals(entry.getKey().getColumnFamily())) {
          currentArray.add(fromBytes(currentSchema, entry.getValue().get()));
          continue;
        } else {
          persistent.put(currentPos, new GenericData.Array<T>(currentField.schema(), currentArray));
          currentArray = null;
        }
      }

      switch (field.schema().getType()) {
      case MAP:  // first entry only. Next are handled above on the next loop
        currentMap = new DirtyMapWrapper<Utf8, Object>(new HashMap<Utf8, Object>());
        currentPos = field.pos();
        currentFam = entry.getKey().getColumnFamily();
        currentSchema = field.schema().getValueType();

        currentMap.put(new Utf8(entry.getKey().getColumnQualifierData().toArray()),
            fromBytes(currentSchema, entry.getValue().get()));
        break;
      case ARRAY:
        currentArray = new DirtyListWrapper<Object>(new ArrayList<Object>());
        currentPos = field.pos();
        currentFam = entry.getKey().getColumnFamily();
        currentSchema = field.schema().getElementType();
        currentField = field;

        currentArray.add(fromBytes(currentSchema, entry.getValue().get()));

        break;
      case UNION:// default value of null acts like union with null
        Schema effectiveSchema = field.schema().getTypes()
        .get(firstNotNullSchemaTypeIndex(field.schema()));
        // map and array were coded without union index so need to be read the same way
        if (effectiveSchema.getType() == Type.ARRAY) {
          currentArray = new DirtyListWrapper<Object>(new ArrayList<Object>());
          currentPos = field.pos();
          currentFam = entry.getKey().getColumnFamily();
          currentSchema = field.schema().getElementType();
          currentField = field;

          currentArray.add(fromBytes(currentSchema, entry.getValue().get()));
          break;
        }
        else if (effectiveSchema.getType() == Type.MAP) {
          currentMap = new DirtyMapWrapper<Utf8, Object>(new HashMap<Utf8, Object>());
          currentPos = field.pos();
          currentFam = entry.getKey().getColumnFamily();
          currentSchema = effectiveSchema.getValueType();

          currentMap.put(new Utf8(entry.getKey().getColumnQualifierData().toArray()),
              fromBytes(currentSchema, entry.getValue().get()));
          break;
        }
        // continue like a regular top-level union
      case RECORD:
        SpecificDatumReader<?> reader = new SpecificDatumReader<Schema>(field.schema());
        persistent.put(field.pos(), reader.read(null, DecoderFactory.get().binaryDecoder(val, decoder)));
        break;
      default:
        persistent.put(field.pos(), fromBytes(field.schema(), entry.getValue().get()));
      }
    }

    if (currentMap != null) {
      persistent.put(currentPos, currentMap);
    } else if (currentArray != null) {
      persistent.put(currentPos, new GenericData.Array<T>(currentField.schema(), currentArray));
    }

    persistent.clearDirty();

    return row;
  }

  /**
   * Retrieve field name from entry.
   * @param entry The Key-Value entry
   * @return String The field name
   */
  private String getFieldName(Entry<Key, Value> entry) {
    String fieldName = mapping.columnMap.get(new Pair<Text,Text>(entry.getKey().getColumnFamily(),
        entry.getKey().getColumnQualifier()));
    if (fieldName == null) {
      fieldName = mapping.columnMap.get(new Pair<Text,Text>(entry.getKey().getColumnFamily(), null));
    }
    return fieldName;
  }

  private void setFetchColumns(Scanner scanner, String fields[]) {
    fields = getFieldsToQuery(fields);
    for (String field : fields) {
      Pair<Text,Text> col = mapping.fieldMap.get(field);
      if (col != null) {
        if (col.getSecond() == null) {
          scanner.fetchColumnFamily(col.getFirst());
        } else {
          scanner.fetchColumn(col.getFirst(), col.getSecond());
        }
      } else {
        LOG.error("Mapping not found for field: " + field);
      }
    }
  }

  @Override
  public T get(K key, String[] fields) {
    try {
      // TODO make isolated scanner optional?
      Scanner scanner = new IsolatedScanner(conn.createScanner(mapping.tableName, Constants.NO_AUTHS));
      Range rowRange = new Range(new Text(toBytes(key)));

      scanner.setRange(rowRange);
      setFetchColumns(scanner, fields);

      T persistent = newPersistent();
      ByteSequence row = populate(scanner.iterator(), persistent);
      if (row == null)
        return null;
      return persistent;
    } catch (TableNotFoundException e) {
      LOG.error(e.getMessage(), e);
      return null;
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public void put(K key, T val) {

    try{
      Mutation m = new Mutation(new Text(toBytes(key)));

      Schema schema = val.getSchema();
      List<Field> fields = schema.getFields();
      int count = 0;

      for (int i = 0; i < fields.size(); i++) {
        if (!val.isDirty(i)) {
          continue;
        }
        Field field = fields.get(i);

        Object o = val.get(field.pos());

        Pair<Text,Text> col = mapping.fieldMap.get(field.name());

        if (col == null) {
          throw new GoraException("Please define the gora to accumulo mapping for field " + field.name());
        }

        switch (field.schema().getType()) {
        case MAP:
          count = putMap(m, count, field.schema().getValueType(), o, col);
          break;
        case ARRAY:
          count = putArray(m, count, o, col);
          break;
        case UNION: // default value of null acts like union with null
          Schema effectiveSchema = field.schema().getTypes()
          .get(firstNotNullSchemaTypeIndex(field.schema()));
          // map and array need to compute qualifier
          if (effectiveSchema.getType() == Type.ARRAY) {
            count = putArray(m, count, o, col);
            break;
          }
          else if (effectiveSchema.getType() == Type.MAP) {
            count = putMap(m, count, effectiveSchema.getValueType(), o, col);
            break;
          }
          // continue like a regular top-level union
        case RECORD:
          SpecificDatumWriter<Object> writer = new SpecificDatumWriter<Object>(field.schema());
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          org.apache.avro.io.BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(os, null);
          writer.write(o, encoder);
          encoder.flush();
          m.put(col.getFirst(), col.getSecond(), new Value(os.toByteArray()));
          count++;
          break;
        default:
          m.put(col.getFirst(), col.getSecond(), new Value(toBytes(o)));
          count++;
        }

      }

      if (count > 0)
        try {
          getBatchWriter().addMutation(m);
        } catch (MutationsRejectedException e) {
          LOG.error(e.getMessage(), e);
        }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private int putMap(Mutation m, int count, Schema valueType, Object o, Pair<Text, Text> col) throws GoraException {

    // First of all we delete map field on accumulo store
    Text rowKey = new Text(m.getRow());
    Query<K, T> query = newQuery();
    query.setFields(col.getFirst().toString());
    query.setStartKey((K)rowKey.toString());
    query.setEndKey((K)rowKey.toString());
    deleteByQuery(query);
    flush();
    if (o == null){
      return 0;
    }

    Set<?> es = ((Map<?, ?>)o).entrySet();
    for (Object entry : es) {
      Object mapKey = ((Entry<?, ?>) entry).getKey();
      Object mapVal = ((Entry<?, ?>) entry).getValue();
      if ((o instanceof DirtyMapWrapper && ((DirtyMapWrapper<?, ?>)o).isDirty())
          || !(o instanceof DirtyMapWrapper)) { //mapVal instanceof Dirtyable && ((Dirtyable)mapVal).isDirty()) {
        m.put(col.getFirst(), new Text(toBytes(mapKey)), new Value(toBytes(valueType, mapVal)));
        count++;
      }
      // TODO map value deletion
    }
    return count;
  }

  private int putArray(Mutation m, int count, Object o, Pair<Text, Text> col) {

    // First of all we delete array field on accumulo store
    Text rowKey = new Text(m.getRow());
    Query<K, T> query = newQuery();
    query.setFields(col.getFirst().toString());
    query.setStartKey((K)rowKey.toString());
    query.setEndKey((K)rowKey.toString());
    deleteByQuery(query);
    flush();
    if (o == null){
      return 0;
    }

    List<?> array = (List<?>) o;  // both GenericArray and DirtyListWrapper
    int j = 0;
    for (Object item : array) {
      m.put(col.getFirst(), new Text(toBytes(j++)), new Value(toBytes(item)));
      count++;
    }
    return count;
  }

  @Override
  public boolean delete(K key) {
    Query<K,T> q = newQuery();
    q.setKey(key);
    return deleteByQuery(q) > 0;
  }

  @Override
  public long deleteByQuery(Query<K,T> query) {
    try {
      Scanner scanner = createScanner(query);
      // add iterator that drops values on the server side
      scanner.addScanIterator(new IteratorSetting(Integer.MAX_VALUE, SortedKeyIterator.class));
      RowIterator iterator = new RowIterator(scanner.iterator());

      long count = 0;

      while (iterator.hasNext()) {
        Iterator<Entry<Key,Value>> row = iterator.next();
        Mutation m = null;
        while (row.hasNext()) {
          Entry<Key,Value> entry = row.next();
          Key key = entry.getKey();
          if (m == null)
            m = new Mutation(key.getRow());
          // TODO optimize to avoid continually creating column vis? prob does not matter for empty
          m.putDelete(key.getColumnFamily(), key.getColumnQualifier(), new ColumnVisibility(key.getColumnVisibility()), key.getTimestamp());
        }
        getBatchWriter().addMutation(m);
        count++;
      }

      return count;
    } catch (TableNotFoundException e) {
      // TODO return 0?
      LOG.error(e.getMessage(), e);
      return 0;
    } catch (MutationsRejectedException e) {
      LOG.error(e.getMessage(), e);
      return 0;
    } catch (IOException e){
      LOG.error(e.getMessage(), e);
      return 0;
    }
  }

  private Range createRange(Query<K,T> query) {
    Text startRow = null;
    Text endRow = null;

    if (query.getStartKey() != null)
      startRow = new Text(toBytes(query.getStartKey()));

    if (query.getEndKey() != null)
      endRow = new Text(toBytes(query.getEndKey()));

    return new Range(startRow, true, endRow, true);

  }

  private Scanner createScanner(Query<K,T> query) throws TableNotFoundException {
    // TODO make isolated scanner optional?
    Scanner scanner = new IsolatedScanner(conn.createScanner(mapping.tableName, Constants.NO_AUTHS));
    setFetchColumns(scanner, query.getFields());

    scanner.setRange(createRange(query));

    if (query.getStartTime() != -1 || query.getEndTime() != -1) {
      IteratorSetting is = new IteratorSetting(30, TimestampFilter.class);
      if (query.getStartTime() != -1)
        TimestampFilter.setStart(is, query.getStartTime(), true);
      if (query.getEndTime() != -1)
        TimestampFilter.setEnd(is, query.getEndTime(), true);

      scanner.addScanIterator(is);
    }

    return scanner;
  }

  @Override
  public Result<K,T> execute(Query<K,T> query) {
    try {
      Scanner scanner = createScanner(query);
      return new AccumuloResult<K,T>(this, query, scanner);
    } catch (TableNotFoundException e) {
      // TODO return empty result?
      LOG.error(e.getMessage(), e);
      return null;
    }
  }

  @Override
  public Query<K,T> newQuery() {
    return new AccumuloQuery<K,T>(this);
  }

  Text pad(Text key, int bytes) {
    if (key.getLength() < bytes)
      key = new Text(key);

    while (key.getLength() < bytes) {
      key.append(new byte[] {0}, 0, 1);
    }

    return key;
  }

  @Override
  public List<PartitionQuery<K,T>> getPartitions(Query<K,T> query) throws IOException {
    try {
      TabletLocator tl;
      if (conn instanceof MockConnector)
        tl = new MockTabletLocator();
      else
        tl = TabletLocator.getInstance(conn.getInstance(), new Text(Tables.getTableId(conn.getInstance(), mapping.tableName)));

      Map<String,Map<KeyExtent,List<Range>>> binnedRanges = new HashMap<String,Map<KeyExtent,List<Range>>>();

      tl.invalidateCache();
      while (tl.binRanges(Collections.singletonList(createRange(query)), binnedRanges, credentials).size() > 0) {
        // TODO log?
        if (!Tables.exists(conn.getInstance(), Tables.getTableId(conn.getInstance(), mapping.tableName)))
          throw new TableDeletedException(Tables.getTableId(conn.getInstance(), mapping.tableName));
        else if (Tables.getTableState(conn.getInstance(), Tables.getTableId(conn.getInstance(), mapping.tableName)) == TableState.OFFLINE)
          throw new TableOfflineException(conn.getInstance(), Tables.getTableId(conn.getInstance(), mapping.tableName));
        UtilWaitThread.sleep(100);
        tl.invalidateCache();
      }

      List<PartitionQuery<K,T>> ret = new ArrayList<PartitionQuery<K,T>>();

      Text startRow = null;
      Text endRow = null;
      if (query.getStartKey() != null)
        startRow = new Text(toBytes(query.getStartKey()));
      if (query.getEndKey() != null)
        endRow = new Text(toBytes(query.getEndKey()));

      //hadoop expects hostnames, accumulo keeps track of IPs... so need to convert
      HashMap<String,String> hostNameCache = new HashMap<String,String>();

      for (Entry<String,Map<KeyExtent,List<Range>>> entry : binnedRanges.entrySet()) {
        String ip = entry.getKey().split(":", 2)[0];
        String location = hostNameCache.get(ip);
        if (location == null) {
          InetAddress inetAddress = InetAddress.getByName(ip);
          location = inetAddress.getHostName();
          hostNameCache.put(ip, location);
        }

        Map<KeyExtent,List<Range>> tablets = entry.getValue();
        for (KeyExtent ke : tablets.keySet()) {

          K startKey = null;
          if (startRow == null || !ke.contains(startRow)) {
            if (ke.getPrevEndRow() != null) {
              startKey = followingKey(encoder, getKeyClass(), TextUtil.getBytes(ke.getPrevEndRow()));
            }
          } else {
            startKey = fromBytes(getKeyClass(), TextUtil.getBytes(startRow));
          }

          K endKey = null;
          if (endRow == null || !ke.contains(endRow)) {
            if (ke.getEndRow() != null)
              endKey = lastPossibleKey(encoder, getKeyClass(), TextUtil.getBytes(ke.getEndRow()));
          } else {
            endKey = fromBytes(getKeyClass(), TextUtil.getBytes(endRow));
          }

          PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<K,T>(query, startKey, endKey, new String[] {location});
          pqi.setConf(getConf());
          ret.add(pqi);
        }
      }

      return ret;
    } catch (TableNotFoundException e) {
      throw new IOException(e);
    } catch (AccumuloException e) {
      throw new IOException(e);
    } catch (AccumuloSecurityException e) {
      throw new IOException(e);
    }

  }

  static <K> K lastPossibleKey(Encoder encoder, Class<K> clazz, byte[] er) {

    if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
      throw new UnsupportedOperationException();
    } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
      throw new UnsupportedOperationException();
    } else if (clazz.equals(Short.TYPE) || clazz.equals(Short.class)) {
      return fromBytes(encoder, clazz, encoder.lastPossibleKey(2, er));
    } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
      return fromBytes(encoder, clazz, encoder.lastPossibleKey(4, er));
    } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
      return fromBytes(encoder, clazz, encoder.lastPossibleKey(8, er));
    } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
      return fromBytes(encoder, clazz, encoder.lastPossibleKey(4, er));
    } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
      return fromBytes(encoder, clazz, encoder.lastPossibleKey(8, er));
    } else if (clazz.equals(String.class)) {
      throw new UnsupportedOperationException();
    } else if (clazz.equals(Utf8.class)) {
      return fromBytes(encoder, clazz, er);
    }

    throw new IllegalArgumentException("Unknown type " + clazz.getName());
  }



  /**
   * @param keyClass
   * @param bytes
   * @return
   */
  @SuppressWarnings("unchecked")
  static <K> K followingKey(Encoder encoder, Class<K> clazz, byte[] per) {

    if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
      return (K) Byte.valueOf(encoder.followingKey(1, per)[0]);
    } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
      throw new UnsupportedOperationException();
    } else if (clazz.equals(Short.TYPE) || clazz.equals(Short.class)) {
      return fromBytes(encoder, clazz, encoder.followingKey(2, per));
    } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
      return fromBytes(encoder, clazz, encoder.followingKey(4, per));
    } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
      return fromBytes(encoder, clazz, encoder.followingKey(8, per));
    } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
      return fromBytes(encoder, clazz, encoder.followingKey(4, per));
    } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
      return fromBytes(encoder, clazz, encoder.followingKey(8, per));
    } else if (clazz.equals(String.class)) {
      throw new UnsupportedOperationException();
    } else if (clazz.equals(Utf8.class)) {
      return fromBytes(encoder, clazz, Arrays.copyOf(per, per.length + 1));
    }

    throw new IllegalArgumentException("Unknown type " + clazz.getName());
  }

  @Override
  public void flush() {
    try {
      if (batchWriter != null) {
        batchWriter.flush();
      }
    } catch (MutationsRejectedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    try {
      if (batchWriter != null) {
        batchWriter.close();
        batchWriter = null;
      }
    } catch (MutationsRejectedException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
