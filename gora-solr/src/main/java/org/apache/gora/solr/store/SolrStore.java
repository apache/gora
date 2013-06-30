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
import java.util.List;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.util.Utf8;
import org.apache.gora.persistency.StateManager;
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
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
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

public class SolrStore<K, T extends PersistentBase>
    extends DataStoreBase<K, T> {
    
    private static final Logger LOG = LoggerFactory.getLogger( SolrStore.class );

    protected static final String DEFAULT_MAPPING_FILE = "gora-solr-mapping.xml";

    protected static final String SOLR_URL_PROPERTY = "solr.url";

    protected static final String SOLR_CONFIG_PROPERTY = "solr.config";

    protected static final String SOLR_SCHEMA_PROPERTY = "solr.schema";
    
    protected static final String SOLR_BATCH_SIZE_PROPERTY = "solr.batchSize";

    protected static final String SOLR_COMMIT_WITHIN_PROPERTY = "solr.commitWithin";

    protected static final String SOLR_RESULTS_SIZE_PROPERTY = "solr.resultsSize";
    
    protected static final int DEFAULT_BATCH_SIZE = 100;

    protected static final int DEFAULT_COMMIT_WITHIN = 1000;

    protected static final int DEFAULT_RESULTS_SIZE = 100;

    private SolrMapping mapping;

    private String solrServerUrl, solrConfig, solrSchema;

    private SolrServer server, adminServer;

    private ArrayList<SolrInputDocument> batch;

    private int batchSize = DEFAULT_BATCH_SIZE;

    private int commitWithin = DEFAULT_COMMIT_WITHIN;

    private int resultsSize = DEFAULT_RESULTS_SIZE;

    @Override
    public void initialize( Class<K> keyClass, Class<T> persistentClass, Properties properties ) {
        super.initialize( keyClass, persistentClass, properties );
        String mappingFile = DataStoreFactory.getMappingFile( properties, this, DEFAULT_MAPPING_FILE );
        try {
            mapping = readMapping( mappingFile );
        }
        catch ( IOException e ) {
            LOG.error( e.getMessage() );
            LOG.error( e.getStackTrace().toString() );
        }

        solrServerUrl = DataStoreFactory.findProperty( properties, this, SOLR_URL_PROPERTY, null );
        solrConfig = DataStoreFactory.findProperty( properties, this, SOLR_CONFIG_PROPERTY, null );
        solrSchema = DataStoreFactory.findProperty( properties, this, SOLR_SCHEMA_PROPERTY, null );
        LOG.info( "Using Solr server at " + solrServerUrl );
        adminServer = new HttpSolrServer( solrServerUrl );
        server = new HttpSolrServer( solrServerUrl + "/" + mapping.getCoreName() );
        if ( autoCreateSchema ) {
            createSchema();
        }
        String batchSizeString = DataStoreFactory.findProperty( properties, this, SOLR_BATCH_SIZE_PROPERTY, null );
        if ( batchSizeString != null ) {
            try {
                batchSize = Integer.parseInt( batchSizeString );
            }
            catch ( NumberFormatException nfe ) {
                LOG.warn( "Invalid batch size '" + batchSizeString + "', using default " + DEFAULT_BATCH_SIZE );
            }
        }
        batch = new ArrayList<SolrInputDocument>( batchSize );
        String commitWithinString = DataStoreFactory.findProperty( properties, this, SOLR_COMMIT_WITHIN_PROPERTY, null );
        if ( commitWithinString != null ) {
            try {
                commitWithin = Integer.parseInt( commitWithinString );
            }
            catch ( NumberFormatException nfe ) {
                LOG.warn( "Invalid commit within '" + commitWithinString + "', using default " + DEFAULT_COMMIT_WITHIN );
            }
        }
        String resultsSizeString = DataStoreFactory.findProperty( properties, this, SOLR_RESULTS_SIZE_PROPERTY, null );
        if ( resultsSizeString != null ) {
            try {
                resultsSize = Integer.parseInt( resultsSizeString );
            }
            catch ( NumberFormatException nfe ) {
                LOG.warn( "Invalid results size '" + resultsSizeString + "', using default " + DEFAULT_RESULTS_SIZE );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private SolrMapping readMapping( String filename )
        throws IOException {
        SolrMapping map = new SolrMapping();
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build( getClass().getClassLoader().getResourceAsStream( filename ) );

            List<Element> classes = doc.getRootElement().getChildren( "class" );

            for ( Element classElement : classes ) {
                if ( classElement.getAttributeValue( "keyClass" ).equals( keyClass.getCanonicalName() )
                    && classElement.getAttributeValue( "name" ).equals( persistentClass.getCanonicalName() ) ) {

                    String tableName = getSchemaName( classElement.getAttributeValue( "table" ), persistentClass );
                    map.setCoreName( tableName );

                    Element primaryKeyEl = classElement.getChild( "primarykey" );
                    map.setPrimaryKey( primaryKeyEl.getAttributeValue( "column" ) );

                    List<Element> fields = classElement.getChildren( "field" );

                    for ( Element field : fields ) {
                        String fieldName = field.getAttributeValue( "name" );
                        String columnName = field.getAttributeValue( "column" );
                        map.addField( fieldName, columnName );
                    }
                    break;
                }
            }
        }
        catch ( Exception ex ) {
            throw new IOException( ex );
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
            if ( !schemaExists() )
                CoreAdminRequest.createCore( mapping.getCoreName(), mapping.getCoreName(), adminServer, solrConfig,
                                             solrSchema );
        }
        catch ( Exception e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
    }

    @Override
    /** Default implementation deletes and recreates the schema*/
    public void truncateSchema() {
        try {
            server.deleteByQuery( "*:*" );
            server.commit();
        }
        catch ( Exception e ) {
            // ignore?
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
    }

    @Override
    public void deleteSchema() {
        // XXX should this be only in truncateSchema ???
        try {
            server.deleteByQuery( "*:*" );
            server.commit();
        }
        catch ( Exception e ) {
            // ignore?
            // LOG.error(e.getMessage());
            // LOG.error(e.getStackTrace().toString());
        }
        try {
            CoreAdminRequest.unloadCore( mapping.getCoreName(), adminServer );
        }
        catch ( Exception e ) {
            if ( e.getMessage().contains( "No such core" ) ) {
                return; // it's ok, the core is not there
            }
            else {
                LOG.error( e.getMessage(), e.getStackTrace().toString() );
            }
        }
    }

    @Override
    public boolean schemaExists() {
        boolean exists = false;
        try {
            CoreAdminResponse rsp = CoreAdminRequest.getStatus( mapping.getCoreName(), adminServer );
            exists = rsp.getUptime( mapping.getCoreName() ) != null;
        }
        catch ( Exception e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
        return exists;
    }

    private static final String toDelimitedString( String[] arr, String sep ) {
        if ( arr == null || arr.length == 0 ) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < arr.length; i++ ) {
            if ( i > 0 )
                sb.append( sep );
            sb.append( arr[i] );
        }
        return sb.toString();
    }

    public static String escapeQueryKey( String key ) {
        if ( key == null ) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < key.length(); i++ ) {
            char c = key.charAt( i );
            switch ( c ) {
                case ':':
                case '*':
                    sb.append( "\\" + c );
                    break;
                default:
                    sb.append( c );
            }
        }
        return sb.toString();
    }

    @Override
    public T get( K key, String[] fields ) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set( CommonParams.QT, "/get" );
        params.set( CommonParams.FL, toDelimitedString( fields, "," ) );
        params.set( "id",  key.toString() );
        try {
            QueryResponse rsp = server.query( params );
            Object o = rsp.getResponse().get( "doc" );
            if ( o == null ) {
                return null;
            }
            return newInstance( (SolrDocument)o, fields );
        }
        catch ( Exception e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
        return null;
    }

    public T newInstance( SolrDocument doc, String[] fields )
        throws IOException {
        T persistent = newPersistent();
        if ( fields == null ) {
            fields = fieldMap.keySet().toArray( new String[fieldMap.size()] );
        }
        String pk = mapping.getPrimaryKey();
        for ( String f : fields ) {
            Field field = fieldMap.get( f );
            Schema fieldSchema = field.schema();
            String sf = null;
            if ( pk.equals( f ) ) {
                sf = f;
            }
            else {
                sf = mapping.getSolrField( f );                
            }
            Object sv = doc.get( sf );
            Object v;
            if ( sv == null ) {
                continue;
            }
            switch ( fieldSchema.getType() ) {
                case MAP:
                case ARRAY:
                case RECORD:
                    v = IOUtils.deserialize( (byte[]) sv, datumReader, fieldSchema, persistent.get( field.pos() ) );
                    persistent.put( field.pos(), v );
                    break;
                case ENUM:
                    v = AvroUtils.getEnumValue( fieldSchema, (String) sv );
                    persistent.put( field.pos(), v );
                    break;
                case FIXED:
                    throw new IOException( "???" );
                    // break;
                case BYTES:
                    persistent.put( field.pos(), ByteBuffer.wrap( (byte[]) sv ) );
                    break;
                case BOOLEAN:
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                    persistent.put( field.pos(), sv );
                    break;
                case STRING:
                    persistent.put( field.pos(), new Utf8( sv.toString() ) );
                    break;
                case UNION:
                    LOG.error( "Union is not supported yet" );
                    break;
                default:
                    LOG.error( "Unknown field type: " + fieldSchema.getType() );
            }
            persistent.setDirty( field.pos() );
        }
        persistent.clearDirty();
        return persistent;
    }

    @Override
    public void put( K key, T persistent ) {
        Schema schema = persistent.getSchema();
        StateManager stateManager = persistent.getStateManager();
        if ( !stateManager.isDirty( persistent ) ) {
            // nothing to do
            return;
        }
        SolrInputDocument doc = new SolrInputDocument();
        // add primary key
        doc.addField( mapping.getPrimaryKey(), key );
        // populate the doc
        List<Field> fields = schema.getFields();
        for ( Field field : fields ) {
            String sf = mapping.getSolrField( field.name() );
            // Solr will append values to fields in a SolrInputDocument, even the key
            // mapping won't find the primary
            if ( sf == null ) {
                continue;
            }
            Schema fieldSchema = field.schema();
            Object v = persistent.get( field.pos() );
            if ( v == null ) {
                continue;
            }
            switch ( fieldSchema.getType() ) {
                case MAP:
                case ARRAY:
                case RECORD:
                    byte[] data = null;
                    try {
                        data = IOUtils.serialize( datumWriter, fieldSchema, v );
                    }
                    catch ( IOException e ) {
                        LOG.error( e.getMessage(), e.getStackTrace().toString() );
                    }
                    doc.addField( sf, data );
                    break;
                case BYTES:
                    doc.addField( sf, ( (ByteBuffer) v ).array() );
                    break;
                case ENUM:
                case STRING:
                    doc.addField( sf, v.toString() );
                    break;
                case BOOLEAN:
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                    doc.addField( sf, v );
                    break;
                case UNION:
                    LOG.error( "Union is not supported yet" );
                    break;
                default:
                    LOG.error( "Unknown field type: " + fieldSchema.getType() );
            }
        }
        System.out.println( "DOCUMENT: " + doc );
        batch.add( doc );
        if ( batch.size() >= batchSize ) {
            try {
                add( batch, commitWithin );
                batch.clear();
            }
            catch ( Exception e ) {
                LOG.error( e.getMessage(), e.getStackTrace().toString() );
            }
        }
    }

    @Override
    public boolean delete( K key ) {
        String keyField = mapping.getPrimaryKey();
        try {
            UpdateResponse rsp = server.deleteByQuery( keyField + ":" + escapeQueryKey( key.toString() ) );
            server.commit();
            LOG.info( rsp.toString() );
            return true;
        }
        catch ( Exception e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
        return false;
    }

    @Override
    public long deleteByQuery( Query<K, T> query ) {
        String q = ( (SolrQuery<K, T>) query ).toSolrQuery();
        try {
            UpdateResponse rsp = server.deleteByQuery( q );
            server.commit();
            LOG.info( rsp.toString() );
        }
        catch ( Exception e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
        return 0;
    }

    @Override
    public Result<K, T> execute( Query<K, T> query ) {
        try {
            return new SolrResult<K, T>( this, query, server, resultsSize );
        }
        catch ( IOException e ) {
            LOG.error( e.getMessage(), e.getStackTrace().toString() );
        }
        return null;
    }

    @Override
    public Query<K, T> newQuery() {
        return new SolrQuery<K, T>( this );
    }

    @Override
    public List<PartitionQuery<K, T>> getPartitions( Query<K, T> query )
        throws IOException {
        // TODO: implement this using Hadoop DB support

        ArrayList<PartitionQuery<K, T>> partitions = new ArrayList<PartitionQuery<K, T>>();
        partitions.add( new PartitionQueryImpl<K, T>( query ) );

        return partitions;
    }

    @Override
    public void flush() {
        try {
            if ( batch.size() > 0 ) {
                add( batch, commitWithin );
                batch.clear();
            }
        }
        catch ( Exception e ) {
            LOG.error(e.getMessage(), e.getStackTrace());
        }
    }

    @Override
    public void close() {
        // In testing, the index gets closed before the commit in flush() can happen
        // so an exception gets thrown
        //flush();
    }
    
    private void add(ArrayList<SolrInputDocument> batch, int commitWithin) throws SolrServerException, IOException {
        if (commitWithin == 0) {
            server.add( batch );
            server.commit( false, true, true );
        }
        else {
            server.add( batch, commitWithin );            
        }
    }
    
}
