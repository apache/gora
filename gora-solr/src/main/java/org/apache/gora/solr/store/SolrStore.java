/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.gora.solr.store;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.solr.query.SolrQuery;
import org.apache.gora.solr.query.SolrResult;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.*;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  private static final Logger LOG = LoggerFactory.getLogger(SolrStore.class);

  /** The default file name value to be used for obtaining the Solr object field mapping's */
  protected static final String DEFAULT_MAPPING_FILE = "gora-solr-mapping.xml";

  /** The URL of the Solr server - defined in <code>gora.properties</code> */
  protected static final String SOLR_URL_PROPERTY = "solr.url";

  /** The <code>solrconfig.xml</code> file to be used - defined in <code>gora.properties</code>*/
  protected static final String SOLR_CONFIG_PROPERTY = "solr.config";

  /** The <code>schema.xml</code> file to be used - defined in <code>gora.properties</code>*/
  protected static final String SOLR_SCHEMA_PROPERTY = "solr.schema";

  /** A batch size unit (ArrayList) of SolrDocument's to be used for writing to Solr.
   * Should be defined in <code>gora.properties</code>. 
   * A default value of 100 is used if this value is absent. This value must be of type <b>Integer</b>.
   */
  protected static final String SOLR_BATCH_SIZE_PROPERTY = "solr.batch_size";

  /** The solrj implementation to use. This has a default value of <i>http</i> for HttpSolrServer.
   * Available options include <b>http</b>, <b>cloud</b>, <b>concurrent</b> and <b>loadbalance</b>. 
   * Defined in <code>gora.properties</code>
   * This value must be of type <b>String</b>.
   */
  protected static final String SOLR_SOLRJSERVER_IMPL = "solr.solrjserver";

  /** Whether to use secured Solr client or not.
   * Available options include <b>true</b>, and <b>false</b>.
   * Defined in <code>gora.properties</code>
   * This value must be of type <b>boolean</b>.
   */
  protected static final String SOLR_SERVER_USER_AUTH = "solr.solrjserver.user_auth";

  /** Solr client username.
   * Solr client user authentication should be enabled for this property.
   * Defined in <code>gora.properties</code>
   * This value must be of type <b>String</b>.
   */
  protected static final String SOLR_SERVER_USERNAME = "solr.solrjserver.username";

  /** Solr client password.
   * Solr client user authentication should be enabled for this property.
   * Defined in <code>gora.properties</code>
   * This value must be of type <b>String</b>.
   */
  protected static final String SOLR_SERVER_PASSWORD = "solr.solrjserver.password";

  /** A batch commit unit for SolrDocument's used when making (commit) calls to Solr.
   * Should be defined in <code>gora.properties</code>. 
   * A default value of 1000 is used if this value is absent. This value must be of type <b>Integer</b>.
   */
  protected static final String SOLR_COMMIT_WITHIN_PROPERTY = "solr.commit_within";

  /** The maximum number of result to return when we make a call to 
   * {@link org.apache.gora.solr.store.SolrStore#execute(Query)}. This should be 
   * defined in <code>gora.properties</code>. This value must be of type <b>Integer</b>.
   */
  protected static final String SOLR_RESULTS_SIZE_PROPERTY = "solr.results_size";

  /** The default batch size (ArrayList) of SolrDocuments to be used in the event of an absent 
   * value for <code>solr.batchSize</code>. 
   * Set to 100 by default.
   */
  protected static final int DEFAULT_BATCH_SIZE = 100;

  /** The default commit size of SolrDocuments to be used in the event of an absent 
   * value for <code>solr.commitSize</code>. 
   * Set to 1000 by default.
   */
  protected static final int DEFAULT_COMMIT_WITHIN = 1000;

  /** The default results size of SolrDocuments to be used in the event of an absent 
   * value for <code>solr.resultsSize</code>. 
   * Set to 100 by default.
   */
  protected static final int DEFAULT_RESULTS_SIZE = 100;

  private SolrMapping mapping;

  private String solrServerUrl, solrConfig, solrSchema, solrJServerImpl;

  private SolrServer server, adminServer;

  private boolean serverUserAuth;

  private String serverUsername;

  private String serverPassword;

  private ArrayList<SolrInputDocument> batch;

  private int batchSize = DEFAULT_BATCH_SIZE;

  private int commitWithin = DEFAULT_COMMIT_WITHIN;

  private int resultsSize = DEFAULT_RESULTS_SIZE;

  /**
   * Default schema index with value "0" used when AVRO Union data types are
   * stored
   */
  public static final int DEFAULT_UNION_SCHEMA = 0;

  /*
   * Create a threadlocal map for the datum readers and writers, because they
   * are not thread safe, at least not before Avro 1.4.0 (See AVRO-650). When
   * they are thread safe, it is possible to maintain a single reader and writer
   * pair for every schema, instead of one for every thread.
   */

  public static final ConcurrentHashMap<Schema, SpecificDatumReader<?>> readerMap = new ConcurrentHashMap<>();

  public static final ConcurrentHashMap<Schema, SpecificDatumWriter<?>> writerMap = new ConcurrentHashMap<>();

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) {
    super.initialize(keyClass, persistentClass, properties);
    try {
      String mappingFile = DataStoreFactory.getMappingFile(properties, this,
          DEFAULT_MAPPING_FILE);
      mapping = readMapping(mappingFile);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }

    solrServerUrl = DataStoreFactory.findProperty(properties, this,
        SOLR_URL_PROPERTY, null);
    solrConfig = DataStoreFactory.findProperty(properties, this,
        SOLR_CONFIG_PROPERTY, null);
    solrSchema = DataStoreFactory.findProperty(properties, this,
        SOLR_SCHEMA_PROPERTY, null);
    solrJServerImpl = DataStoreFactory.findProperty(properties, this, 
        SOLR_SOLRJSERVER_IMPL, "http");
    serverUserAuth = DataStoreFactory.findBooleanProperty(properties, this,
        SOLR_SERVER_USER_AUTH, "false");
    if (serverUserAuth) {
      serverUsername = DataStoreFactory.findProperty(properties, this,
          SOLR_SERVER_USERNAME, null);
      serverPassword = DataStoreFactory.findProperty(properties, this,
          SOLR_SERVER_PASSWORD, null);
    }
    LOG.info("Using Solr server at " + solrServerUrl);
    String solrJServerType = ((solrJServerImpl == null || solrJServerImpl.equals(""))?"http":solrJServerImpl);
    // HttpSolrServer - denoted by "http" in properties
    if (solrJServerType.toLowerCase(Locale.getDefault()).equals("http")) {
      LOG.info("Using HttpSolrServer Solrj implementation.");
      this.adminServer = new HttpSolrServer(solrServerUrl);
      this.server = new HttpSolrServer( solrServerUrl + "/" + mapping.getCoreName() );
      if (serverUserAuth) {
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((HttpSolrServer) adminServer).getHttpClient(),
            serverUsername, serverPassword);
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((HttpSolrServer) server).getHttpClient(),
            serverUsername, serverPassword);
      }
      // CloudSolrServer - denoted by "cloud" in properties
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("cloud")) {
      LOG.info("Using CloudSolrServer Solrj implementation.");
      this.adminServer = new CloudSolrServer(solrServerUrl);
      this.server = new CloudSolrServer( solrServerUrl + "/" + mapping.getCoreName() );
      if (serverUserAuth) {
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((CloudSolrServer) adminServer).getLbServer().getHttpClient(),
            serverUsername, serverPassword);
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((CloudSolrServer) server).getLbServer().getHttpClient(),
            serverUsername, serverPassword);
      }
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("concurrent")) {
      LOG.info("Using ConcurrentUpdateSolrServer Solrj implementation.");
      this.adminServer = new ConcurrentUpdateSolrServer(solrServerUrl, 1000, 10);
      this.server = new ConcurrentUpdateSolrServer( solrServerUrl + "/" + mapping.getCoreName(), 1000, 10);
      // LBHttpSolrServer - denoted by "loadbalance" in properties
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("loadbalance")) {
      LOG.info("Using LBHttpSolrServer Solrj implementation.");
      String[] solrUrlElements = StringUtils.split(solrServerUrl);
      try {
        this.adminServer = new LBHttpSolrServer(solrUrlElements);
      } catch (MalformedURLException e) {
        LOG.error(e.getMessage());
        throw new RuntimeException(e);
      }
      try {
        this.server = new LBHttpSolrServer( solrUrlElements + "/" + mapping.getCoreName() );
      } catch (MalformedURLException e) {
        LOG.error(e.getMessage());
        throw new RuntimeException(e);
      }
      if (serverUserAuth) {
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((LBHttpSolrServer) adminServer).getHttpClient(),
            serverUsername, serverPassword);
        HttpClientUtil.setBasicAuth(
            (DefaultHttpClient) ((LBHttpSolrServer) server).getHttpClient(),
            serverUsername, serverPassword);
      }
    }
    if (autoCreateSchema) {
      createSchema();
    }
    String batchSizeString = DataStoreFactory.findProperty(properties, this,
        SOLR_BATCH_SIZE_PROPERTY, null);
    if (batchSizeString != null) {
      try {
        batchSize = Integer.parseInt(batchSizeString);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid batch size '{}', using default {}", batchSizeString, DEFAULT_BATCH_SIZE);
      }
    }
    batch = new ArrayList<>(batchSize);
    String commitWithinString = DataStoreFactory.findProperty(properties, this,
        SOLR_COMMIT_WITHIN_PROPERTY, null);
    if (commitWithinString != null) {
      try {
        commitWithin = Integer.parseInt(commitWithinString);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid commit within '{}' , using default {}", commitWithinString, DEFAULT_COMMIT_WITHIN);
      }
    }
    String resultsSizeString = DataStoreFactory.findProperty(properties, this,
        SOLR_RESULTS_SIZE_PROPERTY, null);
    if (resultsSizeString != null) {
      try {
        resultsSize = Integer.parseInt(resultsSizeString);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid results size '{}' , using default {}", resultsSizeString, DEFAULT_RESULTS_SIZE);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private SolrMapping readMapping(String filename) throws IOException {
    SolrMapping map = new SolrMapping();
    try {
      SAXBuilder builder = new SAXBuilder();
      Document doc = builder.build(getClass().getClassLoader()
          .getResourceAsStream(filename));

      List<Element> classes = doc.getRootElement().getChildren("class");

      for (Element classElement : classes) {
        if (classElement.getAttributeValue("keyClass").equals(
            keyClass.getCanonicalName())
            && classElement.getAttributeValue("name").equals(
                persistentClass.getCanonicalName())) {

          String tableName = getSchemaName(
              classElement.getAttributeValue("table"), persistentClass);
          map.setCoreName(tableName);

          Element primaryKeyEl = classElement.getChild("primarykey");
          map.setPrimaryKey(primaryKeyEl.getAttributeValue("column"));

          List<Element> fields = classElement.getChildren("field");

          for (Element field : fields) {
            String fieldName = field.getAttributeValue("name");
            String columnName = field.getAttributeValue("column");
            map.addField(fieldName, columnName);
          }
          break;
        }
        LOG.warn("Check that 'keyClass' and 'name' parameters in gora-solr-mapping.xml "
            + "match with intended values. A mapping mismatch has been found therefore "
            + "no mapping has been initialized for class mapping at position " 
            + " {} in mapping file.", classes.indexOf(classElement));
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    return map;
  }

  public SolrMapping getMapping() {
    return mapping;
  }

  @Override
  public String getSchemaName() {
    return mapping.getCoreName();
  }

  @Override
  public void createSchema() {
    try {
      if (!schemaExists())
        CoreAdminRequest.createCore(mapping.getCoreName(),
            mapping.getCoreName(), adminServer, solrConfig, solrSchema);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  /** Default implementation deletes and recreates the schema*/
  public void truncateSchema() {
    try {
      server.deleteByQuery("*:*");
      server.commit();
    } catch (Exception e) {
      // ignore?
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void deleteSchema() {
    // XXX should this be only in truncateSchema ???
    try {
      server.deleteByQuery("*:*");
      server.commit();
    } catch (Exception e) {
      // ignore?
      // LOG.error(e.getMessage(), e);
    }
    try {
      CoreAdminRequest.unloadCore(mapping.getCoreName(), adminServer);
    } catch (Exception e) {
      if (e.getMessage().contains("No such core")) {
        return; // it's ok, the core is not there
      } else {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean schemaExists() {
    boolean exists = false;
    try {
      CoreAdminResponse rsp = CoreAdminRequest.getStatus(mapping.getCoreName(),
          adminServer);
      exists = rsp.getUptime(mapping.getCoreName()) != null;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return exists;
  }

  private static final String toDelimitedString(String[] arr, String sep) {
    if (arr == null || arr.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < arr.length; i++) {
      if (i > 0)
        sb.append(sep);
      sb.append(arr[i]);
    }
    return sb.toString();
  }

  public static String escapeQueryKey(String key) {
    if (key == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      switch (c) {
      case ':':
      case '*':
        sb.append("\\").append(c);
        break;
      default:
        sb.append(c);
      }
    }
    return sb.toString();
  }

  @Override
  public T get(K key, String[] fields) {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(CommonParams.QT, "/get");
    params.set(CommonParams.FL, toDelimitedString(fields, ","));
    params.set("id", key.toString());
    try {
      QueryResponse rsp = server.query(params);
      Object o = rsp.getResponse().get("doc");
      if (o == null) {
        return null;
      }
      return newInstance((SolrDocument) o, fields);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  public T newInstance(SolrDocument doc, String[] fields) throws IOException {
    T persistent = newPersistent();
    if (fields == null) {
      fields = fieldMap.keySet().toArray(new String[fieldMap.size()]);
    }
    String pk = mapping.getPrimaryKey();
    for (String f : fields) {
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      String sf = null;
      if (pk.equals(f)) {
        sf = f;
      } else {
        sf = mapping.getSolrField(f);
      }
      Object sv = doc.get(sf);
      if (sv == null) {
        continue;
      }

      Object v = deserializeFieldValue(field, fieldSchema, sv, persistent);
      persistent.put(field.pos(), v);
      persistent.setDirty(field.pos());

    }
    persistent.clearDirty();
    return persistent;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumReader getDatumReader(Schema fieldSchema) {
    SpecificDatumReader<?> reader = readerMap.get(fieldSchema);
    if (reader == null) {
      reader = new SpecificDatumReader(fieldSchema);// ignore dirty bits
      SpecificDatumReader localReader = null;
      if ((localReader = readerMap.putIfAbsent(fieldSchema, reader)) != null) {
        reader = localReader;
      }
    }
    return reader;
  }

  @SuppressWarnings("rawtypes")
  private SpecificDatumWriter getDatumWriter(Schema fieldSchema) {
    SpecificDatumWriter writer = writerMap.get(fieldSchema);
    if (writer == null) {
      writer = new SpecificDatumWriter(fieldSchema);// ignore dirty bits
      writerMap.put(fieldSchema, writer);
    }

    return writer;
  }

  @SuppressWarnings("unchecked")
  private Object deserializeFieldValue(Field field, Schema fieldSchema,
      Object solrValue, T persistent) throws IOException {
    Object fieldValue = null;
    switch (fieldSchema.getType()) {
    case MAP:
    case ARRAY:
    case RECORD:
      @SuppressWarnings("rawtypes")
      SpecificDatumReader reader = getDatumReader(fieldSchema);
      fieldValue = IOUtils.deserialize((byte[]) solrValue, reader,
          persistent.get(field.pos()));
      break;
    case ENUM:
      fieldValue = AvroUtils.getEnumValue(fieldSchema, (String) solrValue);
      break;
    case FIXED:
      throw new IOException("???");
      // break;
    case BYTES:
      fieldValue = ByteBuffer.wrap((byte[]) solrValue);
      break;
    case STRING:
      fieldValue = new Utf8(solrValue.toString());
      break;
    case UNION:
      if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
        // schema [type0, type1]
        Type type0 = fieldSchema.getTypes().get(0).getType();
        Type type1 = fieldSchema.getTypes().get(1).getType();

        // Check if types are different and there's a "null", like
        // ["null","type"] or ["type","null"]
        if (!type0.equals(type1)) {
          if (type0.equals(Schema.Type.NULL))
            fieldSchema = fieldSchema.getTypes().get(1);
          else
            fieldSchema = fieldSchema.getTypes().get(0);
        } else {
          fieldSchema = fieldSchema.getTypes().get(0);
        }
        fieldValue = deserializeFieldValue(field, fieldSchema, solrValue,
            persistent);
      } else {
        @SuppressWarnings("rawtypes")
        SpecificDatumReader unionReader = getDatumReader(fieldSchema);
        fieldValue = IOUtils.deserialize((byte[]) solrValue, unionReader,
            persistent.get(field.pos()));
        break;
      }
      break;
    default:
      fieldValue = solrValue;
    }
    return fieldValue;
  }

  @Override
  public void put(K key, T persistent) {
    Schema schema = persistent.getSchema();
    if (!persistent.isDirty()) {
      // nothing to do
      return;
    }
    SolrInputDocument doc = new SolrInputDocument();
    // add primary key
    doc.addField(mapping.getPrimaryKey(), key);
    // populate the doc
    List<Field> fields = schema.getFields();
    for (Field field : fields) {
      String sf = mapping.getSolrField(field.name());
      // Solr will append values to fields in a SolrInputDocument, even the key
      // mapping won't find the primary
      if (sf == null) {
        continue;
      }
      Schema fieldSchema = field.schema();
      Object v = persistent.get(field.pos());
      if (v == null) {
        continue;
      }
      v = serializeFieldValue(fieldSchema, v);
      doc.addField(sf, v);

    }
    LOG.info("Putting DOCUMENT: " + doc);
    batch.add(doc);
    if (batch.size() >= batchSize) {
      try {
        add(batch, commitWithin);
        batch.clear();
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Object serializeFieldValue(Schema fieldSchema, Object fieldValue) {
    switch (fieldSchema.getType()) {
    case MAP:
    case ARRAY:
    case RECORD:
      byte[] data = null;
      try {
        @SuppressWarnings("rawtypes")
        SpecificDatumWriter writer = getDatumWriter(fieldSchema);
        data = IOUtils.serialize(writer, fieldValue);
      } catch (IOException e) {
        LOG.error(e.getMessage(), e);
      }
      fieldValue = data;
      break;
    case BYTES:
      fieldValue = ((ByteBuffer) fieldValue).array();
      break;
    case ENUM:
    case STRING:
      fieldValue = fieldValue.toString();
      break;
    case UNION:
      // If field's schema is null and one type, we do undertake serialization.
      // All other types are serialized.
      if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
        int schemaPos = getUnionSchema(fieldValue, fieldSchema);
        Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
        fieldValue = serializeFieldValue(unionSchema, fieldValue);
      } else {
        byte[] serilazeData = null;
        try {
          @SuppressWarnings("rawtypes")
          SpecificDatumWriter writer = getDatumWriter(fieldSchema);
          serilazeData = IOUtils.serialize(writer, fieldValue);
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
        fieldValue = serilazeData;
      }
      break;
    default:
      // LOG.error("Unknown field type: " + fieldSchema.getType());
      break;
    }
    return fieldValue;
  }

  private boolean isNullable(Schema unionSchema) {
    for (Schema innerSchema : unionSchema.getTypes()) {
      if (innerSchema.getType().equals(Schema.Type.NULL)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given an object and the object schema this function obtains, from within
   * the UNION schema, the position of the type used. If no data type can be
   * inferred then we return a default value of position 0.
   * 
   * @param pValue
   * @param pUnionSchema
   * @return the unionSchemaPosition.
   */
  private int getUnionSchema(Object pValue, Schema pUnionSchema) {
    int unionSchemaPos = 0;
    for (Schema currentSchema : pUnionSchema.getTypes()) {
      Type schemaType = currentSchema.getType();
      if (pValue instanceof Utf8 && schemaType.equals(Type.STRING))
        return unionSchemaPos;
      else if (pValue instanceof ByteBuffer && schemaType.equals(Type.BYTES))
        return unionSchemaPos;
      else if (pValue instanceof Integer && schemaType.equals(Type.INT))
        return unionSchemaPos;
      else if (pValue instanceof Long && schemaType.equals(Type.LONG))
        return unionSchemaPos;
      else if (pValue instanceof Double && schemaType.equals(Type.DOUBLE))
        return unionSchemaPos;
      else if (pValue instanceof Float && schemaType.equals(Type.FLOAT))
        return unionSchemaPos;
      else if (pValue instanceof Boolean && schemaType.equals(Type.BOOLEAN))
        return unionSchemaPos;
      else if (pValue instanceof Map && schemaType.equals(Type.MAP))
        return unionSchemaPos;
      else if (pValue instanceof List && schemaType.equals(Type.ARRAY))
        return unionSchemaPos;
      else if (pValue instanceof Persistent && schemaType.equals(Type.RECORD))
        return unionSchemaPos;
      unionSchemaPos++;
    }
    // if we weren't able to determine which data type it is, then we return the
    // default
    return DEFAULT_UNION_SCHEMA;
  }

  @Override
  public boolean delete(K key) {
    String keyField = mapping.getPrimaryKey();
    try {
      UpdateResponse rsp = server.deleteByQuery(keyField + ":"
          + escapeQueryKey(key.toString()));
      server.commit();
      LOG.info(rsp.toString());
      return true;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return false;
  }

  @Override
  public long deleteByQuery(Query<K, T> query) {
    String q = ((SolrQuery<K, T>) query).toSolrQuery();
    try {
      UpdateResponse rsp = server.deleteByQuery(q);
      server.commit();
      LOG.info(rsp.toString());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) {
    try {
      return new SolrResult<>(this, query, server, resultsSize);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public Query<K, T> newQuery() {
    return new SolrQuery<>(this);
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query)
      throws IOException {
    // TODO: implement this using Hadoop DB support

    ArrayList<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> pqi = new PartitionQueryImpl<>(query);
    pqi.setConf(getConf());
    partitions.add(pqi);

    return partitions;
  }

  @Override
  public void flush() {
    try {
      if (batch.size() > 0) {
        add(batch, commitWithin);
        batch.clear();
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    flush();
  }

  private void add(ArrayList<SolrInputDocument> batch, int commitWithin)
      throws SolrServerException, IOException {
    if (commitWithin == 0) {
      server.add(batch);
      server.commit(false, true, true);
    } else {
      server.add(batch, commitWithin);
    }
  }
}
