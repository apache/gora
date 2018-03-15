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
package org.apache.gora.lucene.store;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.util.Utf8;
import org.apache.gora.lucene.query.LuceneQuery;
import org.apache.gora.lucene.query.LuceneResult;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.FileSplitPartitionQuery;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.gora.store.impl.FileBackedDataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.GoraException;
import org.apache.gora.util.IOUtils;
import org.apache.gora.util.OperationNotSupportedException;
import org.apache.hadoop.conf.Configurable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class LuceneStore<K, T extends PersistentBase> extends FileBackedDataStoreBase<K, T> implements Configurable {

  private static final Logger LOG = LoggerFactory.getLogger( LuceneStore.class );

  private static final String DEFAULT_MAPPING_FILE = "gora-lucene-mapping.xml";
  private static final String LUCENE_VERSION_KEY = "gora.lucene.index.version";
  private static final String DEFAULT_LUCENE_VERSION = "LATEST";
  private static final String LUCENE_RAM_BUFFER_KEY = "gora.lucene.index.writer.rambuffer";
  private static final String DEFAULT_LUCENE_RAMBUFFER = "16";

  private LuceneMapping mapping;
  private IndexWriter writer;
  private SearcherManager searcherManager;

  @Override
  public void initialize(Class<K> keyClass, Class<T> persistentClass, Properties properties) {
    try {
      super.initialize(keyClass, persistentClass, properties);
    } catch (GoraException e1) {
      e1.printStackTrace();
    }

    String mappingFile = null;
    try {
      mappingFile = DataStoreFactory.getMappingFile(
              properties, (DataStore<?, ?>) this, DEFAULT_MAPPING_FILE );
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    String luceneVersion = properties.getProperty(
            LUCENE_VERSION_KEY, DEFAULT_LUCENE_VERSION);
    String ramBuffer = properties.getProperty(
            LUCENE_RAM_BUFFER_KEY, DEFAULT_LUCENE_RAMBUFFER);

    LOG.debug("Lucene index version: {}", luceneVersion);
    LOG.debug("Lucene index writer RAM buffer size: {}", ramBuffer);

    try {
      mapping = readMapping(mappingFile);
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    try(Directory dir = FSDirectory.open(FileSystems.getDefault().getPath(outputPath))) {

      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      iwc.setRAMBufferSizeMB(Double.parseDouble(ramBuffer));

      writer = new IndexWriter(dir, iwc);
      //do we definately want all past deletions to be applied.
      searcherManager = new SearcherManager(writer, true, true, new SearcherFactory());
    } catch (IOException e ) {
      LOG.error("Error opening {} with Lucene FSDirectory.", outputPath, e);
    }
  }

  LuceneMapping readMapping(String filename) throws IOException {
    try {

      LuceneMapping mapping = new LuceneMapping();

      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      org.w3c.dom.Document dom = db.parse(getClass().getClassLoader().getResourceAsStream(filename));

      Element root = dom.getDocumentElement();

      NodeList nl = root.getElementsByTagName("class");
      for (int i = 0; i < nl.getLength(); i++) {

        Element classElement = (Element) nl.item(i);
        if (classElement.getAttribute("keyClass").equals(keyClass.getCanonicalName())
                && classElement.getAttribute("name").equals(persistentClass.getCanonicalName())) {

          NodeList fields;
          Element fe;

          fields = classElement.getElementsByTagName("primarykey");
          for (int j = 0; j < fields.getLength(); j++) {
            fe = (Element) fields.item(j);
            mapping.setPrimaryKey(fe.getAttribute("column"));
          }

          fields = classElement.getElementsByTagName("field");
          for (int j = 0; j < fields.getLength(); j++) {
            fe = (Element) fields.item(j);

            String name = fe.getAttribute("name");
            String column = fe.getAttribute("column");

            mapping.addField(name, column);
          }
        }
      }
      return mapping;
    } catch (Exception ex) {
      throw new IOException("Unable to read " + filename, ex);
    }
  }


  @Override
  public boolean delete(K key) {
    try {
      writer.deleteDocuments(new Term(mapping.getPrimaryKey(), key.toString()));
      searcherManager.maybeRefresh();
      return true;
    } catch (IOException e) {
      LOG.error("Unable to delete key: {}", key.toString(), e);
    }
    return false;
  }


  @Override
  public long deleteByQuery(Query<K,T> query) {
    try {
      // Figure out how many were there before
      LuceneQuery<K,T> q = (LuceneQuery<K,T>)query;
      LuceneResult<K,T> r = (LuceneResult<K,T>)q.execute();
      int before = r.getScoreDocs().length;

      // Delete them
      writer.deleteDocuments(q.toLuceneQuery());
      searcherManager.maybeRefresh();

      // Figure out how many there are after
      r = (LuceneResult<K,T>)q.execute();
      int after = r.getScoreDocs().length;

      return before - after;
    } catch (IOException e) {
      LOG.error("Unable to deleteByQuery: {}", query.toString(), e);
    }
    return 0;
  }


  @Override
  public void deleteSchema() {
    try {
      writer.deleteAll();
      searcherManager.maybeRefresh();
    } catch (IOException e) {
      LOG.error("Unable to deleteAll: {}", e);
    }
  }


  @Override
  public T get(K key, String[] fieldsToLoad) {

    Set<String> fields;
    if (fieldsToLoad != null) {
      fields = new HashSet<>(fieldsToLoad.length);
      fields.addAll(Arrays.asList(fieldsToLoad));
    }
    else {
      fields = new HashSet<>();
      fields.addAll(mapping.getLuceneFields());
    }
    try {
      final IndexSearcher s = searcherManager.acquire();
      TermQuery q = new TermQuery(new Term(mapping.getPrimaryKey(), key.toString()));
      ScoreDoc[] hits = s.search(q, 2).scoreDocs;
      if (hits.length > 0) {
        Document doc = s.doc(hits[0].doc, fields);
        LOG.debug("get:Document: {}", doc.toString());
        String[] a = {};
        return newInstance( doc, fields.toArray(a) );
      }
      searcherManager.release(s);
    } catch (IOException e) {
      LOG.error("Error in get: {}", e);
    }
    return null;
  }


  public T newInstance(Document doc, String[] fields) throws IOException {
    T persistent = newPersistent();
    if ( fields == null ) {
      fields = fieldMap.keySet().toArray( new String[fieldMap.size()] );
    }
    String pk = mapping.getPrimaryKey();

    for ( String f : fields ) {
      org.apache.avro.Schema.Field field = fieldMap.get( f );
      Schema fieldSchema = field.schema();
      String sf;
      if ( pk.equals( f ) ) {
        sf = f;
      } else {
        sf = mapping.getLuceneField(f);
      }
      Object v;
      Object sv;
      switch ( fieldSchema.getType() ) {
        case MAP:
        case ARRAY:
        case RECORD:
        case UNION:
        sv = doc.getBinaryValue(sf);
        if (sv == null)
          continue;
        BytesRef b = (BytesRef)sv;
        v = IOUtils.deserialize(b.bytes, datumReader, (T) persistent.get( field.pos() ));
        persistent.put( field.pos(), v );
        break;
        case ENUM:
        sv = doc.get(sf);
        if (sv == null)
          continue;
        v = AvroUtils.getEnumValue( fieldSchema, (String) sv );
        persistent.put( field.pos(), v );
        break;
        default:
        sv = doc.get(sf);
        if (sv == null)
          continue;
        put(persistent, field.pos(), fieldSchema.getType(), sv);
      }
      persistent.setDirty( field.pos() );
    }
    persistent.clearDirty();
    return persistent;
  }


  void put(T p, int pos, Type t, Object o) {
    switch(t) {
      case FIXED:
      // Could we combine this with the BYTES section below and
      // either fix the size of the array or not depending on Type?
      // This might be a buffer copy. Do we need to pad if the
      // fixed sized data is smaller than the type? Do we truncate
      // if the data is larger than the type?
      LOG.error( "Fixed-sized fields are not supported yet" );
      break;
      case BYTES:
      p.put( pos, ByteBuffer.wrap( (byte[]) o ) );
      break;
      case BOOLEAN:
      p.put( pos, Boolean.parseBoolean((String)o) );
      break;
      case DOUBLE:
      p.put( pos, Double.parseDouble((String)o) );
      break;
      case FLOAT:
      p.put( pos, Float.parseFloat((String)o) );
      break;
      case INT:
      p.put( pos, Integer.parseInt((String)o) );
      break;
      case LONG:
      p.put( pos, Long.parseLong((String)o) );
      break;
      case STRING:
      p.put( pos, new Utf8( o.toString() ) );
      break;
      default:
      LOG.error( "Unknown field type: {}", t );
    }
  }


  @Override
  public String getSchemaName() {
    return "default";
  }

  @Override
  public Query<K,T> newQuery() {
    return new LuceneQuery<>( this );
  }

  @Override
  public void put(K key, T persistent) {
    Schema schema = persistent.getSchema();
    Document doc = new Document();

    // populate the doc
    List<org.apache.avro.Schema.Field> fields = schema.getFields();
    for ( org.apache.avro.Schema.Field field : fields ) {
      if (!persistent.isDirty( field.name() )) {
        continue;
      }
      String sf = mapping.getLuceneField( field.name() );
      if ( sf == null ) {
        continue;
      }
      Schema fieldSchema = field.schema();
      Object v = persistent.get( field.pos() );
      if ( v == null ) {
        continue;
      }
      switch ( fieldSchema.getType() ) {
        case MAP:   //TODO: These should be handled better
        case ARRAY:
        case RECORD:
        case UNION:
        // For now we'll just store the bytes
        byte[] data = new byte[0];
        try {
          data = IOUtils.serialize(datumWriter, (T) v);
        } catch ( IOException e ) {
          LOG.error( e.getMessage(), e );
        }
        doc.add(new StoredField(sf, data));
        break;
        case BYTES:
        doc.add(new StoredField( sf, ( (ByteBuffer) v ).array() ));
        break;
        case ENUM:
        case STRING:
        //TODO make this Text based on a mapping.xml attribute
        doc.add( new StringField(sf, v.toString(), Store.YES));
        break;
        case BOOLEAN:
        doc.add(new StringField(sf, v.toString(), Store.YES));
        break;
        case DOUBLE:
        doc.add(new StoredField(sf, (Double)v));
        break;
        case FLOAT:
        doc.add(new StoredField(sf, (Float)v));
        break;
        case INT:
        doc.add(new StoredField(sf, (Integer)v));
        break;
        case LONG:
        doc.add(new StoredField(sf, (Long)v));
        break;
        default:
        LOG.error( "Unknown field type: {}", fieldSchema.getType() );
      }
    }
    LOG.info( "DOCUMENT: {}", doc );
    try {
      doc.add(new StringField(mapping.getPrimaryKey(), key.toString(), Store.YES));
      LOG.info( "DOCUMENT: {}", doc );
      if (get(key, null) == null) {
        writer.addDocument(doc);
      }
      else {
        writer.updateDocument(
                new Term(mapping.getPrimaryKey(), key.toString()),
                doc);
      }
      searcherManager.maybeRefresh();
    } catch (IOException e) {
      LOG.error("Error updating document: {}",e);
    }

  }

  @Override
  protected Result<K,T> executePartial(FileSplitPartitionQuery<K,T> arg0)
          throws IOException {
    throw new OperationNotSupportedException("executePartial is not supported for LuceneStore");
  }

  @Override
  protected Result<K,T> executeQuery(Query<K,T> query) throws IOException {
    try {
      return new LuceneResult<>( this, query, searcherManager );
    } catch ( IOException e ) {
      LOG.error(e.getMessage(), e );
    }
    return null;
  }

  @Override
  public List<PartitionQuery<K, T>> getPartitions(Query<K, T> query){
    throw new OperationNotSupportedException("getPartitions is not supported for LuceneStore");
  }

  @Override
  public void flush() {
    try {
      writer.commit();
      searcherManager.maybeRefreshBlocking();
    } catch (IOException e) {
      LOG.error("Error in commit: {}", e);
    }
  }

  @Override
  public void close() {
    try {
      searcherManager.close();
      writer.close();
    } catch (IOException e) {
      LOG.error("Error in close: {}", e);
    }
    super.close();
  }

  public LuceneMapping getMapping() {
    return mapping;
  }

}
