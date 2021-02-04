/*
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
package org.apache.gora.elasticsearch.store;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMapping;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMappingBuilder;
import org.apache.gora.elasticsearch.mapping.Field;
import org.apache.gora.elasticsearch.utils.ElasticsearchParameters;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of a Apache Elasticsearch data store to be used by Apache Gora.
 *
 * @param <K> class to be used for the key
 * @param <T> class to be persisted within the store
 */
public class ElasticsearchStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

    public static final Logger LOG = LoggerFactory.getLogger(ElasticsearchStore.class);
    private static final String DEFAULT_MAPPING_FILE = "gora-elasticsearch-mapping.xml";
    public static final String PARSE_MAPPING_FILE_KEY = "gora.elasticsearch.mapping.file";
    private static final String XML_MAPPING_DEFINITION = "gora.mapping";
    public static final String XSD_VALIDATION = "gora.xsd_validation";

    /**
     * Elasticsearch client
     */
    private RestHighLevelClient client;

    /**
     * Mapping definition for Elasticsearch
     */
    private ElasticsearchMapping elasticsearchMapping;

    @Override
    public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) throws GoraException {
        try {
            LOG.debug("Initializing Elasticsearch store");
            ElasticsearchParameters parameters = ElasticsearchParameters.load(properties, getConf());
            super.initialize(keyClass, persistentClass, properties);
            ElasticsearchMappingBuilder<K, T> builder = new ElasticsearchMappingBuilder<>(this);
            InputStream mappingStream;
            if (properties.containsKey(XML_MAPPING_DEFINITION)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("{} = {}", XML_MAPPING_DEFINITION, properties.getProperty(XML_MAPPING_DEFINITION));
                }
                mappingStream = org.apache.commons.io.IOUtils.toInputStream(properties.getProperty(XML_MAPPING_DEFINITION), (Charset) null);
            } else {
                mappingStream = getClass().getClassLoader().getResourceAsStream(properties.getProperty(PARSE_MAPPING_FILE_KEY, DEFAULT_MAPPING_FILE));
            }
            String xsdValidation = properties.getProperty(XSD_VALIDATION, "false");
            builder.readMappingFile(mappingStream, Boolean.parseBoolean(xsdValidation));
            elasticsearchMapping = builder.getElasticsearchMapping();
            client = createClient(parameters);
            LOG.info("Elasticsearch store was successfully initialized.");
        } catch (Exception ex) {
            LOG.error("Error while initializing Elasticsearch store", ex);
            throw new GoraException(ex);
        }
    }

    private RestHighLevelClient createClient(ElasticsearchParameters parameters) {
        RestClientBuilder clientBuilder = RestClient.builder(new HttpHost(parameters.getHost(), parameters.getPort()));

        // Choosing the authentication method.
        switch (parameters.getAuthenticationMethod()) {
            case "BASIC":
                if (parameters.getUsername() != null && parameters.getPassword() != null) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(parameters.getUsername(), parameters.getPassword()));
                    clientBuilder.setHttpClientConfigCallback(
                            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
                } else {
                    throw new IllegalArgumentException("Missing username or password for BASIC authentication.");
                }
                break;
            case "TOKEN":
                if (parameters.getAuthorizationToken() != null) {
                    Header[] defaultHeaders = new Header[]{new BasicHeader("Authorization",
                            parameters.getAuthorizationToken())};
                    clientBuilder.setDefaultHeaders(defaultHeaders);
                } else {
                    throw new IllegalArgumentException("Missing authorization token for TOKEN authentication.");
                }
                break;
            case "APIKEY":
                if (parameters.getApiKeyId() != null && parameters.getApiKeySecret() != null) {
                    String apiKeyAuth = Base64.getEncoder()
                            .encodeToString((parameters.getApiKeyId() + ":" + parameters.getApiKeySecret())
                                    .getBytes(StandardCharsets.UTF_8));
                    Header[] defaultHeaders = new Header[]{new BasicHeader("Authorization", "ApiKey " + apiKeyAuth)};
                    clientBuilder.setDefaultHeaders(defaultHeaders);
                } else {
                    throw new IllegalArgumentException("Missing API Key ID or API Key Secret for APIKEY authentication.");
                }
                break;
        }

        if (parameters.getConnectTimeout() != 0) {
            clientBuilder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder.setConnectTimeout(parameters.getConnectTimeout()));
        }

        if (parameters.getSocketTimeout() != 0) {
            clientBuilder.setRequestConfigCallback(requestConfigBuilder ->
                    requestConfigBuilder.setSocketTimeout(parameters.getSocketTimeout()));
        }

        if (parameters.getIoThreadCount() != 0) {
            clientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom()
                            .setIoThreadCount(parameters.getIoThreadCount()).build()));
        }
        return new RestHighLevelClient(clientBuilder);
    }

    public ElasticsearchMapping getMapping() {
        return elasticsearchMapping;
    }

    @Override
    public String getSchemaName() {
        return elasticsearchMapping.getIndexName();
    }

    @Override
    public String getSchemaName(final String mappingSchemaName, final Class<?> persistentClass) {
        return super.getSchemaName(mappingSchemaName, persistentClass);
    }

    @Override
    public void createSchema() throws GoraException {
        CreateIndexRequest request = new CreateIndexRequest(elasticsearchMapping.getIndexName());
        Map<String, Object> properties = new HashMap<>();
        for (Map.Entry<String, Field> entry : elasticsearchMapping.getFields().entrySet()) {
            Map<String, Object> fieldType = new HashMap<>();
            fieldType.put("type", entry.getValue().getDataType().getType().name().toLowerCase(Locale.ROOT));
            properties.put(entry.getKey(), fieldType);
        }
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        request.mapping(mapping);
        try {
            if (!client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT)) {
                client.indices().create(request, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public void deleteSchema() throws GoraException {
        DeleteIndexRequest request = new DeleteIndexRequest(elasticsearchMapping.getIndexName());
        try {
            if (client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT)) {
                client.indices().delete(request, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public boolean schemaExists() throws GoraException {
        try {
            return client.indices().exists(
                    new GetIndexRequest(elasticsearchMapping.getIndexName()), RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public boolean exists(K key) throws GoraException {
        GetRequest getRequest = new GetRequest(elasticsearchMapping.getIndexName(), (String) key);
        getRequest.fetchSourceContext(new FetchSourceContext(false)).storedFields("_none_");
        try {
            return client.exists(getRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public T get(K key, String[] fields) throws GoraException {
        String[] requestedFields = getFieldsToQuery(fields);
        List<String> documentFields = new ArrayList<>();
        for (String requestedField : requestedFields) {
            documentFields.add(elasticsearchMapping.getFields().get(requestedField).getName());
        }
        try {
            // Prepare the Elasticsearch request
            GetRequest getRequest = new GetRequest(elasticsearchMapping.getIndexName(), (String) key);
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                Map<String, Object> sourceMap = getResponse.getSourceAsMap();

                // Map of field's name and its value from the Document
                Map<String, Object> fieldsAndValues = new HashMap<>();
                for (String field : documentFields) {
                    fieldsAndValues.put(field, sourceMap.get(field));
                }

                // Build the corresponding persistent
                return newInstance(fieldsAndValues, requestedFields);
            } else {
                return null;
            }
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public void put(K key, T obj) throws GoraException {
        if (obj.isDirty()) {
            Schema schemaObj = obj.getSchema();
            List<Schema.Field> fields = schemaObj.getFields();
            Map<String, Object> jsonMap = new HashMap<>();
            for (Schema.Field field : fields) {
                Field mappedField = elasticsearchMapping.getFields().get(field.name());
                if (mappedField != null) {
                    Object fieldValue = obj.get(field.pos());
                    if (fieldValue != null) {
                        Schema fieldSchema = field.schema();
                        Object serializedObj = serializeFieldValue(fieldSchema, fieldValue);
                        jsonMap.put(mappedField.getName(), serializedObj);
                    }
                }
            }
            // Prepare the Elasticsearch request
            IndexRequest request = new IndexRequest(elasticsearchMapping.getIndexName()).id((String) key).source(jsonMap);
            try {
                client.index(request, RequestOptions.DEFAULT);
            } catch (IOException ex) {
                throw new GoraException(ex);
            }
        } else {
            LOG.info("Ignored putting object {} in the store as it is neither "
                    + "new, neither dirty.", new Object[]{obj});
        }
    }

    @Override
    public boolean delete(K key) throws GoraException {
        DeleteRequest request = new DeleteRequest(elasticsearchMapping.getIndexName(), (String) key);
        try {
            DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
            return deleteResponse.getResult() != DocWriteResponse.Result.NOT_FOUND;
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public long deleteByQuery(Query<K, T> query) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) throws GoraException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Query<K, T> newQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void flush() throws GoraException {
        try {
            client.indices().flush(new FlushRequest(), RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
            LOG.info("Elasticsearch datastore destroyed successfully.");
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * Build a new instance of the persisted class from the Document retrieved from the database.
     *
     * @param fieldsAndValues Map of field's name and its value from the Document
     *                        that results from the query to the database
     * @param requestedFields the list of fields to be mapped to the persistence class instance
     * @return a persistence class instance which content was deserialized from the Document
     * @throws IOException
     */
    public T newInstance(Map<String, Object> fieldsAndValues, String[] requestedFields) throws IOException {
        // Create new empty persistent bean instance
        T persistent = newPersistent();

        requestedFields = getFieldsToQuery(requestedFields);
        // Populate each field
        for (String objField : requestedFields) {
            Schema.Field field = fieldMap.get(objField);
            Schema fieldSchema = field.schema();
            String docFieldName = elasticsearchMapping.getFields().get(objField).getName();
            Object fieldValue = fieldsAndValues.get(docFieldName);

            Object result = deserializeFieldValue(field, fieldSchema, fieldValue, persistent);
            persistent.put(field.pos(), result);
        }
        persistent.clearDirty();
        return persistent;
    }

    /**
     * Convert an Elasticsearch Object to a persistent Java Object.
     *
     * @param field field in Elasticsearch Document to be deserialized
     * @param fieldSchema field schema for Java class
     * @param elasticsearchValue field value of the given Elasticsearch data type
     * @param persistent given persistent Java Object
     * @return deserialized Java Object from Elasticsearch Object
     */
    private Object deserializeFieldValue(Schema.Field field, Schema fieldSchema,
                                         Object elasticsearchValue, T persistent) throws GoraException {
        Object fieldValue;
        switch (fieldSchema.getType()) {
            case MAP:
                fieldValue = fromElasticsearchMap(avroField, avroFieldSchema.getValueType(), (Map<String, Object>) elasticsearchValue);
                break;
            case RECORD:
                throw new UnsupportedOperationException();
            case ARRAY:
                fieldValue = fromElasticsearchList(field, fieldSchema, elasticsearchValue, persistent);
                break;
            case BOOLEAN:
                fieldValue = Boolean.parseBoolean(elasticsearchValue.toString());
                break;
            case BYTES:
                fieldValue = ByteBuffer.wrap(Base64.getDecoder().decode(elasticsearchValue.toString()));
                break;
            case FIXED:
            case NULL:
                fieldValue = null;
                break;
            case UNION:
                Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
                Schema.Type type1 = fieldSchema.getTypes().get(1).getType();
                if (fieldSchema.getTypes().size() == 2 &&
                        (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL)) &&
                        !type0.equals(type1)) {
                    int schemaPos = getUnionSchema(elasticsearchValue, fieldSchema);
                    Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
                    fieldValue = deserializeFieldValue(field, unionSchema, elasticsearchValue, persistent);
                } else if (fieldSchema.getTypes().size() == 3) {
                    Schema.Type type2 = fieldSchema.getTypes().get(2).getType();
                    if ((type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL) || type2.equals(Schema.Type.NULL)) &&
                            (type0.equals(Schema.Type.STRING) || type1.equals(Schema.Type.STRING) || type2.equals(Schema.Type.STRING))) {
                        if (elasticsearchValue == null) {
                            fieldValue = null;
                        } else if (elasticsearchValue instanceof String) {
                            throw new GoraException("Elasticsearch supports Union data type only represented as Record or Null.");
                        } else {
                            // TODO: Record
                            throw new UnsupportedOperationException();
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
                break;
            case DOUBLE:
                fieldValue = Double.parseDouble(elasticsearchValue.toString());
                break;
            case ENUM:
                fieldValue = AvroUtils.getEnumValue(fieldSchema, elasticsearchValue.toString());
                break;
            case FLOAT:
                fieldValue = Float.parseFloat(elasticsearchValue.toString());
                break;
            case INT:
                fieldValue = Integer.parseInt(elasticsearchValue.toString());
                break;
            case LONG:
                fieldValue = Long.parseLong(elasticsearchValue.toString());
                break;
            case STRING:
                fieldValue = new Utf8(elasticsearchValue.toString());
                break;
            default:
                fieldValue = elasticsearchValue;
        }
        return fieldValue;
    }

    /**
     * Convert an Elasticsearch List to Java Collection as used in Gora generated classes
     * that can safely be deserialized from Elasticsearch.
     *
     * @param field field in Elasticsearch to be deserialized
     * @param fieldSchema field schema for Java class
     * @param elasticsearchValue field value of the given Elasticsearch data type
     * @param persistent given persistent Java class
     * @return deserialized java collection from Elasticsearch
     * @throws GoraException
     */
    Object fromElasticsearchList(Schema.Field field, Schema fieldSchema, Object elasticsearchValue,
                                 T persistent) throws GoraException {
        List<Object> list = new ArrayList<>();
        for (Object item : (List<Object>) elasticsearchValue) {
            Object result = deserializeFieldValue(field, fieldSchema, item, persistent);
            list.add(result);
        }
        return new DirtyListWrapper<>(list);
    }

    // Add javadoc
    private Object fromElasticsearchMap(Schema.Field field, Schema fieldSchema, Map<String, Object> elasticsearchMap,
                                T persistent) throws GoraException {
        Map<Utf8, Object> deserializedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : elasticsearchMap.entrySet()) {
            String mapKey = entry.getKey();
            Object mapValue = deserializeFieldValue(avroField, avroFieldSchema, entry.getValue());
            deserializedMap.put(new Utf8(mapKey), mapValue);
        }
        return new DirtyMapWrapper<>(deserializedMap);
    }

    /**
     * Convert a Java Object as used in Gora generated classes to
     * an Object that can be written in Elasticsearch.
     *
     * @param fieldSchema field schema for Java class
     * @param fieldValue field value in given persistent Java Object
     * @return serialized field value
     * @throws GoraException
     */
    private Object serializeFieldValue(Schema fieldSchema, Object fieldValue) throws GoraException {
        Object output = fieldValue;
        switch (fieldSchema.getType()) {
            case ARRAY:
                output = arrayToElasticsearch((List<?>) fieldValue, fieldSchema.getElementType());
                break;
            case MAP:
                output = mapToElasticsearch((Map<CharSequence, ?>) fieldValue, fieldSchema.getValueType());
                break;
            case RECORD:
                throw new UnsupportedOperationException();
            case BYTES:
                output = Base64.getEncoder().encodeToString(((ByteBuffer) fieldValue).array());
                break;
            case UNION:
                Schema.Type type0 = fieldSchema.getTypes().get(0).getType();
                Schema.Type type1 = fieldSchema.getTypes().get(1).getType();
                if (fieldSchema.getTypes().size() == 2 &&
                        (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL)) &&
                        !type0.equals(type1)) {
                    int schemaPos = getUnionSchema(fieldValue, fieldSchema);
                    Schema unionSchema = fieldSchema.getTypes().get(schemaPos);
                    output = serializeFieldValue(unionSchema, fieldValue);
                } else if (fieldSchema.getTypes().size() == 3) {
                    Schema.Type type2 = fieldSchema.getTypes().get(2).getType();
                    if ((type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL) || type2.equals(Schema.Type.NULL)) &&
                            (type0.equals(Schema.Type.STRING) || type1.equals(Schema.Type.STRING) || type2.equals(Schema.Type.STRING))) {
                        if (fieldValue == null) {
                            output = null;
                        } else if (fieldValue instanceof String) {
                            throw new GoraException("Elasticsearch supports Union data type only represented as Record or Null.");
                        } else {
                            // TODO: Record
                            throw new UnsupportedOperationException();
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
                break;
            case BOOLEAN:
            case DOUBLE:
            case ENUM:
            case FLOAT:
            case INT:
            case LONG:
            case STRING:
                output = fieldValue.toString();
                break;
            case FIXED:
                break;
            case NULL:
                output = null;
                break;
        }
        return output;
    }

    /**
     * Convert a Java collection as used in Gora generated classes to a
     * List that can safely be serialized into Elasticsearch.
     *
     * @param collection the collection to be serialized
     * @param fieldSchema field schema underlying type
     * @return a List version of the collection that can be safely serialized into Elasticsearch.
     */
    private List<Object> arrayToElasticsearch(Collection<?> collection, Schema fieldSchema) throws GoraException {
        List<Object> list = new ArrayList<>();
        for (Object item : collection) {
            Object result = serializeFieldValue(fieldSchema, item);
            list.add(result);
        }
        return list;
    }

    //Add javadoc
    private Map<CharSequence, ?> mapToElasticsearch(Map<CharSequence, ?> map, Schema fieldSchema) throws GoraException {
        Map<CharSequence, Object> serializedMap = new HashMap<>();
        for (Map.Entry<CharSequence, ?> entry : map.entrySet()) {
            String mapKey = entry.getKey().toString();
            Object mapValue = entry.getValue();
            Object result = serializeFieldValue(fieldSchema, mapValue);
            serializedMap.put(mapKey, result);
        }
        return serializedMap;
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
