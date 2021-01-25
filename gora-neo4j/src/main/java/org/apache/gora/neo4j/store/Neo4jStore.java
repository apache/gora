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
package org.apache.gora.neo4j.store;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.neo4j.mapping.Neo4jMapping;
import org.apache.gora.neo4j.mapping.Neo4jMappingBuilder;
import org.apache.gora.neo4j.mapping.Property;
import org.apache.gora.neo4j.utils.CypherDDL;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.IOUtils;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Literal;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Neo4j data store to be used by gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class Neo4jStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {
  
  protected static final String PARSE_MAPPING_FILE_KEY = "gora.neo4j.mapping.file";
  protected static final String DEFAULT_MAPPING_FILE = "gora-neo4j-mapping.xml";
  protected static final String XML_MAPPING_DEFINITION = "gora.mapping";
  protected static final String XSD_VALIDATION = "gora.xsd_validation";
  
  public static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Neo4jMapping neo4jMapping;
  private Connection connection;
  
  private static final ConcurrentHashMap<Schema, SpecificDatumReader<?>> readerMap = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Schema, SpecificDatumWriter<?>> writerMap = new ConcurrentHashMap<>();

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file. Initialize is called when then
   * the call to {@link org.apache.gora.store.DataStoreFactory#createDataStore}
   * is made.
   *
   * @param keyClass Gora's key class
   * @param persistentClass Persistent class
   * @param properties Configurations for the data store
   * @throws org.apache.gora.util.GoraException Unexpected exception during
   * initialization
   */
  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
    try {
      super.initialize(keyClass, persistentClass, properties);
      
      InputStream mappingStream;
      if (properties.containsKey(XML_MAPPING_DEFINITION)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("{} = {}", XML_MAPPING_DEFINITION, properties.getProperty(XML_MAPPING_DEFINITION));
        }
        mappingStream = org.apache.commons.io.IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
      } else {
        mappingStream = getClass().getClassLoader().getResourceAsStream(getConf().get(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
      }
      
      Neo4jMappingBuilder mappingBuilder = new Neo4jMappingBuilder(this);
      neo4jMapping = mappingBuilder.readMapping(mappingStream, Boolean.valueOf(properties.getProperty(XSD_VALIDATION, "false")));
      Neo4jParameters load = Neo4jParameters.load(properties, getConf());
      connection = connectToServer(load);
      LOG.info("Neo4j store was successfully initialized");
      if (autoCreateSchema && !schemaExists()) {
        createSchema();
      }
    } catch (IOException | ClassNotFoundException | SQLException ex) {
      throw new GoraException(ex);
    }
  }
  
  private Connection connectToServer(Neo4jParameters params) throws ClassNotFoundException, GoraException, SQLException {
    Class.forName(Neo4jConstants.DRIVER_NAME);
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append("jdbc:neo4j:");
    switch (params.getProtocol()) {
      case "Bolt":
        urlBuilder.append("bolt");
        break;
      case "Bolt+Routing":
        urlBuilder.append("bolt+routing");
        break;
      case "HTTP":
        urlBuilder.append("http");
        break;
      default:
        throw new GoraException("Incorrect protocol");
    }
    urlBuilder.append("://");
    urlBuilder.append(params.getHost());
    if (params.getPort() != null) {
      urlBuilder.append(":").append(params.getPort());
    }
    return DriverManager.getConnection(urlBuilder.toString(), params.getUsername(), params.getPassword());
  }
  
  @Override
  public String getSchemaName() {
    return this.neo4jMapping.getLabel();
  }
  
  @Override
  public void createSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
              "Impossible to create the schema as no connection has been initiated.");
    }
    if (schemaExists()) {
      return;
    }
    List<String> createCQLs = Lists.newArrayList();
    createCQLs.add(CypherDDL.createNodeKeyConstraint(this.neo4jMapping.getLabel(), this.neo4jMapping.getNodeKey().getName()));
    for (Map.Entry<String, Property> it : this.neo4jMapping.getProperties().entrySet()) {
      if (it.getValue().isExists()) {
        createCQLs.add(CypherDDL.createExistsConstraint(this.neo4jMapping.getLabel(), it.getValue().getName()));
      }
    }
    for (String create : createCQLs) {
      try (PreparedStatement stmt = this.connection.prepareStatement(create)) {
        stmt.execute();
      } catch (SQLException ex) {
        throw new GoraException(ex);
      }
    }
  }
  
  @Override
  public void deleteSchema() throws GoraException {
    if (connection == null) {
      throw new GoraException(
              "Impossible to delete the schema as no connection has been initiated.");
    }
    if (!schemaExists()) {
      return;
    }
    List<String> createCQLs = Lists.newArrayList();
    createCQLs.add(CypherDDL.dropNodeKeyConstraint(this.neo4jMapping.getLabel(), this.neo4jMapping.getNodeKey().getName()));
    for (Map.Entry<String, Property> it : this.neo4jMapping.getProperties().entrySet()) {
      if (it.getValue().isExists()) {
        createCQLs.add(CypherDDL.dropExistsConstraint(this.neo4jMapping.getLabel(), it.getValue().getName()));
      }
    }
    for (String delete : createCQLs) {
      try (PreparedStatement stmt = this.connection.prepareStatement(delete)) {
        stmt.execute();
      } catch (SQLException ex) {
        throw new GoraException(ex);
      }
    }
  }
  
  @Override
  public boolean schemaExists() throws GoraException {
    boolean exits = false;
    Statement build = Cypher.call("db.constraints").build();
    Renderer defaultRenderer = Renderer.getDefaultRenderer();
    String render = defaultRenderer.render(build);
    try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          if (rs.getString("name").equals(CypherDDL.createNodeKeyConstraintName(this.neo4jMapping.getLabel(), this.neo4jMapping.getNodeKey().getName()))) {
            exits = true;
          }
        }
      }
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return exits;
  }
  
  @Override
  public boolean exists(K key) throws GoraException {
    boolean found = false;
    Node named = Cypher.node(this.neo4jMapping.getLabel()).withProperties(this.neo4jMapping.getNodeKey().getName(), Cypher.literalOf(key)).named("r");
    Literal<Boolean> literalTrue = Cypher.literalTrue();
    Statement build = Cypher.match(named).returning(literalTrue).build();
    Renderer defaultRenderer = Renderer.getDefaultRenderer();
    String render = defaultRenderer.render(build);
    try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          found = true;
        }
      }
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return found;
  }
  
  @Override
  public T get(K key, String[] fields) throws GoraException {
    String[] avFields = getFieldsToQuery(fields);
    List<String> dbFields = Lists.newArrayList();
    for (String af : avFields) {
      dbFields.add(this.neo4jMapping.getProperties().get(af).getName());
    }
    try {
      T resp = null;
      
      Node node = Cypher.node(this.neo4jMapping.getLabel()).named("ds");
      node = node.withProperties(this.neo4jMapping.getNodeKey().getName(), Cypher.literalOf(key));
      List<org.neo4j.cypherdsl.core.Property> returnProperties = Lists.newArrayList();
      for (String props : dbFields) {
        returnProperties.add(node.property(props));
      }
      org.neo4j.cypherdsl.core.Property[] propertiesArray = returnProperties.toArray(new org.neo4j.cypherdsl.core.Property[0]);
      Statement build = Cypher.match(node).returning(propertiesArray).build();
      Renderer defaultRenderer = Renderer.getDefaultRenderer();
      String render = defaultRenderer.render(build);
      try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
        ResultSet rs = stmt.executeQuery();
        boolean data = rs.next();
        if (data) {
          resp = newInstance(rs, fields);
          if (rs.next()) {
            LOG.warn("Multiple results for primary key {} in the schema {}, ignoring additional rows.", key, this.neo4jMapping.getLabel());
          }
        }
        rs.close();
      }
      return resp;
    } catch (Exception ex) {
      throw new GoraException(ex);
    }
  }
  
  public T newInstance(ResultSet resultset, String[] fields) throws GoraException, SQLException, IOException {
    fields = getFieldsToQuery(fields);
    T persistent = newPersistent();
    for (String f : fields) {
      Schema.Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();
      String dbField = "ds." + this.neo4jMapping.getProperties().get(f).getName();
      Object fieldValue = resultset.getObject(dbField);
      if (fieldValue == null) {
        continue;
      }
      Object v = deserializeFieldValue(field, fieldSchema, fieldValue, persistent);
      persistent.put(field.pos(), v);
      persistent.setDirty(field.pos());
    }
    return persistent;
  }
  
  @Override
  public void put(K key, T obj) throws GoraException {
    try {
      if (obj.isDirty()) {
        // Create a new node
        Node node = Cypher.node(this.neo4jMapping.getLabel());
        //Add Node Key property
        List<Object> props = Lists.newArrayList();
        props.add(this.neo4jMapping.getNodeKey().getName());
        props.add(Cypher.literalOf(key));
        //Add data properties
        Schema schemaObj = obj.getSchema();
        List<Schema.Field> fields = schemaObj.getFields();
        for (Schema.Field field : fields) {
          Schema schema1 = field.schema();
          Object fieldValue = obj.get(field.pos());
          Property get = this.neo4jMapping.getProperties().get(field.name());
          Object serializeFieldValue = serializeFieldValue(schema1, fieldValue);
          if (serializeFieldValue == null) {
            continue;
          }
          props.add(get.getName());
          props.add(Cypher.literalOf(serializeFieldValue));
        }
        node = node.withProperties(props.toArray());
        Statement build = Cypher.merge(node).build();
        Renderer defaultRenderer = Renderer.getDefaultRenderer();
        String render = defaultRenderer.render(build);
        try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
          stmt.execute();
        } catch (SQLException ex) {
          throw new GoraException(ex);
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
    boolean response = false;
    Node named = Cypher.node(this.neo4jMapping.getLabel()).withProperties(this.neo4jMapping.getNodeKey().getName(), Cypher.literalOf(key)).named("r");
    Statement build = Cypher.match(named).delete(named).build();
    Renderer defaultRenderer = Renderer.getDefaultRenderer();
    String render = defaultRenderer.render(build);
    try (PreparedStatement stmt = this.connection.prepareStatement(render)) {
      response = stmt.execute();
    } catch (SQLException ex) {
      throw new GoraException(ex);
    }
    return response;
  }
  
  @Override
  public long deleteByQuery(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public Result<K, T> execute(Query<K, T> query) throws GoraException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public Query<K, T> newQuery() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public void flush() throws GoraException {
  }
  
  @Override
  public void close() {
    try {
      connection.close();
      LOG.info("Neo4j datastore destroyed successfully.");
    } catch (SQLException ex) {
      LOG.error(ex.getMessage(), ex);
    }
  }
  
  @SuppressWarnings("unchecked")
  private Object deserializeFieldValue(Schema.Field field, Schema fieldSchema,
          Object storeValue, T persistent) throws IOException {
    Object fieldValue = null;
    switch (fieldSchema.getType()) {
      case MAP:
      case ARRAY:
      case RECORD:
        @SuppressWarnings("rawtypes") SpecificDatumReader reader = getDatumReader(fieldSchema);
        fieldValue = IOUtils.deserialize((byte[]) ((String)storeValue).getBytes(), reader,
                persistent.get(field.pos()));
        break;
      case ENUM:
        fieldValue = AvroUtils.getEnumValue(fieldSchema, storeValue.toString());
        break;
      case FIXED:
        break;
      case BYTES:
        fieldValue = ByteBuffer.wrap((byte[]) ((String)storeValue).getBytes());
        break;
      case STRING:
        fieldValue = new Utf8(storeValue.toString());
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(storeValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          fieldValue = deserializeFieldValue(field, unionSchema, storeValue, persistent);
        } else {
          reader = getDatumReader(fieldSchema);
          fieldValue = IOUtils.deserialize((byte[]) ((String)storeValue).getBytes(), reader,
                  persistent.get(field.pos()));
        }
        break;
      case INT:
        fieldValue = Integer.valueOf(storeValue.toString());
        break;
      default:
        fieldValue = storeValue;
    }
    return fieldValue;
  }
  
  @SuppressWarnings("unchecked")
  private Object serializeFieldValue(Schema fieldSchema, Object fieldValue) {
    Object output = fieldValue;
    switch (fieldSchema.getType()) {
      case ARRAY:
      case MAP:
      case RECORD:
        byte[] data = null;
        try {
          @SuppressWarnings("rawtypes")
          SpecificDatumWriter writer = getDatumWriter(fieldSchema);
          data = IOUtils.serialize(writer, fieldValue);
        } catch (IOException e) {
          LOG.error(e.getMessage(), e);
        }
        output = new String(data, Charset.defaultCharset());
        break;
      case UNION:
        if (fieldSchema.getTypes().size() == 2 && isNullable(fieldSchema)) {
          int schemaPos = getUnionSchema(fieldValue, fieldSchema);
          Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
          output = serializeFieldValue(unionSchema, fieldValue);
        } else {
          data = null;
          try {
            @SuppressWarnings("rawtypes")
            SpecificDatumWriter writer = getDatumWriter(fieldSchema);
            data = IOUtils.serialize(writer, fieldValue);
          } catch (IOException e) {
            LOG.error(e.getMessage(), e);
          }
          output = new String(data, Charset.defaultCharset());
        }
        break;
      case FIXED:
        break;
      case ENUM:
      case STRING:
        output = fieldValue.toString();
        break;
      case BYTES:
        output = ((ByteBuffer) fieldValue).array();
        break;
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
      case NULL:
        break;
      default:
        throw new AssertionError(fieldSchema.getType().name());
    }
    return output;
  }
  
  @SuppressWarnings("rawtypes")
  private SpecificDatumReader getDatumReader(Schema fieldSchema) {
    SpecificDatumReader<?> reader = readerMap.get(fieldSchema);
    if (reader == null) {
      reader = new SpecificDatumReader(fieldSchema);// ignore dirty bits
      SpecificDatumReader localReader;
      if ((localReader = readerMap.putIfAbsent(fieldSchema, reader)) != null) {
        reader = localReader;
      }
    }
    return reader;
  }
  
  @SuppressWarnings("rawtypes")
  private SpecificDatumWriter getDatumWriter(Schema fieldSchema) {
    SpecificDatumWriter writer = writerMap.computeIfAbsent(fieldSchema, (t) -> {
      return new SpecificDatumWriter(t);// ignore dirty bits
    });
    return writer;
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
   * Method to retrieve the corresponding schema type index of a particular
   * object having UNION schema. As UNION type can have one or more types and at
   * a given instance, it holds an object of only one type of the defined types,
   * this method is used to figure out the corresponding instance's schema type
   * index.
   *
   * @param instanceValue value that the object holds
   * @param unionSchema union schema containing all of the data types
   * @return the unionSchemaPosition corresponding schema position
   */
  private int getUnionSchema(Object instanceValue, Schema unionSchema) {
    int unionSchemaPos = 0;
    for (Schema currentSchema : unionSchema.getTypes()) {
      Schema.Type schemaType = currentSchema.getType();
      if (instanceValue instanceof CharSequence && schemaType.equals(Schema.Type.STRING)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof ByteBuffer && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.BYTES)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Integer && schemaType.equals(Schema.Type.INT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Long && schemaType.equals(Schema.Type.LONG)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Double && schemaType.equals(Schema.Type.DOUBLE)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Float && schemaType.equals(Schema.Type.FLOAT)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Boolean && schemaType.equals(Schema.Type.BOOLEAN)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Map && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof List && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof Persistent && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.MAP)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.RECORD)) {
        return unionSchemaPos;
      }
      if (instanceValue instanceof byte[] && schemaType.equals(Schema.Type.ARRAY)) {
        return unionSchemaPos;
      }
      unionSchemaPos++;
    }
    return 0;
  }
  
}
