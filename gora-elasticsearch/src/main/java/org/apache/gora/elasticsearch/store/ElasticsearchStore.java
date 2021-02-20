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
import org.apache.avro.util.Utf8;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMapping;
import org.apache.gora.elasticsearch.mapping.ElasticsearchMappingBuilder;
import org.apache.gora.elasticsearch.mapping.Field;
import org.apache.gora.elasticsearch.query.ElasticsearchQuery;
import org.apache.gora.elasticsearch.query.ElasticsearchResult;
import org.apache.gora.elasticsearch.utils.ElasticsearchParameters;
import org.apache.gora.filter.Filter;
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
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

    public static RestHighLevelClient createClient(ElasticsearchParameters parameters) {
        RestClientBuilder clientBuilder = RestClient.builder(new HttpHost(parameters.getHost(), parameters.getPort()));

        // Choosing the authentication method.
        switch (parameters.getAuthenticationType()) {
            case BASIC:
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
            case TOKEN:
                if (parameters.getAuthorizationToken() != null) {
                    Header[] defaultHeaders = new Header[]{new BasicHeader("Authorization",
                            parameters.getAuthorizationToken())};
                    clientBuilder.setDefaultHeaders(defaultHeaders);
                } else {
                    throw new IllegalArgumentException("Missing authorization token for TOKEN authentication.");
                }
                break;
            case APIKEY:
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
            if (entry.getValue().getDataType().getType() == Field.DataType.SCALED_FLOAT) {
                fieldType.put("scaling_factor", entry.getValue().getDataType().getScalingFactor());
            }
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
        try {
            BulkRequest requestTry = new BulkRequest();
            long startId = Long.parseLong((String) query.getStartKey());
            long endId = Long.parseLong((String) query.getEndKey());
            for (long i = startId; i <= endId; i++) {
                requestTry.add(new DeleteRequest(elasticsearchMapping.getIndexName(), Long.toString(i)));
            }
            client.bulk(requestTry, RequestOptions.DEFAULT);
            return endId - startId + 1;
        } catch (IOException ex) {
            throw new GoraException(ex);
        }
    }

    @Override
    public Result<K, T> execute(Query<K, T> query) throws GoraException {
        String startKey = (String) query.getStartKey();
        String endKey = (String) query.getEndKey();

        if (startKey != null && endKey != null) {
            MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
            long startId = Long.parseLong(startKey);
            long endId = Long.parseLong(endKey);
            for (long i = startId; i < endId; i++) {
                SearchRequest searchRequest = new SearchRequest(elasticsearchMapping.getIndexName());
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(QueryBuilders.termQuery("_id", Long.toString(i)));
                searchRequest.source(searchSourceBuilder);
                multiSearchRequest.add(searchRequest);
            }

            try {
                MultiSearchResponse multiSearchResponse = client.msearch(multiSearchRequest, RequestOptions.DEFAULT);

                Filter<K, T> queryFilter = query.getFilter();
                String[] avroFields = getFieldsToQuery(query.getFields());
                List<K> hitId = new ArrayList<>();
                List<T> filteredObjects = new ArrayList<>();

                for (MultiSearchResponse.Item item : multiSearchResponse.getResponses()) {
                    for (SearchHit hit : item.getResponse().getHits()) {
                        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                        if (queryFilter == null || !queryFilter.filter((K) hit.getId(), newInstance(sourceAsMap, avroFields))) {
                            filteredObjects.add(newInstance(sourceAsMap, avroFields));
                            hitId.add((K) hit.getId());
                        }
                    }
                }

                return new ElasticsearchResult<>(this, query, hitId, filteredObjects);
            } catch (IOException e) {
                throw new GoraException(e);
            }
        } else {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            int size = (int) query.getLimit();
            if (size != -1) {
                searchSourceBuilder.size(size);
            }
            System.out.println(size);

            try {
                // Build the actual Elasticsearch query
                SearchRequest searchRequest = new SearchRequest(elasticsearchMapping.getIndexName());
                searchSourceBuilder.query(QueryBuilders.matchAllQuery());
                searchRequest.source(searchSourceBuilder);
                System.out.println(searchRequest);
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                System.out.println(searchResponse);

                // check filter
                Filter<K, T> queryFilter = query.getFilter();
                SearchHits hits = searchResponse.getHits();
                SearchHit[] searchHits = hits.getHits();

                String[] avroFields = getFieldsToQuery(query.getFields());

                List<K> hitId = new ArrayList<>();
                List<T> filteredObjects = new ArrayList<>();
                for (SearchHit hit : searchHits) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    if (queryFilter == null || !queryFilter.filter((K) hit.getId(), newInstance(sourceAsMap, avroFields))) {
                        filteredObjects.add(newInstance(sourceAsMap, avroFields));
                        hitId.add((K) hit.getId());
                    }
                }
                return new ElasticsearchResult<>(this, query, hitId, filteredObjects);
            } catch (IOException ex) {
                throw new GoraException(ex);
            }
        }
    }

    @Override
    public Query<K, T> newQuery() {
        ElasticsearchQuery<K, T> query = new ElasticsearchQuery<>(this);
        query.setFields(getFieldsToQuery(null));
        return query;
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query) throws IOException {
        List<PartitionQuery<K, T>> partitions = new ArrayList<>();
        PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
                query);
        partitionQuery.setConf(getConf());
        partitions.add(partitionQuery);
        return partitions;
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

            Object result = deserializeFieldValue(field, fieldSchema, fieldValue);
            persistent.put(field.pos(), result);
        }
        persistent.clearDirty();
        return persistent;
    }

    /**
     * Deserialize an Elasticsearch object to a persistent Avro object.
     *
     * @param avroField          persistent Avro class field to which the value will be deserialized
     * @param avroFieldSchema    schema for the persistent Avro class field
     * @param elasticsearchValue Elasticsearch field value to be deserialized
     * @return deserialized Avro object from the Elasticsearch object
     * @throws GoraException when the given Elasticsearch value cannot be deserialized
     */
    private Object deserializeFieldValue(Schema.Field avroField, Schema avroFieldSchema,
                                         Object elasticsearchValue) throws GoraException {
        Object fieldValue;
        switch (avroFieldSchema.getType()) {
            case MAP:
                fieldValue = fromElasticsearchMap(avroField, avroFieldSchema.getValueType(), (Map<String, Object>) elasticsearchValue);
                break;
            case RECORD:
                fieldValue = fromElasticsearchRecord(avroFieldSchema, (Map<String, Object>) elasticsearchValue);
                break;
            case ARRAY:
                fieldValue = fromElasticsearchList(avroField, avroFieldSchema.getElementType(), elasticsearchValue);
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
                fieldValue = fromElasticsearchUnion(avroField, avroFieldSchema, elasticsearchValue);
                break;
            case DOUBLE:
                fieldValue = Double.parseDouble(elasticsearchValue.toString());
                break;
            case ENUM:
                fieldValue = AvroUtils.getEnumValue(avroFieldSchema, elasticsearchValue.toString());
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
     * Deserialize an Elasticsearch List to an Avro List as used in Gora generated classes
     * that can safely be written into Avro persistent object.
     *
     * @param avroField          persistent Avro class field to which the value will be deserialized
     * @param avroFieldSchema    schema for the persistent Avro class field
     * @param elasticsearchValue Elasticsearch field value to be deserialized
     * @return deserialized Avro List from the given Elasticsearch value
     * @throws GoraException when one of the underlying values cannot be deserialized
     */
    private Object fromElasticsearchList(Schema.Field avroField, Schema avroFieldSchema,
                                         Object elasticsearchValue) throws GoraException {
        List<Object> list = new ArrayList<>();
        for (Object item : (List<Object>) elasticsearchValue) {
            Object result = deserializeFieldValue(avroField, avroFieldSchema, item);
            list.add(result);
        }
        return new DirtyListWrapper<>(list);
    }

    /**
     * Deserialize an Elasticsearch Map to an Avro Map as used in Gora generated classes
     * that can safely be written into Avro persistent object.
     *
     * @param avroField        persistent Avro class field to which the value will be deserialized
     * @param avroFieldSchema  schema for the persistent Avro class field
     * @param elasticsearchMap Elasticsearch Map value to be deserialized
     * @return deserialized Avro Map from the given Elasticsearch Map value
     * @throws GoraException when one of the underlying values cannot be deserialized
     */
    private Object fromElasticsearchMap(Schema.Field avroField, Schema avroFieldSchema,
                                        Map<String, Object> elasticsearchMap) throws GoraException {
        Map<Utf8, Object> deserializedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : elasticsearchMap.entrySet()) {
            String mapKey = entry.getKey();
            Object mapValue = deserializeFieldValue(avroField, avroFieldSchema, entry.getValue());
            deserializedMap.put(new Utf8(mapKey), mapValue);
        }
        return new DirtyMapWrapper<>(deserializedMap);
    }

    /**
     * Deserialize an Elasticsearch Record to an Avro Object as used in Gora generated classes
     * that can safely be written into Avro persistent object.
     *
     * @param avroFieldSchema     schema for the persistent Avro class field
     * @param elasticsearchRecord Elasticsearch Record value to be deserialized
     * @return deserialized Avro Object from the given Elasticsearch Record value
     * @throws GoraException when one of the underlying values cannot be deserialized
     */
    private Object fromElasticsearchRecord(Schema avroFieldSchema,
                                           Map<String, Object> elasticsearchRecord) throws GoraException {
        Class<?> clazz;
        try {
            clazz = ClassLoadingUtils.loadClass(avroFieldSchema.getFullName());
        } catch (ClassNotFoundException ex) {
            throw new GoraException(ex);
        }
        PersistentBase record = (PersistentBase) new BeanFactoryImpl(keyClass, clazz).newPersistent();
        for (Schema.Field recField : avroFieldSchema.getFields()) {
            Schema innerSchema = recField.schema();
            Field innerDocField = elasticsearchMapping.getFields().getOrDefault(recField.name(), new Field(recField.name(), null));
            record.put(recField.pos(), deserializeFieldValue(recField, innerSchema, elasticsearchRecord.get(innerDocField.getName())));
        }
        return record;
    }

    /**
     * Deserialize an Elasticsearch Union to an Avro Object as used in Gora generated classes
     * that can safely be written into Avro persistent object.
     *
     * @param avroField          persistent Avro class field to which the value will be deserialized
     * @param avroFieldSchema    schema for the persistent Avro class field
     * @param elasticsearchUnion Elasticsearch Union value to be deserialized
     * @return deserialized Avro Object from the given Elasticsearch Union value
     * @throws GoraException when one of the underlying values cannot be deserialized
     */
    private Object fromElasticsearchUnion(Schema.Field avroField, Schema avroFieldSchema, Object elasticsearchUnion) throws GoraException {
        Object deserializedUnion;
        Schema.Type type0 = avroFieldSchema.getTypes().get(0).getType();
        Schema.Type type1 = avroFieldSchema.getTypes().get(1).getType();
        if (avroFieldSchema.getTypes().size() == 2 &&
                (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL)) &&
                !type0.equals(type1)) {
            int schemaPos = getUnionSchema(elasticsearchUnion, avroFieldSchema);
            Schema unionSchema = avroFieldSchema.getTypes().get(schemaPos);
            deserializedUnion = deserializeFieldValue(avroField, unionSchema, elasticsearchUnion);
        } else if (avroFieldSchema.getTypes().size() == 3) {
            Schema.Type type2 = avroFieldSchema.getTypes().get(2).getType();
            if ((type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL) || type2.equals(Schema.Type.NULL)) &&
                    (type0.equals(Schema.Type.STRING) || type1.equals(Schema.Type.STRING) || type2.equals(Schema.Type.STRING))) {
                if (elasticsearchUnion == null) {
                    deserializedUnion = null;
                } else if (elasticsearchUnion instanceof String) {
                    throw new GoraException("Elasticsearch supports Union data type only represented as Record or Null.");
                } else {
                    int schemaPos = getUnionSchema(elasticsearchUnion, avroFieldSchema);
                    Schema unionSchema = avroFieldSchema.getTypes().get(schemaPos);
                    deserializedUnion = fromElasticsearchRecord(unionSchema, (Map<String, Object>) elasticsearchUnion);
                }
            } else {
                throw new GoraException("Elasticsearch only supports Union of two types field: Record or Null.");
            }
        } else {
            throw new GoraException("Elasticsearch only supports Union of two types field: Record or Null.");
        }
        return deserializedUnion;
    }

    /**
     * Serialize a persistent Avro object as used in Gora generated classes to
     * an object that can be written into Elasticsearch.
     *
     * @param avroFieldSchema schema for the persistent Avro class field
     * @param avroFieldValue  persistent Avro field value to be serialized
     * @return serialized field value
     * @throws GoraException when the given Avro object cannot be serialized
     */
    private Object serializeFieldValue(Schema avroFieldSchema, Object avroFieldValue) throws GoraException {
        Object output = avroFieldValue;
        switch (avroFieldSchema.getType()) {
            case ARRAY:
                output = arrayToElasticsearch((List<?>) avroFieldValue, avroFieldSchema.getElementType());
                break;
            case MAP:
                output = mapToElasticsearch((Map<CharSequence, ?>) avroFieldValue, avroFieldSchema.getValueType());
                break;
            case RECORD:
                output = recordToElasticsearch(avroFieldValue, avroFieldSchema);
                break;
            case BYTES:
                output = Base64.getEncoder().encodeToString(((ByteBuffer) avroFieldValue).array());
                break;
            case UNION:
                output = unionToElasticsearch(avroFieldValue, avroFieldSchema);
                break;
            case BOOLEAN:
            case DOUBLE:
            case ENUM:
            case FLOAT:
            case INT:
            case LONG:
            case STRING:
                output = avroFieldValue.toString();
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
     * Serialize a Java collection of persistent Avro objects as used in Gora generated classes to a
     * List that can safely be written into Elasticsearch.
     *
     * @param collection      the collection to be serialized
     * @param avroFieldSchema field schema for the underlying type
     * @return a List version of the collection that can be safely written into Elasticsearch
     * @throws GoraException when one of the underlying values cannot be serialized
     */
    private List<Object> arrayToElasticsearch(Collection<?> collection, Schema avroFieldSchema) throws GoraException {
        List<Object> list = new ArrayList<>();
        for (Object item : collection) {
            Object result = serializeFieldValue(avroFieldSchema, item);
            list.add(result);
        }
        return list;
    }

    /**
     * Serialize a Java map of persistent Avro objects as used in Gora generated classes to a
     * map that can safely be written into Elasticsearch.
     *
     * @param map             the map to be serialized
     * @param avroFieldSchema field schema for the underlying type
     * @return a Map version of the Java map that can be safely written into Elasticsearch
     * @throws GoraException when one of the underlying values cannot be serialized
     */
    private Map<CharSequence, ?> mapToElasticsearch(Map<CharSequence, ?> map, Schema avroFieldSchema) throws GoraException {
        Map<CharSequence, Object> serializedMap = new HashMap<>();
        for (Map.Entry<CharSequence, ?> entry : map.entrySet()) {
            String mapKey = entry.getKey().toString();
            Object mapValue = entry.getValue();
            Object result = serializeFieldValue(avroFieldSchema, mapValue);
            serializedMap.put(mapKey, result);
        }
        return serializedMap;
    }

    /**
     * Serialize a Java object of persistent Avro objects as used in Gora generated classes to a
     * record that can safely be written into Elasticsearch.
     *
     * @param record          the object to be serialized
     * @param avroFieldSchema field schema for the underlying type
     * @return a record version of the Java object that can be safely written into Elasticsearch
     * @throws GoraException when one of the underlying values cannot be serialized
     */
    private Map<CharSequence, Object> recordToElasticsearch(Object record, Schema avroFieldSchema) throws GoraException {
        Map<CharSequence, Object> serializedRecord = new HashMap<>();
        for (Schema.Field member : avroFieldSchema.getFields()) {
            Object innerValue = ((PersistentBase) record).get(member.pos());
            serializedRecord.put(member.name(), serializeFieldValue(member.schema(), innerValue));
        }
        return serializedRecord;
    }

    /**
     * Serialize a Java object of persistent Avro objects as used in Gora generated classes to a
     * object that can safely be written into Elasticsearch.
     *
     * @param union           the object to be serialized
     * @param avroFieldSchema field schema for the underlying type
     * @return a object version of the Java object that can be safely written into Elasticsearch
     * @throws GoraException when one of the underlying values cannot be serialized
     */
    private Object unionToElasticsearch(Object union, Schema avroFieldSchema) throws GoraException {
        Object serializedUnion;
        Schema.Type type0 = avroFieldSchema.getTypes().get(0).getType();
        Schema.Type type1 = avroFieldSchema.getTypes().get(1).getType();
        if (avroFieldSchema.getTypes().size() == 2 &&
                (type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL)) &&
                !type0.equals(type1)) {
            int schemaPos = getUnionSchema(union, avroFieldSchema);
            Schema unionSchema = avroFieldSchema.getTypes().get(schemaPos);
            serializedUnion = serializeFieldValue(unionSchema, union);
        } else if (avroFieldSchema.getTypes().size() == 3) {
            Schema.Type type2 = avroFieldSchema.getTypes().get(2).getType();
            if ((type0.equals(Schema.Type.NULL) || type1.equals(Schema.Type.NULL) || type2.equals(Schema.Type.NULL)) &&
                    (type0.equals(Schema.Type.STRING) || type1.equals(Schema.Type.STRING) || type2.equals(Schema.Type.STRING))) {
                if (union == null) {
                    serializedUnion = null;
                } else if (union instanceof String) {
                    throw new GoraException("Elasticsearch does not support foreign key IDs in Union data type.");
                } else {
                    int schemaPos = getUnionSchema(union, avroFieldSchema);
                    Schema unionSchema = avroFieldSchema.getTypes().get(schemaPos);
                    serializedUnion = recordToElasticsearch(union, unionSchema);
                }
            } else {
                throw new GoraException("Elasticsearch only supports Union of two types field: Record or Null.");
            }
        } else {
            throw new GoraException("Elasticsearch only supports Union of two types field: Record or Null.");
        }
        return serializedUnion;
    }

    /**
     * Method to retrieve the corresponding schema type index of a particular
     * object having UNION schema. As UNION type can have one or more types and at
     * a given instance, it holds an object of only one type of the defined types,
     * this method is used to figure out the corresponding instance's schema type
     * index.
     *
     * @param instanceValue value that the object holds
     * @param unionSchema   union schema containing all of the data types
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
            if (instanceValue instanceof String && schemaType.equals(Schema.Type.BYTES)) {
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
            if (instanceValue instanceof Map && schemaType.equals(Schema.Type.RECORD)) {
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
