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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.apache.gora.util.GoraException;
import org.apache.gora.util.IOUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
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

/**
 * Implementation of a Solr data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
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

  /** The solrj implementation to use. This has a default value of <i>http</i> for HttpSolrClient.
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

  private String SolrClientUrl, solrConfig, solrSchema, solrJServerImpl;

  private SolrClient server, adminServer;

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

  protected static final int DEFAULT_SOLR_CONCURRENT_CLIENT_QUEUE_SIZE = 1000;

  protected static final int DEFAULT_SOLR_CONCURRENT_CLIENT_THREAD_COUNT = 10;

  protected static final String SOLR_CONCURRENT_CLIENT_QUEUE_SIZE_PROPERTY
          = "solr.concurrentclient.queue_size";

  protected static final String SOLR_CONCURRENT_CLIENT_THREAD_COUNT_PROPERTY
          = "solr.concurrentclient.thread_count";

  private int queueSize = DEFAULT_SOLR_CONCURRENT_CLIENT_QUEUE_SIZE;

  private int threadCount = DEFAULT_SOLR_CONCURRENT_CLIENT_THREAD_COUNT;


  /**
   * Initialize the data store by reading the credentials, setting the client's properties up and
   * reading the mapping file. Initialize is called when then the call to
   * {@link org.apache.gora.store.DataStoreFactory#createDataStore} is made.
   *
   * @param keyClass the {@link Class} being used to map an entry to object value
   * @param persistentClass the {@link Class} of the object value being persisted
   * @param properties datastore initiailization and runtime properties
   * @throws GoraException if there is an error during initialization
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass,
      Properties properties) throws GoraException {
    super.initialize(keyClass, persistentClass, properties);
    try {
      String mappingFile = DataStoreFactory.getMappingFile(properties, this,
          DEFAULT_MAPPING_FILE);
      mapping = readMapping(mappingFile);
    } catch (IOException e) {
      throw new GoraException(e);
    }

    String queueSizeString = DataStoreFactory.findProperty(properties, this,
            SOLR_CONCURRENT_CLIENT_QUEUE_SIZE_PROPERTY, null);
    if (queueSizeString != null) {
      try {
        queueSize = Integer.parseInt(queueSizeString);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid concurrent client queue size '{}' , using default {}", queueSizeString,
                DEFAULT_SOLR_CONCURRENT_CLIENT_QUEUE_SIZE);
      }
    }

    String threadCountString = DataStoreFactory.findProperty(properties, this,
            SOLR_CONCURRENT_CLIENT_THREAD_COUNT_PROPERTY, null);
    if (threadCountString != null) {
      try {
        threadCount = Integer.parseInt(threadCountString);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid concurrent client thread count '{}' , using default {}", threadCountString,
                DEFAULT_SOLR_CONCURRENT_CLIENT_THREAD_COUNT);
      }
    }

    SolrClientUrl = DataStoreFactory.findProperty(properties, this,
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
    LOG.info("Using Solr server at " + SolrClientUrl);
    String solrJServerType = ((solrJServerImpl == null || solrJServerImpl.equals("")) ? "http" : solrJServerImpl);
    // HttpSolrClient - denoted by "http" in properties
    if (solrJServerType.toLowerCase(Locale.getDefault()).equals("http")) {
      LOG.info("Using HttpSolrClient Solrj implementation.");
      if (serverUserAuth) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("httpBasicAuthUser", serverUsername);
        params.set("httpBasicAuthPassword", serverPassword);
        this.adminServer = new HttpSolrClient.Builder(SolrClientUrl)
                .withHttpClient(HttpClientUtil.createClient(params))
                .build();
        this.server = new HttpSolrClient.Builder(SolrClientUrl + "/" + mapping.getCoreName())
                .withHttpClient(HttpClientUtil.createClient(params))
                .build();
      } else {
        this.adminServer = new HttpSolrClient.Builder(SolrClientUrl)
                .build();
        this.server = new HttpSolrClient.Builder(SolrClientUrl + "/" + mapping.getCoreName())
                .build();
      }
      // CloudSolrClient - denoted by "cloud" in properties
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("cloud")) {
      LOG.info("Using CloudSolrClient Solrj implementation.");
      if (serverUserAuth) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("httpBasicAuthUser", serverUsername);
        params.set("httpBasicAuthPassword", serverPassword);
        List<String> adminSolrUrls = new ArrayList();
        adminSolrUrls.add(SolrClientUrl);
        this.adminServer = new CloudSolrClient.Builder(adminSolrUrls)
                .withHttpClient(HttpClientUtil.createClient(params))
                .build();
        List<String> serverSolrUrls = new ArrayList();
        serverSolrUrls.add(SolrClientUrl + "/" + mapping.getCoreName());
        this.server = new CloudSolrClient.Builder(serverSolrUrls)
                .withHttpClient(HttpClientUtil.createClient(params))
                .build();
      } else {
        List<String> adminSolrUrls = new ArrayList();
        adminSolrUrls.add(SolrClientUrl);
        this.adminServer = new CloudSolrClient.Builder(adminSolrUrls)
                .build();
        List<String> serverSolrUrls = new ArrayList();
        serverSolrUrls.add(SolrClientUrl + "/" + mapping.getCoreName());
        this.server = new CloudSolrClient.Builder(serverSolrUrls)
                .build();
      }
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("concurrent")) {
      LOG.info("Using ConcurrentUpdateSolrClient Solrj implementation.");
      // LBHttpSolrClient - denoted by "loadbalance" in properties
      if (serverUserAuth) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("httpBasicAuthUser", serverUsername);
        params.set("httpBasicAuthPassword", serverPassword);
        this.adminServer = new ConcurrentUpdateSolrClient.Builder(SolrClientUrl)
                .withHttpClient(HttpClientUtil.createClient(params))
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
        this.server = new ConcurrentUpdateSolrClient.Builder(SolrClientUrl + "/" + mapping.getCoreName())
                .withHttpClient(HttpClientUtil.createClient(params))
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
      } else {
        this.adminServer = new ConcurrentUpdateSolrClient.Builder(SolrClientUrl)
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
        this.server = new ConcurrentUpdateSolrClient.Builder(SolrClientUrl + "/" + mapping.getCoreName())
                .withQueueSize(queueSize)
                .withThreadCount(threadCount)
                .build();
      }
    } else if (solrJServerType.toLowerCase(Locale.getDefault()).equals("loadbalance")) {
      LOG.info("Using LBHttpSolrClient Solrj implementation.");
      if (serverUserAuth) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("httpBasicAuthUser", serverUsername);
        params.set("httpBasicAuthPassword", serverPassword);
        String[] solrUrlElements = StringUtils.split(SolrClientUrl);
        this.adminServer = new LBHttpSolrClient.Builder()
                .withBaseSolrUrls(solrUrlElements)
                .withHttpClient(HttpClientUtil.createClient(params))
                .build();
        if (solrUrlElements.length > 0) {
          for (int counter = 0; counter < solrUrlElements.length; counter++) {
            solrUrlElements[counter] = solrUrlElements[counter] + "/" + mapping.getCoreName();
          }
        }
        this.server = new LBHttpSolrClient.Builder()
                .withHttpClient(HttpClientUtil.createClient(params))
                .withBaseSolrUrls(solrUrlElements)
                .build();
      } else {
        String[] solrUrlElements = StringUtils.split(SolrClientUrl);
        this.adminServer = new LBHttpSolrClient.Builder()
                .withBaseSolrUrls(solrUrlElements)
                .build();
        if (solrUrlElements.length > 0) {
          for (int counter = 0; counter < solrUrlElements.length; counter++) {
            solrUrlElements[counter] = solrUrlElements[counter] + "/" + mapping.getCoreName();
          }
        }
        this.server = new LBHttpSolrClient.Builder()
                .withBaseSolrUrls(solrUrlElements)
                .build();
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
  public void createSchema() throws GoraException {
    try {
      if (!schemaExists())
        CoreAdminRequest.createCore(mapping.getCoreName(),
            mapping.getCoreName(), adminServer, solrConfig, solrSchema);
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  /** Default implementation deletes and recreates the schema*/
  public void truncateSchema() throws GoraException {
    try {
      server.deleteByQuery("*:*");
      server.commit();
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void deleteSchema() throws GoraException {
    // XXX should this be only in truncateSchema ???
    try {
      server.deleteByQuery("*:*");
      server.commit();

      CoreAdminRequest.unloadCore(mapping.getCoreName(), adminServer);
    } catch (Exception e) {
      if (e.getMessage().contains("No such core")) {
        return; // it's ok, the core is not there
      } else {
        throw new GoraException(e);
      }
    }
  }

  @Override
  public boolean schemaExists() throws GoraException {
    boolean exists = false;
    try {
      CoreAdminResponse rsp = CoreAdminRequest.getStatus(mapping.getCoreName(),
          adminServer);
      exists = rsp.getUptime(mapping.getCoreName()) != null;
    } catch (Exception e) {
      throw new GoraException(e);
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
  public boolean exists(K key) throws GoraException {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(CommonParams.QT, "/get");
    params.set(CommonParams.FL, " ");
    params.set("id", key.toString());
    try {
      QueryResponse rsp = server.query(params);
      Object o = rsp.getResponse().get("doc");
      return o != null;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public T get(K key, String[] fields) throws GoraException {
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
      throw new GoraException(e);
    }
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
  public void put(K key, T persistent) throws GoraException {
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
        throw new GoraException(e);
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
  public boolean delete(K key) throws GoraException {
    String keyField = mapping.getPrimaryKey();
    try {
      UpdateResponse rsp = server.deleteByQuery(keyField + ":"
          + escapeQueryKey(key.toString()));
      server.commit();
      LOG.info(rsp.toString());
      return true;
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    UpdateResponse rsp;
    try {
      /*
        In this If block we check whether, user needs to delete full document or some fields in the document. We can't delete fields in a document by using solr deleteByQuery method.
        therefore what we have done here is setting the particular fields values into null.
       */
      if (query.getFields() != null && query.getFields().length < mapping.mapping.size() && !(Arrays.asList(query.getFields()).contains(mapping.getPrimaryKey()))) {
        Result<K, T> result = query.execute();
        Map<String, String> partialUpdateNull = new HashMap<>();
        partialUpdateNull.put("set", null);
        while (result.next()) {
          SolrInputDocument inputDoc = new SolrInputDocument();
          inputDoc.setField(mapping.getPrimaryKey(), result.getKey());
          for (String field : query.getFields()) {
            inputDoc.setField(field, partialUpdateNull);
          }
          batch.add(inputDoc);
        }
        if (commitWithin == 0) {
          rsp = server.add(batch);
          server.commit(false, true, true);
          batch.clear();
          LOG.info(rsp.toString());
        } else {
          rsp = server.add(batch, commitWithin);
          batch.clear();
          LOG.info(rsp.toString());
        }
      } else {
        SolrQuery<K, T> solrQuery = (SolrQuery<K, T>) query;
        String q = solrQuery.toSolrQuery();
        rsp = server.deleteByQuery(q);
        server.commit();
        LOG.info(rsp.toString());
      }
    } catch (GoraException e) {
      throw e;
    } catch (Exception e) {
      throw new GoraException(e);
    }
    return 0;
  }

  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException{
    return new SolrResult<>(this, query, server, resultsSize);
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
  public void flush() throws GoraException {
    try {
      if (batch.size() > 0) {
        add(batch, commitWithin);
        batch.clear();
      }
    } catch (Exception e) {
      throw new GoraException(e);
    }
  }

  @Override
  public void close() {
    try {
      flush();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      // Ignore the exception. Just nothing more to do if does not got close
    }
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
